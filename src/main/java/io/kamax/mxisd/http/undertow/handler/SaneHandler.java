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
import com.google.gson.stream.MalformedJsonException;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.matrix.json.InvalidJsonException;
import io.kamax.mxisd.exception.*;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class SaneHandler extends BasicHttpHandler {

    private static final Logger log = LoggerFactory.getLogger(SaneHandler.class);

    private static final String CorsOriginName = "Access-Control-Allow-Origin";
    private static final String CorsOriginValue = "*";
    private static final String CorsMethodsName = "Access-Control-Allow-Methods";
    private static final String CorsMethodsValue = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String CorsHeadersName = "Access-Control-Allow-Headers";
    private static final String CorsHeadersValue = "Origin, X-Requested-With, Content-Type, Accept, Authorization";

    public static SaneHandler around(HttpHandler h) {
        return new SaneHandler(h);
    }

    private final HttpHandler child;

    private SaneHandler(HttpHandler child) {
        this.child = child;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.startBlocking();

        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
        } else {
            try {
                // CORS headers as per spec
                putHeader(exchange, CorsOriginName, CorsOriginValue);
                putHeader(exchange, CorsMethodsName, CorsMethodsValue);
                putHeader(exchange, CorsHeadersName, CorsHeadersValue);

                child.handleRequest(exchange);
            } catch (IllegalArgumentException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, GsonUtil.makeObj("error", e.getMessage()));
            } catch (BadRequestException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, "M_BAD_REQUEST", e.getMessage());
            } catch (MappingAlreadyExistsException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, "M_ALREADY_EXISTS", e.getMessage());
            } catch (JsonMemberNotFoundException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, "M_JSON_MISSING_KEYS", e.getMessage());
            } catch (InvalidResponseJsonException | JsonSyntaxException | MalformedJsonException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, "M_INVALID_JSON", e.getMessage());
            } catch (InvalidJsonException e) {
                respond(exchange, HttpStatus.SC_BAD_REQUEST, e.getErrorCode(), e.getError());
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
                    log.error("Transaction #{} - {}", e.getReference(), e.getInternalReason());
                } else {
                    log.error("Transaction #{}", e);
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
                String message = e.getMessage();
                if (StringUtils.isBlank(message)) {
                    message = "An internal server error occurred. Contact your administrator with reference Transaction #" + Instant.now().toEpochMilli();
                }
                respond(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, buildErrorBody(exchange, "M_UNKNOWN", message));
            } finally {
                exchange.endExchange();
            }
        }
    }

}
