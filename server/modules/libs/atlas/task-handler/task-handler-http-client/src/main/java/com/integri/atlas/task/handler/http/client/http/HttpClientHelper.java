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

package com.integri.atlas.task.handler.http.client.http;

import com.integri.atlas.task.handler.http.client.HttpClientTaskConstants.RequestMethod;
import com.integri.atlas.task.handler.http.client.authentication.HttpAuthentication;
import com.integri.atlas.task.handler.http.client.header.HttpHeader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * @author Matija Petanjek
 */
public class HttpClientHelper {

    private final HttpClient httpClient;
    private final HttpAuthentication httpAuthentication;

    public HttpClientHelper(HttpAuthentication httpAuthentication, long timeout) {
        this.httpAuthentication = httpAuthentication;

        httpClient =
            java.net.http.HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
    }

    public HttpResponse<?> send(
        RequestMethod requestMethod,
        String uri,
        List<HttpHeader> headers,
        HttpRequest.BodyPublisher bodyPublisher,
        HttpResponse.BodyHandler<?> bodyHandler
    ) throws Exception {
        HttpRequest.Builder httpRequestBuilder = HttpRequest
            .newBuilder()
            .method(requestMethod.name(), bodyPublisher)
            .uri(URI.create(uri))
            .header("Authorization", httpAuthentication.getAuthorizationHeader());

        for (HttpHeader httpHeader : headers) {
            httpRequestBuilder.header(httpHeader.getName(), httpHeader.getValue());
        }

        return httpClient.send(httpRequestBuilder.build(), bodyHandler);
    }
}
