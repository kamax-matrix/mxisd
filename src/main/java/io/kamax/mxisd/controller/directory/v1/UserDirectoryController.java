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

package io.kamax.mxisd.controller.directory.v1;

import com.google.gson.Gson;
import io.kamax.mxisd.controller.directory.v1.io.UserDirectorySearchRequest;
import io.kamax.mxisd.controller.directory.v1.io.UserDirectorySearchResult;
import io.kamax.mxisd.directory.DirectoryManager;
import io.kamax.mxisd.util.GsonParser;
import io.kamax.mxisd.util.GsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;

@RestController
@CrossOrigin
@RequestMapping(path = "/_matrix/client/r0/user_directory", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class UserDirectoryController {

    private Gson gson = GsonUtil.build();
    private GsonParser parser = new GsonParser(gson);

    @Autowired
    private DirectoryManager mgr;

    @RequestMapping(path = "/search", method = RequestMethod.POST)
    public String search(HttpServletRequest request, @RequestParam("access_token") String accessToken) throws IOException {
        UserDirectorySearchRequest searchQuery = parser.parse(request, UserDirectorySearchRequest.class);
        URI target = URI.create(request.getRequestURL().toString());
        UserDirectorySearchResult result = mgr.search(target, accessToken, searchQuery.getSearchTerm());
        return gson.toJson(result);
    }

}
