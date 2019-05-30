/*
 * mxisd - Matrix Identity Server Daemon
 * Copyright (C) 2017 Kamax Sarl
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

package io.kamax.mxisd.lookup;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.kamax.matrix.MatrixID;
import io.kamax.matrix._MatrixID;
import io.kamax.mxisd.http.io.identity.SingeLookupReplyJson;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class SingleLookupReply {

    private static Gson gson = new Gson();

    private boolean isRecursive;
    private String body;
    private SingleLookupRequest request;
    private _MatrixID mxid;
    private Instant notBefore;
    private Instant notAfter;
    private Instant timestamp;
    private Map<String, Map<String, String>> signatures = new HashMap<>();

    public static SingleLookupReply fromRecursive(SingleLookupRequest request, String body) {
        SingleLookupReply reply = new SingleLookupReply();
        reply.isRecursive = true;
        reply.request = request;
        reply.body = body;

        try {
            SingeLookupReplyJson json = gson.fromJson(body, SingeLookupReplyJson.class);
            reply.mxid = MatrixID.asAcceptable(json.getMxid());
            reply.notAfter = Instant.ofEpochMilli(json.getNot_after());
            reply.notBefore = Instant.ofEpochMilli(json.getNot_before());
            reply.timestamp = Instant.ofEpochMilli(json.getTs());
            reply.signatures = new HashMap<>(json.getSignatures());
        } catch (JsonSyntaxException e) {
            // stub - we only want to try, nothing more
        }

        return reply;
    }

    private SingleLookupReply() {
        // stub
    }

    public SingleLookupReply(SingleLookupRequest request, String mxid) {
        this(request, MatrixID.asAcceptable(mxid));
    }

    public SingleLookupReply(SingleLookupRequest request, _MatrixID mxid) {
        this(request, mxid, Instant.now(), Instant.now().minusSeconds(60), Instant.now().plusSeconds(5 * 60));
    }

    public SingleLookupReply(SingleLookupRequest request, _MatrixID mxid, Instant timestamp, Instant notBefore, Instant notAfter) {
        this.request = request;
        this.mxid = mxid;
        this.timestamp = timestamp;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
    }

    public boolean isRecursive() {
        return isRecursive;
    }

    public String getBody() {
        return body;
    }

    public SingleLookupRequest getRequest() {
        return request;
    }

    public _MatrixID getMxid() {
        return mxid;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public Instant getNotAfter() {
        return notAfter;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Map<String, String>> getSignatures() {
        return signatures;
    }

    public Map<String, String> getSignature(String host) {
        return signatures.computeIfAbsent(host, k -> new HashMap<>());
    }

}
