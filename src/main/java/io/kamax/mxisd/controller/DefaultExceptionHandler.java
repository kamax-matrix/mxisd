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

package io.kamax.mxisd.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.kamax.mxisd.exception.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;

@ControllerAdvice
@ResponseBody
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DefaultExceptionHandler {

    private Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    private static Gson gson = new Gson();

    private String handle(HttpServletRequest req, String erroCode, String error) {
        JsonObject obj = new JsonObject();
        obj.addProperty("errcode", erroCode);
        obj.addProperty("error", error);
        obj.addProperty("success", false);
        log.info("Request {} {} - Error {}: {}", req.getMethod(), req.getRequestURL(), erroCode, error);
        return gson.toJson(obj);
    }

    @ExceptionHandler(InternalServerError.class)
    public String handle(HttpServletRequest request, HttpServletResponse response, InternalServerError e) {
        if (StringUtils.isNotBlank(e.getInternalReason())) {
            log.error("Reference #{} - {}", e.getReference(), e.getInternalReason());
        } else {
            log.error("Reference #{}", e);
        }

        return handleGeneric(request, response, e);
    }

    @ExceptionHandler(FeatureNotAvailable.class)
    public String handle(HttpServletRequest request, HttpServletResponse response, FeatureNotAvailable e) {
        if (StringUtils.isNotBlank(e.getInternalReason())) {
            log.error("Feature not available: {}", e.getInternalReason());
        }

        return handleGeneric(request, response, e);
    }

    @ExceptionHandler(MatrixException.class)
    public String handleGeneric(HttpServletRequest request, HttpServletResponse response, MatrixException e) {
        response.setStatus(e.getStatus());
        return handle(request, e.getErrorCode(), e.getError());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handle(HttpServletRequest req, MissingServletRequestParameterException e) {
        return handle(req, "M_INCOMPLETE_REQUEST", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidResponseJsonException.class)
    public String handle(HttpServletRequest req, InvalidResponseJsonException e) {
        return handle(req, "M_INVALID_JSON", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(JsonSyntaxException.class)
    public String handle(HttpServletRequest req, JsonSyntaxException e) {
        return handle(req, "M_INVALID_JSON", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(JsonMemberNotFoundException.class)
    public String handle(HttpServletRequest req, JsonMemberNotFoundException e) {
        return handle(req, "M_JSON_MISSING_KEYS", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MappingAlreadyExistsException.class)
    public String handle(HttpServletRequest req, MappingAlreadyExistsException e) {
        return handle(req, "M_ALREADY_EXISTS", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public String handle(HttpServletRequest req, BadRequestException e) {
        return handle(req, "M_BAD_REQUEST", e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RuntimeException.class)
    public String handle(HttpServletRequest req, RuntimeException e) {
        log.error("Unknown error when handling {}", req.getRequestURL(), e);
        return handle(
                req,
                "M_UNKNOWN",
                StringUtils.defaultIfBlank(
                        e.getMessage(),
                        "An internal server error occurred. If this error persists, please contact support with reference #" +
                                Instant.now().toEpochMilli()
                )
        );
    }

}
