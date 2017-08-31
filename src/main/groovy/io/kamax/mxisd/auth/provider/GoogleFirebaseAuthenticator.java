package io.kamax.mxisd.auth.provider;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseCredential;
import com.google.firebase.auth.FirebaseCredentials;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.auth.UserAuthResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;

public class GoogleFirebaseAuthenticator implements AuthenticatorProvider {

    private Logger log = LoggerFactory.getLogger(GoogleFirebaseAuthenticator.class);

    private boolean isEnabled;
    private FirebaseApp fbApp;
    private FirebaseAuth fbAuth;

    public GoogleFirebaseAuthenticator(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public GoogleFirebaseAuthenticator(String credsPath, String db) {
        this(true);
        try {
            fbApp = FirebaseApp.initializeApp(getOpts(credsPath, db));
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

    @Override
    public UserAuthResult authenticate(String id, String password) {
        if (!isEnabled()) {
            throw new IllegalStateException();
        }

        final UserAuthResult result = new UserAuthResult();

        try {
            log.info("Trying to authenticate {}", id);
            _MatrixID mxId = new MatrixID(id);
            fbAuth.verifyIdToken(password).addOnSuccessListener(token -> {
                if (!StringUtils.equals(mxId.getLocalPart(), token.getUid())) {
                    log.info("Failture to authenticate {}: Matrix ID localpart '{}' does not match Firebase UID '{}'", id, mxId.getLocalPart(), token.getUid());
                    result.failure();
                }

                log.info("{} was successfully authenticated", id);
                result.success(id, token.getName());
            }).addOnFailureListener(e -> {
                if (e instanceof IllegalArgumentException) {
                    log.info("Failure to authenticate {}: invalid firebase token", id);
                } else {
                    log.info("Failure to authenticate {}", id, e.getMessage());
                    log.debug("Exception", e);
                }

                result.failure();
            });
        } catch (IllegalArgumentException e) {
            log.warn("Could not validate {} as a Matrix ID: {}", id, e.getMessage());
            result.failure();
        }

        return result;
    }

}
