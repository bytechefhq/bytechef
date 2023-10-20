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

package com.integri.atlas.task.handler.http.client.auth;

import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_PASSWORD;
import static com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.PROPERTY_USERNAME;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.task.auth.TaskAuth;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class BasicHttpAuth implements HttpAuth {

    @Override
    public void apply(HttpRequest.Builder httpRequestBuilder, TaskAuth taskAuth) {
        httpRequestBuilder.header("Authorization", getHeaderValue(taskAuth.getProperties()));
    }

    private String getHeaderValue(Accessor properties) {
        String credentials = properties.getRequiredString(PROPERTY_USERNAME) + ":" + PROPERTY_PASSWORD;

        Base64.Encoder encoder = Base64.getEncoder();

        String base64Credentials = encoder.encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return "Basic " + base64Credentials;
    }
}
