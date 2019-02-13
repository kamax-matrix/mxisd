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

import io.kamax.mxisd.as.AppSvcManager;
import io.kamax.mxisd.auth.AuthManager;
import io.kamax.mxisd.auth.AuthProviders;
import io.kamax.mxisd.backend.IdentityStoreSupplier;
import io.kamax.mxisd.backend.sql.synapse.Synapse;
import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.crypto.CryptoFactory;
import io.kamax.mxisd.directory.DirectoryManager;
import io.kamax.mxisd.directory.DirectoryProviders;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.dns.FederationDnsOverwrite;
import io.kamax.mxisd.invitation.InvitationManager;
import io.kamax.mxisd.lookup.ThreePidProviders;
import io.kamax.mxisd.lookup.fetcher.IRemoteIdentityServerFetcher;
import io.kamax.mxisd.lookup.provider.BridgeFetcher;
import io.kamax.mxisd.lookup.provider.RemoteIdentityServerFetcher;
import io.kamax.mxisd.lookup.strategy.LookupStrategy;
import io.kamax.mxisd.lookup.strategy.RecursivePriorityLookupStrategy;
import io.kamax.mxisd.matrix.IdentityServerUtils;
import io.kamax.mxisd.notification.NotificationHandlerSupplier;
import io.kamax.mxisd.notification.NotificationHandlers;
import io.kamax.mxisd.notification.NotificationManager;
import io.kamax.mxisd.profile.ProfileManager;
import io.kamax.mxisd.profile.ProfileProviders;
import io.kamax.mxisd.registration.RegistrationManager;
import io.kamax.mxisd.session.SessionManager;
import io.kamax.mxisd.storage.IStorage;
import io.kamax.mxisd.storage.crypto.Ed25519KeyManager;
import io.kamax.mxisd.storage.crypto.KeyManager;
import io.kamax.mxisd.storage.crypto.SignatureManager;
import io.kamax.mxisd.storage.ormlite.OrmLiteSqlStorage;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.ServiceLoader;

public class Mxisd {

    private MxisdConfig cfg;

    private CloseableHttpClient httpClient;
    private IRemoteIdentityServerFetcher srvFetcher;

    private IStorage store;

    private Ed25519KeyManager keyMgr;
    private SignatureManager signMgr;
    private ClientDnsOverwrite clientDns;

    // Features
    private AuthManager authMgr;
    private DirectoryManager dirMgr;
    private LookupStrategy idStrategy;
    private InvitationManager invMgr;
    private ProfileManager pMgr;
    private AppSvcManager asHander;
    private SessionManager sessMgr;
    private NotificationManager notifMgr;
    private RegistrationManager regMgr;

    public Mxisd(MxisdConfig cfg) {
        this.cfg = cfg.build();
    }

    private void build() {
        httpClient = HttpClients.custom()
                .setUserAgent("mxisd")
                .setMaxConnPerRoute(Integer.MAX_VALUE)
                .setMaxConnTotal(Integer.MAX_VALUE)
                .build();

        IdentityServerUtils.setHttpClient(httpClient);
        srvFetcher = new RemoteIdentityServerFetcher(httpClient);

        store = new OrmLiteSqlStorage(cfg);
        keyMgr = CryptoFactory.getKeyManager(cfg.getKey());
        signMgr = CryptoFactory.getSignatureManager(keyMgr);
        clientDns = new ClientDnsOverwrite(cfg.getDns().getOverwrite());
        FederationDnsOverwrite fedDns = new FederationDnsOverwrite(cfg.getDns().getOverwrite());
        Synapse synapse = new Synapse(cfg.getSynapseSql());
        BridgeFetcher bridgeFetcher = new BridgeFetcher(cfg.getLookup().getRecursive().getBridge(), srvFetcher);

        ServiceLoader.load(IdentityStoreSupplier.class).iterator().forEachRemaining(p -> p.accept(this));
        ServiceLoader.load(NotificationHandlerSupplier.class).iterator().forEachRemaining(p -> p.accept(this));

        idStrategy = new RecursivePriorityLookupStrategy(cfg.getLookup(), ThreePidProviders.get(), bridgeFetcher);
        pMgr = new ProfileManager(ProfileProviders.get(), clientDns, httpClient);
        notifMgr = new NotificationManager(cfg.getNotification(), NotificationHandlers.get());
        sessMgr = new SessionManager(cfg.getSession(), cfg.getMatrix(), store, notifMgr, idStrategy, httpClient);
        invMgr = new InvitationManager(cfg, store, idStrategy, keyMgr, signMgr, fedDns, notifMgr);
        authMgr = new AuthManager(cfg, AuthProviders.get(), idStrategy, invMgr, clientDns, httpClient);
        dirMgr = new DirectoryManager(cfg.getDirectory(), clientDns, httpClient, DirectoryProviders.get());
        regMgr = new RegistrationManager(httpClient, clientDns, idStrategy, invMgr);
        asHander = new AppSvcManager(cfg, store, pMgr, notifMgr, synapse);
    }

    public MxisdConfig getConfig() {
        return cfg;
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public ClientDnsOverwrite getClientDns() {
        return clientDns;
    }

    public IRemoteIdentityServerFetcher getServerFetcher() {
        return srvFetcher;
    }

    public KeyManager getKeyManager() {
        return keyMgr;
    }

    public InvitationManager getInvitationManager() {
        return invMgr;
    }

    public LookupStrategy getIdentity() {
        return idStrategy;
    }

    public AuthManager getAuth() {
        return authMgr;
    }

    public SessionManager getSession() {
        return sessMgr;
    }

    public DirectoryManager getDirectory() {
        return dirMgr;
    }

    public ProfileManager getProfile() {
        return pMgr;
    }

    public SignatureManager getSign() {
        return signMgr;
    }

    public RegistrationManager getReg() {
        return regMgr;
    }

    public AppSvcManager getAs() {
        return asHander;
    }

    public NotificationManager getNotif() {
        return notifMgr;
    }

    public void start() {
        build();
    }

    public void stop() {
        // no-op
    }

}
