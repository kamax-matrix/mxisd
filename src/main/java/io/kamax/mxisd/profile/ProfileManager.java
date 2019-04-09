/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sàrl
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

package io.kamax.mxisd.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix._ThreePid;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.proxy.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProfileManager {

    private transient final Logger log = LoggerFactory.getLogger(ProfileManager.class);

    private List<ProfileProvider> providers;
    private ClientDnsOverwrite dns;
    private CloseableHttpClient client;

    public ProfileManager(List<? extends ProfileProvider> providers, ClientDnsOverwrite dns, CloseableHttpClient client) {
        this.dns = dns;
        this.client = client;
        this.providers = new ArrayList<>(providers);

        log.info("Profile Providers:");
        providers.forEach(p -> log.info("  - {}", p.getClass().getSimpleName()));
    }

    public <T> List<T> getList(Function<ProfileProvider, List<T>> function) {
        return providers.stream()
                .map(function)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public <T> Optional<T> getOpt(Function<ProfileProvider, Optional<T>> function) {
        return providers.stream()
                .map(function)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public Optional<String> getDisplayName(_MatrixID user) {
        return getOpt(p -> p.getDisplayName(user));
    }

    public List<_ThreePid> getThreepids(_MatrixID user) {
        return getList(p -> p.getThreepids(user));
    }

    public List<String> getRoles(_MatrixID user) {
        return getList(p -> p.getRoles(user));
    }

    public Response enhance(_MatrixID userId, HttpRequestBase request) {
        try {
            request.setURI(dns.transform(request.getURI()).build());

            Response res = new Response();
            try (CloseableHttpResponse hsResponse = client.execute(request)) {
                res.setStatus(hsResponse.getStatusLine().getStatusCode());
                JsonElement el = GsonUtil.parse(EntityUtils.toString(hsResponse.getEntity()));
                if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    List<_ThreePid> list = getThreepids(userId);
                    obj.add("threepids", GsonUtil.get().toJsonTree(list));
                }

                res.setBody(GsonUtil.get().toJson(el));
                return res;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (URISyntaxException e) {
            log.error("Unable to build target URL for profile proxy enhancement", e);
            throw new InternalServerError(e);
        }
    }

    public boolean hasAnyRole(_MatrixID user, List<String> requiredRoles) {
        return !requiredRoles.isEmpty() || Collections.disjoint(getRoles(user), requiredRoles);
    }

}
