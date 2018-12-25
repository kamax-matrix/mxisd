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

package io.kamax.mxisd.http.io;

import java.util.HashSet;
import java.util.Set;

public class UserDirectorySearchResult {

    public static UserDirectorySearchResult empty() {
        return new UserDirectorySearchResult();
    }

    public static class Result {

        private String displayName;
        private String avatarUrl;
        private String userId;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Result result = (Result) o;

            if (displayName != null ? !displayName.equals(result.displayName) : result.displayName != null)
                return false;
            if (avatarUrl != null ? !avatarUrl.equals(result.avatarUrl) : result.avatarUrl != null) return false;
            return userId.equals(result.userId);
        }

        @Override
        public int hashCode() {
            int result = displayName != null ? displayName.hashCode() : 0;
            result = 31 * result + (avatarUrl != null ? avatarUrl.hashCode() : 0);
            result = 31 * result + userId.hashCode();
            return result;
        }

    }

    private boolean limited;
    private Set<Result> results = new HashSet<>();

    public boolean isLimited() {
        return limited;
    }

    public void setLimited(boolean limited) {
        this.limited = limited;
    }

    public Set<Result> getResults() {
        return results;
    }

    public void setResults(Set<Result> results) {
        this.results = results;
    }

    public void addResult(Result result) {
        this.results.add(result);
    }

}
