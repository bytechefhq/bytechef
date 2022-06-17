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

package com.bytechef.task.handler.httpclient.v1_0.http;

import static com.bytechef.hermes.auth.AuthenticationConstants.AUTHENTICATION_ID;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.AuthType;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.FOLLOW_REDIRECT;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.PROXY;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.RESPONSE_FORMAT;
import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.TIMEOUT;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.auth.service.AuthenticationService;
import com.bytechef.task.handler.httpclient.HTTPClientTaskConstants;
import com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.BodyContentType;
import com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.RequestMethod;
import com.bytechef.task.handler.httpclient.v1_0.auth.Auth;
import com.bytechef.task.handler.httpclient.v1_0.auth.AuthRegistry;
import com.bytechef.task.handler.httpclient.v1_0.body.HTTPBodyFactory;
import com.bytechef.task.handler.httpclient.v1_0.header.HTTPHeader;
import com.bytechef.task.handler.httpclient.v1_0.header.HTTPHeaderFactory;
import com.bytechef.task.handler.httpclient.v1_0.param.HTTPQueryParam;
import com.bytechef.task.handler.httpclient.v1_0.param.HTTPQueryParamFactory;
import com.bytechef.task.handler.httpclient.v1_0.response.HTTPResponseHandler;
import com.github.mizosoft.methanol.Methanol;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class HTTPClientHelper {

    private final AuthenticationService authenticationService;
    private final HTTPBodyFactory httpBodyFactory;
    private final HTTPHeaderFactory httpHeaderFactory;
    private final HTTPQueryParamFactory queryParamFactory;
    private final HTTPResponseHandler httpResponseHandler;

    public HTTPClientHelper(
            AuthenticationService authenticationService,
            HTTPBodyFactory httpBodyFactory,
            HTTPHeaderFactory httpHeaderFactory,
            HTTPQueryParamFactory queryParamFactory,
            HTTPResponseHandler httpResponseHandler) {
        this.authenticationService = authenticationService;
        this.httpBodyFactory = httpBodyFactory;
        this.httpHeaderFactory = httpHeaderFactory;
        this.queryParamFactory = queryParamFactory;
        this.httpResponseHandler = httpResponseHandler;
    }

    public Object send(TaskExecution taskExecution, RequestMethod requestMethod) throws Exception {
        HttpClient httpClient = buildHTTPClient(taskExecution);

        List<HTTPHeader> httpHeaders = httpHeaderFactory.getHTTPHeaders(taskExecution);

        List<HTTPQueryParam> queryParameters = queryParamFactory.getQueryParams(taskExecution);

        Authentication authentication =
                authenticationService.fetchAuthentication(taskExecution.getString(AUTHENTICATION_ID));

        if (authentication != null) {
            Auth auth = AuthRegistry.get(AuthType.valueOf(StringUtils.upperCase(authentication.getType())));

            auth.apply(httpHeaders, queryParameters, authentication);
        }

        HttpRequest httpRequest = getHTTPRequest(taskExecution, requestMethod, httpHeaders, queryParameters);

        HttpResponse<?> httpResponse = httpClient.send(httpRequest, getBodyHandler(taskExecution));

        return httpResponseHandler.handle(taskExecution, httpResponse);
    }

    protected HttpClient buildHTTPClient(TaskExecution taskExecution) {
        Methanol.Builder builder = Methanol.newBuilder().version(HttpClient.Version.HTTP_1_1);

        if (taskExecution.getBoolean(ALLOW_UNAUTHORIZED_CERTS, false)) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                sslContext.init(
                        null,
                        new TrustManager[] {
                            new X509ExtendedTrustManager() {
                                public X509Certificate[] getAcceptedIssuers() {
                                    return null;
                                }

                                public void checkClientTrusted(
                                        final X509Certificate[] a_certificates, final String a_auth_type) {}

                                public void checkServerTrusted(
                                        final X509Certificate[] a_certificates, final String a_auth_type) {}

                                public void checkClientTrusted(
                                        final X509Certificate[] a_certificates,
                                        final String a_auth_type,
                                        final Socket a_socket) {}

                                public void checkServerTrusted(
                                        final X509Certificate[] a_certificates,
                                        final String a_auth_type,
                                        final Socket a_socket) {}

                                public void checkClientTrusted(
                                        final X509Certificate[] a_certificates,
                                        final String a_auth_type,
                                        final SSLEngine a_engine) {}

                                public void checkServerTrusted(
                                        final X509Certificate[] a_certificates,
                                        final String a_auth_type,
                                        final SSLEngine a_engine) {}
                            }
                        },
                        null);

                builder.sslContext(sslContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (taskExecution.getBoolean(FOLLOW_REDIRECT, false)) {
            builder.followRedirects(HttpClient.Redirect.NORMAL);
        }

        if (taskExecution.getBoolean(FOLLOW_ALL_REDIRECTS, false)) {
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

    private HttpRequest getHTTPRequest(
            TaskExecution taskExecution,
            RequestMethod requestMethod,
            List<HTTPHeader> httpHeaders,
            List<HTTPQueryParam> queryParameters)
            throws IOException {
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .method(requestMethod.name(), httpBodyFactory.getBodyPublisher(taskExecution, httpHeaders));

        for (HTTPHeader httpHeader : httpHeaders) {
            httpRequestBuilder.header(httpHeader.getName(), httpHeader.getValue());
        }

        httpRequestBuilder.uri(
                resolveURI(taskExecution.getRequiredString(HTTPClientTaskConstants.URI), queryParameters));

        return httpRequestBuilder.build();
    }

    private String fromQueryParameters(List<HTTPQueryParam> queryParameters) {
        List<String> queryParameterList = new ArrayList<>();

        StringBuilder sb = new StringBuilder();

        for (HTTPQueryParam queryParam : queryParameters) {
            sb.append(queryParam.getName());
            sb.append("=");
            sb.append(queryParam.getValue());

            queryParameterList.add(sb.toString());
        }

        return StringUtils.join(queryParameterList, "&");
    }

    private URI resolveURI(String uri, List<HTTPQueryParam> queryParameters) {
        if (queryParameters.isEmpty()) {
            return URI.create(uri);
        }

        return URI.create(uri + '?' + fromQueryParameters(queryParameters));
    }
}
