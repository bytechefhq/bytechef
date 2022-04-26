/*
 * Copyright 2021 <your company/name>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.integri.atlas.task.handler.http.client.authentication;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author Matija Petanjek
 */
public class BasicAuthentication implements HttpAuthentication {

    public BasicAuthentication(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getAuthorizationHeader() {
        String credentials = username + ":" + password;

        Base64.Encoder encoder = Base64.getEncoder();

        String base64Credentials = encoder.encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return "Basic " + base64Credentials;
    }

    private final String password;
    private final String username;
}
