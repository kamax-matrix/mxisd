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
import io.kamax.matrix.ThreePid;

public class ThreePidMapping {

    private static Gson gson = new Gson();

    private String medium;
    private String value;
    private String mxid;

    public ThreePidMapping() {
        // stub
    }

    public ThreePidMapping(ThreePid threePid, String mxid) {
        this(threePid.getMedium(), threePid.getAddress(), mxid);
    }

    public ThreePidMapping(String medium, String value, String mxid) {
        setMedium(medium);
        setValue(value);
        setMxid(mxid);
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMxid() {
        return mxid;
    }

    public void setMxid(String mxid) {
        this.mxid = mxid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThreePidMapping that = (ThreePidMapping) o;

        if (medium != null ? !medium.equals(that.medium) : that.medium != null) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = medium != null ? medium.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }

}
