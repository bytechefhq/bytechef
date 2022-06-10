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

package com.integri.atlas.task.handler.httpclient.http;

import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.AuthType;

import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.RequestMethod;
import com.integri.atlas.task.handler.httpclient.auth.HttpAuth;
import com.integri.atlas.task.handler.httpclient.auth.HttpAuthRegistry;
import com.integri.atlas.task.handler.httpclient.header.HttpHeader;
import com.integri.atlas.task.handler.httpclient.params.HttpQueryParam;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Matija Petanjek
 */
public class HttpClientHelper {

    private final HttpClient httpClient;

    public HttpClientHelper(long timeout) {
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
        List<HttpQueryParam> queryParameters,
        HttpRequest.BodyPublisher bodyPublisher,
        HttpResponse.BodyHandler<?> bodyHandler,
        TaskAuth taskAuth
    ) throws Exception {
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().method(requestMethod.name(), bodyPublisher);

        if (taskAuth != null) {
            HttpAuth httpAuth = HttpAuthRegistry.get(AuthType.valueOf(StringUtils.upperCase(taskAuth.getType())));

            httpAuth.apply(headers, queryParameters, taskAuth);
        }

        for (HttpHeader httpHeader : headers) {
            httpRequestBuilder.header(httpHeader.getName(), httpHeader.getValue());
        }

        httpRequestBuilder.uri(URI.create(resolveURI(uri, queryParameters)));

        return httpClient.send(httpRequestBuilder.build(), bodyHandler);
    }

    private String fromQueryParameters(List<HttpQueryParam> queryParameters) {
        List<String> queryParameterList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        for (HttpQueryParam queryParam : queryParameters) {
            sb.append(queryParam.getName());
            sb.append("=");
            sb.append(queryParam.getValue());

            queryParameterList.add(sb.toString());
        }

        return StringUtils.join(queryParameterList, "&");
    }

    private String resolveURI(String uri, List<HttpQueryParam> queryParameters) {
        if (queryParameters.isEmpty()) {
            return uri;
        }

        return uri + '?' + fromQueryParameters(queryParameters);
    }
}
