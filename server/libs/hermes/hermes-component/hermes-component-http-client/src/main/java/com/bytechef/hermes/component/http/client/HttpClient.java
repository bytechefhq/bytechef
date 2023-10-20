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

import com.bytechef.commons.json.JsonUtils;
import com.bytechef.commons.xml.XmlUtils;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.http.client.auth.resolver.AuthResolver;
import com.bytechef.hermes.component.http.client.auth.resolver.AuthResolverRegistry;
import com.bytechef.hermes.component.http.client.constants.HttpClientConstants;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.net.InetSocketAddress;
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

    public Object execute(
            Context context, ExecutionParameters executionParameters, HttpClientConstants.RequestMethod requestMethod)
            throws Exception {
        Map<String, List<String>> headers =
                getNameValuesMap(executionParameters, HttpClientConstants.HEADER_PARAMETERS);

        Map<String, List<String>> queryParams =
                getNameValuesMap(executionParameters, HttpClientConstants.QUERY_PARAMETERS);

        java.net.http.HttpClient httpClient = createHTTPClient(context, executionParameters, headers, queryParams);

        HttpRequest httpRequest = createHTTPRequest(context, executionParameters, requestMethod, headers, queryParams);

        HttpResponse<?> httpResponse = httpClient.send(httpRequest, createBodyHandler(executionParameters));

        return handleResponse(context, executionParameters, httpResponse);
    }

    protected HttpResponse.BodyHandler<?> createBodyHandler(ExecutionParameters executionParameters) {
        HttpResponse.BodyHandler<?> bodyHandler;

        if (executionParameters.containsKey(HttpClientConstants.RESPONSE_FORMAT)) {
            HttpClientConstants.ResponseFormat responseFormat = HttpClientConstants.ResponseFormat.valueOf(
                    executionParameters.getString(HttpClientConstants.RESPONSE_FORMAT));

            if (responseFormat == HttpClientConstants.ResponseFormat.FILE) {
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

        HttpClientConstants.BodyContentType bodyContentType = HttpClientConstants.BodyContentType.valueOf(
                StringUtils.upperCase(executionParameters.getString(HttpClientConstants.BODY_CONTENT_TYPE, "JSON")));

        if (executionParameters.containsKey(HttpClientConstants.BODY_PARAMETERS)) {
            if (bodyContentType == HttpClientConstants.BodyContentType.FORM_DATA) {
                List<Map<String, ?>> bodyParameters = executionParameters.getList(HttpClientConstants.BODY_PARAMETERS);

                MultipartBodyPublisher.Builder builder = MultipartBodyPublisher.newBuilder();

                for (Map<String, ?> parameter : bodyParameters) {
                    if (parameter.get(HttpClientConstants.VALUE) instanceof Map
                            && FileEntry.isFileEntry((Map<String, String>) parameter.get(HttpClientConstants.VALUE))) {
                        FileEntry fileEntry = executionParameters.getFileEntry(parameter, HttpClientConstants.VALUE);

                        builder.formPart(
                                (String) parameter.get(HttpClientConstants.KEY),
                                fileEntry.getName(),
                                MoreBodyPublishers.ofMediaType(
                                        HttpRequest.BodyPublishers.ofInputStream(
                                                () -> context.getFileStream(fileEntry)),
                                        MediaType.parse(fileEntry.getMimeType())));
                    } else {
                        builder.textPart(
                                (String) parameter.get(HttpClientConstants.KEY),
                                parameter.get(HttpClientConstants.VALUE));
                    }
                }

                bodyPublisher = builder.build();
            } else if (bodyContentType == HttpClientConstants.BodyContentType.FORM_URLENCODED) {
                List<Map<String, String>> bodyParameters =
                        executionParameters.getList(HttpClientConstants.BODY_PARAMETERS);

                FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();

                for (Map<String, String> parameter : bodyParameters) {
                    builder.query(parameter.get(HttpClientConstants.KEY), parameter.get(HttpClientConstants.VALUE));
                }

                bodyPublisher = builder.build();
            } else if (bodyContentType == HttpClientConstants.BodyContentType.JSON) {
                bodyPublisher = MoreBodyPublishers.ofMediaType(
                        HttpRequest.BodyPublishers.ofString(
                                JsonUtils.write(executionParameters.getMap(HttpClientConstants.BODY_PARAMETERS))),
                        MediaType.APPLICATION_JSON);
            } else if (bodyContentType == HttpClientConstants.BodyContentType.XML) {
                bodyPublisher = MoreBodyPublishers.ofMediaType(
                        HttpRequest.BodyPublishers.ofString(
                                XmlUtils.write(executionParameters.getMap(HttpClientConstants.BODY_PARAMETERS))),
                        MediaType.APPLICATION_XML);
            } else {
                MediaType mediaType;

                if (executionParameters.containsKey(HttpClientConstants.MIME_TYPE)) {
                    mediaType = MediaType.parse(executionParameters.getString(HttpClientConstants.MIME_TYPE));
                } else {
                    mediaType = MediaType.TEXT_PLAIN;
                }

                bodyPublisher = MoreBodyPublishers.ofMediaType(
                        HttpRequest.BodyPublishers.ofString(
                                executionParameters.getString(HttpClientConstants.BODY_PARAMETERS)),
                        mediaType);
            }
        } else if (executionParameters.containsKey(FILE_ENTRY)) {
            FileEntry fileEntry = executionParameters.getFileEntry(FILE_ENTRY);
            MediaType mediaType;

            if (bodyContentType == HttpClientConstants.BodyContentType.BINARY) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            } else if (bodyContentType == HttpClientConstants.BodyContentType.JSON) {
                mediaType = MediaType.APPLICATION_JSON;
            } else if (bodyContentType == HttpClientConstants.BodyContentType.XML) {
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

        if (executionParameters.getBoolean(HttpClientConstants.ALLOW_UNAUTHORIZED_CERTS, false)) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                sslContext.init(null, new TrustManager[] {new UnauthorizedCertsX509ExtendedTrustManager()}, null);

                builder.sslContext(sslContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        context.fetchConnection().ifPresent(connectionParameters -> {
            AuthResolver authResolver = AuthResolverRegistry.get(
                    HttpClientConstants.AuthType.valueOf(StringUtils.upperCase(connectionParameters.getName())));

            authResolver.apply(builder, headers, queryParams, connectionParameters);
        });

        if (executionParameters.getBoolean(HttpClientConstants.FOLLOW_REDIRECT, false)) {
            builder.followRedirects(java.net.http.HttpClient.Redirect.NORMAL);
        }

        if (executionParameters.getBoolean(HttpClientConstants.FOLLOW_ALL_REDIRECTS, false)) {
            builder.followRedirects(java.net.http.HttpClient.Redirect.ALWAYS);
        }

        String proxy = executionParameters.getString(HttpClientConstants.PROXY);

        if (proxy != null) {
            String[] proxyAddress = proxy.split(":");

            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyAddress[0], Integer.parseInt(proxyAddress[1]))));
        }

        builder.connectTimeout(Duration.ofMillis(executionParameters.getInteger(HttpClientConstants.TIMEOUT, 10000)));

        return builder.build();
    }

    protected HttpRequest createHTTPRequest(
            Context context,
            ExecutionParameters executionParameters,
            HttpClientConstants.RequestMethod requestMethod,
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

        if (executionParameters.getString(HttpClientConstants.RESPONSE_FORMAT) != null) {
            HttpClientConstants.ResponseFormat responseFormat = HttpClientConstants.ResponseFormat.valueOf(
                    executionParameters.getString(HttpClientConstants.RESPONSE_FORMAT));

            if (responseFormat == HttpClientConstants.ResponseFormat.FILE) {
                String filename = executionParameters.getString(HttpClientConstants.RESPONSE_FILENAME);

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
            } else if (responseFormat == HttpClientConstants.ResponseFormat.JSON) {
                body = JsonUtils.read(httpResponse.body().toString());
            } else if (responseFormat == HttpClientConstants.ResponseFormat.TEXT) {
                body = httpResponse.body().toString();
            } else {
                body = XmlUtils.read(httpResponse.body().toString());
            }

            if (executionParameters.getBoolean(HttpClientConstants.FULL_RESPONSE, false)) {
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
                nameValuesMap.compute(property.get(HttpClientConstants.KEY), (key, values) -> {
                    if (values == null) {
                        values = new ArrayList<>();
                    }

                    values.add(property.get(HttpClientConstants.VALUE));

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
}
