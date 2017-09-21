package io.kamax.mxisd.controller.v1.remote;

import io.kamax.mxisd.session.SessionMananger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@CrossOrigin
@RequestMapping(path = RemoteIdentityAPIv1.BASE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RemoteSessionController {

    private Logger log = LoggerFactory.getLogger(RemoteSessionController.class);

    @Autowired
    private SessionMananger mgr;

    @RequestMapping(path = "/validate/requestToken")
    public String requestToken(
            HttpServletRequest request,
            @RequestParam String sid,
            @RequestParam("client_secret") String secret,
            @RequestParam String token) {
        log.info("Request {}: {}", request.getMethod(), request.getRequestURL(), request.getQueryString());
        mgr.createRemote(sid, secret, token);

        return "{}";
    }

}
