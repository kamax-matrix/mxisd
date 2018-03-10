package io.kamax.mxisd.controller.auth.v1;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.kamax.mxisd.dns.ClientDnsOverwrite;
import io.kamax.mxisd.util.GsonParser;
import io.kamax.mxisd.util.GsonUtil;
import io.kamax.mxisd.util.RestClientUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@RestController
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class RegistrationController {

    private final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final String registerV1Url = "/_matrix/client/r0/register";

    private ClientDnsOverwrite dns;
    private CloseableHttpClient client;
    private Gson gson;
    private GsonParser parser;

    @Autowired
    public RegistrationController(ClientDnsOverwrite dns, CloseableHttpClient client) {
        this.dns = dns;
        this.client = client;
        this.gson = GsonUtil.build();
        this.parser = new GsonParser(gson);
    }

    private String resolveProxyUrl(HttpServletRequest req) {
        URI target = URI.create(req.getRequestURL().toString());
        URIBuilder builder = dns.transform(target);
        String urlToLogin = builder.toString();
        log.info("Proxy resolution: {} to {}", target.toString(), urlToLogin);
        return urlToLogin;
    }

    @RequestMapping(path = registerV1Url, method = RequestMethod.GET)
    public String getLogin(HttpServletRequest req, HttpServletResponse res) {
        try (CloseableHttpResponse hsResponse = client.execute(new HttpGet(resolveProxyUrl(req)))) {
            res.setStatus(hsResponse.getStatusLine().getStatusCode());
            return EntityUtils.toString(hsResponse.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(path = registerV1Url, method = RequestMethod.POST)
    public String register(HttpServletRequest req, HttpServletResponse res) {
        try {
            JsonObject reqJsonObject = parser.parse(req.getInputStream());
            GsonUtil.findObj(reqJsonObject, "auth").ifPresent(auth -> {
                GsonUtil.findPrimitive(auth, "type").ifPresent(type -> {
                    if (StringUtils.equals("io.kamax.google.auth", type.getAsString())) {
                        log.info("Got registration attempt with Google account");
                        if (!auth.has("googleId")) {
                            throw new IllegalArgumentException("Google ID is missing");
                        }

                        String gId = auth.get("googleId").getAsString();
                        log.info("Google ID: {}", gId);
                        auth.addProperty("type", "m.login.dummy");
                        auth.remove("googleId");
                        reqJsonObject.addProperty("password", UUID.randomUUID().toString());
                    }
                });
            });

            log.info("Sending body: {}", gson.toJson(reqJsonObject));
            HttpPost httpPost = RestClientUtils.post(resolveProxyUrl(req), gson, reqJsonObject);
            try (CloseableHttpResponse httpResponse = client.execute(httpPost)) {
                int sc = httpResponse.getStatusLine().getStatusCode();
                String body = EntityUtils.toString(httpResponse.getEntity());
                JsonObject json = parser.parse(body);
                if (sc == 200 && json.has("user_id")) {
                    log.info("User was registered, adding 3PID");
                    String userId = json.get("user_id").getAsString();

                }
                res.setStatus(sc);
                return body;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
