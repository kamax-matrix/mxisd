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
