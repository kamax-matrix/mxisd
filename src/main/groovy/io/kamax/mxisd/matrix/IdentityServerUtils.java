package io.kamax.mxisd.matrix;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

// FIXME placeholder, this must go in matrix-java-sdk for 1.0
public class IdentityServerUtils {

    public static final String THREEPID_TEST_MEDIUM = "email";
    public static final String THREEPID_TEST_ADDRESS = "mxisd-email-forever-unknown@forever-invalid.kamax.io";

    private static Logger log = LoggerFactory.getLogger(IdentityServerUtils.class);
    private static JsonParser parser = new JsonParser();

    public static boolean isUsable(String remote) {
        try {
            // FIXME use Apache HTTP client
            HttpURLConnection rootSrvConn = (HttpURLConnection) new URL(
                    remote + "/_matrix/identity/api/v1/lookup?medium=" + THREEPID_TEST_MEDIUM + "&address=" + THREEPID_TEST_ADDRESS
            ).openConnection();
            // TODO turn this into a configuration property
            rootSrvConn.setConnectTimeout(2000);

            if (rootSrvConn.getResponseCode() != 200) {
                return false;
            }

            JsonElement el = parser.parse(IOUtils.toString(rootSrvConn.getInputStream(), StandardCharsets.UTF_8));
            if (!el.isJsonObject()) {
                log.debug("IS {} did not send back a JSON object for single 3PID lookup");
                return false;
            }

            if (el.getAsJsonObject().has("address")) {
                log.debug("IS {} did not send back a JSON object for single 3PID lookup");
                return false;
            }

            return true;
        } catch (IOException | JsonParseException e) {
            log.info("{} is not a usable Identity Server: {}", remote, e.getMessage());
            return false;
        }
    }

    public static String getSrvRecordName(String domain) {
        return "_matrix-identity._tcp." + domain;
    }

    public static Optional<String> findIsUrlForDomain(String domainOrUrl) {
        try {
            try {
                domainOrUrl = new URL(domainOrUrl).getHost();
            } catch (MalformedURLException e) {
                log.info("{} is not an URL, using as-is", domainOrUrl);
            }

            log.info("Discovery Identity Server for {}", domainOrUrl);
            log.info("Performing SRV lookup");
            String lookupDns = getSrvRecordName(domainOrUrl);
            log.info("Lookup name: {}", lookupDns);

            SRVRecord[] records = (SRVRecord[]) new Lookup(lookupDns, Type.SRV).run();
            if (records != null) {
                Arrays.sort(records, Comparator.comparingInt(SRVRecord::getPriority));

                for (SRVRecord record : records) {
                    log.info("Found SRV record: {}", record.toString());
                    String baseUrl = "https://${record.getTarget().toString(true)}:${record.getPort()}";
                    if (isUsable(baseUrl)) {
                        log.info("Found Identity Server for domain {} at {}", domainOrUrl, baseUrl);
                        return Optional.of(baseUrl);
                    } else {
                        log.info("{} is not a usable Identity Server", baseUrl);
                        return Optional.empty();
                    }
                }
            } else {
                log.info("No SRV record for {}", lookupDns);
            }

            log.info("Performing basic lookup using domain name {}", domainOrUrl);
            String baseUrl = "https://" + domainOrUrl;
            if (isUsable(baseUrl)) {
                log.info("Found Identity Server for domain {} at {}", domainOrUrl, baseUrl);
                return Optional.of(baseUrl);
            } else {
                log.info("{} is not a usable Identity Server", baseUrl);
                return Optional.empty();
            }
        } catch (TextParseException e) {
            log.warn(domainOrUrl + " is not a valid domain name");
            return Optional.empty();
        }
    }

}
