/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Maxime Dor
 *
 * https://max.kamax.io/
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

package io.kamax.mxisd.backend.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.*
import com.google.firebase.internal.NonNull
import com.google.firebase.tasks.OnFailureListener
import com.google.firebase.tasks.OnSuccessListener
import io.kamax.matrix.ThreePidMedium
import io.kamax.matrix._MatrixID
import io.kamax.mxisd.ThreePid
import io.kamax.mxisd.UserIdType
import io.kamax.mxisd.auth.provider.AuthenticatorProvider
import io.kamax.mxisd.auth.provider.BackendAuthResult
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

public class GoogleFirebaseAuthenticator implements AuthenticatorProvider {

    private Logger log = LoggerFactory.getLogger(GoogleFirebaseAuthenticator.class);

    private static final Pattern matrixIdLaxPattern = Pattern.compile("@(.*):(.+)"); // FIXME use matrix-java-sdk

    private boolean isEnabled;
    private String domain;
    private FirebaseApp fbApp;
    private FirebaseAuth fbAuth;

    private void waitOnLatch(BackendAuthResult result, CountDownLatch l, long timeout, TimeUnit unit, String purpose) {
        try {
            l.await(timeout, unit);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for " + purpose);
            result.failure();
        }
    }

    public GoogleFirebaseAuthenticator(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public GoogleFirebaseAuthenticator(String credsPath, String db, String domain) {
        this(true);
        this.domain = domain;
        try {
            fbApp = FirebaseApp.initializeApp(getOpts(credsPath, db), "AuthenticationProvider");
            fbAuth = FirebaseAuth.getInstance(fbApp);

            log.info("Google Firebase Authentication is ready");
        } catch (IOException e) {
            throw new RuntimeException("Error when initializing Firebase", e);
        }
    }

    private FirebaseCredential getCreds(String credsPath) throws IOException {
        if (StringUtils.isNotBlank(credsPath)) {
            return FirebaseCredentials.fromCertificate(new FileInputStream(credsPath));
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

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    private void waitOnLatch(CountDownLatch l) {
        try {
            l.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for Firebase auth check");
        }
    }

    @Override
    public BackendAuthResult authenticate(_MatrixID mxid, String password) {
        if (!isEnabled()) {
            throw new IllegalStateException();
        }

        log.info("Trying to authenticate {}", mxid);

        BackendAuthResult result = BackendAuthResult.failure();

        String localpart = m.group(1);
        CountDownLatch l = new CountDownLatch(1);
        fbAuth.verifyIdToken(password).addOnSuccessListener(new OnSuccessListener<FirebaseToken>() {
            @Override
            void onSuccess(FirebaseToken token) {
                try {
                    if (!StringUtils.equals(localpart, token.getUid())) {
                        log.info("Failture to authenticate {}: Matrix ID localpart '{}' does not match Firebase UID '{}'", id, localpart, token.getUid());
                        result = BackendAuthResult.failure();
                        return;
                    }

                    result = BackendAuthResult.success(mxid.getId(), UserIdType.MatrixID, token.getName());
                    log.info("{} was successfully authenticated", mxid);
                    log.info("Fetching profile for {}", mxid);
                    CountDownLatch userRecordLatch = new CountDownLatch(1);
                    fbAuth.getUser(token.getUid()).addOnSuccessListener(new OnSuccessListener<UserRecord>() {
                        @Override
                        void onSuccess(UserRecord user) {
                            try {
                                if (StringUtils.isNotBlank(user.getEmail())) {
                                    result.withThreePid(new ThreePid(ThreePidMedium.Email.getId(), user.getEmail()));
                                }

                                if (StringUtils.isNotBlank(user.getPhoneNumber())) {
                                    result.withThreePid(new ThreePid(ThreePidMedium.PhoneNumber.getId(), user.getPhoneNumber()));
                                }

                            } finally {
                                userRecordLatch.countDown();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        void onFailure(@NonNull Exception e) {
                            try {
                                log.warn("Unable to fetch Firebase user profile for {}", mxid);
                                result = BackendAuthResult.failure();
                            } finally {
                                userRecordLatch.countDown();
                            }
                        }
                    });

                    waitOnLatch(result, userRecordLatch, 30, TimeUnit.SECONDS, "Firebase user profile");
                } finally {
                    l.countDown()
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            void onFailure(@NonNull Exception e) {
                try {
                    if (e instanceof IllegalArgumentException) {
                        log.info("Failure to authenticate {}: invalid firebase token", mxid);
                    } else {
                        log.info("Failure to authenticate {}: {}", id, e.getMessage(), e);
                        log.info("Exception", e);
                    }

                    result = BackendAuthResult.failure();
                } finally {
                    l.countDown()
                }
            }
        });

        waitOnLatch(result, l, 30, TimeUnit.SECONDS, "Firebase auth check");
        return result;
    }

}
