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

package io.kamax.mxisd.http.io.identity;

import io.kamax.mxisd.lookup.SingleLookupReply;

import java.util.HashMap;
import java.util.Map;

public class SingeLookupReplyJson {

    private String address;
    private String medium;
    private String mxid;
    private long not_after;
    private long not_before;
    private long ts;
    private Map<String, Map<String, String>> signatures = new HashMap<>();

    public SingeLookupReplyJson(SingleLookupReply reply) {
        this.address = reply.getRequest().getThreePid();
        this.medium = reply.getRequest().getType();
        this.mxid = reply.getMxid().getId();
        this.not_after = reply.getNotAfter().toEpochMilli();
        this.not_before = reply.getNotBefore().toEpochMilli();
        this.ts = reply.getTimestamp().toEpochMilli();
    }

    public String getAddress() {
        return address;
    }

    public String getMedium() {
        return medium;
    }

    public String getMxid() {
        return mxid;
    }

    public long getNot_after() {
        return not_after;
    }

    public long getNot_before() {
        return not_before;
    }

    public long getTs() {
        return ts;
    }

    public Map<String, Map<String, String>> getSignatures() {
        return signatures;
    }

}
