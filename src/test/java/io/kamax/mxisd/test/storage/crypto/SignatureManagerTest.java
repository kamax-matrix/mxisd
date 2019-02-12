/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2019 Kamax Sàrl
 *
 * https://www.kamax.io/
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

package io.kamax.mxisd.test.storage.crypto;

import com.google.gson.JsonObject;
import io.kamax.matrix.json.GsonUtil;
import io.kamax.matrix.json.MatrixJson;
import io.kamax.mxisd.storage.crypto.*;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class SignatureManagerTest {

    private static SignatureManager signMgr;

    private static SignatureManager build(String keySeed) {
        Ed25519Key key = new Ed25519Key(new Ed2219RegularKeyIdentifier("0"), keySeed);
        KeyStore store = new MemoryKeyStore();
        store.add(key);

        return new Ed25519SignatureManager(new Ed25519KeyManager(store));
    }

    @BeforeClass
    public static void beforeClass() {
        signMgr = build(KeyTest.Private);
    }

    private void testSign(String value, String sign) {
        assertThat(signMgr.sign(value).getSignature(), is(equalTo(sign)));
    }

    // As per https://matrix.org/docs/spec/appendices.html#json-signing
    @Test
    public void onEmptyObject() {
        String value = "{}";
        String sign = "K8280/U9SSy9IVtjBuVeLr+HpOB4BQFWbg+UZaADMtTdGYI7Geitb76LTrr5QV/7Xg4ahLwYGYZzuHGZKM5ZAQ";

        testSign(value, sign);
    }

    // As per https://matrix.org/docs/spec/appendices.html#json-signing
    @Test
    public void onSimpleObject() {
        JsonObject data = new JsonObject();
        data.addProperty("one", 1);
        data.addProperty("two", "Two");

        String value = GsonUtil.get().toJson(data);
        String sign = "KqmLSbO39/Bzb0QIYE82zqLwsA+PDzYIpIRA2sRQ4sL53+sN6/fpNSoqE7BP7vBZhG6kYdD13EIMJpvhJI+6Bw";

        testSign(value, sign);
    }

    @Test
    public void onFederationHeader() {
        SignatureManager mgr = build("1QblgjFeL3IxoY4DKOR7p5mL5sQTC0ChmeMJlqb4d5M");

        JsonObject o = new JsonObject();
        o.addProperty("method", "GET");
        o.addProperty("uri", "/_matrix/federation/v1/query/directory?room_alias=%23a%3Amxhsd.local.kamax.io%3A8447");
        o.addProperty("origin", "synapse.local.kamax.io");
        o.addProperty("destination", "mxhsd.local.kamax.io:8447");

        String signExpected = "SEMGSOJEsoalrBfHqPO2QrSlbLaUYLHLk4e3q4IJ2JbgvCynT1onp7QF1U4Sl3G3NzybrgdnVvpqcaEgV0WPCw";
        Signature signProduced = mgr.sign(o);
        assertThat(signProduced.getSignature(), is(equalTo(signExpected)));
    }

    @Test
    public void onIdentityLookup() {
        String value = MatrixJson.encodeCanonical("{\n" + "  \"address\": \"mxisd-federation-test@kamax.io\",\n"
                + "  \"medium\": \"email\",\n" + "  \"mxid\": \"@mxisd-lookup-test:kamax.io\",\n"
                + "  \"not_after\": 253402300799000,\n" + "  \"not_before\": 0,\n" + "  \"ts\": 1523482030147\n" + "}");

        String sign = "ObKA4PNQh2g6c7Yo2QcTcuDgIwhknG7ZfqmNYzbhrbLBOqZomU22xX9raufN2Y3ke1FXsDqsGs7WBDodmzZJCg";
        testSign(value, sign);
    }

}
