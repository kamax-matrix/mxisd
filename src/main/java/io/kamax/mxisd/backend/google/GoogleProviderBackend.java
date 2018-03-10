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

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import io.kamax.mxisd.config.GoogleConfig;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleProviderBackend implements AuthenticatorProvider {

    private final Logger log = LoggerFactory.getLogger(GoogleProviderBackend.class);
    private final GoogleConfig cfg;
    private final MatrixConfig mxCfg;
    private final LookupStrategy lookup;

    private GoogleIdTokenVerifier verifier;

    @Autowired
    public GoogleProviderBackend(GoogleConfig cfg, MatrixConfig mxCfg, LookupStrategy lookup) {
        this.cfg = cfg;
        this.mxCfg = mxCfg;
        this.lookup = lookup;

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
    public BackendAuthResult authenticate(_MatrixID mxid, String password) {
        try {
            GoogleIdToken idToken = verifier.verify(password);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                if (!payload.getEmailVerified()) { // We only want users who validated their email
                    return BackendAuthResult.failure();
                }

                // Get user identifier
                String userId = payload.getSubject();

                // We validate that the user who authenticated has his Google account associated already
                return lookup.find("io.kamax.google.id", userId, false).map(r -> {
                    if (!r.getMxid().equals(mxid)) {
                        return BackendAuthResult.failure();
                    }

                    // Get profile information from payload
                    String name = (String) payload.get("name");

                    return BackendAuthResult.success(mxid.getId(), UserIdType.MatrixID, name);
                }).orElse(BackendAuthResult.failure());
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
    }

}
