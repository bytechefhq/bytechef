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
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.AuthType;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.BODY_CONTENT_TYPE;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.BODY_PARAMETERS;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.FILE_ENTRY;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.FOLLOW_REDIRECT;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.FULL_RESPONSE;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.HEADER_PARAMETERS;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.KEY;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.MIME_TYPE;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.PROXY;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.QUERY_PARAMETERS;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.RESPONSE_FILE_NAME;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.RESPONSE_FORMAT;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.ResponseFormat;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.TIMEOUT;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.URI;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.VALUE;

import com.bytechef.atlas.task.execution.domain.TaskExecution;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.auth.service.AuthenticationService;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.task.commons.json.JsonHelper;
import com.bytechef.task.commons.xml.XmlHelper;
import com.bytechef.task.handler.httpclient.HttpClientTaskConstants.BodyContentType;
import com.bytechef.task.handler.httpclient.HttpClientTaskConstants.RequestMethod;
import com.bytechef.task.handler.httpclient.v1_0.auth.AuthResolver;
import com.bytechef.task.handler.httpclient.v1_0.auth.AuthResolverRegistry;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import java.io.InputStream;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component
public class HttpClientHelper {
    private final AuthenticationService authenticationService;
    private final FileStorageService fileStorageService;
    private final JsonHelper jsonHelper;
    private final XmlHelper xmlHelper;

    public HttpClientHelper(
            AuthenticationService authenticationService,
            FileStorageService fileStorageService,
            JsonHelper jsonHelper,
            XmlHelper xmlHelper) {
        this.authenticationService = authenticationService;
        this.fileStorageService = fileStorageService;
        this.jsonHelper = jsonHelper;
        this.xmlHelper = xmlHelper;
    }

    public Object send(TaskExecution taskExecution, RequestMethod requestMethod) throws Exception {
        Map<String, List<String>> headers = getNameValuesMap(taskExecution, HEADER_PARAMETERS);

        Map<String, List<String>> queryParams = getNameValuesMap(taskExecution, QUERY_PARAMETERS);

        HttpClient httpClient = createHTTPClient(taskExecution, headers, queryParams);

        HttpRequest httpRequest = createHTTPRequest(taskExecution, requestMethod, headers, queryParams);

        HttpResponse<?> httpResponse = httpClient.send(httpRequest, createBodyHandler(taskExecution));

        return handleResponse(taskExecution, httpResponse);
    }

    protected HttpResponse.BodyHandler<?> createBodyHandler(TaskExecution taskExecution) {
        HttpResponse.BodyHandler<?> bodyHandler;

        if (taskExecution.containsKey(RESPONSE_FORMAT)) {
            ResponseFormat responseFormat = ResponseFormat.valueOf(taskExecution.getString(RESPONSE_FORMAT));

            if (responseFormat == ResponseFormat.FILE) {
                bodyHandler = HttpResponse.BodyHandlers.ofInputStream();
            } else {
                bodyHandler = HttpResponse.BodyHandlers.ofString();
            }
        } else {
            bodyHandler = HttpResponse.BodyHandlers.discarding();
        }

        return bodyHandler;
    }

    @SuppressWarnings("unchecked")
    protected HttpRequest.BodyPublisher createBodyPublisher(TaskExecution taskExecution) {
        HttpRequest.BodyPublisher bodyPublisher;

        BodyContentType bodyContentType =
                BodyContentType.valueOf(StringUtils.upperCase(taskExecution.getString(BODY_CONTENT_TYPE, "JSON")));

        if (taskExecution.containsKey(BODY_PARAMETERS)) {
            if (bodyContentType == BodyContentType.FORM_DATA) {
                List<Map<String, ?>> bodyParameters = taskExecution.get(BODY_PARAMETERS, List.class);

                MultipartBodyPublisher.Builder builder = MultipartBodyPublisher.newBuilder();

                for (Map<String, ?> parameter : bodyParameters) {
                    if (parameter.get(VALUE) instanceof Map
                            && FileEntry.isFileEntry((Map<String, String>) parameter.get(VALUE))) {
                        FileEntry fileEntry = FileEntry.of((Map<String, String>) parameter.get(VALUE));

                        builder.formPart(
                                (String) parameter.get(KEY),
                                fileEntry.getName(),
                                MoreBodyPublishers.ofMediaType(
                                        HttpRequest.BodyPublishers.ofInputStream(
                                                () -> fileStorageService.getFileContentStream(fileEntry.getUrl())),
                                        MediaType.parse(fileEntry.getMimeType())));
                    } else {
                        builder.textPart((String) parameter.get(KEY), parameter.get(VALUE));
                    }
                }

                bodyPublisher = builder.build();
            } else if (bodyContentType == BodyContentType.FORM_URLENCODED) {
                List<Map<String, String>> bodyParameters = taskExecution.get(BODY_PARAMETERS, List.class);

                FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();

                for (Map<String, String> parameter : bodyParameters) {
                    builder.query(parameter.get(KEY), parameter.get(VALUE));
                }

                bodyPublisher = builder.build();
            } else if (bodyContentType == BodyContentType.JSON) {
                bodyPublisher = MoreBodyPublishers.ofMediaType(
                        HttpRequest.BodyPublishers.ofString(jsonHelper.write(taskExecution.get(BODY_PARAMETERS))),
                        MediaType.APPLICATION_JSON);
            } else if (bodyContentType == BodyContentType.XML) {
                bodyPublisher = MoreBodyPublishers.ofMediaType(
                        HttpRequest.BodyPublishers.ofString(xmlHelper.write(taskExecution.get(BODY_PARAMETERS))),
                        MediaType.APPLICATION_XML);
            } else {
                MediaType mediaType;

                if (taskExecution.containsKey(MIME_TYPE)) {
                    mediaType = MediaType.parse(taskExecution.get(MIME_TYPE));
                } else {
                    mediaType = MediaType.TEXT_PLAIN;
                }

                bodyPublisher = MoreBodyPublishers.ofMediaType(
                        HttpRequest.BodyPublishers.ofString(taskExecution.get(BODY_PARAMETERS)), mediaType);
            }
        } else if (taskExecution.containsKey(FILE_ENTRY)) {
            FileEntry fileEntry = taskExecution.get(FILE_ENTRY, FileEntry.class);
            MediaType mediaType;

            if (bodyContentType == BodyContentType.BINARY) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            } else if (bodyContentType == BodyContentType.JSON) {
                mediaType = MediaType.APPLICATION_JSON;
            } else if (bodyContentType == BodyContentType.XML) {
                mediaType = MediaType.APPLICATION_XML;
            } else {
                mediaType = MediaType.TEXT_PLAIN;
            }

            bodyPublisher = MoreBodyPublishers.ofMediaType(
                    HttpRequest.BodyPublishers.ofInputStream(
                            () -> fileStorageService.getFileContentStream(fileEntry.getUrl())),
                    mediaType);
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }

        return bodyPublisher;
    }

    protected HttpClient createHTTPClient(
            TaskExecution taskExecution, Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
        Methanol.Builder builder = Methanol.newBuilder().version(HttpClient.Version.HTTP_1_1);

        if (taskExecution.getBoolean(ALLOW_UNAUTHORIZED_CERTS, false)) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                sslContext.init(null, new TrustManager[] {new UnauthorizedCertsX509ExtendedTrustManager()}, null);

                builder.sslContext(sslContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (taskExecution.containsKey(AUTHENTICATION_ID)) {
            Authentication authentication =
                    authenticationService.fetchAuthentication(taskExecution.getString(AUTHENTICATION_ID));

            if (authentication != null) {
                AuthResolver authResolver =
                        AuthResolverRegistry.get(AuthType.valueOf(StringUtils.upperCase(authentication.getType())));

                authResolver.apply(builder, headers, queryParams, authentication);
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

    protected HttpRequest createHTTPRequest(
            TaskExecution taskExecution,
            RequestMethod requestMethod,
            Map<String, List<String>> headers,
            Map<String, List<String>> queryParams) {

        HttpRequest.Builder httpRequestBuilder =
                HttpRequest.newBuilder().method(requestMethod.name(), createBodyPublisher(taskExecution));

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                httpRequestBuilder.header(entry.getKey(), value);
            }
        }

        httpRequestBuilder.uri(createURI(taskExecution.getRequiredString(URI), queryParams));

        return httpRequestBuilder.build();
    }

    protected Object handleResponse(TaskExecution taskExecution, HttpResponse<?> httpResponse) throws Exception {
        Object body = null;

        if (taskExecution.getString(RESPONSE_FORMAT) != null) {
            ResponseFormat responseFormat = ResponseFormat.valueOf(taskExecution.getString(RESPONSE_FORMAT));

            if (responseFormat == ResponseFormat.FILE) {
                String filename = taskExecution.getString(RESPONSE_FILE_NAME);

                if (StringUtils.isEmpty(filename)) {
                    Map<String, List<String>> headersMap =
                            httpResponse.headers().map();

                    if (headersMap.containsKey("Content-Type")) {
                        MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();

                        List<String> values = headersMap.get("Content-Type");

                        MimeType mimeType = mimeTypes.forName(values.get(0));

                        filename = "file" + mimeType.getExtension();
                    } else {
                        filename = "file.txt";
                    }
                }

                body = fileStorageService.storeFileContent(filename, (InputStream) httpResponse.body());
            } else if (responseFormat == ResponseFormat.JSON) {
                body = jsonHelper.read(httpResponse.body().toString());
            } else if (responseFormat == ResponseFormat.TEXT) {
                body = httpResponse.body().toString();
            } else {
                body = xmlHelper.read(httpResponse.body().toString());
            }

            if (taskExecution.getBoolean(FULL_RESPONSE, false)) {
                body = new HttpResponseEntry(body, httpResponse.headers().map(), httpResponse.statusCode());
            }
        }

        return body;
    }

    private URI createURI(String uriString, Map<String, List<String>> queryParams) {
        URI uri;

        if (queryParams.isEmpty()) {
            uri = java.net.URI.create(uriString);
        } else {
            String queryParamsString = queryParams.entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream().map(value -> entry.getKey() + "=" + value))
                    .collect(Collectors.joining("&"));

            uri = java.net.URI.create(uriString + '?' + queryParamsString);
        }

        return uri;
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> getNameValuesMap(TaskExecution taskExecution, String propertyKey) {
        Map<String, List<String>> nameValuesMap = new HashMap<>();

        if (taskExecution.containsKey(propertyKey)) {
            List<Map<String, String>> properties = taskExecution.get(propertyKey, List.class);

            for (Map<String, String> property : properties) {
                nameValuesMap.compute(property.get(KEY), (key, values) -> {
                    if (values == null) {
                        values = new ArrayList<>();
                    }

                    values.add(property.get(VALUE));

                    return values;
                });
            }
        }

        return nameValuesMap;
    }

    private static class UnauthorizedCertsX509ExtendedTrustManager extends X509ExtendedTrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(final X509Certificate[] a_certificates, final String a_auth_type) {}

        public void checkServerTrusted(final X509Certificate[] a_certificates, final String a_auth_type) {}

        public void checkClientTrusted(
                final X509Certificate[] a_certificates, final String a_auth_type, final Socket a_socket) {}

        public void checkServerTrusted(
                final X509Certificate[] a_certificates, final String a_auth_type, final Socket a_socket) {}

        public void checkClientTrusted(
                final X509Certificate[] a_certificates, final String a_auth_type, final SSLEngine a_engine) {}

        public void checkServerTrusted(
                final X509Certificate[] a_certificates, final String a_auth_type, final SSLEngine a_engine) {}
    }

    public record HttpResponseEntry(Object body, Map<String, List<String>> headers, int statusCode) {}
}
