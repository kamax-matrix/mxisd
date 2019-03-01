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
import io.kamax.matrix.event.EventKey;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.backend.sql.synapse.Synapse;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.exception.HttpMatrixException;
import io.kamax.mxisd.exception.NotAllowedException;
import io.kamax.mxisd.notification.NotificationManager;
import io.kamax.mxisd.profile.ProfileManager;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.ormlite.dao.ASTransactionDao;
import io.kamax.mxisd.util.GsonParser;
import org.apache.commons.io.IOUtils;
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

    private transient final Logger log = LoggerFactory.getLogger(AppSvcManager.class);

    private final GsonParser parser;

    private MatrixConfig cfg;
    private IStorage store;
    private Map<String, EventTypeProcessor> processors = new HashMap<>();

    private Map<String, CompletableFuture<String>> transactionsInProgress;

    public AppSvcManager(MxisdConfig cfg, IStorage store, ProfileManager profiler, NotificationManager notif, Synapse synapse) {
        this.cfg = cfg.getMatrix();
        this.store = store;

        parser = new GsonParser();
        transactionsInProgress = new ConcurrentHashMap<>();

        processors.put("m.room.member", new MembershipProcessor(cfg.getMatrix(), profiler, notif, synapse));

        processConfig();
    }

    private void processConfig() {
        String synapseRegFile = cfg.getListener().getSynapse().getRegistrationFile();
        if (StringUtils.isNotBlank(synapseRegFile)) {
            SynapseRegistrationYaml syncCfg = SynapseRegistrationYaml.parse(cfg.getListener());

            Representer rep = new Representer();
            rep.getPropertyUtils().setBeanAccess(BeanAccess.FIELD);
            Yaml yaml = new Yaml(rep);
            String synCfgRaw = yaml.dump(syncCfg);

            try {
                IOUtils.write(synCfgRaw, new FileOutputStream(synapseRegFile), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException("Unable to write synapse appservice registration file", e);
            }
        }
    }

    public AppSvcManager withToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new HttpMatrixException(401, "M_UNAUTHORIZED", "No HS token");
        }

        if (!StringUtils.equals(cfg.getListener().getToken().getHs(), token)) {
            throw new NotAllowedException("Invalid HS token");
        }

        return this;
    }

    public CompletableFuture<String> processTransaction(String txnId, InputStream is) {
        if (StringUtils.isEmpty(txnId)) {
            throw new IllegalArgumentException("Transaction ID cannot be empty");
        }

        synchronized (this) {
            Optional<ASTransactionDao> dao = store.getTransactionResult(cfg.getListener().getLocalpart(), txnId);
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
                store.insertTransactionResult(cfg.getListener().getLocalpart(), txnId, end, result);
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
