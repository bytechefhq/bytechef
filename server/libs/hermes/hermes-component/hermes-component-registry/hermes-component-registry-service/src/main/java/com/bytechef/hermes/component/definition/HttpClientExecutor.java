
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

package com.bytechef.hermes.component.definition;

import com.bytechef.commons.util.MimeTypeUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.definition.Authorization.ApplyResponse;
import com.bytechef.hermes.component.definition.Context.FileEntry;
import com.bytechef.hermes.component.definition.Context.Http;
import com.bytechef.hermes.component.definition.Context.Http.Body;
import com.bytechef.hermes.component.definition.Context.Http.Configuration;
import com.bytechef.hermes.component.definition.Context.Http.RequestMethod;
import com.bytechef.hermes.component.definition.Context.Http.Response;
import com.bytechef.hermes.component.definition.Context.Http.ResponseType;
import com.bytechef.hermes.component.registry.service.RemoteConnectionDefinitionService;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.execution.constants.FileEntryConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Component
public class HttpClientExecutor {

    private final RemoteConnectionDefinitionService connectionDefinitionService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    @SuppressFBWarnings("EI")
    public HttpClientExecutor(
        RemoteConnectionDefinitionService connectionDefinitionService, FileStorageService fileStorageService,
        ObjectMapper objectMapper, XmlMapper xmlMapper) {

        this.connectionDefinitionService = connectionDefinitionService;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.xmlMapper = xmlMapper;
    }

    public Response execute(
        String urlString, Map<String, List<String>> headers, Map<String, List<String>> queryParameters, Body body,
        Configuration configuration, RequestMethod requestMethod, Connection connection) throws Exception {

        HttpClient httpClient = createHttpClient(headers, queryParameters, configuration, connection);
        HttpRequest httpRequest = createHTTPRequest(
            urlString, requestMethod, headers, queryParameters, body, connection);

        HttpResponse<?> httpResponse = httpClient.send(httpRequest, createBodyHandler(configuration));

        return handleResponse(httpResponse, configuration);
    }

    HttpResponse.BodyHandler<?> createBodyHandler(Configuration configuration) {
        HttpResponse.BodyHandler<?> bodyHandler;
        ResponseType responseType = configuration.getResponseType();

        if (responseType == null) {
            bodyHandler = HttpResponse.BodyHandlers.discarding();
        } else {
            if (responseType == Http.ResponseType.BINARY) {
                bodyHandler = HttpResponse.BodyHandlers.ofInputStream();
            } else {
                bodyHandler = HttpResponse.BodyHandlers.ofString();
            }
        }

        return bodyHandler;
    }

    BodyPublisher createBodyPublisher(Body body) {
        BodyPublisher bodyPublisher;

        if (body == null) {
            bodyPublisher = BodyPublishers.noBody();
        } else {
            if (body.getContentType() == Http.BodyContentType.BINARY
                && body.getContent() instanceof FileEntry fileEntry) {
                bodyPublisher = getBinaryBodyPublisher(body, fileEntry);
            } else if (body.getContentType() == Http.BodyContentType.FORM_DATA) {
                bodyPublisher = getFormDataBodyPublisher(body);
            } else if (body.getContentType() == Http.BodyContentType.FORM_URL_ENCODED) {
                bodyPublisher = getFormUrlEncodedBodyPublisher(body);
            } else if (body.getContentType() == Http.BodyContentType.JSON) {
                bodyPublisher = getJsonBodyPublisher(body);
            } else if (body.getContentType() == Http.BodyContentType.XML) {
                bodyPublisher = getXmlBodyPublisher(body);
            } else {
                bodyPublisher = getStringBodyPublisher(body);
            }
        }

        return bodyPublisher;
    }

    HttpClient createHttpClient(
        Map<String, List<String>> headers, Map<String, List<String>> queryParameters, Configuration configuration,
        Connection connection) {

        Methanol.Builder builder = Methanol.newBuilder()
            .version(HttpClient.Version.HTTP_1_1);

        if (configuration.isAllowUnauthorizedCerts()) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                sslContext.init(null, new TrustManager[] {
                    new UnauthorizedCertsX509ExtendedTrustManager()
                }, null);

                builder.sslContext(sslContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        applyAuthorization(headers, queryParameters, connection);

        if (configuration.isFollowRedirect()) {
            builder.followRedirects(HttpClient.Redirect.NORMAL);
        }

        if (configuration.isFollowAllRedirects()) {
            builder.followRedirects(HttpClient.Redirect.ALWAYS);
        }

        String proxy = configuration.getProxy();

        if (proxy != null) {
            String[] proxyAddress = proxy.split(":");

            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyAddress[0], Integer.parseInt(proxyAddress[1]))));
        }

        builder.connectTimeout(configuration.getTimeout());

        return builder.build();
    }

    HttpRequest createHTTPRequest(
        String urlString, RequestMethod requestMethod, Map<String, List<String>> headers,
        Map<String, List<String>> queryParameters, Body body, Connection connection) {

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
            .method(requestMethod.name(), createBodyPublisher(body));

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                httpRequestBuilder.header(entry.getKey(), value);
            }
        }

        httpRequestBuilder.uri(
            createURI(
                getConnectionUrl(urlString, connection),
                Objects.requireNonNullElse(queryParameters, Collections.emptyMap())));

        return httpRequestBuilder.build();
    }

    Response handleResponse(HttpResponse<?> httpResponse, Configuration configuration) {
        Response response;
        HttpHeaders httpHeaders = httpResponse.headers();

        Map<String, List<String>> headers = httpHeaders.map();

        if (configuration.getResponseType() == null) {
            response = new Response(headers, null, httpResponse.statusCode());
        } else {
            Object httpResponseBody = httpResponse.body();
            ResponseType responseType = configuration.getResponseType();

            Object body;

            if (!isEmpty(httpResponseBody) && responseType == Http.ResponseType.BINARY) {
                body = storeBinaryResponseBody(configuration, headers, (InputStream) httpResponseBody);
            } else if (responseType == Http.ResponseType.JSON) {
                body = isEmpty(httpResponseBody) ? null
                    : com.bytechef.commons.util.JsonUtils.read(httpResponseBody.toString(), objectMapper);
            } else if (responseType == Http.ResponseType.TEXT) {
                body = isEmpty(httpResponseBody) ? null : httpResponseBody.toString();
            } else {
                body = isEmpty(httpResponseBody) ? null
                    : com.bytechef.commons.util.XmlUtils.read(httpResponseBody.toString(), xmlMapper);
            }

            response = new Response(headers, body, httpResponse.statusCode());
        }

        return response;
    }

    private void addFileEntry(MultipartBodyPublisher.Builder builder, String name, FileEntry fileEntry) {
        builder.formPart(
            name, fileEntry.getName(),
            MoreBodyPublishers.ofMediaType(
                BodyPublishers.ofInputStream(() -> fileStorageService.getFileStream(
                    FileEntryConstants.DOCUMENTS_DIR, ((ContextFileEntryImpl) fileEntry).getFileEntry())),
                MediaType.parse(fileEntry.getMimeType())));
    }

    private void applyAuthorization(
        Map<String, List<String>> headers, Map<String, List<String>> queryParameters, Connection connection) {

        ApplyResponse applyResponse = connectionDefinitionService.executeAuthorizationApply(connection);

        if (applyResponse != null) {
            headers.putAll(applyResponse.getHeaders());
            queryParameters.putAll(applyResponse.getQueryParameters());
        }
    }

    private URI createURI(String urlString, @Nonnull Map<String, List<String>> queryParameters) {
        URI uri;

        if (queryParameters.isEmpty()) {
            uri = URI.create(urlString);
        } else {
            String parameter = queryParameters.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                    .stream()
                    .map(value -> entry.getKey() + "=" + value))
                .collect(Collectors.joining("&"));

            uri = URI.create(urlString + '?' + parameter);
        }

        return uri;
    }

    private String getConnectionUrl(String urlString, Connection connection) {
        if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
            return urlString;
        }

        if (connection == null) {
            return urlString;
        } else {
            return OptionalUtils.map(
                connectionDefinitionService.executeBaseUri(connection),
                baseUri -> baseUri + urlString);
        }
    }

    private BodyPublisher getBinaryBodyPublisher(Body body, FileEntry fileEntry) {
        return MoreBodyPublishers.ofMediaType(
            BodyPublishers.ofInputStream(() -> fileStorageService.getFileStream(
                FileEntryConstants.DOCUMENTS_DIR, ((ContextFileEntryImpl) fileEntry).getFileEntry())),
            MediaType.parse(body.getMimeType() == null ? fileEntry.getMimeType() : body.getMimeType()));
    }

    private BodyPublisher getFormDataBodyPublisher(Body body) {
        Map<?, ?> bodyParameters = (Map<?, ?>) body.getContent();

        MultipartBodyPublisher.Builder builder = MultipartBodyPublisher.newBuilder();

        for (Map.Entry<?, ?> parameter : bodyParameters.entrySet()) {
            if (parameter.getValue() instanceof FileEntry fileEntry) {
                addFileEntry(builder, (String) parameter.getKey(), fileEntry);
            } else {
                builder.textPart((String) parameter.getKey(), parameter.getValue());
            }
        }

        return builder.build();
    }

    private BodyPublisher getFormUrlEncodedBodyPublisher(Body body) {
        Map<?, ?> bodyParameters = (Map<?, ?>) body.getContent();

        FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();

        for (Map.Entry<?, ?> parameter : bodyParameters.entrySet()) {
            Object value = Objects.requireNonNull(parameter.getValue());

            builder.query((String) parameter.getKey(), value.toString());
        }

        return builder.build();
    }

    private BodyPublisher getStringBodyPublisher(Body body) {
        Object content = body.getContent();

        return MoreBodyPublishers.ofMediaType(
            BodyPublishers.ofString(content.toString()),
            MediaType.parse(body.getMimeType()));
    }

    private boolean isEmpty(final Object object) {
        if (object == null) {
            return true;
        }

        if (object instanceof CharSequence) {
            return ((CharSequence) object).isEmpty();
        }

        return false;
    }

    private FileEntry storeBinaryResponseBody(
        Configuration configuration, Map<String, List<String>> headers, InputStream httpResponseBody) {

        String filename = configuration.getFilename();

        if (filename == null || filename.isEmpty()) {
            if (headers.containsKey("Content-Type")) {
                List<String> values = headers.get("Content-Type");

                filename = "file" + MimeTypeUtils.getDefaultExt(values.get(0));
            } else {
                filename = "file.txt";
            }
        }

        return new ContextFileEntryImpl(
            fileStorageService.storeFileContent(FileEntryConstants.DOCUMENTS_DIR, filename, httpResponseBody));
    }

    private BodyPublisher getJsonBodyPublisher(Body body) {
        return MoreBodyPublishers.ofMediaType(
            BodyPublishers
                .ofString(com.bytechef.commons.util.JsonUtils.write(body.getContent(), objectMapper)),
            MediaType.APPLICATION_JSON);
    }

    private BodyPublisher getXmlBodyPublisher(Body body) {
        return MoreBodyPublishers.ofMediaType(
            BodyPublishers.ofString(com.bytechef.commons.util.XmlUtils.write(body.getContent(), xmlMapper)),
            MediaType.APPLICATION_XML);
    }

    private static class UnauthorizedCertsX509ExtendedTrustManager extends X509ExtendedTrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(final X509Certificate[] a_certificates, final String a_auth_type) {
        }

        public void checkServerTrusted(final X509Certificate[] a_certificates, final String a_auth_type) {
        }

        public void checkClientTrusted(
            final X509Certificate[] a_certificates, final String a_auth_type, final Socket a_socket) {
        }

        public void checkServerTrusted(
            final X509Certificate[] a_certificates, final String a_auth_type, final Socket a_socket) {
        }

        public void checkClientTrusted(
            final X509Certificate[] a_certificates, final String a_auth_type, final SSLEngine a_engine) {
        }

        public void checkServerTrusted(
            final X509Certificate[] a_certificates, final String a_auth_type, final SSLEngine a_engine) {
        }
    }
}
