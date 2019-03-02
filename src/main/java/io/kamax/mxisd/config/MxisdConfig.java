/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2018 Kamax Sàrl
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

package io.kamax.mxisd.config;

import io.kamax.mxisd.config.ldap.generic.GenericLdapConfig;
import io.kamax.mxisd.config.ldap.netiq.NetIqLdapConfig;
import io.kamax.mxisd.config.memory.MemoryStoreConfig;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import io.kamax.mxisd.config.sql.generic.GenericSqlProviderConfig;
import io.kamax.mxisd.config.sql.synapse.SynapseSqlProviderConfig;
import io.kamax.mxisd.config.threepid.ThreePidConfig;
import io.kamax.mxisd.config.threepid.notification.NotificationConfig;
import io.kamax.mxisd.config.wordpress.WordpressConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MxisdConfig {

    private static final Logger log = LoggerFactory.getLogger(MxisdConfig.class);

    public static class Dns {

        private DnsOverwriteConfig overwrite = new DnsOverwriteConfig();

        public DnsOverwriteConfig getOverwrite() {
            return overwrite;
        }

        public void setOverwrite(DnsOverwriteConfig overwrite) {
            this.overwrite = overwrite;
        }

        public void build() {
            overwrite.build();
        }

    }

    public static class Lookup {

        private BulkLookupConfig bulk = new BulkLookupConfig();
        private RecursiveLookupConfig recursive = new RecursiveLookupConfig();

        public BulkLookupConfig getBulk() {
            return bulk;
        }

        public void setBulk(BulkLookupConfig bulk) {
            this.bulk = bulk;
        }

        public RecursiveLookupConfig getRecursive() {
            return recursive;
        }

        public void setRecursive(RecursiveLookupConfig recursive) {
            this.recursive = recursive;
        }

        public void build() {
            getBulk().build();
            getRecursive().build();
        }

    }

    private AppServiceConfig appsvc = new AppServiceConfig();
    private AuthenticationConfig auth = new AuthenticationConfig();
    private DirectoryConfig directory = new DirectoryConfig();
    private Dns dns = new Dns();
    private ExecConfig exec = new ExecConfig();
    private FirebaseConfig firebase = new FirebaseConfig();
    private ForwardConfig forward = new ForwardConfig();
    private InvitationConfig invite = new InvitationConfig();
    private KeyConfig key = new KeyConfig();
    private GenericLdapConfig ldap = new GenericLdapConfig();
    private Lookup lookup = new Lookup();
    private MatrixConfig matrix = new MatrixConfig();
    private MemoryStoreConfig memory = new MemoryStoreConfig();
    private NotificationConfig notification = new NotificationConfig();
    private NetIqLdapConfig netiq = new NetIqLdapConfig();
    private RegisterConfig register = new RegisterConfig();
    private ServerConfig server = new ServerConfig();
    private SessionConfig session = new SessionConfig();
    private StorageConfig storage = new StorageConfig();
    private RestBackendConfig rest = new RestBackendConfig();
    private GenericSqlProviderConfig sql = new GenericSqlProviderConfig();
    private SynapseSqlProviderConfig synapseSql = new SynapseSqlProviderConfig();
    private ThreePidConfig threepid = new ThreePidConfig();
    private ViewConfig view = new ViewConfig();
    private WordpressConfig wordpress = new WordpressConfig();

    public AppServiceConfig getAppsvc() {
        return appsvc;
    }

    public void setAppsvc(AppServiceConfig appsvc) {
        this.appsvc = appsvc;
    }

    public AuthenticationConfig getAuth() {
        return auth;
    }

    public void setAuth(AuthenticationConfig auth) {
        this.auth = auth;
    }

    public DirectoryConfig getDirectory() {
        return directory;
    }

    public void setDirectory(DirectoryConfig directory) {
        this.directory = directory;
    }

    public Dns getDns() {
        return dns;
    }

    public void setDns(Dns dns) {
        this.dns = dns;
    }

    public ExecConfig getExec() {
        return exec;
    }

    public void setExec(ExecConfig exec) {
        this.exec = exec;
    }

    public FirebaseConfig getFirebase() {
        return firebase;
    }

    public void setFirebase(FirebaseConfig firebase) {
        this.firebase = firebase;
    }

    public ForwardConfig getForward() {
        return forward;
    }

    public void setForward(ForwardConfig forward) {
        this.forward = forward;
    }

    public InvitationConfig getInvite() {
        return invite;
    }

    public void setInvite(InvitationConfig invite) {
        this.invite = invite;
    }

    public KeyConfig getKey() {
        return key;
    }

    public void setKey(KeyConfig key) {
        this.key = key;
    }

    public GenericLdapConfig getLdap() {
        return ldap;
    }

    public void setLdap(GenericLdapConfig ldap) {
        this.ldap = ldap;
    }

    public Lookup getLookup() {
        return lookup;
    }

    public void setLookup(Lookup lookup) {
        this.lookup = lookup;
    }

    public MatrixConfig getMatrix() {
        return matrix;
    }

    public void setMatrix(MatrixConfig matrix) {
        this.matrix = matrix;
    }

    public MemoryStoreConfig getMemory() {
        return memory;
    }

    public void setMemory(MemoryStoreConfig memory) {
        this.memory = memory;
    }

    public NotificationConfig getNotification() {
        return notification;
    }

    public void setNotification(NotificationConfig notification) {
        this.notification = notification;
    }

    public NetIqLdapConfig getNetiq() {
        return netiq;
    }

    public void setNetiq(NetIqLdapConfig netiq) {
        this.netiq = netiq;
    }

    public RegisterConfig getRegister() {
        return register;
    }

    public void setRegister(RegisterConfig register) {
        this.register = register;
    }

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }

    public SessionConfig getSession() {
        return session;
    }

    public void setSession(SessionConfig session) {
        this.session = session;
    }

    public StorageConfig getStorage() {
        return storage;
    }

    public void setStorage(StorageConfig storage) {
        this.storage = storage;
    }

    public RestBackendConfig getRest() {
        return rest;
    }

    public void setRest(RestBackendConfig rest) {
        this.rest = rest;
    }

    public GenericSqlProviderConfig getSql() {
        return sql;
    }

    public void setSql(GenericSqlProviderConfig sql) {
        this.sql = sql;
    }

    public SynapseSqlProviderConfig getSynapseSql() {
        return synapseSql;
    }

    public void setSynapseSql(SynapseSqlProviderConfig synapseSql) {
        this.synapseSql = synapseSql;
    }

    public ThreePidConfig getThreepid() {
        return threepid;
    }

    public void setThreepid(ThreePidConfig threepid) {
        this.threepid = threepid;
    }

    public ViewConfig getView() {
        return view;
    }

    public void setView(ViewConfig view) {
        this.view = view;
    }

    public WordpressConfig getWordpress() {
        return wordpress;
    }

    public void setWordpress(WordpressConfig wordpress) {
        this.wordpress = wordpress;
    }

    public MxisdConfig build() {
        if (StringUtils.isBlank(getServer().getName())) {
            getServer().setName(getMatrix().getDomain());
            log.debug("server.name is empty, using matrix.domain");
        }

        getAppsvc().build();
        getAuth().build();
        getDirectory().build();
        getExec().build();
        getFirebase().build();
        getForward().build();
        getInvite().build();
        getKey().build();
        getLdap().build();
        getLookup().build();
        getMatrix().build();
        getMemory().build();
        getNetiq().build();
        getNotification().build();
        getRegister().build();
        getRest().build();
        getSession().build();
        getServer().build();
        getSql().build();
        getStorage().build();
        getSynapseSql().build();
        getThreepid().build();
        getView().build();
        getWordpress().build();

        return this;
    }

}
