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

import io.kamax.mxisd.config.MxisdConfig;
import io.kamax.mxisd.http.undertow.handler.OptionsHandler;
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
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

public class HttpMxisd {

    // Core
    private Mxisd m;

    // I/O
    private Undertow httpSrv;

    public HttpMxisd(MxisdConfig cfg) {
        m = new Mxisd(cfg);
    }

    public void start() {
        m.start();

        HttpHandler helloHandler = SaneHandler.around(new HelloHandler());
        HttpHandler asNotFoundHandler = SaneHandler.around(new AsNotFoundHandler(m.getAs()));
        HttpHandler asTxnHandler = SaneHandler.around(new AsTransactionHandler(m.getAs()));
        HttpHandler storeInvHandler = SaneHandler.around(new StoreInviteHandler(m.getConfig().getServer(), m.getInvitationManager(), m.getKeyManager()));
        HttpHandler sessValidateHandler = SaneHandler.around(new SessionValidateHandler(m.getSession(), m.getConfig().getServer(), m.getConfig().getView()));

        httpSrv = Undertow.builder().addHttpListener(m.getConfig().getServer().getPort(), "0.0.0.0").setHandler(Handlers.routing()

                .add("OPTIONS", "/**", SaneHandler.around(new OptionsHandler()))

                // Status endpoints
                .get(StatusHandler.Path, SaneHandler.around(new StatusHandler()))

                // Authentication endpoints
                .get(LoginHandler.Path, SaneHandler.around(new LoginGetHandler(m.getAuth(), m.getHttpClient())))
                .post(LoginHandler.Path, SaneHandler.around(new LoginPostHandler(m.getAuth())))
                .post(RestAuthHandler.Path, SaneHandler.around(new RestAuthHandler(m.getAuth())))

                // Directory endpoints
                .post(UserDirectorySearchHandler.Path, SaneHandler.around(new UserDirectorySearchHandler(m.getDirectory())))

                // Key endpoints
                .get(KeyGetHandler.Path, SaneHandler.around(new KeyGetHandler(m.getKeyManager())))
                .get(RegularKeyIsValidHandler.Path, SaneHandler.around(new RegularKeyIsValidHandler(m.getKeyManager())))
                .get(EphemeralKeyIsValidHandler.Path, SaneHandler.around(new EphemeralKeyIsValidHandler(m.getKeyManager())))

                // Identity endpoints
                .get(HelloHandler.Path, helloHandler)
                .get(HelloHandler.Path + "/", helloHandler) // Be lax with possibly trailing slash
                .get(SingleLookupHandler.Path, SaneHandler.around(new SingleLookupHandler(m.getConfig(), m.getIdentity(), m.getSign())))
                .post(BulkLookupHandler.Path, SaneHandler.around(new BulkLookupHandler(m.getIdentity())))
                .post(StoreInviteHandler.Path, storeInvHandler)
                .post(SessionStartHandler.Path, SaneHandler.around(new SessionStartHandler(m.getSession())))
                .get(SessionValidateHandler.Path, sessValidateHandler)
                .post(SessionValidateHandler.Path, sessValidateHandler)
                .get(SessionTpidGetValidatedHandler.Path, SaneHandler.around(new SessionTpidGetValidatedHandler(m.getSession())))
                .post(SessionTpidBindHandler.Path, SaneHandler.around(new SessionTpidBindHandler(m.getSession(), m.getInvitationManager())))
                .post(SessionTpidUnbindHandler.Path, SaneHandler.around(new SessionTpidUnbindHandler(m.getSession())))
                .post(SignEd25519Handler.Path, SaneHandler.around(new SignEd25519Handler(m.getConfig(), m.getInvitationManager(), m.getSign())))

                // Profile endpoints
                .get(ProfileHandler.Path, SaneHandler.around(new ProfileHandler(m.getProfile())))
                .get(InternalProfileHandler.Path, SaneHandler.around(new InternalProfileHandler(m.getProfile())))

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
        m.stop();
    }

}
