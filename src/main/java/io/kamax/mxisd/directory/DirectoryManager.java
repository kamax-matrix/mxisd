/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

package io.kamax.mxisd.directory;

import com.google.gson.JsonSyntaxException;
import io.kamax.matrix.MatrixErrorInfo;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.config.DirectoryConfig;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.exception.HttpMatrixException;
import io.kamax.mxisd.exception.InternalServerError;
import io.kamax.mxisd.http.io.UserDirectorySearchRequest;
import io.kamax.mxisd.http.io.UserDirectorySearchResult;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class DirectoryManager {

    private transient final Logger log = LoggerFactory.getLogger(DirectoryManager.class);

    private DirectoryConfig cfg;
    private ClientDnsOverwrite dns;
    private CloseableHttpClient client;
    private List<DirectoryProvider> providers;

    public DirectoryManager(DirectoryConfig cfg, ClientDnsOverwrite dns, CloseableHttpClient client, List<? extends DirectoryProvider> providers) {
        this.cfg = cfg;
        this.dns = dns;
        this.client = client;
        this.providers = new ArrayList<>(providers);

        log.info("Directory providers:");
        this.providers.forEach(p -> log.info("  - {}", p.getClass().getName()));
    }

    public UserDirectorySearchResult search(URI target, String accessToken, String query) {
        if (StringUtils.startsWith(query, "@")) {
            query = query.substring(1);
        }

        log.info("Performing search for '{}'", query);
        log.info("Original request URL: {}", target);
        UserDirectorySearchResult result = new UserDirectorySearchResult();

        if (cfg.getExclude().getHomeserver()) {
            log.info("Skipping HS directory data, disabled in config");
        } else {
            URIBuilder builder = dns.transform(target);
            log.info("Querying HS at {}", builder);
            builder.setParameter("access_token", accessToken);
            HttpPost req = RestClientUtils.post(
                    builder.toString(),
                    new UserDirectorySearchRequest(query));
            try (CloseableHttpResponse res = client.execute(req)) {
                int status = res.getStatusLine().getStatusCode();
                Charset charset = ContentType.getOrDefault(res.getEntity()).getCharset();
                String body = IOUtils.toString(res.getEntity().getContent(), charset);

                if (status != 200) {
                    MatrixErrorInfo info = GsonUtil.get().fromJson(body, MatrixErrorInfo.class);
                    if (StringUtils.equals("M_UNRECOGNIZED", info.getErrcode())) { // FIXME no hardcoding, use Enum
                        log.warn("Homeserver does not support Directory feature, skipping");
                    } else {
                        log.error("Homeserver returned an error while performing directory search");
                        throw new HttpMatrixException(status, info.getErrcode(), info.getError());
                    }
                }

                UserDirectorySearchResult resultHs = GsonUtil.get().fromJson(body, UserDirectorySearchResult.class);
                log.info("Found {} match(es) in HS for '{}'", resultHs.getResults().size(), query);
                result.getResults().addAll(resultHs.getResults());
                if (resultHs.isLimited()) {
                    result.setLimited(true);
                }
            } catch (JsonSyntaxException e) {
                throw new InternalServerError("Invalid JSON reply from the HS: " + e.getMessage());
            } catch (IOException e) {
                throw new InternalServerError("Unable to query the HS: I/O error: " + e.getMessage());
            }
        }

        for (DirectoryProvider provider : providers) {
            log.info("Using Directory provider {}", provider.getClass().getSimpleName());
            UserDirectorySearchResult resultProvider = provider.searchByDisplayName(query);
            log.info("Display name: found {} match(es) for '{}'", resultProvider.getResults().size(), query);
            result.getResults().addAll(resultProvider.getResults());
            if (resultProvider.isLimited()) {
                result.setLimited(true);
            }

            if (cfg.getExclude().getThreepid()) {
                log.info("Skipping 3PID data, disabled in config");
            } else {
                resultProvider = provider.searchBy3pid(query);
                log.info("Threepid: found {} match(es) for '{}'", resultProvider.getResults().size(), query);
                result.getResults().addAll(resultProvider.getResults());
                if (resultProvider.isLimited()) {
                    result.setLimited(true);
                }
            }
        }

        log.info("Total matches: {} - limited? {}", result.getResults().size(), result.isLimited());
        return result;
    }

}
