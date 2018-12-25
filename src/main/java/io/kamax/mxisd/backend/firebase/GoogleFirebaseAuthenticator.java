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

import com.google.firebase.auth.UserInfo;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.kamax.matrix.ThreePid;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.UserIdType;
import io.kamax.mxisd.auth.provider.AuthenticatorProvider;
import io.kamax.mxisd.auth.provider.BackendAuthResult;
import io.kamax.mxisd.config.FirebaseConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GoogleFirebaseAuthenticator extends GoogleFirebaseBackend implements AuthenticatorProvider {

    private transient final Logger log = LoggerFactory.getLogger(GoogleFirebaseAuthenticator.class);

    private PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

    public GoogleFirebaseAuthenticator(FirebaseConfig cfg) {
        this(cfg.isEnabled(), cfg.getCredentials(), cfg.getDatabase());
    }

    public GoogleFirebaseAuthenticator(boolean isEnabled, String credsPath, String db) {
        super(isEnabled, "AuthenticationProvider", credsPath, db);
    }

    private void waitOnLatch(BackendAuthResult result, CountDownLatch l, String purpose) {
        try {
            l.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for " + purpose);
            result.fail();
        }
    }

    private void toEmail(BackendAuthResult result, String email) {
        if (StringUtils.isBlank(email)) {
            return;
        }

        result.withThreePid(new ThreePid(ThreePidMedium.Email.getId(), email));
    }

    private void toMsisdn(BackendAuthResult result, String phoneNumber) {
        if (StringUtils.isBlank(phoneNumber)) {
            return;
        }

        try {
            String number = phoneUtil.format(
                    phoneUtil.parse(
                            phoneNumber,
                            null // No default region
                    ),
                    PhoneNumberUtil.PhoneNumberFormat.E164
            ).substring(1); // We want without the leading +
            result.withThreePid(new ThreePid(ThreePidMedium.PhoneNumber.getId(), number));
        } catch (NumberParseException e) {
            log.warn("Invalid phone number: {}", phoneNumber);
        }
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

        final BackendAuthResult result = BackendAuthResult.failure();

        String localpart = mxid.getLocalPart();
        CountDownLatch l = new CountDownLatch(1);
        getFirebase().verifyIdToken(password).addOnSuccessListener(token -> {
            try {
                if (!StringUtils.equals(localpart, token.getUid())) {
                    log.info("Failure to authenticate {}: Matrix ID localpart '{}' does not match Firebase UID '{}'", mxid, localpart, token.getUid());
                    result.fail();
                    return;
                }

                result.succeed(mxid.getId(), UserIdType.MatrixID.getId(), token.getName());
                log.info("{} was successfully authenticated", mxid);
                log.info("Fetching profile for {}", mxid);
                CountDownLatch userRecordLatch = new CountDownLatch(1);
                getFirebase().getUser(token.getUid()).addOnSuccessListener(user -> {
                    try {
                        toEmail(result, user.getEmail());
                        toMsisdn(result, user.getPhoneNumber());

                        for (UserInfo info : user.getProviderData()) {
                            toEmail(result, info.getEmail());
                            toMsisdn(result, info.getPhoneNumber());
                        }

                        log.info("Got {} 3PIDs in profile", result.getProfile().getThreePids().size());
                    } finally {
                        userRecordLatch.countDown();
                    }
                }).addOnFailureListener(e -> {
                    try {
                        log.warn("Unable to fetch Firebase user profile for {}", mxid);
                        result.fail();
                    } finally {
                        userRecordLatch.countDown();
                    }
                });

                waitOnLatch(result, userRecordLatch, "Firebase user profile");
            } finally {
                l.countDown();
            }
        }).addOnFailureListener(e -> {
            try {
                if (e instanceof IllegalArgumentException) {
                    log.info("Failure to authenticate {}: invalid firebase token", mxid);
                } else {
                    log.info("Failure to authenticate {}: {}", mxid, e.getMessage(), e);
                    log.info("Exception", e);
                }

                result.fail();
            } finally {
                l.countDown();
            }
        });

        waitOnLatch(result, l, "Firebase auth check");
        return result;
    }

}
