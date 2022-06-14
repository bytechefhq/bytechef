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

package com.integri.atlas.task.handler.httpclient.v1_0.http;

import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.AuthType;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.FOLLOW_ALL_REDIRECTS;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.FOLLOW_REDIRECT;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.PROXY;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.RESPONSE_FORMAT;
import static com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.TIMEOUT;

import com.github.mizosoft.methanol.Methanol;
import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.task.execution.TaskExecution;
import com.integri.atlas.task.auth.TaskAuth;
import com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants;
import com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.BodyContentType;
import com.integri.atlas.task.handler.httpclient.HttpClientTaskConstants.RequestMethod;
import com.integri.atlas.task.handler.httpclient.v1_0.auth.Auth;
import com.integri.atlas.task.handler.httpclient.v1_0.auth.AuthRegistry;
import com.integri.atlas.task.handler.httpclient.v1_0.body.HttpBodyFactory;
import com.integri.atlas.task.handler.httpclient.v1_0.header.HttpHeader;
import com.integri.atlas.task.handler.httpclient.v1_0.header.HttpHeaderFactory;
import com.integri.atlas.task.handler.httpclient.v1_0.param.HttpQueryParam;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.integri.atlas.task.handler.httpclient.v1_0.param.HttpQueryParamFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class HttpClientHelper {

    private final HttpBodyFactory httpBodyFactory;
    private final HttpHeaderFactory httpHeaderFactory;
    private final HttpQueryParamFactory queryParamFactory;

    public HttpClientHelper(HttpBodyFactory httpBodyFactory, HttpHeaderFactory httpHeaderFactory, HttpQueryParamFactory queryParamFactory) {
        this.httpBodyFactory = httpBodyFactory;
        this.httpHeaderFactory = httpHeaderFactory;
        this.queryParamFactory = queryParamFactory;
    }

    public HttpResponse<?> send(TaskExecution taskExecution, RequestMethod requestMethod) throws Exception {
        HttpClient httpClient = buildHttpClient(taskExecution);

        List<HttpHeader> httpHeaders = httpHeaderFactory.getHttpHeaders(taskExecution);

        List<HttpQueryParam> queryParameters = queryParamFactory.getQueryParams(taskExecution);

        TaskAuth taskAuth = taskExecution.get(Constants.AUTH, TaskAuth.class);

        if (taskAuth != null) {
            Auth httpAuth = AuthRegistry.get(AuthType.valueOf(StringUtils.upperCase(taskAuth.getType())));

            httpAuth.apply(httpHeaders, queryParameters, taskAuth);
        }

        HttpRequest httpRequest = getHttpRequest(taskExecution, requestMethod, httpHeaders, queryParameters);

        return httpClient.send(httpRequest, getBodyHandler(taskExecution));
    }

    protected HttpClient buildHttpClient(TaskExecution taskExecution) {
        Methanol.Builder builder = Methanol
            .newBuilder()
            .version(HttpClient.Version.HTTP_1_1);

        boolean followRedirect = taskExecution.getBoolean(FOLLOW_REDIRECT, false);

        if (followRedirect) {
            builder.followRedirects(HttpClient.Redirect.NORMAL);
        }

        boolean followAllRedirects = taskExecution.getBoolean(FOLLOW_ALL_REDIRECTS, false);

        if (followAllRedirects) {
            builder.followRedirects(HttpClient.Redirect.ALWAYS);
        }

        String proxy = taskExecution.getString(PROXY);

        if (proxy != null) {
            String[] proxyAddress = proxy.split(":");

            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyAddress[0], Integer.parseInt(proxyAddress[1]))));
        }

        builder.connectTimeout(Duration.ofMillis(taskExecution.getLong(TIMEOUT, 10000)));

        return builder.build();
    }

    private HttpResponse.BodyHandler<?> getBodyHandler(TaskExecution taskExecution) {
        if (!taskExecution.containsKey(RESPONSE_FORMAT)) {
            return HttpResponse.BodyHandlers.discarding();
        }

        BodyContentType bodyContentType = BodyContentType.valueOf(taskExecution.getString(RESPONSE_FORMAT));

        if (bodyContentType == BodyContentType.BINARY) {
            return HttpResponse.BodyHandlers.ofInputStream();
        }

        return HttpResponse.BodyHandlers.ofString();
    }

    private HttpRequest getHttpRequest(TaskExecution taskExecution, RequestMethod requestMethod, List<HttpHeader> httpHeaders, List<HttpQueryParam> queryParameters) throws IOException {
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().method(requestMethod.name(), httpBodyFactory.getBodyPublisher(taskExecution, httpHeaders));

        for (HttpHeader httpHeader : httpHeaders) {
            httpRequestBuilder.header(httpHeader.getName(), httpHeader.getValue());
        }

        httpRequestBuilder.uri(resolveURI(taskExecution.getRequiredString(HttpClientTaskConstants.URI), queryParameters));

        return httpRequestBuilder.build();
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

    private URI resolveURI(String uri, List<HttpQueryParam> queryParameters) {
        if (queryParameters.isEmpty()) {
            return URI.create(uri);
        }

        return URI.create(uri + '?' + fromQueryParameters(queryParameters));
    }
}
