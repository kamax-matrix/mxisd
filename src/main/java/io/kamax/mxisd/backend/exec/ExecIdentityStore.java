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
import com.google.gson.JsonParseException;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.UserID;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.backend.rest.LookupBulkResponseJson;
import io.kamax.mxisd.backend.rest.LookupSingleResponseJson;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ExecIdentityStore extends ExecStore implements IThreePidProvider {

    private final Logger log = LoggerFactory.getLogger(ExecIdentityStore.class);

    private final ExecConfig.Identity cfg;
    private final MatrixConfig mxCfg;

    @Autowired
    public ExecIdentityStore(ExecConfig cfg, MatrixConfig mxCfg) {
        this(cfg.getIdentity(), mxCfg);
    }

    public ExecIdentityStore(ExecConfig.Identity cfg, MatrixConfig mxCfg) {
        this.cfg = cfg;
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

    private _MatrixID getUserId(UserID id) {
        if (Objects.isNull(id)) {
            throw new JsonParseException("User id key is not present");
        }

        if (UserIdType.Localpart.is(id.getType())) {
            return MatrixID.asAcceptable(id.getValue(), mxCfg.getDomain());
        }

        if (UserIdType.MatrixID.is(id.getType())) {
            return MatrixID.asAcceptable(id.getValue());
        }

        throw new InternalServerError("Unknown user type: " + id.getType());
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        Processor<Optional<SingleLookupReply>> p = new Processor<>();
        p.withConfig(cfg.getLookup().getSingle());

        p.addTokenMapper(getSingleCfg().getToken().getMedium(), request::getType);
        p.addTokenMapper(getSingleCfg().getToken().getAddress(), request::getThreePid);

        p.addJsonInputTemplate(tokens -> new ThreePid(tokens.getMedium(), tokens.getAddress()));
        p.addInputTemplate(PlainType, tokens -> tokens.getMedium()
                + System.lineSeparator()
                + tokens.getAddress()
        );

        p.addSuccessMapper(JsonType, output -> {
            if (StringUtils.isBlank(output)) {
                return Optional.empty();
            }

            return GsonUtil.findObj(GsonUtil.parseObj(output), "lookup")
                    .filter(obj -> !obj.entrySet().isEmpty())
                    .map(json -> GsonUtil.get().fromJson(json, LookupSingleResponseJson.class))
                    .map(lookup -> getUserId(lookup.getId()))
                    .map(mxId -> new SingleLookupReply(request, mxId));
        });

        p.addSuccessMapper(PlainType, output -> {
            String[] lines = output.split("\\R");
            if (lines.length > 2) {
                throw new InternalServerError("Exec auth command returned more than 2 lines (" + lines.length + ")");
            }

            if (lines.length == 1 && StringUtils.isBlank(lines[0])) {
                return Optional.empty();
            }

            String type = StringUtils.trimToEmpty(lines.length == 1 ? UserIdType.Localpart.getId() : lines[0]);
            String value = StringUtils.trimToEmpty(lines.length == 2 ? lines[1] : lines[0]);

            if (UserIdType.Localpart.is(type)) {
                return Optional.of(new SingleLookupReply(request, MatrixID.asAcceptable(value, mxCfg.getDomain())));
            }

            if (UserIdType.MatrixID.is(type)) {
                return Optional.of(new SingleLookupReply(request, MatrixID.asAcceptable(value)));
            }

            throw new InternalServerError("Invalid user type: " + type);
        });

        p.withFailureDefault(o -> Optional.empty());

        return p.execute();
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        Processor<List<ThreePidMapping>> p = new Processor<>();
        p.withConfig(cfg.getLookup().getBulk());

        p.addInput(JsonType, () -> {
            JsonArray tpids = GsonUtil.asArray(mappings.stream()
                    .map(mapping -> GsonUtil.get().toJsonTree(new ThreePid(mapping.getMedium(), mapping.getValue())))
                    .collect(Collectors.toList()));
            return GsonUtil.get().toJson(GsonUtil.makeObj("lookup", tpids));
        });
        p.addInput(PlainType, () -> {
            StringBuilder input = new StringBuilder();
            for (ThreePidMapping mapping : mappings) {
                input.append(mapping.getMedium()).append("\t").append(mapping.getValue()).append(System.lineSeparator());
            }
            return input.toString();
        });

        p.addSuccessMapper(JsonType, output -> {
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

        p.withFailureDefault(output -> Collections.emptyList());

        return p.execute();
    }

}
