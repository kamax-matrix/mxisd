package io.kamax.mxisd.lookup

import groovy.json.JsonSlurper
import io.kamax.mxisd.api.ThreePidType
import org.springframework.stereotype.Component

@Component
class RootProvider implements ThreePidProvider {

    private List<String> roots = Arrays.asList("https://matrix.org", "https://vector.im")
    private JsonSlurper json = new JsonSlurper()

    @Override
    int getPriority() {
        return 0
    }

    @Override
    Optional<?> find(ThreePidType type, String threePid) {
        for (String root : roots) {
            HttpURLConnection rootSrvConn = (HttpURLConnection) new URL(
                    "${root}/_matrix/identity/api/v1/lookup?medium=${type}&address=${threePid}"
            ).openConnection()

            def output = json.parseText(rootSrvConn.getInputStream().getText())
            if (output['address'] != null) {
                return Optional.of(output)
            }
        }

        return Optional.empty()
    }

}
