/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://www.kamax.io/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.mxisd.backend.exec;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.backend.rest.LookupBulkResponseJson;
import io.kamax.mxisd.config.ExecConfig;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ExecIdentityStore extends ExecStore implements IThreePidProvider {

    private final Logger log = LoggerFactory.getLogger(ExecIdentityStore.class);

    private final ExecConfig.Identity cfg;
    private final MatrixConfig mxCfg;

    @Autowired
    public ExecIdentityStore(ExecConfig cfg, MatrixConfig mxCfg) {
        this.cfg = cfg.getIdentity();
        this.mxCfg = mxCfg;
    }

    @Override
    public boolean isEnabled() {
        return cfg.isEnabled();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public int getPriority() {
        return cfg.getPriority();
    }

    private ExecConfig.Process getSingleCfg() {
        return cfg.getLookup().getSingle();
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        Processor<Optional<SingleLookupReply>> processor = new Processor<>();
        processor.withConfig(cfg.getLookup().getSingle());

        processor.addTokenMapper(getSingleCfg().getToken().getMedium(), request::getType);
        processor.addTokenMapper(getSingleCfg().getToken().getAddress(), request::getThreePid);

        processor.addInputTemplate(JsonType, tokens -> {
            JsonObject json = new JsonObject();
            json.addProperty("medium", tokens.getMedium());
            json.addProperty("address", tokens.getAddress());
            return GsonUtil.get().toJson(json);
        });
        processor.addInputTemplate(MultilinesType, tokens -> tokens.getMedium()
                + System.lineSeparator()
                + tokens.getAddress()
        );

        processor.addSuccessMapper(JsonType, output -> {
            if (StringUtils.isBlank(output)) {
                return Optional.empty();
            }

            return GsonUtil.findObj(GsonUtil.parseObj(output), "lookup").map(lookup -> {
                String type = GsonUtil.getStringOrThrow(lookup, "type");
                String value = GsonUtil.getStringOrThrow(lookup, "value");
                if (UserIdType.Localpart.is(type)) {
                    return MatrixID.asAcceptable(value, mxCfg.getDomain());
                }

                if (UserIdType.MatrixID.is(type)) {
                    return MatrixID.asAcceptable(value);
                }

                throw new InternalServerError("Invalid user type: " + type);
            }).map(mxId -> new SingleLookupReply(request, mxId));
        });

        processor.addSuccessMapper(MultilinesType, output -> {
            String[] lines = output.split("\\R");
            if (lines.length > 2) {
                throw new InternalServerError("Exec auth command returned more than 2 lines (" + lines.length + ")");
            }

            if (lines.length == 1 && StringUtils.isBlank(lines[0])) {
                return Optional.empty();
            }

            String type = StringUtils.trimToEmpty(lines.length == 1 ? "uid" : lines[0]);
            String value = StringUtils.trimToEmpty(lines.length == 2 ? lines[1] : lines[0]);

            if (UserIdType.Localpart.is(type)) {
                return Optional.of(new SingleLookupReply(request, MatrixID.asAcceptable(value, mxCfg.getDomain())));
            }

            if (UserIdType.MatrixID.is(type)) {
                return Optional.of(new SingleLookupReply(request, MatrixID.asAcceptable(value)));
            }

            throw new InternalServerError("Invalid user type: " + type);
        });

        processor.withFailureDefault(o -> Optional.empty());

        return processor.execute();
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        Processor<List<ThreePidMapping>> processor = new Processor<>();
        processor.withConfig(cfg.getLookup().getBulk());

        processor.addInput(JsonType, () -> {
            JsonArray tpids = GsonUtil.asArray(mappings.stream()
                    .map(mapping -> GsonUtil.get().toJsonTree(new ThreePid(mapping.getMedium(), mapping.getValue())))
                    .collect(Collectors.toList()));
            return GsonUtil.get().toJson(GsonUtil.makeObj("lookup", tpids));
        });
        processor.addInput(MultilinesType, () -> {
            StringBuilder input = new StringBuilder();
            for (ThreePidMapping mapping : mappings) {
                input.append(mapping.getMedium()).append("\t").append(mapping.getValue()).append(System.lineSeparator());
            }
            return input.toString();
        });

        processor.addSuccessMapper(JsonType, output -> {
            if (StringUtils.isBlank(output)) {
                return Collections.emptyList();
            }

            LookupBulkResponseJson response = GsonUtil.get().fromJson(output, LookupBulkResponseJson.class);
            return response.getLookup().stream().map(item -> {
                ThreePidMapping mapping = new ThreePidMapping();
                mapping.setMedium(item.getMedium());
                mapping.setValue(item.getAddress());

                if (UserIdType.Localpart.is(item.getId().getType())) {
                    mapping.setValue(MatrixID.asAcceptable(item.getId().getValue(), mxCfg.getDomain()).getId());
                    return mapping;
                }

                if (UserIdType.MatrixID.is(item.getId().getType())) {
                    mapping.setValue(MatrixID.asAcceptable(item.getId().getValue()).getId());
                    return mapping;
                }

                throw new InternalServerError("Invalid user type: " + item.getId().getType());
            }).collect(Collectors.toList());
        });

        processor.withFailureDefault(output -> Collections.emptyList());

        return processor.execute();
    }

}
