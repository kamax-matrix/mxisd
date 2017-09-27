package io.kamax.mxisd.controller.identity.v1.remote;

import io.kamax.mxisd.config.ViewConfig;
import io.kamax.mxisd.exception.SessionNotValidatedException;
import io.kamax.mxisd.session.SessionMananger;
import io.kamax.mxisd.threepid.session.IThreePidSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

import static io.kamax.mxisd.controller.identity.v1.remote.RemoteIdentityAPIv1.SESSION_CHECK;
import static io.kamax.mxisd.controller.identity.v1.remote.RemoteIdentityAPIv1.SESSION_REQUEST_TOKEN;

@Controller
public class RemoteSessionController {

    private Logger log = LoggerFactory.getLogger(RemoteSessionController.class);

    @Autowired
    private ViewConfig viewCfg;

    @Autowired
    private SessionMananger mgr;

    @RequestMapping(path = SESSION_REQUEST_TOKEN)
    public String requestToken(
            HttpServletRequest request,
            @RequestParam String sid,
            @RequestParam("client_secret") String secret,
            Model model
    ) {
        log.info("Request {}: {}", request.getMethod(), request.getRequestURL());
        IThreePidSession session = mgr.createRemote(sid, secret);
        model.addAttribute("checkLink", RemoteIdentityAPIv1.getSessionCheck(session.getId(), session.getSecret()));
        return viewCfg.getSession().getRemote().getOnRequest().getSuccess();
    }

    @RequestMapping(path = SESSION_CHECK)
    public String check(
            HttpServletRequest request,
            @RequestParam String sid,
            @RequestParam("client_secret") String secret) {
        log.info("Request {}: {}", request.getMethod(), request.getRequestURL());

        try {
            mgr.validateRemote(sid, secret);
            return viewCfg.getSession().getRemote().getOnCheck().getSuccess();
        } catch (SessionNotValidatedException e) {
            return viewCfg.getSession().getRemote().getOnCheck().getFailure();
        }
    }

}
