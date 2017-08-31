package io.kamax.mxisd.auth.provider

import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.*
import com.google.firebase.internal.NonNull
import com.google.firebase.tasks.OnCompleteListener
import com.google.firebase.tasks.OnFailureListener
import com.google.firebase.tasks.OnSuccessListener
import com.google.firebase.tasks.Task
import io.kamax.matrix.MatrixID
import io.kamax.matrix.ThreePidMedium
import io.kamax.mxisd.GlobalProvider
import io.kamax.mxisd.auth.UserAuthResult
import io.kamax.mxisd.lookup.SingleLookupRequest
import io.kamax.mxisd.lookup.ThreePidMapping
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.regex.Matcher
import java.util.regex.Pattern

public class GoogleFirebaseAuthenticator implements GlobalProvider {

    private Logger log = LoggerFactory.getLogger(GoogleFirebaseAuthenticator.class);

    private static final Pattern matrixIdLaxPattern = Pattern.compile("@(.*):(.+)");

    private boolean isEnabled;
    private String domain;
    private FirebaseApp fbApp;
    private FirebaseAuth fbAuth;

    public GoogleFirebaseAuthenticator(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public GoogleFirebaseAuthenticator(String credsPath, String db, String domain) {
        this(true);
        this.domain = domain;
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
        UserRecord r;
        CountDownLatch l = new CountDownLatch(1);

        OnSuccessListener<UserRecord> success = new OnSuccessListener<UserRecord>() {
            @Override
            void onSuccess(UserRecord result) {
                r = result;
            }
        };

        OnFailureListener failure = new OnFailureListener() {
            @Override
            void onFailure(@NonNull Exception e) {
                r = null;
            }
        };

        OnCompleteListener<UserRecord> complete = new OnCompleteListener<UserRecord>() {
            @Override
            void onComplete(@NonNull Task<UserRecord> task) {
                l.countDown();
            }
        };

        if (ThreePidMedium.Email.is(medium)) {
            fbAuth.getUserByEmail(address)
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure)
                    .addOnCompleteListener(complete);
            waitOnLatch(l);
        } else if (ThreePidMedium.PhoneNumber.is(medium)) {
            fbAuth.getUserByPhoneNumber(address)
                    .addOnSuccessListener(success)
                    .addOnFailureListener(failure)
                    .addOnCompleteListener(complete);
            waitOnLatch(l);
        } else {
            log.info("{} is not a supported 3PID medium", medium);
            r = null;
        }

        return Optional.ofNullable(r);
    }

    @Override
    public Optional<?> find(SingleLookupRequest request) {
        Optional<UserRecord> urOpt = findInternal(request.getType(), request.getThreePid())
        if (urOpt.isPresent()) {
            return [
                    address   : request.getThreePid(),
                    medium    : request.getType(),
                    mxid      : new MatrixID(urOpt.get().getUid(), domain).getId(),
                    not_before: 0,
                    not_after : 9223372036854775807,
                    ts        : 0
            ]
        } else {
            return Optional.empty();
        }
    }

    @Override
    public List<ThreePidMapping> populate(List<ThreePidMapping> mappings) {
        List<ThreePidMapping> results = new ArrayList<>();
        mappings.parallelStream().forEach(new Consumer<ThreePidMapping>() {
            @Override
            void accept(ThreePidMapping o) {
                Optional<UserRecord> urOpt = findInternal(o.getMedium(), o.getValue());
                if (urOpt.isPresent()) {
                    ThreePidMapping result = new ThreePidMapping();
                    result.setMedium(o.getMedium())
                    result.setValue(o.getValue())
                    result.setMxid(new MatrixID(urOpt.get().getUid(), domain).getId())
                    results.add(result)
                }
            }
        });
        return results;
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
        fbAuth.verifyIdToken(password).addOnSuccessListener(new OnSuccessListener<FirebaseToken>() {
            @Override
            void onSuccess(FirebaseToken token) {
                if (!StringUtils.equals(localpart, token.getUid())) {
                    log.info("Failture to authenticate {}: Matrix ID localpart '{}' does not match Firebase UID '{}'", id, localpart, token.getUid());
                    result.failure();
                }

                log.info("{} was successfully authenticated", id);
                result.success(id, token.getName());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            void onFailure(@NonNull Exception e) {
                if (e instanceof IllegalArgumentException) {
                    log.info("Failure to authenticate {}: invalid firebase token", id);
                } else {
                    log.info("Failure to authenticate {}: {}", id, e.getMessage(), e);
                    log.info("Exception", e);
                }

                result.failure();
            }
        }).addOnCompleteListener(new OnCompleteListener<FirebaseToken>() {
            @Override
            void onComplete(@NonNull Task<FirebaseToken> task) {
                l.countDown()
            }
        });

        try {
            l.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for Firebase auth check");
            result.failure();
        }

        return result;
    }

}
