
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

package com.bytechef.hermes.component.util;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.util.HttpClientUtils.Body;
import com.bytechef.hermes.component.util.HttpClientUtils.Configuration;
import com.bytechef.hermes.component.util.HttpClientUtils.RequestMethod;
import com.bytechef.hermes.component.util.HttpClientUtils.Response;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
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
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
@Component
public class HttpClientExecutor implements HttpClientUtils.HttpClientExecutor {

    private final JsonMapper jsonMapper;
    private final XmlMapper xmlMapper;

    public HttpClientExecutor(JsonMapper jsonMapper, XmlMapper xmlMapper) {
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
    }

    @Override
    public Response execute(
        String urlString, Map<String, List<String>> headers, Map<String, List<String>> queryParameters, Body body,
        Configuration configuration, RequestMethod requestMethod) throws Exception {

        ComponentContextThreadLocal.ComponentContext componentContext = ComponentContextThreadLocal.get();

        HttpClient httpClient = createHttpClient(
            componentContext == null ? null : componentContext.context(),
            componentContext == null ? null : componentContext.componentDefinition(), headers, queryParameters,
            configuration);

        HttpRequest httpRequest = createHTTPRequest(
            componentContext == null ? null : componentContext.context(), urlString, requestMethod, headers,
            queryParameters, body);

        HttpResponse<?> httpResponse = httpClient.send(httpRequest, createBodyHandler(configuration));

        return handleResponse(
            componentContext == null ? null : componentContext.context(), httpResponse, configuration);
    }

    HttpResponse.BodyHandler<?> createBodyHandler(Configuration configuration) {
        HttpResponse.BodyHandler<?> bodyHandler;
        HttpClientUtils.ResponseFormat responseFormat = configuration.getResponseFormat();

        if (responseFormat == null) {
            bodyHandler = HttpResponse.BodyHandlers.discarding();
        } else {
            if (responseFormat == HttpClientUtils.ResponseFormat.BINARY) {
                bodyHandler = HttpResponse.BodyHandlers.ofInputStream();
            } else {
                bodyHandler = HttpResponse.BodyHandlers.ofString();
            }
        }

        return bodyHandler;
    }

    HttpRequest.BodyPublisher createBodyPublisher(Context context, Body body) {
        HttpRequest.BodyPublisher bodyPublisher;

        if (body == null) {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        } else {
            if (body.getContentType() == HttpClientUtils.BodyContentType.BINARY &&
                body.getContent() instanceof Context.FileEntry fileEntry) {

                bodyPublisher = getBinaryBodyPublisher(context, body, fileEntry);
            } else if (body.getContentType() == HttpClientUtils.BodyContentType.FORM_DATA) {
                bodyPublisher = getFormDataBodyPublisher(context, body);
            } else if (body.getContentType() == HttpClientUtils.BodyContentType.FORM_URL_ENCODED) {
                bodyPublisher = getFormUrlEncodedBodyPublisher(body);
            } else if (body.getContentType() == HttpClientUtils.BodyContentType.JSON) {
                bodyPublisher = getJsonBodyPublisher(body);
            } else if (body.getContentType() == HttpClientUtils.BodyContentType.XML) {
                bodyPublisher = getXmlBodyPublisher(body);
            } else {
                bodyPublisher = getStringBodyPublisher(body);
            }
        }

        return bodyPublisher;
    }

    HttpClient createHttpClient(
        Context context, ComponentDefinition componentDefinition, Map<String, List<String>> headers,
        Map<String, List<String>> queryParameters, Configuration configuration) {

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

        applyAuthorization(context, componentDefinition, headers, queryParameters);

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
        Context context, String urlString, RequestMethod requestMethod, Map<String, List<String>> headers,
        Map<String, List<String>> queryParameters, Body body) {

        HttpRequest.Builder httpRequestBuilder = HttpRequest
            .newBuilder()
            .method(requestMethod.name(), createBodyPublisher(context, body));

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                httpRequestBuilder.header(entry.getKey(), value);
            }
        }

        httpRequestBuilder.uri(
            createURI(
                getConnectionUri(context, urlString),
                Objects.requireNonNullElse(queryParameters, Collections.emptyMap())));

        return httpRequestBuilder.build();
    }

    Response handleResponse(Context context, HttpResponse<?> httpResponse, Configuration configuration)
        throws Exception {

        Response response;
        HttpHeaders httpHeaders = httpResponse.headers();

        Map<String, List<String>> headers = httpHeaders.map();

        if (configuration.getResponseFormat() == null) {
            response = new Response(headers, null, httpResponse.statusCode());
        } else {
            Object httpResponseBody = httpResponse.body();
            HttpClientUtils.ResponseFormat responseFormat = configuration.getResponseFormat();

            Object body;

            if (!isEmpty(httpResponseBody) && responseFormat == HttpClientUtils.ResponseFormat.BINARY) {
                body = storeBinaryResponseBody(context, configuration, headers, (InputStream) httpResponseBody);
            } else if (responseFormat == HttpClientUtils.ResponseFormat.JSON) {
                body = isEmpty(httpResponseBody) ? null : jsonMapper.read(httpResponseBody.toString());
            } else if (responseFormat == HttpClientUtils.ResponseFormat.TEXT) {
                body = isEmpty(httpResponseBody) ? null : httpResponseBody.toString();
            } else {
                body = isEmpty(httpResponseBody) ? null : xmlMapper.read(httpResponseBody.toString());
            }

            response = new Response(headers, body, httpResponse.statusCode());
        }

        return response;
    }

    private static void addFileEntry(
        Context context, MultipartBodyPublisher.Builder builder, String name, Context.FileEntry fileEntry) {

        Objects.requireNonNull(context, "'context' must not be null");

        builder.formPart(
            name, fileEntry.getName(),
            MoreBodyPublishers.ofMediaType(
                HttpRequest.BodyPublishers.ofInputStream(() -> context.getFileStream(fileEntry)),
                MediaType.parse(fileEntry.getMimeType())));
    }

    private static void applyAuthorization(
        Context context, ComponentDefinition componentDefinition, Map<String, List<String>> headers,
        Map<String, List<String>> queryParameters) {

        if (context == null) {
            return;
        }

        if (componentDefinition == null) {
            OptionalUtils.ifPresent(
                context.fetchConnection(),
                (connection -> connection.applyAuthorization(
                    new AuthorizationContextImpl(headers, queryParameters, new HashMap<>()))));
        } else {
            ConnectionDefinition connectionDefinition = componentDefinition.getConnection()
                .orElse(null);

            if (connectionDefinition != null && connectionDefinition.isAuthorizationRequired()) {
                Context.Connection connection = context.getConnection();

                connection.applyAuthorization(new AuthorizationContextImpl(headers, queryParameters, new HashMap<>()));
            } else {
                OptionalUtils.ifPresent(
                    context.fetchConnection(),
                    connection -> connection.applyAuthorization(
                        new AuthorizationContextImpl(headers, queryParameters, new HashMap<>())));
            }
        }
    }

    private static URI createURI(String uriString, @Nonnull Map<String, List<String>> queryParameters) {
        URI uri;

        if (queryParameters.isEmpty()) {
            uri = URI.create(uriString);
        } else {
            String parameter = queryParameters.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                    .stream()
                    .map(value -> entry.getKey() + "=" + value))
                .collect(Collectors.joining("&"));

            uri = URI.create(uriString + '?' + parameter);
        }

        return uri;
    }

    private static String getConnectionUri(Context context, String uriString) {
        if (context == null) {
            return uriString;
        }

        return context.fetchConnection()
            .flatMap(Context.Connection::fetchBaseUri)
            .map(baseUri -> baseUri + uriString)
            .orElse(uriString);
    }

    private static HttpRequest.BodyPublisher getBinaryBodyPublisher(
        Context context, Body body, Context.FileEntry fileEntry) {

        Objects.requireNonNull(context, "'context' must not be null");

        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofInputStream(() -> context.getFileStream(fileEntry)),
            MediaType.parse(body.getMimeType() == null ? fileEntry.getMimeType() : body.getMimeType()));
    }

    private static HttpRequest.BodyPublisher getFormDataBodyPublisher(Context context, Body body) {
        Map<?, ?> bodyParameters = (Map<?, ?>) body.getContent();

        MultipartBodyPublisher.Builder builder = MultipartBodyPublisher.newBuilder();

        for (Map.Entry<?, ?> parameter : bodyParameters.entrySet()) {
            if (parameter.getValue() instanceof Context.FileEntry fileEntry) {
                addFileEntry(context, builder, (String) parameter.getKey(), fileEntry);
            } else {
                builder.textPart((String) parameter.getKey(), parameter.getValue());
            }
        }

        return builder.build();
    }

    private static HttpRequest.BodyPublisher getFormUrlEncodedBodyPublisher(Body body) {
        Map<?, ?> bodyParameters = (Map<?, ?>) body.getContent();

        FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();

        for (Map.Entry<?, ?> parameter : bodyParameters.entrySet()) {
            Object value = parameter.getValue();

            builder.query((String) parameter.getKey(), value.toString());
        }

        return builder.build();
    }

    private static HttpRequest.BodyPublisher getStringBodyPublisher(Body body) {
        Object content = body.getContent();

        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofString(content.toString()),
            MediaType.parse(body.getMimeType()));
    }

    private static boolean isEmpty(final Object object) {
        if (object == null) {
            return true;
        }

        if (object instanceof CharSequence) {
            return ((CharSequence) object).length() == 0;
        }

        return false;
    }

    private static Context.FileEntry storeBinaryResponseBody(
        Context context, Configuration configuration, Map<String, List<String>> headers,
        InputStream httpResponseBody) throws MimeTypeException {

        Objects.requireNonNull(context, "'context' must not be null");

        String filename = configuration.getFilename();

        if (filename == null || filename.length() == 0) {
            if (headers.containsKey("Content-Type")) {
                MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();
                List<String> values = headers.get("Content-Type");

                MimeType mimeType = mimeTypes.forName(values.get(0));

                filename = "file" + mimeType.getExtension();
            } else {
                filename = "file.txt";
            }
        }

        return context.storeFileContent(filename, httpResponseBody);
    }

    private HttpRequest.BodyPublisher getJsonBodyPublisher(Body body) {
        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofString(jsonMapper.write(body.getContent())),
            MediaType.APPLICATION_JSON);
    }

    private HttpRequest.BodyPublisher getXmlBodyPublisher(Body body) {
        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofString(xmlMapper.write(body.getContent())),
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

    @SuppressFBWarnings("EI")
    record AuthorizationContextImpl(
        Map<String, List<String>> headers, Map<String, List<String>> queryParameters, Map<String, String> body)
        implements Authorization.AuthorizationContext {

        private static final Base64.Encoder ENCODER = Base64.getEncoder();

        @Override
        public void setHeaders(Map<String, List<String>> headers) {
            this.headers.putAll(headers);
        }

        @Override
        public void setQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters.putAll(queryParameters);
        }

        @Override
        public void setBody(Map<String, String> body) {
            this.body.putAll(body);
        }

        @Override
        public void setUsernamePassword(String username, String password) {
            String valueToEncode = username + ":" + password;

            headers.put(
                "Authorization",
                List.of("Basic " + ENCODER.encodeToString(valueToEncode.getBytes(StandardCharsets.UTF_8))));
        }
    }
}
