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

package io.kamax.mxisd.backend.rest;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.matrix.json.InvalidJsonException;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.profile.JsonProfileRequest;
import io.kamax.mxisd.profile.JsonProfileResult;
import io.kamax.mxisd.profile.ProfileProvider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class RestProfileProvider extends RestProvider implements ProfileProvider {

    private static final Logger log = LoggerFactory.getLogger(RestProfileProvider.class);

    public RestProfileProvider(RestBackendConfig cfg) {
        super(cfg);
    }

    private <T> Optional<T> doRequest(
            _MatrixID userId,
            Function<RestBackendConfig.ProfileEndpoints, Optional<String>> endpoint,
            Function<JsonProfileResult, Optional<T>> value
    ) {
        Optional<String> url = endpoint.apply(cfg.getEndpoints().getProfile());
        if (!url.isPresent()) {
            return Optional.empty();
        }

        try {
            URIBuilder builder = new URIBuilder(url.get());
            HttpPost req = new HttpPost(builder.build());
            req.setEntity(new StringEntity(GsonUtil.get().toJson(new JsonProfileRequest(userId)), ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse res = client.execute(req)) {
                int sc = res.getStatusLine().getStatusCode();
                if (sc == 404) {
                    log.info("Got 404 - No result found");
                    return Optional.empty();
                }

                if (sc != 200) {
                    throw new InternalServerError("Unexpected backed status code: " + sc);
                }

                String body = IOUtils.toString(res.getEntity().getContent(), StandardCharsets.UTF_8);
                if (StringUtils.isBlank(body)) {
                    log.warn("Backend response body is empty/blank, expected JSON object with profile key");
                    return Optional.empty();
                }

                Optional<JsonObject> pJson = GsonUtil.findObj(GsonUtil.parseObj(body), "profile");
                if (!pJson.isPresent()) {
                    log.warn("Backend response body is invalid, expected JSON object with profile key");
                    return Optional.empty();
                }

                JsonProfileResult profile = gson.fromJson(pJson.get(), JsonProfileResult.class);
                return value.apply(profile);
            }
        } catch (JsonSyntaxException | InvalidJsonException e) {
            log.error("Unable to parse backend response as JSON", e);
            throw new InternalServerError(e);
        } catch (URISyntaxException e) {
            log.error("Unable to build a valid request URL", e);
            throw new InternalServerError(e);
        } catch (IOException e) {
            log.error("I/O Error during backend request", e);
            throw new InternalServerError();
        }
    }

    @Override
    public Optional<String> getDisplayName(_MatrixID userId) {
        return doRequest(userId, p -> {
            if (StringUtils.isBlank(p.getDisplayName())) {
                return Optional.empty();
            }
            return Optional.ofNullable(p.getDisplayName());
        }, profile -> Optional.ofNullable(profile.getDisplayName()));
    }

    @Override
    public List<_ThreePid> getThreepids(_MatrixID userId) {
        return doRequest(userId, p -> {
            if (StringUtils.isBlank(p.getThreepids())) {
                return Optional.empty();
            }
            return Optional.ofNullable(p.getThreepids());
        }, profile -> {
            List<_ThreePid> t = new ArrayList<>();
            if (Objects.nonNull(profile.getThreepids())) {
                t.addAll(profile.getThreepids());
            }
            return Optional.of(t);
        }).orElseGet(Collections::emptyList);
    }

    @Override
    public List<String> getRoles(_MatrixID userId) {
        return doRequest(userId, p -> {
            if (StringUtils.isBlank(p.getRoles())) {
                return Optional.empty();
            }
            return Optional.ofNullable(p.getRoles());
        }, profile -> {
            List<String> t = new ArrayList<>();
            if (Objects.nonNull(profile.getRoles())) {
                t.addAll(profile.getRoles());
            }
            return Optional.of(t);
        }).orElseGet(Collections::emptyList);
    }

}
