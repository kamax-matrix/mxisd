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

package io.kamax.mxisd.controller.v1

import io.kamax.mxisd.config.ServerConfig
import io.kamax.mxisd.config.ViewConfig
import io.kamax.mxisd.controller.v1.remote.RemoteIdentityAPIv1
import io.kamax.mxisd.session.SessionMananger
import io.kamax.mxisd.session.ValidationResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping(path = IdentityAPIv1.BASE)
class SessionController {

    private Logger log = LoggerFactory.getLogger(SessionController.class)

    @Autowired
    private ServerConfig srvCfg;

    @Autowired
    private SessionMananger mgr

    @Autowired
    private ViewConfig viewCfg;

    @RequestMapping(value = "/validate/{medium}/submitToken")
    String validate(
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam String sid,
            @RequestParam("client_secret") String secret,
            @RequestParam String token,
            Model model
    ) {
        log.info("Requested: {}?{}", request.getRequestURL(), request.getQueryString())

        ValidationResult r = mgr.validate(sid, secret, token)
        log.info("Session {} was validated", sid)
        if (r.getNextUrl().isPresent()) {
            String url = srvCfg.getPublicUrl() + r.getNextUrl().get()
            log.info("Session {} validation: next URL is present, redirecting to {}", sid, url)
            response.sendRedirect(url)
        } else {
            if (r.isCanRemote()) {
                String url = srvCfg.getPublicUrl() + RemoteIdentityAPIv1.getRequestToken(r.getSession().getId(), r.getSession().getSecret());
                model.addAttribute("remoteSessionLink", url)
                return viewCfg.getSession().getLocalRemote().getOnTokenSubmit().getSuccess()
            } else {
                return viewCfg.getSession().getLocal().getOnTokenSubmit().getSuccess()
            }
        }
    }

}
