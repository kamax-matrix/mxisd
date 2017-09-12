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
import io.kamax.matrix.ThreePid;
import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.exception.MappingAlreadyExistsException;
import io.kamax.mxisd.invitation.sender.IInviteSender;
import io.kamax.mxisd.lookup.ThreePidMapping;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.signature.SignatureManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
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
import org.xbill.DNS.Lookup;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    }

    String getSrvRecordName(String domain) {
        return "_matrix._tcp." + domain;
    }

    // TODO use caching mechanism
    // TODO export in matrix-java-sdk
    String findHomeserverForDomain(String domain) {
        log.debug("Performing SRV lookup for {}", domain);
        String lookupDns = getSrvRecordName(domain);
        log.info("Lookup name: {}", lookupDns);

        try {
            SRVRecord[] records = (SRVRecord[]) new Lookup(lookupDns, Type.SRV).run();
            if (records != null) {
                Arrays.sort(records, Comparator.comparingInt(SRVRecord::getPriority));
                for (SRVRecord record : records) {
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

        log.info("Handling invite for {}:{} from {} in room {}", pid.getMedium(), pid.getAddress(), invitation.getSender(), invitation.getRoomId());
        if (invitations.containsKey(pid)) {
            log.info("Invite is already pending for {}:{}, returning data", pid.getMedium(), pid.getAddress());
            return invitations.get(pid);
        }

        Optional<?> result = lookupMgr.find(invitation.getMedium(), invitation.getAddress(), true);
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
        String hsUrlOpt = findHomeserverForDomain(domain);

        // TODO this is needed as this will block if called during authentication cycle due to synapse implementation
        new Thread(() -> { // FIXME need to make this retry-able and within a general background working pool
            HttpPost req = new HttpPost(hsUrlOpt + "/_matrix/federation/v1/3pid/onbind");
            // Expected body: https://matrix.to/#/!HUeDbmFUsWAhxHHvFG:matrix.org/$150469846739DCLWc:matrix.trancendances.fr
            JSONObject obj = new JSONObject(); // TODO use Gson instead
            obj.put("mxid", threePid.getMxid());
            obj.put("token", reply.getToken());
            obj.put("signatures", signMgr.signMessageJson(obj.toString()));

            JSONObject objUp = new JSONObject();
            objUp.put("mxid", threePid.getMxid());
            objUp.put("medium", threePid.getMedium());
            objUp.put("address", threePid.getValue());
            objUp.put("sender", reply.getInvite().getSender().getId());
            objUp.put("room_id", reply.getInvite().getRoomId());
            objUp.put("signed", obj);

            JSONObject content = new JSONObject(); // TODO use Gson instead
            JSONArray invites = new JSONArray();
            invites.put(objUp);
            content.put("invites", invites);
            content.put("medium", threePid.getMedium());
            content.put("address", threePid.getValue());
            content.put("mxid", threePid.getMxid());

            content.put("signatures", signMgr.signMessageJson(content.toString()));

            StringEntity entity = new StringEntity(content.toString(), StandardCharsets.UTF_8);
            entity.setContentType("application/json");
            req.setEntity(entity);
            try {
                log.info("Posting onBind event to {}", req.getURI());
                CloseableHttpResponse response = client.execute(req);
                int statusCode = response.getStatusLine().getStatusCode();
                log.info("Answer code: {}", statusCode);
                if (statusCode >= 400) {
                    log.warn("Answer body: {}", IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
                }
                response.close();
            } catch (IOException e) {
                log.warn("Unable to tell HS {} about invite being mapped", domain, e);
            }
            invitations.remove(key);
        }).start();
    }

}
