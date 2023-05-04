
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.ComponentContextThreadLocal.ComponentContext;
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
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class HttpClientUtils {

    public enum BodyContentType {
        BINARY,
        FORM_DATA,
        FORM_URL_ENCODED,
        JSON,
        RAW,
        XML
    }

    public enum ResponseFormat {
        BINARY,
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

    private HttpClientUtils() {
    }

    public static Configuration allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
        Configuration configuration = new Configuration();

        configuration.allowUnauthorizedCerts = allowUnauthorizedCerts;

        return configuration;
    }

    public static Configuration followAllRedirects(boolean followAllRedirects) {
        Configuration configuration = new Configuration();

        configuration.followAllRedirects = followAllRedirects;

        return configuration;
    }

    public static Configuration filename(String filename) {
        Configuration configuration = new Configuration();

        configuration.filename = filename;

        return configuration;
    }

    public static Configuration followRedirect(boolean followRedirect) {
        Configuration configuration = new Configuration();

        configuration.followRedirect = followRedirect;

        return configuration;
    }

    public static Configuration proxy(String proxy) {
        Configuration configuration = new Configuration();

        configuration.proxy = proxy;

        return configuration;
    }

    public static Configuration responseFormat(ResponseFormat responseFormat) {
        Configuration configuration = new Configuration();

        configuration.responseFormat = responseFormat;

        return configuration;
    }

    public static Configuration timeout(Duration timeout) {
        Configuration configuration = new Configuration();

        configuration.timeout = timeout;

        return configuration;
    }

    public static Executor delete(String uri) {
        return new Executor().delete(uri);
    }

    public static Executor exchange(String uri, RequestMethod requestMethod) {
        return new Executor().exchange(uri, requestMethod);
    }

    public static Executor head(String uri) {
        return new Executor().head(uri);
    }

    public static Executor get(String uri) {
        return new Executor().get(uri);
    }

    public static Executor patch(String uri) {
        return new Executor().patch(uri);
    }

    public static Executor post(String uri) {
        return new Executor().post(uri);
    }

    public static Executor put(String uri) {
        return new Executor().put(uri);
    }

    static HttpResponse.BodyHandler<?> createBodyHandler(Configuration configuration) {
        HttpResponse.BodyHandler<?> bodyHandler;
        ResponseFormat responseFormat = configuration.getResponseFormat();

        if (responseFormat == null) {
            bodyHandler = HttpResponse.BodyHandlers.discarding();
        } else {
            if (responseFormat == ResponseFormat.BINARY) {
                bodyHandler = HttpResponse.BodyHandlers.ofInputStream();
            } else {
                bodyHandler = HttpResponse.BodyHandlers.ofString();
            }
        }

        return bodyHandler;
    }

    static HttpRequest.BodyPublisher createBodyPublisher(Context context, Body body) {
        HttpRequest.BodyPublisher bodyPublisher;

        if (body == null) {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        } else {
            if (body.contentType == BodyContentType.BINARY
                && body.content instanceof Context.FileEntry fileEntry) {
                bodyPublisher = getBinaryBodyPublisher(context, body, fileEntry);
            } else if (body.contentType == BodyContentType.FORM_DATA) {
                bodyPublisher = getFormDataBodyPublisher(context, body);
            } else if (body.contentType == BodyContentType.FORM_URL_ENCODED) {
                bodyPublisher = getFormUrlEncodedBodyPublisher(body);
            } else if (body.contentType == BodyContentType.JSON) {
                bodyPublisher = getJsonBodyPublisher(body);
            } else if (body.contentType == BodyContentType.XML) {
                bodyPublisher = getXmlBodyPublisher(body);
            } else {
                bodyPublisher = getStringBodyPublisher(body);
            }
        }

        return bodyPublisher;
    }

    static HttpClient createHttpClient(
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

    static HttpRequest createHTTPRequest(
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

    static Response handleResponse(Context context, HttpResponse<?> httpResponse, Configuration configuration)
        throws Exception {

        Response response;
        HttpHeaders httpHeaders = httpResponse.headers();

        Map<String, List<String>> headers = httpHeaders.map();

        if (configuration.getResponseFormat() == null) {
            response = new Response(headers, null, httpResponse.statusCode());
        } else {
            Object httpResponseBody = httpResponse.body();
            ResponseFormat responseFormat = configuration.getResponseFormat();

            Object body;

            if (!ObjectUtils.isEmpty(httpResponseBody) && responseFormat == ResponseFormat.BINARY) {
                body = storeBinaryResponseBody(context, configuration, headers, (InputStream) httpResponseBody);
            } else if (responseFormat == ResponseFormat.JSON) {
                body = ObjectUtils.isEmpty(httpResponseBody) ? null : JsonUtils.read(httpResponseBody.toString());
            } else if (responseFormat == ResponseFormat.TEXT) {
                body = ObjectUtils.isEmpty(httpResponseBody) ? null : httpResponseBody.toString();
            } else {
                body = ObjectUtils.isEmpty(httpResponseBody) ? null : XmlUtils.read(httpResponseBody.toString());
            }

            response = new Response(headers, body, httpResponse.statusCode());
        }

        return response;
    }

    private static Response execute(
        ComponentContext componentContext, String urlString, Map<String, List<String>> headers,
        Map<String, List<String>> queryParameters,
        Body body, Configuration configuration, RequestMethod requestMethod) throws Exception {

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
            context
                .fetchConnection()
                .ifPresent(
                    connection -> connection.applyAuthorization(
                        new AuthorizationContext(headers, queryParameters, new HashMap<>())));
        } else {
            ConnectionDefinition connectionDefinition = componentDefinition
                .getConnection()
                .orElse(null);

            if (connectionDefinition != null && connectionDefinition.isAuthorizationRequired()) {
                Context.Connection connection = context.getConnection();

                connection.applyAuthorization(
                    new AuthorizationContext(headers, queryParameters, new HashMap<>()));
            } else {
                context
                    .fetchConnection()
                    .ifPresent(
                        connection -> connection.applyAuthorization(
                            new AuthorizationContext(headers, queryParameters, new HashMap<>())));
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
            MediaType.parse(body.mimeType == null ? fileEntry.getMimeType() : body.mimeType));
    }

    private static HttpRequest.BodyPublisher getFormDataBodyPublisher(Context context, Body body) {
        Map<?, ?> bodyParameters = (Map<?, ?>) body.content;

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
        Map<?, ?> bodyParameters = (Map<?, ?>) body.content;

        FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();

        for (Map.Entry<?, ?> parameter : bodyParameters.entrySet()) {
            Object value = parameter.getValue();

            builder.query((String) parameter.getKey(), value.toString());
        }

        return builder.build();
    }

    private static HttpRequest.BodyPublisher getJsonBodyPublisher(Body body) {
        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofString(JsonUtils.write(body.content)),
            MediaType.APPLICATION_JSON);
    }

    private static HttpRequest.BodyPublisher getStringBodyPublisher(Body body) {
        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofString(body.content.toString()),
            MediaType.parse(body.mimeType));
    }

    private static HttpRequest.BodyPublisher getXmlBodyPublisher(Body body) {
        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofString(XmlUtils.write(body.content)),
            MediaType.APPLICATION_XML);
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

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    public static class Response {

        private final Object body;
        private final Map<String, List<String>> headers;
        private final int statusCode;

        public Response(Map<String, List<String>> headers, Object body, int statusCode) {
            this.body = body;
            this.headers = headers;
            this.statusCode = statusCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Response response = (Response) o;

            return statusCode == response.statusCode &&
                Objects.equals(body, response.body) && Objects.equals(headers, response.headers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(body, headers, statusCode);
        }

        public Object getBody() {
            return body;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    public static class Configuration {

        private boolean allowUnauthorizedCerts;
        private String filename;
        private boolean followAllRedirects;
        private boolean followRedirect;
        private String proxy;
        private ResponseFormat responseFormat;
        private Duration timeout = Duration.ofMillis(1000);

        private Configuration() {
        }

        public static Configuration configuration() {
            return new Configuration();
        }

        public Configuration allowUnauthorizedCerts(boolean allowUnauthorizedCerts) {
            this.allowUnauthorizedCerts = allowUnauthorizedCerts;

            return this;
        }

        public Configuration followAllRedirects(boolean followAllRedirects) {
            this.followAllRedirects = followAllRedirects;

            return this;
        }

        public Configuration filename(String filename) {
            this.filename = filename;

            return this;
        }

        public Configuration followRedirect(boolean followRedirect) {
            this.followRedirect = followRedirect;

            return this;
        }

        public Configuration proxy(String proxy) {
            this.proxy = proxy;

            return this;
        }

        public Configuration responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;

            return this;
        }

        public Configuration timeout(Duration timeout) {
            this.timeout = timeout;

            return this;
        }

        public boolean isAllowUnauthorizedCerts() {
            return allowUnauthorizedCerts;
        }

        public boolean isFollowAllRedirects() {
            return followAllRedirects;
        }

        public boolean isFollowRedirect() {
            return followRedirect;
        }

        public String getFilename() {
            return filename;
        }

        public ResponseFormat getResponseFormat() {
            return responseFormat;
        }

        public String getProxy() {
            return proxy;
        }

        public Duration getTimeout() {
            return timeout;
        }
    }

    public static class Executor {

        private Configuration configuration = new Configuration();
        private Map<String, List<String>> headers = Collections.emptyMap();
        private Map<String, List<String>> queryParameters = Collections.emptyMap();
        private Body body;
        private RequestMethod requestMethod;
        private String url;

        private Executor() {
        }

        public Executor configuration(Configuration configuration) {
            this.configuration = Objects.requireNonNull(configuration);

            return this;
        }

        public Executor delete(String uri) {
            this.url = Objects.requireNonNull(uri);
            this.requestMethod = RequestMethod.DELETE;

            return this;
        }

        public Executor exchange(String uri, RequestMethod requestMethod) {
            this.url = Objects.requireNonNull(uri);
            this.requestMethod = Objects.requireNonNull(requestMethod);

            return this;
        }

        public Executor head(String uri) {
            this.url = Objects.requireNonNull(uri);
            this.requestMethod = RequestMethod.HEAD;

            return this;
        }

        public Executor headers(Map<String, List<String>> headers) {
            if (headers != null) {
                this.headers = new HashMap<>(headers);
            }

            return this;
        }

        public Executor get(String uri) {
            this.url = Objects.requireNonNull(uri);
            this.requestMethod = RequestMethod.GET;

            return this;
        }

        public Executor queryParameters(Map<String, List<String>> queryParameters) {
            if (queryParameters != null) {
                this.queryParameters = new HashMap<>(queryParameters);
            }

            return this;
        }

        public Executor patch(String uri) {
            this.url = Objects.requireNonNull(uri);
            this.requestMethod = RequestMethod.PATCH;

            return this;
        }

        public Executor body(Body body) {
            this.body = body;

            return this;
        }

        public Executor post(String uri) {
            this.url = Objects.requireNonNull(uri);
            this.requestMethod = RequestMethod.POST;

            return this;
        }

        public Executor put(String url) {
            this.url = Objects.requireNonNull(url);
            this.requestMethod = RequestMethod.PUT;

            return this;
        }

        public Response execute() {
            try {
                return HttpClientUtils.execute(
                    ComponentContextThreadLocal.get(), url, headers, queryParameters, body, configuration,
                    requestMethod);
            } catch (Exception e) {
                throw new ComponentExecutionException("Unable to execute HTTP request", e);
            }
        }
    }

    public static class Body {

        private final BodyContentType contentType;
        private final String mimeType;
        private final Object content;

        private Body(Object content, BodyContentType contentType) {
            this(content, contentType, null);
        }

        private Body(Object content, BodyContentType contentType, String mimeType) {
            this.content = content;
            this.contentType = contentType;
            this.mimeType = mimeType;
        }

        public static Body of(Context.FileEntry fileEntry) {
            return new Body(fileEntry, BodyContentType.BINARY);
        }

        public static Body of(Context.FileEntry fileEntry, String mimeType) {
            Objects.requireNonNull(fileEntry);

            return new Body(fileEntry, BodyContentType.BINARY, mimeType);
        }

        public static Body of(List<?> list) {
            Objects.requireNonNull(list);

            return new Body(list, BodyContentType.JSON);
        }

        public static Body of(List<?> list, BodyContentType bodyContentType) {
            Objects.requireNonNull(list);
            Objects.requireNonNull(bodyContentType);

            return new Body(list, bodyContentType);
        }

        public static Body of(Map<String, Object> map) {
            Objects.requireNonNull(map);

            return new Body(map, BodyContentType.JSON);
        }

        public static Body of(Map<String, Object> content, BodyContentType bodyContentType) {
            Objects.requireNonNull(content);
            Objects.requireNonNull(bodyContentType);

            return new Body(content, bodyContentType);
        }

        public static Body of(String string) {
            Objects.requireNonNull(string);

            return new Body(string, BodyContentType.RAW, "text/plain");
        }

        public static Body of(String string, String mimeType) {
            Objects.requireNonNull(string);
            Objects.requireNonNull(mimeType);

            return new Body(string, BodyContentType.RAW, mimeType);
        }

        public static Body of(String string, BodyContentType bodyContentType) {
            Objects.requireNonNull(string);
            Objects.requireNonNull(bodyContentType);

            return new Body(string, bodyContentType, null);
        }
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
