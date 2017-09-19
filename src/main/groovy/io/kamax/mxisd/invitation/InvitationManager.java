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

package io.kamax.mxisd.invitation;

import com.google.gson.Gson;
import io.kamax.matrix.MatrixID;
import io.kamax.mxisd.config.DnsOverwrite;
import io.kamax.mxisd.config.DnsOverwriteEntry;
import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.exception.MappingAlreadyExistsException;
import io.kamax.mxisd.invitation.generator.IInviteContentGenerator;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.signature.SignatureManager;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.ormlite.ThreePidInviteIO;
import io.kamax.mxisd.threepid.connector.IThreePidConnector;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Component
public class InvitationManager {

    private Logger log = LoggerFactory.getLogger(InvitationManager.class);

    private Map<String, IThreePidInviteReply> invitations = new ConcurrentHashMap<>();

    @Autowired
    private IStorage storage;

    @Autowired
    private LookupStrategy lookupMgr;

    @Autowired
    private SignatureManager signMgr;

    @Autowired
    private DnsOverwrite dns;

    private Map<String, IInviteContentGenerator> generators;
    private Map<String, IThreePidConnector> connectors;

    private CloseableHttpClient client;
    private Gson gson;
    private Timer refreshTimer;

    @Autowired
    public InvitationManager(
            List<IInviteContentGenerator> generatorList,
            List<IThreePidConnector> connectorList
    ) {
        generators = new HashMap<>();
        generatorList.forEach(sender -> { // FIXME to support several possible implementations
            if (generators.containsKey(sender.getMedium())) {
                throw new RuntimeException("More than one " + sender.getMedium() + " content generator");
            }

            generators.put(sender.getMedium(), sender);
        });

        connectors = new HashMap<>();
        connectorList.forEach(connector -> { // FIXME to support several possible implementations
            if (connectors.containsKey(connector.getMedium())) {
                throw new RuntimeException("More than one " + connector.getMedium() + " connector");
            }

            connectors.put(connector.getMedium(), connector);
        });
    }

    @PostConstruct
    private void postConstruct() {
        gson = new Gson();

        log.info("Loading saved invites");
        Collection<ThreePidInviteIO> ioList = storage.getInvites();
        ioList.forEach(io -> {
            log.info("Processing invite {}", gson.toJson(io));
            ThreePidInvite invite = new ThreePidInvite(
                    new MatrixID(io.getSender()),
                    io.getMedium(),
                    io.getAddress(),
                    io.getRoomId(),
                    io.getProperties()
            );

            ThreePidInviteReply reply = new ThreePidInviteReply(getId(invite), invite, io.getToken(), "");
            invitations.put(reply.getId(), reply);
        });

        // FIXME export such madness into matrix-java-sdk with a nice wrapper to talk to a homeserver
        try {
            SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial(new TrustSelfSignedStrategy()).build();
            HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();
            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
            client = HttpClients.custom().setSSLSocketFactory(sslSocketFactory).build();
        } catch (Exception e) {
            // FIXME do better...
            throw new RuntimeException(e);
        }

        log.info("Setting up invitation mapping refresh timer");
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    lookupMappingsForInvites();
                } catch (Throwable t) {
                    log.error("Error when running background mapping refresh", t);
                }
            }
        }, 5000L, TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES)); // FIXME make configurable
    }

    @PreDestroy
    private void preDestroy() {
        refreshTimer.cancel();
        ForkJoinPool.commonPool().awaitQuiescence(1, TimeUnit.MINUTES);
    }

    private String getId(IThreePidInvite invite) {
        return invite.getSender().getDomain().toLowerCase() + invite.getMedium().toLowerCase() + invite.getAddress().toLowerCase();
    }

    private String getIdForLog(IThreePidInviteReply reply) {
        return reply.getInvite().getSender().getId() + ":" + reply.getInvite().getRoomId() + ":" + reply.getInvite().getMedium() + ":" + reply.getInvite().getAddress();
    }

    String getSrvRecordName(String domain) {
        return "_matrix._tcp." + domain;
    }

    // TODO use caching mechanism
    // TODO export in matrix-java-sdk
    String findHomeserverForDomain(String domain) {
        Optional<DnsOverwriteEntry> entryOpt = dns.findHost(domain);
        if (entryOpt.isPresent()) {
            DnsOverwriteEntry entry = entryOpt.get();
            log.info("Found DNS overwrite for {} to {}", entry.getName(), entry.getTarget());
            return "https://" + entry.getTarget();
        }

        log.debug("Performing SRV lookup for {}", domain);
        String lookupDns = getSrvRecordName(domain);
        log.info("Lookup name: {}", lookupDns);

        try {
            List<SRVRecord> srvRecords = new ArrayList<>();
            Record[] rawRecords = new Lookup(lookupDns, Type.SRV).run();
            if (rawRecords != null && rawRecords.length > 0) {
                for (Record record : rawRecords) {
                    if (Type.SRV == record.getType()) {
                        srvRecords.add((SRVRecord) record);
                    } else {
                        log.info("Got non-SRV record: {}", record.toString());
                    }
                }

                srvRecords.sort(Comparator.comparingInt(SRVRecord::getPriority));
                for (SRVRecord record : srvRecords) {
                    log.info("Found SRV record: {}", record.toString());
                    return "https://" + record.getTarget().toString(true) + ":" + record.getPort();
                }
            } else {
                log.info("No SRV record for {}", lookupDns);
            }
        } catch (TextParseException e) {
            log.warn("Unable to perform DNS SRV query for {}: {}", lookupDns, e.getMessage());
        }

        log.info("Performing basic lookup using domain name {}", domain);
        return "https://" + domain + ":8448";
    }

    public synchronized IThreePidInviteReply storeInvite(IThreePidInvite invitation) { // TODO better sync
        IInviteContentGenerator generator = generators.get(invitation.getMedium());
        IThreePidConnector connector = connectors.get(invitation.getMedium());
        if (generator == null || connector == null) {
            throw new BadRequestException("Medium type " + invitation.getMedium() + " is not supported");
        }

        String invId = getId(invitation);
        log.info("Handling invite for {}:{} from {} in room {}", invitation.getMedium(), invitation.getAddress(), invitation.getSender(), invitation.getRoomId());
        if (invitations.containsKey(invId)) {
            log.info("Invite is already pending for {}:{}, returning data", invitation.getMedium(), invitation.getAddress());
            return invitations.get(invId);
        }

        Optional<?> result = lookupMgr.find(invitation.getMedium(), invitation.getAddress(), true);
        if (result.isPresent()) {
            log.info("Mapping for {}:{} already exists, refusing to store invite", invitation.getMedium(), invitation.getAddress());
            throw new MappingAlreadyExistsException();
        }

        String token = RandomStringUtils.randomAlphanumeric(64);
        String displayName = invitation.getAddress().substring(0, 3) + "...";

        IThreePidInviteReply reply = new ThreePidInviteReply(invId, invitation, token, displayName);

        log.info("Performing invite to {}:{}", invitation.getMedium(), invitation.getAddress());
        connector.send(reply, generator.generate(reply));

        log.info("Storing invite under ID {}", invId);
        storage.insertInvite(reply);
        invitations.put(invId, reply);
        log.info("A new invite has been created for {}:{} on HS {}", invitation.getMedium(), invitation.getAddress(), invitation.getSender().getDomain());

        return reply;
    }

    public void lookupMappingsForInvites() {
        if (!invitations.isEmpty()) {
            log.info("Checking for existing mapping for pending invites");
            for (IThreePidInviteReply reply : invitations.values()) {
                log.info("Processing invite {}", getIdForLog(reply));
                ForkJoinPool.commonPool().submit(new MappingChecker(reply));
            }
        }
    }

    public void publishMappingIfInvited(ThreePidMapping threePid) {
        log.info("Looking up possible pending invites for {}:{}", threePid.getMedium(), threePid.getValue());
        for (IThreePidInviteReply reply : invitations.values()) {
            if (StringUtils.equalsIgnoreCase(reply.getInvite().getMedium(), threePid.getMedium()) && StringUtils.equalsIgnoreCase(reply.getInvite().getAddress(), threePid.getValue())) {
                log.info("{}:{} has an invite pending on HS {}, publishing mapping", threePid.getMedium(), threePid.getValue(), reply.getInvite().getSender().getDomain());
                publishMapping(reply, threePid.getMxid());
            }
        }
    }

    private void publishMapping(IThreePidInviteReply reply, String mxid) {
        String medium = reply.getInvite().getMedium();
        String address = reply.getInvite().getAddress();
        String domain = reply.getInvite().getSender().getDomain();
        log.info("Discovering HS for domain {}", domain);
        String hsUrlOpt = findHomeserverForDomain(domain);

        // TODO this is needed as this will block if called during authentication cycle due to synapse implementation
        new Thread(() -> { // FIXME need to make this retry-able and within a general background working pool
            HttpPost req = new HttpPost(hsUrlOpt + "/_matrix/federation/v1/3pid/onbind");
            // Expected body: https://matrix.to/#/!HUeDbmFUsWAhxHHvFG:matrix.org/$150469846739DCLWc:matrix.trancendances.fr
            JSONObject obj = new JSONObject(); // TODO use Gson instead
            obj.put("mxid", mxid);
            obj.put("token", reply.getToken());
            obj.put("signatures", signMgr.signMessageJson(obj.toString()));

            JSONObject objUp = new JSONObject();
            objUp.put("mxid", mxid);
            objUp.put("medium", medium);
            objUp.put("address", address);
            objUp.put("sender", reply.getInvite().getSender().getId());
            objUp.put("room_id", reply.getInvite().getRoomId());
            objUp.put("signed", obj);

            JSONObject content = new JSONObject(); // TODO use Gson instead
            JSONArray invites = new JSONArray();
            invites.put(objUp);
            content.put("invites", invites);
            content.put("medium", medium);
            content.put("address", address);
            content.put("mxid", mxid);

            content.put("signatures", signMgr.signMessageJson(content.toString()));

            StringEntity entity = new StringEntity(content.toString(), StandardCharsets.UTF_8);
            entity.setContentType("application/json");
            req.setEntity(entity);
            try {
                log.info("Posting onBind event to {}", req.getURI());
                CloseableHttpResponse response = client.execute(req);
                int statusCode = response.getStatusLine().getStatusCode();
                log.info("Answer code: {}", statusCode);
                if (statusCode >= 300) {
                    log.warn("Answer body: {}", IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
                } else {
                    invitations.remove(getId(reply.getInvite()));
                    storage.deleteInvite(reply.getId());
                    log.info("Removed invite from internal store");
                }
                response.close();
            } catch (IOException e) {
                log.warn("Unable to tell HS {} about invite being mapped", domain, e);
            }
        }).start();
    }

    private class MappingChecker implements Runnable {

        private IThreePidInviteReply reply;

        public MappingChecker(IThreePidInviteReply reply) {
            this.reply = reply;
        }

        @Override
        public void run() {
            try {
                log.info("Searching for mapping created since invite {} was created", getIdForLog(reply));
                Optional<SingleLookupReply> result = lookupMgr.find(reply.getInvite().getMedium(), reply.getInvite().getAddress(), true);
                if (result.isPresent()) {
                    SingleLookupReply lookup = result.get();
                    log.info("Found mapping for pending invite {}", getIdForLog(reply));
                    publishMapping(reply, lookup.getMxid().getId());
                } else {
                    log.info("No mapping for pending invite {}", getIdForLog(reply));
                }
            } catch (Throwable t) {
                log.error("Unable to process invite", t);
            }
        }
    }

}
