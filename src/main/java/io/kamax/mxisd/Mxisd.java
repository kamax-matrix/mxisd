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

package io.kamax.mxisd;

import io.kamax.matrix.crypto.KeyManager;
import io.kamax.matrix.crypto.SignatureManager;
import io.kamax.mxisd.as.AppSvcManager;
import io.kamax.mxisd.auth.AuthManager;
import io.kamax.mxisd.auth.AuthProviders;
import io.kamax.mxisd.backend.IdentityStoreSupplier;
import io.kamax.mxisd.backend.sql.synapse.Synapse;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.directory.DirectoryManager;
import io.kamax.mxisd.directory.DirectoryProviders;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.dns.FederationDnsOverwrite;
import io.kamax.mxisd.http.undertow.handler.SaneHandler;
import io.kamax.mxisd.http.undertow.handler.as.v1.AsNotFoundHandler;
import io.kamax.mxisd.http.undertow.handler.as.v1.AsTransactionHandler;
import io.kamax.mxisd.http.undertow.handler.auth.RestAuthHandler;
import io.kamax.mxisd.http.undertow.handler.auth.v1.LoginGetHandler;
import io.kamax.mxisd.http.undertow.handler.auth.v1.LoginHandler;
import io.kamax.mxisd.http.undertow.handler.auth.v1.LoginPostHandler;
import io.kamax.mxisd.http.undertow.handler.directory.v1.UserDirectorySearchHandler;
import io.kamax.mxisd.http.undertow.handler.identity.v1.*;
import io.kamax.mxisd.http.undertow.handler.profile.v1.InternalProfileHandler;
import io.kamax.mxisd.http.undertow.handler.profile.v1.ProfileHandler;
import io.kamax.mxisd.http.undertow.handler.status.StatusHandler;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.lookup.ThreePidProviders;
import io.kamax.mxisd.lookup.provider.BridgeFetcher;
import io.kamax.mxisd.lookup.provider.RemoteIdentityServerFetcher;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.lookup.strategy.RecursivePriorityLookupStrategy;
import io.kamax.mxisd.notification.NotificationHandlers;
import io.kamax.mxisd.notification.NotificationManager;
import io.kamax.mxisd.profile.ProfileManager;
import io.kamax.mxisd.profile.ProfileProviders;
import io.kamax.mxisd.session.SessionMananger;
import io.kamax.mxisd.spring.CryptoFactory;
import io.kamax.mxisd.storage.ormlite.OrmLiteSqliteStorage;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.ServiceLoader;

public class Mxisd {

    private MxisdConfig cfg;

    private CloseableHttpClient httpClient;

    private KeyManager keyMgr;
    private SignatureManager signMgr;

    // Features
    private AuthManager authMgr;
    private DirectoryManager dirMgr;
    private LookupStrategy idStrategy;
    private InvitationManager invMgr;
    private ProfileManager pMgr;
    private AppSvcManager asHander;
    private SessionMananger sessMgr;

    // I/O
    private Undertow httpSrv;

    public Mxisd(MxisdConfig cfg) {
        this.cfg = cfg.build();
    }

    private void build() {
        httpClient = HttpClients.custom()
                .setUserAgent("mxisd")
                .setMaxConnPerRoute(Integer.MAX_VALUE)
                .setMaxConnTotal(Integer.MAX_VALUE)
                .build();

        OrmLiteSqliteStorage storage = new OrmLiteSqliteStorage(cfg);
        keyMgr = CryptoFactory.getKeyManager(cfg.getKey());
        signMgr = CryptoFactory.getSignatureManager(keyMgr, cfg.getServer());
        ClientDnsOverwrite clientDns = new ClientDnsOverwrite(cfg.getDns().getOverwrite());
        FederationDnsOverwrite fedDns = new FederationDnsOverwrite(cfg.getDns().getOverwrite());
        pMgr = new ProfileManager(ProfileProviders.get(), clientDns, httpClient);
        NotificationManager notifMgr = new NotificationManager(cfg.getNotification(), NotificationHandlers.get());
        Synapse synapse = new Synapse(cfg.getSynapseSql());
        RemoteIdentityServerFetcher srvFetcher = new RemoteIdentityServerFetcher(httpClient);
        BridgeFetcher bridgeFetcher = new BridgeFetcher(cfg.getLookup().getRecursive().getBridge(), srvFetcher);
        idStrategy = new RecursivePriorityLookupStrategy(cfg.getLookup(), ThreePidProviders.get(), bridgeFetcher);
        invMgr = new InvitationManager(cfg.getInvite(), storage, idStrategy, signMgr, fedDns, notifMgr);
        sessMgr = new SessionMananger(cfg.getSession(), cfg.getMatrix(), storage, notifMgr, httpClient);

        authMgr = new AuthManager(cfg, AuthProviders.get(), idStrategy, invMgr, clientDns, httpClient);
        dirMgr = new DirectoryManager(cfg.getDirectory(), clientDns, httpClient, DirectoryProviders.get());
        asHander = new AppSvcManager(cfg, storage, pMgr, notifMgr, synapse);

        ServiceLoader.load(IdentityStoreSupplier.class).iterator().forEachRemaining(p -> p.accept(this));
    }

    public MxisdConfig getConfig() {
        return cfg;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public InvitationManager getInvitationManager() {
        return invMgr;
    }

    public void start() {
        build();

        HttpHandler asNotFoundHandler = SaneHandler.around(new AsNotFoundHandler(asHander));
        HttpHandler asTxnHandler = SaneHandler.around(new AsTransactionHandler(asHander));
        HttpHandler storeInvHandler = SaneHandler.around(new StoreInviteHandler(cfg.getServer(), invMgr, keyMgr));
        HttpHandler sessValidateHandler = SaneHandler.around(new SessionValidateHandler(sessMgr, cfg.getServer(), cfg.getView()));

        httpSrv = Undertow.builder().addHttpListener(cfg.getServer().getPort(), "0.0.0.0").setHandler(Handlers.routing()

                // Status endpoints
                .get(StatusHandler.Path, SaneHandler.around(new StatusHandler()))

                // Authentication endpoints
                .get(LoginHandler.Path, SaneHandler.around(new LoginGetHandler(authMgr, httpClient)))
                .post(LoginHandler.Path, SaneHandler.around(new LoginPostHandler(authMgr)))
                .post(RestAuthHandler.Path, SaneHandler.around(new RestAuthHandler(authMgr)))

                // Directory endpoints
                .post(UserDirectorySearchHandler.Path, SaneHandler.around(new UserDirectorySearchHandler(dirMgr)))

                // Key endpoints
                .get(KeyGetHandler.Path, SaneHandler.around(new KeyGetHandler(keyMgr)))
                .get(RegularKeyIsValidHandler.Path, SaneHandler.around(new RegularKeyIsValidHandler(keyMgr)))
                .get(EphemeralKeyIsValidHandler.Path, SaneHandler.around(new EphemeralKeyIsValidHandler()))

                // Identity endpoints
                .get(HelloHandler.Path, new HelloHandler())
                .get(SingleLookupHandler.Path, new SingleLookupHandler(idStrategy, signMgr))
                .post(BulkLookupHandler.Path, new BulkLookupHandler(idStrategy))
                .post(StoreInviteHandler.Path, storeInvHandler)
                .post(SessionStartHandler.Path, SaneHandler.around(new SessionStartHandler(sessMgr)))
                .get(SessionValidateHandler.Path, sessValidateHandler)
                .post(SessionValidateHandler.Path, sessValidateHandler)
                .get(SessionTpidGetValidatedHandler.Path, SaneHandler.around(new SessionTpidGetValidatedHandler(sessMgr)))
                .post(SessionTpidBindHandler.Path, SaneHandler.around(new SessionTpidBindHandler(sessMgr, invMgr)))
                .get(RemoteIdentityAPIv1.SESSION_REQUEST_TOKEN, SaneHandler.around(new RemoteSessionStartHandler(sessMgr, cfg.getView())))
                .get(RemoteIdentityAPIv1.SESSION_CHECK, SaneHandler.around(new RemoteSessionCheckHandler(sessMgr, cfg.getView())))

                // Profile endpoints
                .get(ProfileHandler.Path, SaneHandler.around(new ProfileHandler(pMgr)))
                .get(InternalProfileHandler.Path, SaneHandler.around(new InternalProfileHandler(pMgr)))

                // Application Service endpoints
                .get("/_matrix/app/v1/users/**", asNotFoundHandler)
                .get("/users/**", asNotFoundHandler) // Legacy endpoint
                .get("/_matrix/app/v1/rooms/**", asNotFoundHandler)
                .get("/rooms/**", asNotFoundHandler) // Legacy endpoint
                .put(AsTransactionHandler.Path, asTxnHandler)
                .put("/transactions/{" + AsTransactionHandler.ID + "}", asTxnHandler) // Legacy endpoint

        ).build();

        httpSrv.start();
    }

    public void stop() {
        httpSrv.stop();
    }

}
