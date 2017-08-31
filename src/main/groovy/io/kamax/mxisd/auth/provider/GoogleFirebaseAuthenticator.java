package io.kamax.mxisd.auth.provider;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseCredential;
import com.google.firebase.auth.FirebaseCredentials;
import io.kamax.mxisd.auth.UserAuthResult;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleFirebaseAuthenticator implements AuthenticatorProvider {

    private Logger log = LoggerFactory.getLogger(GoogleFirebaseAuthenticator.class);

    private static final Pattern matrixIdLaxPattern = Pattern.compile("@(.*):(.+)");

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

        log.info("Trying to authenticate {}", id);
        Matcher m = matrixIdLaxPattern.matcher(id);
        if (!m.matches()) {
            log.warn("Could not validate {} as a Matrix ID", id);
            result.failure();
        }

        String localpart = m.group(1);

        CountDownLatch l = new CountDownLatch(1);
        fbAuth.verifyIdToken(password).addOnSuccessListener(token -> {
            if (!StringUtils.equals(localpart, token.getUid())) {
                log.info("Failture to authenticate {}: Matrix ID localpart '{}' does not match Firebase UID '{}'", id, localpart, token.getUid());
                result.failure();
            }

            log.info("{} was successfully authenticated", id);
            result.success(id, token.getName());
        }).addOnFailureListener(e -> {
            if (e instanceof IllegalArgumentException) {
                log.info("Failure to authenticate {}: invalid firebase token", id);
            } else {
                log.info("Failure to authenticate {}: {}", id, e.getMessage(), e);
                log.info("Exception", e);
            }

            result.failure();
        }).addOnCompleteListener(t -> l.countDown());

        try {
            l.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for Firebase auth check");
            result.failure();
        }

        return result;
    }

}
