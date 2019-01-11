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

package io.kamax.mxisd.backend.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseCredential;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.database.FirebaseDatabase;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class GoogleFirebaseBackend {

    private transient final Logger log = LoggerFactory.getLogger(GoogleFirebaseBackend.class);

    private boolean isEnabled;
    private FirebaseAuth fbAuth;
    protected FirebaseDatabase fbDb;

    GoogleFirebaseBackend(boolean isEnabled, String name, String credsPath, String db) {
        this.isEnabled = isEnabled;
        if (!isEnabled) {
            return;
        }

        try {
            FirebaseApp fbApp = FirebaseApp.initializeApp(getOpts(credsPath, db), name);
            fbAuth = FirebaseAuth.getInstance(fbApp);
            FirebaseDatabase.getInstance(fbApp);

            log.info("Google Firebase Authentication is ready");
        } catch (IOException e) {
            throw new RuntimeException("Error when initializing Firebase", e);
        }
    }

    private FirebaseCredential getCreds(String credsPath) throws IOException {
        if (StringUtils.isNotBlank(credsPath)) {
            try (FileInputStream is = new FileInputStream(credsPath)) {
                return FirebaseCredentials.fromCertificate(is);
            }
        } else {
            return FirebaseCredentials.applicationDefault();
        }
    }

    private FirebaseOptions getOpts(String credsPath, String db) throws IOException {
        if (StringUtils.isBlank(db)) {
            throw new IllegalArgumentException("Firebase database is not configured");
        }

        return new FirebaseOptions.Builder()
                .setCredential(getCreds(credsPath))
                .setDatabaseUrl(db)
                .build();
    }

    FirebaseAuth getFirebase() {
        return fbAuth;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

}
