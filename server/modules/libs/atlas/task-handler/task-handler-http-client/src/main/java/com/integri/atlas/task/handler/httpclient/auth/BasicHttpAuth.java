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

package com.integri.atlas.task.handler.httpclient.auth;

import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.PASSWORD;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.USERNAME;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.handler.httpclient.header.HttpHeader;
import com.integri.atlas.task.handler.httpclient.params.HttpQueryParam;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class BasicHttpAuth implements HttpAuth {

    @Override
    public void apply(List<HttpHeader> headers, List<HttpQueryParam> queryParameters, TaskAuth taskAuth) {
        headers.add(new HttpHeader("Authorization", getHeaderValue(taskAuth.getProperties())));
    }

    private String getHeaderValue(Accessor properties) {
        String credentials = properties.getRequiredString(USERNAME) + ":" + PASSWORD;

        Base64.Encoder encoder = Base64.getEncoder();

        String base64Credentials = encoder.encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        return "Basic " + base64Credentials;
    }
}
