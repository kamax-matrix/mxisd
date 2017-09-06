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

package io.kamax.mxisd.controller.v1;

import io.kamax.mxisd.exception.BadRequestException;
import io.kamax.mxisd.exception.MappingAlreadyExistsException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
@ResponseBody
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class DefaultExceptionHandler {

    private Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    static String handle(String erroCode, String error) {
        return "{\"errcode\":\"" + erroCode + "\",\"error\":\"" + error + "\"}";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handle(MissingServletRequestParameterException e) {
        return handle("M_INVALID_BODY", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MappingAlreadyExistsException.class)
    public String handle(MappingAlreadyExistsException e) {
        return handle("M_ALREADY_EXISTS", e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public String handle(BadRequestException e) {
        return handle("M_BAD_REQUEST", e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RuntimeException.class)
    public String handle(HttpServletRequest req, RuntimeException e) {
        log.error("Unknown error when handling {}", req.getRequestURL(), e);
        return handle("M_UNKNOWN", StringUtils.defaultIfBlank(e.getMessage(), "An uknown error occured. Contact the server administrator if this persists."));
    }

}
