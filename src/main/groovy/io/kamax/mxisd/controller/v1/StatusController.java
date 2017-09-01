package io.kamax.mxisd.controller.v1;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class StatusController {

    @RequestMapping(value = "/_matrix/identity/status")
    public String getStatus() {
        // TODO link to backend
        return "{\"status\":{\"health\":\"OK\"}}";
    }

}
