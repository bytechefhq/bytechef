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

package com.bytechef.hermes.component.http.client;

import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;
import static com.bytechef.hermes.component.constants.ComponentConstants.KEY;
import static com.bytechef.hermes.component.constants.ComponentConstants.VALUE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.BODY_CONTENT_TYPE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.BODY_PARAMETERS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.FOLLOW_REDIRECT;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.FULL_RESPONSE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.HEADER_PARAMETERS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.MIME_TYPE;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.PROXY;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.QUERY_PARAMETERS;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.RESPONSE_FILENAME;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.RESPONSE_FORMAT;
import static com.bytechef.hermes.component.http.client.constants.HttpClientConstants.TIMEOUT;

import com.bytechef.commons.json.JsonUtils;
import com.bytechef.commons.xml.XmlUtils;
import com.bytechef.hermes.component.AuthorizationContext;
import com.bytechef.hermes.component.ConnectionParameters;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.http.client.constants.HttpClientConstants;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public class HttpClient {

    public enum BodyContentType {
        BINARY,
        FORM_DATA,
        FORM_URLENCODED,
        JSON,
        RAW,
        XML
    }

    public enum ResponseFormat {
        FILE,
        JSON,
        TEXT,
        XML,
    }

    public enum RequestMethod {
        DELETE,
        GET,
        HEAD,
        PATCH,
        POST,
        PUT,
    }

    public Object execute(Context context, ExecutionParameters executionParameters, RequestMethod requestMethod)
            throws Exception {
        Map<String, List<String>> headers = getNameValuesMap(executionParameters, HEADER_PARAMETERS);

        Map<String, List<String>> queryParams = getNameValuesMap(executionParameters, QUERY_PARAMETERS);

        java.net.http.HttpClient httpClient = createHTTPClient(context, executionParameters, headers, queryParams);

        HttpRequest httpRequest = createHTTPRequest(context, executionParameters, requestMethod, headers, queryParams);

        HttpResponse<?> httpResponse = httpClient.send(httpRequest, createBodyHandler(executionParameters));

        return handleResponse(context, executionParameters, httpResponse);
    }

    protected HttpResponse.BodyHandler<?> createBodyHandler(ExecutionParameters executionParameters) {
        HttpResponse.BodyHandler<?> bodyHandler;

        if (executionParameters.containsKey(RESPONSE_FORMAT)) {
            ResponseFormat responseFormat = ResponseFormat.valueOf(executionParameters.getString(RESPONSE_FORMAT));

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
    protected HttpRequest.BodyPublisher createBodyPublisher(Context context, ExecutionParameters executionParameters) {
        HttpRequest.BodyPublisher bodyPublisher;

        BodyContentType bodyContentType = BodyContentType.valueOf(
                StringUtils.upperCase(executionParameters.getString(BODY_CONTENT_TYPE, "JSON")));

        if (executionParameters.containsKey(BODY_PARAMETERS)) {
            if (bodyContentType == BodyContentType.FORM_DATA) {
                List<Map<String, ?>> bodyParameters = executionParameters.getList(BODY_PARAMETERS);

                MultipartBodyPublisher.Builder builder = MultipartBodyPublisher.newBuilder();

                for (Map<String, ?> parameter : bodyParameters) {
                    if (parameter.get(VALUE) instanceof Map
                            && FileEntry.isFileEntry((Map<String, String>) parameter.get(VALUE))) {
                        FileEntry fileEntry = executionParameters.getFileEntry(parameter, VALUE);

                        builder.formPart(
                                (String) parameter.get(KEY),
                                fileEntry.getName(),
                                MoreBodyPublishers.ofMediaType(
                                        HttpRequest.BodyPublishers.ofInputStream(
                                                () -> context.getFileStream(fileEntry)),
                                        MediaType.parse(fileEntry.getMimeType())));
                    } else {
                        builder.textPart((String) parameter.get(KEY), parameter.get(VALUE));
                    }
                }

                bodyPublisher = builder.build();
            } else if (bodyContentType == BodyContentType.FORM_URLENCODED) {
                List<Map<String, String>> bodyParameters = executionParameters.getList(BODY_PARAMETERS);

                FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();

                for (Map<String, String> parameter : bodyParameters) {
                    builder.query(parameter.get(KEY), parameter.get(VALUE));
                }

                bodyPublisher = builder.build();
            } else if (bodyContentType == BodyContentType.JSON) {
                bodyPublisher = MoreBodyPublishers.ofMediaType(
                        HttpRequest.BodyPublishers.ofString(
                                JsonUtils.write(executionParameters.getMap(BODY_PARAMETERS))),
                        MediaType.APPLICATION_JSON);
            } else if (bodyContentType == BodyContentType.XML) {
                bodyPublisher = MoreBodyPublishers.ofMediaType(
                        HttpRequest.BodyPublishers.ofString(
                                XmlUtils.write(executionParameters.getMap(BODY_PARAMETERS))),
                        MediaType.APPLICATION_XML);
            } else {
                MediaType mediaType;

                if (executionParameters.containsKey(MIME_TYPE)) {
                    mediaType = MediaType.parse(executionParameters.getString(MIME_TYPE));
                } else {
                    mediaType = MediaType.TEXT_PLAIN;
                }

                bodyPublisher = MoreBodyPublishers.ofMediaType(
                        HttpRequest.BodyPublishers.ofString(executionParameters.getString(BODY_PARAMETERS)), mediaType);
            }
        } else if (executionParameters.containsKey(FILE_ENTRY)) {
            FileEntry fileEntry = executionParameters.getFileEntry(FILE_ENTRY);
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
                    HttpRequest.BodyPublishers.ofInputStream(() -> context.getFileStream(fileEntry)), mediaType);
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }

        return bodyPublisher;
    }

    protected java.net.http.HttpClient createHTTPClient(
            Context context,
            ExecutionParameters executionParameters,
            Map<String, List<String>> headers,
            Map<String, List<String>> queryParams) {
        Methanol.Builder builder = Methanol.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1);

        if (executionParameters.getBoolean(ALLOW_UNAUTHORIZED_CERTS, false)) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                sslContext.init(null, new TrustManager[] {new UnauthorizedCertsX509ExtendedTrustManager()}, null);

                builder.sslContext(sslContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        context.fetchConnectionParameters().ifPresent(connectionParameters -> {
            ConnectionDefinition connectionDefinition = context.getConnectionDefinition();

            BiConsumer<AuthorizationContext, ConnectionParameters> applyConsumer = null;

            if (connectionDefinition != null) {
                for (Authorization authorization : connectionDefinition.getAuthorizations()) {
                    if (Objects.equals(authorization.getName(), connectionParameters.getAuthorizationName())) {
                        applyConsumer = authorization.getApplyConsumer();

                        break;
                    }
                }
            }

            if (applyConsumer != null) {
                applyConsumer.accept(new AuthorizationContextImpl(builder, headers, queryParams), connectionParameters);
            }
        });

        if (executionParameters.getBoolean(FOLLOW_REDIRECT, false)) {
            builder.followRedirects(java.net.http.HttpClient.Redirect.NORMAL);
        }

        if (executionParameters.getBoolean(FOLLOW_ALL_REDIRECTS, false)) {
            builder.followRedirects(java.net.http.HttpClient.Redirect.ALWAYS);
        }

        String proxy = executionParameters.getString(PROXY);

        if (proxy != null) {
            String[] proxyAddress = proxy.split(":");

            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyAddress[0], Integer.parseInt(proxyAddress[1]))));
        }

        builder.connectTimeout(Duration.ofMillis(executionParameters.getInteger(TIMEOUT, 10000)));

        return builder.build();
    }

    protected HttpRequest createHTTPRequest(
            Context context,
            ExecutionParameters executionParameters,
            RequestMethod requestMethod,
            Map<String, List<String>> headers,
            Map<String, List<String>> queryParams) {

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                .method(requestMethod.name(), createBodyPublisher(context, executionParameters));

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                httpRequestBuilder.header(entry.getKey(), value);
            }
        }

        httpRequestBuilder.uri(createURI(executionParameters.getRequiredString(HttpClientConstants.URI), queryParams));

        return httpRequestBuilder.build();
    }

    protected Object handleResponse(
            Context context, ExecutionParameters executionParameters, HttpResponse<?> httpResponse) throws Exception {
        Object body = null;

        if (executionParameters.getString(RESPONSE_FORMAT) != null) {
            ResponseFormat responseFormat = ResponseFormat.valueOf(executionParameters.getString(RESPONSE_FORMAT));

            if (responseFormat == ResponseFormat.FILE) {
                String filename = executionParameters.getString(RESPONSE_FILENAME);

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

                body = context.storeFileContent(filename, (InputStream) httpResponse.body());
            } else if (responseFormat == ResponseFormat.JSON) {
                body = JsonUtils.read(httpResponse.body().toString());
            } else if (responseFormat == ResponseFormat.TEXT) {
                body = httpResponse.body().toString();
            } else {
                body = XmlUtils.read(httpResponse.body().toString());
            }

            if (executionParameters.getBoolean(FULL_RESPONSE, false)) {
                body = new HttpResponseEntry(body, httpResponse.headers().map(), httpResponse.statusCode());
            }
        }

        return body;
    }

    private URI createURI(String uriString, Map<String, List<String>> queryParams) {
        URI uri;

        if (queryParams.isEmpty()) {
            uri = URI.create(uriString);
        } else {
            String queryParamsString = queryParams.entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream().map(value -> entry.getKey() + "=" + value))
                    .collect(Collectors.joining("&"));

            uri = URI.create(uriString + '?' + queryParamsString);
        }

        return uri;
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> getNameValuesMap(ExecutionParameters executionParameters, String propertyKey) {
        Map<String, List<String>> nameValuesMap = new HashMap<>();

        if (executionParameters.containsKey(propertyKey)) {
            List<Map<String, String>> properties = executionParameters.getList(propertyKey);

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

    @SuppressFBWarnings({"EI", "EI2"})
    public record HttpResponseEntry(Object body, Map<String, List<String>> headers, int statusCode) {}

    private static class AuthorizationContextImpl implements AuthorizationContext {

        private Methanol.Builder builder;
        private Map<String, List<String>> headers;
        private Map<String, List<String>> queryParams;

        public AuthorizationContextImpl(
                Methanol.Builder builder, Map<String, List<String>> headers, Map<String, List<String>> queryParams) {
            this.builder = builder;
            this.headers = headers;
            this.queryParams = queryParams;
        }

        @Override
        public void setHeaders(Map<String, List<String>> headers) {
            this.headers.putAll(headers);
        }

        @Override
        public void setQueryParameters(Map<String, List<String>> queryParams) {
            this.queryParams.putAll(queryParams);
        }

        @Override
        public void setUsernamePassword(String username, String password) {
            this.builder.authenticator(new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password.toCharArray());
                }
            });
        }
    }
}
