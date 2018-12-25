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

package io.kamax.mxisd.http.undertow.handler;

import com.google.gson.JsonSyntaxException;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.mxisd.exception.*;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class SaneHandler extends BasicHttpHandler {

    public static SaneHandler around(HttpHandler h) {
        return new SaneHandler(h);
    }

    private transient final Logger log = LoggerFactory.getLogger(SaneHandler.class);

    private HttpHandler child;

    public SaneHandler(HttpHandler child) {
        this.child = child;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.startBlocking();

        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
        } else {
            try {
                child.handleRequest(exchange);
            } catch (IllegalArgumentException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, GsonUtil.makeObj("error", e.getMessage()));
            } catch (BadRequestException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, "M_BAD_REQUEST", e.getMessage());
            } catch (MappingAlreadyExistsException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, "M_ALREADY_EXISTS", e.getMessage());
            } catch (JsonMemberNotFoundException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, "M_JSON_MISSING_KEYS", e.getMessage());
            } catch (InvalidResponseJsonException | JsonSyntaxException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, "M_INVALID_JSON", e.getMessage());
            } catch (InvalidCredentialsException e) {
                respond(exchange, HttpStatus.SC_UNAUTHORIZED, "M_UNAUTHORIZED", e.getMessage());
            } catch (ObjectNotFoundException e) {
                respond(exchange, HttpStatus.SC_NOT_FOUND, "M_NOT_FOUND", e.getMessage());
            } catch (NotImplementedException e) {
                respond(exchange, HttpStatus.SC_NOT_IMPLEMENTED, "M_NOT_IMPLEMENTED", e.getMessage());
            } catch (FeatureNotAvailable e) {
                if (StringUtils.isNotBlank(e.getInternalReason())) {
                    log.error("Feature not available: {}", e.getInternalReason());
                }

                handleException(exchange, e);
            } catch (InternalServerError e) {
                if (StringUtils.isNotBlank(e.getInternalReason())) {
                    log.error("Reference #{} - {}", e.getReference(), e.getInternalReason());
                } else {
                    log.error("Reference #{}", e);
                }

                handleException(exchange, e);
            } catch (RemoteLoginException e) {
                if (e.getErrorBodyMsgResp() != null) {
                    respond(exchange, e.getStatus(), e.getErrorBodyMsgResp());
                } else {
                    handleException(exchange, e);
                }
            } catch (HttpMatrixException e) {
                respond(exchange, e.getStatus(), buildErrorBody(exchange, e.getErrorCode(), e.getError()));
            } catch (RuntimeException e) {
                log.error("Unknown error when handling {}", exchange.getRequestURL(), e);
                respond(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, buildErrorBody(exchange,
                        "M_UNKNOWN",
                        StringUtils.defaultIfBlank(
                                e.getMessage(),
                                "An internal server error occurred. If this error persists, please contact support with reference #" +
                                        Instant.now().toEpochMilli()
                        )
                ));
            } finally {
                exchange.endExchange();
            }
        }
    }

}
