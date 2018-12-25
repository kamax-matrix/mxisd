package io.kamax.mxisd.test.backend.rest;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.kamax.matrix.ThreePidMedium;
import io.kamax.mxisd.backend.rest.RestThreePidProvider;
import io.kamax.mxisd.config.MatrixConfig;
import io.kamax.mxisd.config.rest.RestBackendConfig;
import io.kamax.mxisd.lookup.SingleLookupReply;
import io.kamax.mxisd.lookup.SingleLookupRequest;
import io.kamax.mxisd.lookup.ThreePidMapping;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RestThreePidProviderTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(65000);

    private RestThreePidProvider p;

    private String lookupSinglePath = "/lookup/single";
    private SingleLookupRequest lookupSingleRequest;
    private String lookupSingleRequestBody = "{\"lookup\":{\"medium\":\"email\",\"address\":\"john.doe@example.org\"}}";
    private String lookupSingleFoundBody = "{\"lookup\":{\"medium\":\"email\",\"address\":\"john.doe@example.org\"" +
            ",\"id\":{\"type\":\"mxid\",\"value\":\"@john:example.org\"}}}";
    private String lookupSingleNotFoundBody = "{}";

    private String lookupBulkPath = "/lookup/bulk";
    private List<ThreePidMapping> lookupBulkList;
    private String lookupBulkRequestBody = "{\"lookup\":[{\"medium\":\"email\",\"address\":\"john.doe@example.org\"}," +
            "{\"medium\":\"msisdn\",\"address\":\"123456789\"}]}";
    private String lookupBulkFoundBody = "{\"lookup\":[{\"medium\":\"email\",\"address\":\"john.doe@example.org\"," +
            "\"id\":{\"type\":\"localpart\",\"value\":\"john\"}},{\"medium\":\"msisdn\",\"address\":\"123456789\"," +
            "\"id\":{\"type\":\"mxid\",\"value\":\"@jane:example.org\"}}]}";
    private String lookupBulkNotFoundBody = "{\"lookup\":[]}";

    @Before
    public void before() {
        MatrixConfig mxCfg = new MatrixConfig();
        mxCfg.setDomain("example.org");
        mxCfg.build();

        RestBackendConfig cfg = new RestBackendConfig();
        cfg.setEnabled(true);
        cfg.setHost("http://localhost:65000");
        cfg.getEndpoints().getIdentity().setSingle(lookupSinglePath);
        cfg.getEndpoints().getIdentity().setBulk(lookupBulkPath);
        cfg.build();

        p = new RestThreePidProvider(cfg, mxCfg);

        lookupSingleRequest = new SingleLookupRequest();
        lookupSingleRequest.setType(ThreePidMedium.Email.getId());
        lookupSingleRequest.setThreePid("john.doe@example.org");

        ThreePidMapping m1 = new ThreePidMapping();
        m1.setMedium(ThreePidMedium.Email.getId());
        m1.setValue("john.doe@example.org");

        ThreePidMapping m2 = new ThreePidMapping();
        m1.setMedium(ThreePidMedium.PhoneNumber.getId());
        m1.setValue("123456789");
        lookupBulkList = new ArrayList<>();
        lookupBulkList.add(m1);
        lookupBulkList.add(m2);
    }

    @Test
    public void lookupSingleFound() {
        stubFor(post(urlEqualTo(lookupSinglePath))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(lookupSingleFoundBody)
                )
        );

        Optional<SingleLookupReply> rep = p.find(lookupSingleRequest);
        assertTrue(rep.isPresent());
        rep.ifPresent(data -> {
            assertNotNull(data.getMxid());
            assertTrue(data.getMxid().getId(), StringUtils.equals(data.getMxid().getId(), "@john:example.org"));
        });

        verify(postRequestedFor(urlMatching("/lookup/single"))
                .withHeader("Content-Type", containing(ContentType.APPLICATION_JSON.getMimeType()))
                .withRequestBody(equalTo(lookupSingleRequestBody))
        );
    }

    @Test
    public void lookupSingleNotFound() {
        stubFor(post(urlEqualTo(lookupSinglePath))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(lookupSingleNotFoundBody)
                )
        );

        Optional<SingleLookupReply> rep = p.find(lookupSingleRequest);
        assertTrue(!rep.isPresent());

        verify(postRequestedFor(urlMatching("/lookup/single"))
                .withHeader("Content-Type", containing(ContentType.APPLICATION_JSON.getMimeType()))
                .withRequestBody(equalTo(lookupSingleRequestBody))
        );
    }

    @Test
    public void lookupBulkFound() {
        stubFor(post(urlEqualTo(lookupBulkPath))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(lookupBulkFoundBody)
                )
        );

        List<ThreePidMapping> mappings = p.populate(lookupBulkList);
        assertNotNull(mappings);
        assertEquals(2, mappings.size());
        assertTrue(StringUtils.equals(mappings.get(0).getMxid(), "@john:example.org"));
        assertTrue(StringUtils.equals(mappings.get(1).getMxid(), "@jane:example.org"));
    }

    @Test
    public void lookupBulkNotFound() {
        stubFor(post(urlEqualTo(lookupBulkPath))
                .willReturn(aResponse()
                        .withHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType())
                        .withBody(lookupBulkNotFoundBody)
                )
        );

        List<ThreePidMapping> mappings = p.populate(lookupBulkList);
        assertNotNull(mappings);
        assertEquals(0, mappings.size());
    }

}
