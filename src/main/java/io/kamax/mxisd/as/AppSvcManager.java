/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sarl
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

package io.kamax.mxisd.as;

import com.google.gson.JsonObject;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.matrix.client.MatrixClientContext;
import io.kamax.matrix.client.as.MatrixApplicationServiceClient;
import io.kamax.matrix.event.EventKey;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.Mxisd;
import io.kamax.mxisd.as.processor.event.EventTypeProcessor;
import io.kamax.mxisd.as.processor.event.MembershipEventProcessor;
import io.kamax.mxisd.as.processor.event.MessageEventProcessor;
import io.kamax.mxisd.as.registration.SynapseRegistrationYaml;
import io.kamax.mxisd.config.AppServiceConfig;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.exception.ConfigurationException;
import io.kamax.mxisd.exception.HttpMatrixException;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.ormlite.dao.ASTransactionDao;
import io.kamax.mxisd.util.GsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AppSvcManager {

    private static final Logger log = LoggerFactory.getLogger(AppSvcManager.class);

    private final AppServiceConfig cfg;
    private final IStorage store;
    private final GsonParser parser = new GsonParser();

    private MatrixApplicationServiceClient client;
    private Map<String, EventTypeProcessor> processors = new HashMap<>();
    private Map<String, CompletableFuture<String>> transactionsInProgress = new ConcurrentHashMap<>();

    public AppSvcManager(Mxisd m) {
        this.cfg = m.getConfig().getAppsvc();
        this.store = m.getStore();

        /*
        We process the configuration to make sure all is fine and setting default values if needed
         */

        // By default, the feature is enabled
        cfg.setEnabled(ObjectUtils.defaultIfNull(cfg.isEnabled(), false));

        if (!cfg.isEnabled()) {
            return;
        }

        if (Objects.isNull(cfg.getEndpoint().getToAS().getUrl())) {
            throw new ConfigurationException("App Service: Endpoint: To AS: URL");
        }

        if (Objects.isNull(cfg.getEndpoint().getToAS().getToken())) {
            throw new ConfigurationException("App Service: Endpoint: To AS: Token", "Must be set, even if to an empty string");
        }

        if (Objects.isNull(cfg.getEndpoint().getToHS().getUrl())) {
            throw new ConfigurationException("App Service: Endpoint: To HS: URL");
        }

        if (Objects.isNull(cfg.getEndpoint().getToHS().getToken())) {
            throw new ConfigurationException("App Service: Endpoint: To HS: Token", "Must be set, even if to an empty string");
        }

        // We set a default status for each feature individually
        cfg.getFeature().getAdmin().setEnabled(ObjectUtils.defaultIfNull(cfg.getFeature().getAdmin().getEnabled(), cfg.isEnabled()));
        cfg.getFeature().setCleanExpiredInvite(ObjectUtils.defaultIfNull(cfg.getFeature().getCleanExpiredInvite(), cfg.isEnabled()));
        cfg.getFeature().setInviteById(ObjectUtils.defaultIfNull(cfg.getFeature().getInviteById(), false));

        if (cfg.getFeature().getAdmin().getEnabled()) {
            if (StringUtils.isBlank(cfg.getUser().getMain())) {
                throw new ConfigurationException("App Service admin feature is enabled, but no main user configured");
            }

            if (cfg.getUser().getMain().startsWith("@") || cfg.getUser().getMain().contains(":")) {
                throw new ConfigurationException("App Service: Users: Main ID: Is not a localpart");
            }
        }

        if (cfg.getFeature().getCleanExpiredInvite()) {
            if (StringUtils.isBlank(cfg.getUser().getInviteExpired())) {
                throw new ConfigurationException("App Service user for Expired Invite is not set");
            }

            if (cfg.getUser().getMain().startsWith("@") || cfg.getUser().getMain().contains(":")) {
                throw new ConfigurationException("App Service: Users: Expired Invite ID: Is not a localpart");
            }
        }

        MatrixClientContext mxContext = new MatrixClientContext();
        mxContext.setDomain(m.getConfig().getMatrix().getDomain());
        mxContext.setToken(cfg.getEndpoint().getToHS().getToken());
        mxContext.setHsBaseUrl(cfg.getEndpoint().getToHS().getUrl());
        client = new MatrixApplicationServiceClient(mxContext);

        processors.put("m.room.member", new MembershipEventProcessor(client, m));
        processors.put("m.room.message", new MessageEventProcessor(m, client));

        processSynapseConfig(m.getConfig());
    }

    private void processSynapseConfig(MxisdConfig cfg) {
        String synapseRegFile = cfg.getAppsvc().getRegistration().getSynapse().getFile();

        if (StringUtils.isBlank(synapseRegFile)) {
            log.info("No synapse registration file path given - skipping generation...");
            return;
        }

        SynapseRegistrationYaml syncCfg = SynapseRegistrationYaml.parse(cfg.getAppsvc(), cfg.getMatrix().getDomain());

        Representer rep = new Representer();
        rep.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
        Yaml yaml = new Yaml(rep);

        // SnakeYAML set the type of object on the first line, which can fail to be parsed on synapse
        // We therefore need to split the resulting string, remove the first line, and then write it
        List<String> lines = new ArrayList<>(Arrays.asList(yaml.dump(syncCfg).split("\\R+")));
        if (StringUtils.equals(lines.get(0), "!!" + SynapseRegistrationYaml.class.getCanonicalName())) {
            lines.remove(0);
        }

        try (FileOutputStream os = new FileOutputStream(synapseRegFile)) {
            IOUtils.writeLines(lines, System.lineSeparator(), os, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write synapse appservice registration file", e);
        }
    }

    private void ensureEnabled() {
        if (!cfg.isEnabled()) {
            throw new HttpMatrixException(503, "M_NOT_AVAILABLE", "This feature is disabled");
        }
    }

    public AppSvcManager withToken(String token) {
        ensureEnabled();

        if (StringUtils.isBlank(token)) {
            log.info("Denying request without a HS token");
            throw new HttpMatrixException(401, "M_UNAUTHORIZED", "No HS token");
        }

        if (!StringUtils.equals(cfg.getEndpoint().getToAS().getToken(), token)) {
            log.info("Denying request with an invalid HS token");
            throw new NotAllowedException("Invalid HS token");
        }

        return this;
    }

    public void processUser(String userId) {
        client.createUser(MatrixID.asAcceptable(userId).getLocalPart());
    }

    public CompletableFuture<String> processTransaction(String txnId, InputStream is) {
        ensureEnabled();

        if (StringUtils.isEmpty(txnId)) {
            throw new IllegalArgumentException("Transaction ID cannot be empty");
        }

        synchronized (this) {
            Optional<ASTransactionDao> dao = store.getTransactionResult(cfg.getUser().getMain(), txnId);
            if (dao.isPresent()) {
                log.info("AS Transaction {} already processed - returning computed result", txnId);
                return CompletableFuture.completedFuture(dao.get().getResult());
            }

            CompletableFuture<String> f = transactionsInProgress.get(txnId);
            if (Objects.nonNull(f)) {
                log.info("Returning future for transaction {}", txnId);
                return f;
            }

            transactionsInProgress.put(txnId, new CompletableFuture<>());
        }

        CompletableFuture<String> future = transactionsInProgress.get(txnId);

        Instant start = Instant.now();
        log.info("Processing AS Transaction {}: start", txnId);
        try {
            List<JsonObject> events = GsonUtil.asList(GsonUtil.getArray(parser.parse(is), "events"), JsonObject.class);
            is.close();
            log.debug("{} event(s) parsed", events.size());

            processTransaction(events);

            Instant end = Instant.now();
            String result = "{}";

            try {
                log.info("Saving transaction details to store");
                store.insertTransactionResult(cfg.getUser().getMain(), txnId, end, result);
            } finally {
                log.debug("Removing CompletedFuture from transaction map");
                transactionsInProgress.remove(txnId);
            }

            log.info("Processed AS transaction {} in {} ms", txnId, (Instant.now().toEpochMilli() - start.toEpochMilli()));
            future.complete(result);
        } catch (Exception e) {
            log.error("Unable to properly process transaction {}", txnId, e);
            future.completeExceptionally(e);
        }

        log.info("Processing AS Transaction {}: end", txnId);
        return future;
    }

    private void processTransaction(List<JsonObject> eventsJson) {
        log.info("Processing transaction events: start");

        eventsJson.forEach(ev -> {
            String evId = EventKey.Id.getStringOrNull(ev);
            if (StringUtils.isBlank(evId)) {
                log.warn("Event has no ID, skipping");
                log.debug("Event:\n{}", GsonUtil.getPrettyForLog(ev));
                return;
            }
            log.debug("Event {}: processing start", evId);

            String roomId = EventKey.RoomId.getStringOrNull(ev);
            if (StringUtils.isBlank(roomId)) {
                log.debug("Event has no room ID, skipping");
                return;
            }

            String senderId = EventKey.Sender.getStringOrNull(ev);
            if (StringUtils.isBlank(senderId)) {
                log.debug("Event has no sender ID, skipping");
                return;
            }
            _MatrixID sender = MatrixID.asAcceptable(senderId);
            log.debug("Sender: {}", senderId);

            String evType = StringUtils.defaultIfBlank(EventKey.Type.getStringOrNull(ev), "<EMPTY/MISSING>");
            EventTypeProcessor p = processors.get(evType);
            if (Objects.isNull(p)) {
                log.debug("No event processor for type {}, skipping", evType);
                return;
            }

            p.process(ev, sender, roomId);

            log.debug("Event {}: processing end", evId);
        });

        log.info("Processing transaction events: end");
    }

}
