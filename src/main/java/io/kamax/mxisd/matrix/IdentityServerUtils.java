package io.kamax.mxisd.matrix;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// FIXME placeholder, this must go in matrix-java-sdk for 1.0
public class IdentityServerUtils {

    private static Logger log = LoggerFactory.getLogger(IdentityServerUtils.class);
    private static JsonParser parser = new JsonParser();

    public static boolean isUsable(String remote) {
        if (StringUtils.isBlank(remote)) {
            return false;
        }

        try {
            // FIXME use Apache HTTP client
            HttpURLConnection rootSrvConn = (HttpURLConnection) new URL(remote + "/_matrix/identity/api/v1/").openConnection();
            // TODO turn this into a configuration property
            rootSrvConn.setConnectTimeout(2000);

            int status = rootSrvConn.getResponseCode();
            if (status != 200) {
                log.info("Usability of {} as Identity server: answer status: {}", remote, status);
                return false;
            }

            JsonElement el = parser.parse(IOUtils.toString(rootSrvConn.getInputStream(), StandardCharsets.UTF_8));
            if (!el.isJsonObject()) {
                log.debug("IS {} did not send back a JSON object for single 3PID lookup");
                return false;
            }

            return true;
        } catch (IllegalArgumentException | IOException | JsonParseException e) {
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

            List<SRVRecord> srvRecords = new ArrayList<>();
            Record[] records = new Lookup(lookupDns, Type.SRV).run();
            if (records == null || records.length == 0) {
                log.info("No SRV record for {}", lookupDns);
                return Optional.empty();
            }

            for (Record record : records) {
                log.info("Record: {}", record.toString());
                if (record.getType() == Type.SRV) {
                    if (record instanceof SRVRecord) {
                        srvRecords.add((SRVRecord) record);
                    } else {
                        log.warn("We requested SRV records but we got {} instead!", record.getClass().getName());
                    }
                } else {
                    log.warn("We request SRV type records but we got type #{} instead!", record.getType());
                }
            }
            srvRecords.sort(Comparator.comparingInt(SRVRecord::getPriority));

            for (SRVRecord srvRecord : srvRecords) {
                String baseUrl = "https://" + srvRecord.getTarget().toString(true) + ":" + srvRecord.getPort();
                if (isUsable(baseUrl)) {
                    log.info("Found Identity Server for domain {} at {}", domainOrUrl, baseUrl);
                    return Optional.of(baseUrl);
                }
            }

            log.info("Found no Identity server for domain {} at {}");
            return Optional.empty();
        } catch (TextParseException e) {
            log.warn(domainOrUrl + " is not a valid domain name");
            return Optional.empty();
        }
    }

}
