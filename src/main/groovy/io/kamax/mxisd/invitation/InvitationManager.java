package io.kamax.mxisd.invitation;

import com.google.gson.Gson;
import io.kamax.matrix.ThreePid;
import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.exception.MappingAlreadyExistsException;
import io.kamax.mxisd.invitation.sender.IInviteSender;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.signature.SignatureManager;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InvitationManager {

    private Logger log = LoggerFactory.getLogger(InvitationManager.class);

    private Map<ThreePid, IThreePidInviteReply> invitations = new ConcurrentHashMap<>();

    @Autowired
    private LookupStrategy lookupMgr;

    @Autowired
    private SignatureManager signMgr;

    private Map<String, IInviteSender> senders;

    private CloseableHttpClient client;
    private Gson gson;

    @PostConstruct
    private void postConstruct() {
        gson = new Gson();

        try {
            HttpClientBuilder b = HttpClientBuilder.create();

            // setup a Trust Strategy that allows all certificates.
            //
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
            b.setSslcontext(sslContext);

            // don't check Hostnames, either.
            //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
            HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

            // here's the special part:
            //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
            //      -- and create a Registry, to register it.
            //
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslSocketFactory)
                    .build();

            // now, we create connection-manager using our Registry.
            //      -- allows multi-threaded use
            PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            b.setConnectionManager(connMgr);

            // finally, build the HttpClient;
            //      -- done!
            client = b.build();
        } catch (Exception e) {
            // FIXME do better...
            throw new RuntimeException(e);
        }
    }

    String getSrvRecordName(String domain) {
        return "_matrix._tcp." + domain;
    }

    // TODO use caching mechanism
    // TODO export in matrix-java-sdk
    Optional<String> findHomeserverForDomain(String domain) {
        log.debug("Performing SRV lookup for {}", domain);
        String lookupDns = getSrvRecordName(domain);
        log.info("Lookup name: {}", lookupDns);

        try {
            SRVRecord[] records = (SRVRecord[]) new Lookup(lookupDns, Type.SRV).run();
            if (records != null) {
                Arrays.sort(records, Comparator.comparingInt(SRVRecord::getPriority));
                for (SRVRecord record : records) {
                    log.info("Found SRV record: {}", record.toString());
                    return Optional.of("https://" + record.getTarget().toString(true) + ":" + record.getPort());
                }
            } else {
                log.info("No SRV record for {}", lookupDns);
            }
        } catch (TextParseException e) {
            log.warn("Unable to perform DNS SRV query for {}: {}", lookupDns, e.getMessage());
        }

        log.info("Performing basic lookup using domain name {}", domain);
        return Optional.of("https://" + domain + ":8448");
    }

    @Autowired
    public InvitationManager(List<IInviteSender> senderList) {
        senders = new HashMap<>();
        senderList.forEach(sender -> senders.put(sender.getMedium(), sender));
    }

    public synchronized IThreePidInviteReply storeInvite(IThreePidInvite invitation) { // TODO better sync
        IInviteSender sender = senders.get(invitation.getMedium());
        if (sender == null) {
            throw new BadRequestException("Medium type " + invitation.getMedium() + " is not supported");
        }

        ThreePid pid = new ThreePid(invitation.getMedium(), invitation.getAddress());

        log.info("Storing invite for {}:{} from {} in room {}", pid.getMedium(), pid.getAddress(), invitation.getSender(), invitation.getRoomId());
        if (invitations.containsKey(pid)) {
            log.info("Invite is already pending for {}:{}, returning data", pid.getMedium(), pid.getAddress());
            return invitations.get(pid);
        }

        SingleLookupRequest request = new SingleLookupRequest();
        request.setType(invitation.getMedium());
        request.setThreePid(invitation.getAddress());
        request.setRecursive(true);
        request.setRequester("Internal");

        Optional<?> result = lookupMgr.findRecursive(request);
        if (result.isPresent()) {
            log.info("Mapping for {}:{} already exists, refusing to store invite", pid.getMedium(), pid.getAddress());
            throw new MappingAlreadyExistsException();
        }

        String token = RandomStringUtils.randomAlphanumeric(64);
        String displayName = invitation.getAddress().substring(0, 3) + "...";

        IThreePidInviteReply reply = new ThreePidInviteReply(invitation, token, displayName);

        log.info("Performing invite to {}:{}", pid.getMedium(), pid.getAddress());
        sender.send(reply);

        invitations.put(pid, reply);
        log.info("A new invite has been created for {}:{}", pid.getMedium(), pid.getAddress());

        return reply;
    }

    public void publishMappingIfInvited(ThreePidMapping threePid) {
        ThreePid key = new ThreePid(threePid.getMedium(), threePid.getValue());
        IThreePidInviteReply reply = invitations.get(key);
        if (reply == null) {
            log.info("{}:{} does not have a pending invite, no mapping to publish", threePid.getMedium(), threePid.getValue());
            return;
        }

        log.info("{}:{} has an invite pending, publishing mapping", threePid.getMedium(), threePid.getValue());
        String domain = reply.getInvite().getSender().getDomain();
        log.info("Discovering HS for domain {}", domain);
        Optional<String> hsUrlOpt = findHomeserverForDomain(domain);
        if (!hsUrlOpt.isPresent()) {
            log.warn("No HS found for domain {} - ignoring publishing", domain);
        } else {
            HttpPost req = new HttpPost(hsUrlOpt.get() + "/_matrix/federation/v1/3pid/onbind");
            JSONObject obj = new JSONObject(); // TODO use Gson instead
            obj.put("mxisd", threePid.getMxid());
            obj.put("token", reply.getToken());
            String mapping = gson.toJson(signMgr.signMessage(obj.toString())); // FIXME we shouldn't need to be doign this

            JSONObject content = new JSONObject(); // TODO use Gson instead
            content.put("invites", Collections.singletonList(mapping));
            content.put("medium", threePid.getMedium());
            content.put("address", threePid.getValue());
            content.put("mxid", threePid.getMxid());

            StringEntity entity = new StringEntity(content.toString(), StandardCharsets.UTF_8);
            entity.setContentType("application/json");
            req.setEntity(entity);
            try {
                log.info("Posting onBind event to {}", req.getURI());
                CloseableHttpResponse response = client.execute(req);
                response.close();
            } catch (IOException e) {
                log.warn("Unable to tell HS {} about invite being mapped", domain, e);
            }
        }

        invitations.remove(key);
    }

}
