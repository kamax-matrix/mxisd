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

package io.kamax.mxisd.backend.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.ThreePid;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import io.kamax.mxisd.config.GoogleConfig;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class GoogleProviderBackend implements AuthenticatorProvider, IThreePidProvider {

    private final Logger log = LoggerFactory.getLogger(GoogleProviderBackend.class);
    private final GoogleConfig cfg;
    private final MatrixConfig mxCfg;

    private GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleProviderBackend(GoogleConfig cfg, MatrixConfig mxCfg) {
        this.cfg = cfg;
        this.mxCfg = mxCfg;

        if (isEnabled()) {
            try {
                HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

                verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                        .setAudience(Collections.singletonList(cfg.getClient().getId()))
                        .build();
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException(e);
            }

        }
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
        return 20;
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        if (!StringUtils.equals(cfg.getMedium(), request.getType())) {
            return Optional.empty();
        }

        return Optional.of(new SingleLookupReply(request, new MatrixID(cfg.getPrefix() + request.getThreePid(), mxCfg.getDomain())));
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        return Collections.emptyList();
    }

    @Override
    public BackendAuthResult authenticate(_MatrixID mxid, String password) {
        if (!StringUtils.startsWith(mxid.getLocalPart(), cfg.getPrefix())) {
            return BackendAuthResult.failure();
        }

        BackendAuthResult result = new BackendAuthResult();
        result.withThreePid(new ThreePid(cfg.getMedium(), mxid.getLocalPart().replace(cfg.getPrefix(), "")));
        result.succeed(mxid.getId(), UserIdType.MatrixID.getId(), null);
        return result;

        /*
        try {
            log.info("ID Token: {}", password);
            GoogleIdToken idToken = verifier.verify(password);
            if (idToken != null) {
                BackendAuthResult
                GoogleIdToken.Payload payload = idToken.getPayload();

                // Get user identifier
                String userId = payload.getSubject();

                // Get profile information from payload
                String email = payload.getEmail();
                if (payload.getEmailVerified()) {

                }
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");
                String locale = (String) payload.get("locale");
                String familyName = (String) payload.get("family_name");
                String givenName = (String) payload.get("given_name");

                return BackendAuthResult.success(userId, UserIdType.Localpart, name);
            } else {
                log.info("Not a valid Google token");
                return BackendAuthResult.failure();
            }
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Unable to authenticate via Google due to network error", e);
            return BackendAuthResult.failure();
        }
        */
    }

}
