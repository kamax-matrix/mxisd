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

package io.kamax.mxisd.backend.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseCredential;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.tasks.OnFailureListener;
import com.google.firebase.tasks.OnSuccessListener;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.provider.IThreePidProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GoogleFirebaseProvider implements IThreePidProvider {

    private Logger log = LoggerFactory.getLogger(GoogleFirebaseProvider.class);

    private boolean isEnabled;
    private String domain;
    private FirebaseAuth fbAuth;

    public GoogleFirebaseProvider(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public GoogleFirebaseProvider(String credsPath, String db, String domain) {
        this(true);
        this.domain = domain;

        try {
            FirebaseApp fbApp = FirebaseApp.initializeApp(getOpts(credsPath, db), "ThreePidProvider");
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

    private String getMxid(UserRecord record) {
        return new MatrixID(record.getUid(), domain).getId();
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public int getPriority() {
        return 25;
    }

    private void waitOnLatch(CountDownLatch l) {
        try {
            l.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for Firebase auth check");
        }
    }

    private Optional<UserRecord> findInternal(String medium, String address) {
        final UserRecord[] r = new UserRecord[1];
        CountDownLatch l = new CountDownLatch(1);

        OnSuccessListener<UserRecord> success = result -> {
            log.info("Found 3PID match for {}:{} - UID is {}", medium, address, result.getUid());
            r[0] = result;
            l.countDown();
        };

        OnFailureListener failure = e -> {
            log.info("No 3PID match for {}:{} - {}", medium, address, e.getMessage());
            r[0] = null;
            l.countDown();
        };

        if (ThreePidMedium.Email.is(medium)) {
            log.info("Performing E-mail 3PID lookup for {}", address);
            fbAuth.getUserByEmail(address)
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure);
            waitOnLatch(l);
        } else if (ThreePidMedium.PhoneNumber.is(medium)) {
            log.info("Performing msisdn 3PID lookup for {}", address);
            fbAuth.getUserByPhoneNumber(address)
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure);
            waitOnLatch(l);
        } else {
            log.info("{} is not a supported 3PID medium", medium);
            r[0] = null;
        }

        return Optional.ofNullable(r[0]);
    }

    @Override
    public Optional<SingleLookupReply> find(SingleLookupRequest request) {
        Optional<UserRecord> urOpt = findInternal(request.getType(), request.getThreePid());
        return urOpt.map(userRecord -> new SingleLookupReply(request, getMxid(userRecord)));

    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        List<ThreePidMapping> results = new ArrayList<>();
        mappings.parallelStream().forEach(o -> {
            Optional<UserRecord> urOpt = findInternal(o.getMedium(), o.getValue());
            if (urOpt.isPresent()) {
                ThreePidMapping result = new ThreePidMapping();
                result.setMedium(o.getMedium());
                result.setValue(o.getValue());
                result.setMxid(getMxid(urOpt.get()));
                results.add(result);
            }
        });
        return results;
    }

}
