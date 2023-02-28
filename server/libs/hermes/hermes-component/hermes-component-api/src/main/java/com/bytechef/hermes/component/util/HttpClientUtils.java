
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

import com.bytechef.hermes.component.AuthorizationContext;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.exception.ActionExecutionException;
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

    static HttpRequest.BodyPublisher createBodyPublisher(Context context, Payload payload) {
        HttpRequest.BodyPublisher bodyPublisher;

        if (payload == null) {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        } else {
            if (payload.bodyContentType == BodyContentType.BINARY && payload.value instanceof FileEntry fileEntry) {
                bodyPublisher = getBinaryBodyPublisher(context, payload, fileEntry);
            } else if (payload.bodyContentType == BodyContentType.FORM_DATA) {
                bodyPublisher = getFormDataBodyPublisher(context, payload);
            } else if (payload.bodyContentType == BodyContentType.FORM_URL_ENCODED) {
                bodyPublisher = getFormUrlEncodedBodyPublisher(payload);
            } else if (payload.bodyContentType == BodyContentType.JSON) {
                bodyPublisher = getJsonBodyPublisher(payload);
            } else if (payload.bodyContentType == BodyContentType.XML) {
                bodyPublisher = getXmlBodyPublisher(payload);
            } else {
                bodyPublisher = getStringBodyPublisher(payload);
            }
        }

        return bodyPublisher;
    }

    static HttpClient createHttpClient(
        Context context, Map<String, List<String>> headers, Map<String, List<String>> queryParameters,
        Configuration configuration) {

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

        acceptAuthorizationApplyConsumer(context, builder, headers, queryParameters);

        if (configuration.isFollowRedirect()) {
            builder.followRedirects(java.net.http.HttpClient.Redirect.NORMAL);
        }

        if (configuration.isFollowAllRedirects()) {
            builder.followRedirects(java.net.http.HttpClient.Redirect.ALWAYS);
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
        Map<String, List<String>> queryParameters, Payload payload) {

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
            .method(requestMethod.name(), createBodyPublisher(context, payload));

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

        Map<String, List<String>> headersMap = httpHeaders.map();

        if (configuration.getResponseFormat() == null) {
            response = new Response(null, headersMap, httpResponse.statusCode());
        } else {
            Object httpResponseBody = httpResponse.body();
            ResponseFormat responseFormat = configuration.getResponseFormat();

            Object body;

            if (!ObjectUtils.isEmpty(httpResponseBody) && responseFormat == ResponseFormat.BINARY) {
                body = storeBinaryResponseBody(context, configuration, headersMap, (InputStream) httpResponseBody);
            } else if (responseFormat == ResponseFormat.JSON) {
                body = ObjectUtils.isEmpty(httpResponseBody) ? null : JsonUtils.read(httpResponseBody.toString());
            } else if (responseFormat == ResponseFormat.TEXT) {
                body = ObjectUtils.isEmpty(httpResponseBody) ? null : httpResponseBody.toString();
            } else {
                body = ObjectUtils.isEmpty(httpResponseBody) ? null : XmlUtils.read(httpResponseBody.toString());
            }

            response = new Response(body, headersMap, httpResponse.statusCode());
        }

        return response;
    }

    private static Response execute(
        Context context, String urlString, Map<String, List<String>> headers, Map<String, List<String>> queryParameters,
        Payload payload, Configuration configuration, RequestMethod requestMethod) throws Exception {

        HttpClient httpClient = createHttpClient(context, headers, queryParameters, configuration);

        HttpRequest httpRequest = createHTTPRequest(
            context, urlString, requestMethod, headers, queryParameters, payload);

        HttpResponse<?> httpResponse = httpClient.send(httpRequest, createBodyHandler(configuration));

        return handleResponse(context, httpResponse, configuration);
    }

    private static void addFileEntry(
        Context context, MultipartBodyPublisher.Builder builder, String name, FileEntry fileEntry) {

        Objects.requireNonNull(context, "'context' must not be null");

        builder.formPart(
            name, fileEntry.getName(),
            MoreBodyPublishers.ofMediaType(
                HttpRequest.BodyPublishers.ofInputStream(() -> context.getFileStream(fileEntry)),
                MediaType.parse(fileEntry.getMimeType())));
    }

    private static void acceptAuthorizationApplyConsumer(
        Context context, Methanol.Builder builder, Map<String, List<String>> headers,
        Map<String, List<String>> queryParameters) {

        if (context == null) {
            return;
        }

        context.fetchConnectionAuthorization()
            .map(Authorization::getApplyConsumer)
            .ifPresent(applyConsumer -> applyConsumer.accept(
                new AuthorizationContextImpl(builder, headers, queryParameters), context.getConnection()));
    }

    private static URI createURI(String uriString, @Nonnull Map<String, List<String>> queryParameters) {
        URI uri;

        if (queryParameters.isEmpty()) {
            uri = URI.create(uriString);
        } else {
            String queryParametersString = queryParameters.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                    .stream()
                    .map(value -> entry.getKey() + "=" + value))
                .collect(Collectors.joining("&"));

            uri = URI.create(uriString + '?' + queryParametersString);
        }

        return uri;
    }

    private static String getConnectionUri(Context context, String uriString) {
        if (context == null) {
            return uriString;
        }

        return context.fetchConnectionBaseUri()
            .map(baseUri -> baseUri + uriString)
            .orElse(uriString);
    }

    private static HttpRequest.BodyPublisher getBinaryBodyPublisher(
        Context context, Payload payload, FileEntry fileEntry) {

        Objects.requireNonNull(context, "'context' must not be null");

        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofInputStream(() -> context.getFileStream(fileEntry)),
            MediaType.parse(payload.mimeType == null ? fileEntry.getMimeType() : payload.mimeType));
    }

    private static HttpRequest.BodyPublisher getFormDataBodyPublisher(Context context, Payload payload) {
        Map<?, ?> bodyParameters = (Map<?, ?>) payload.value;

        MultipartBodyPublisher.Builder builder = MultipartBodyPublisher.newBuilder();

        for (Map.Entry<?, ?> parameter : bodyParameters.entrySet()) {
            if (parameter.getValue()instanceof FileEntry fileEntry) {
                addFileEntry(context, builder, (String) parameter.getKey(), fileEntry);
            } else {
                builder.textPart((String) parameter.getKey(), parameter.getValue());
            }
        }

        return builder.build();
    }

    private static HttpRequest.BodyPublisher getFormUrlEncodedBodyPublisher(Payload payload) {
        Map<?, ?> bodyParameters = (Map<?, ?>) payload.value;

        FormBodyPublisher.Builder builder = FormBodyPublisher.newBuilder();

        for (Map.Entry<?, ?> parameter : bodyParameters.entrySet()) {
            Object value = parameter.getValue();

            builder.query((String) parameter.getKey(), value.toString());
        }

        return builder.build();
    }

    private static HttpRequest.BodyPublisher getJsonBodyPublisher(Payload payload) {
        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofString(JsonUtils.write(payload.value)),
            MediaType.APPLICATION_JSON);
    }

    private static HttpRequest.BodyPublisher getStringBodyPublisher(Payload payload) {
        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofString(payload.value.toString()),
            MediaType.parse(payload.mimeType));
    }

    private static HttpRequest.BodyPublisher getXmlBodyPublisher(Payload payload) {
        return MoreBodyPublishers.ofMediaType(
            HttpRequest.BodyPublishers.ofString(XmlUtils.write(payload.value)),
            MediaType.APPLICATION_XML);
    }

    private static FileEntry storeBinaryResponseBody(
        Context context, Configuration configuration, Map<String, List<String>> headersMap,
        InputStream httpResponseBody) throws MimeTypeException {

        Objects.requireNonNull(context, "'context' must not be null");

        String filename = configuration.getFilename();

        if (filename == null || filename.length() == 0) {
            if (headersMap.containsKey("Content-Type")) {
                MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();
                List<String> values = headersMap.get("Content-Type");

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
    public record Response(Object body, Map<String, List<String>> headers, int statusCode) {

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
        private Payload payload;
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

        public Executor payload(Payload payload) {
            this.payload = payload;

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
                    ContextThreadLocal.get(), url, headers, queryParameters, payload, configuration, requestMethod);
            } catch (Exception e) {
                throw new ActionExecutionException("Unable to execute HTTP request", e);
            }
        }
    }

    public static class Payload {

        private final BodyContentType bodyContentType;
        private String mimeType;
        private final Object value;

        private Payload(Object value, BodyContentType bodyContentType) {
            this.value = value;
            this.bodyContentType = bodyContentType;
        }

        private Payload(Object value, BodyContentType bodyContentType, String mimeType) {
            this.value = value;
            this.bodyContentType = bodyContentType;
            this.mimeType = mimeType;
        }

        public static Payload of(FileEntry fileEntry) {
            return new Payload(fileEntry, BodyContentType.BINARY);
        }

        public static Payload of(FileEntry fileEntry, String mimeType) {
            Objects.requireNonNull(fileEntry);

            return new Payload(fileEntry, BodyContentType.BINARY, mimeType);
        }

        public static Payload of(List<?> list) {
            Objects.requireNonNull(list);

            return new Payload(list, BodyContentType.JSON);
        }

        public static Payload of(List<?> list, BodyContentType bodyContentType) {
            Objects.requireNonNull(list);
            Objects.requireNonNull(bodyContentType);

            return new Payload(list, bodyContentType);
        }

        public static Payload of(Map<String, Object> map) {
            Objects.requireNonNull(map);

            return new Payload(map, BodyContentType.JSON);
        }

        public static Payload of(Map<String, Object> map, BodyContentType bodyContentType) {
            Objects.requireNonNull(map);
            Objects.requireNonNull(bodyContentType);

            return new Payload(map, bodyContentType);
        }

        public static Payload of(String string) {
            Objects.requireNonNull(string);

            return new Payload(string, BodyContentType.RAW, "text/plain");
        }

        public static Payload of(String string, String mimeType) {
            Objects.requireNonNull(string);
            Objects.requireNonNull(mimeType);

            return new Payload(string, BodyContentType.RAW, mimeType);
        }

        public static Payload of(String string, BodyContentType bodyContentType) {
            Objects.requireNonNull(string);
            Objects.requireNonNull(bodyContentType);

            return new Payload(string, bodyContentType, null);
        }
    }

    private record AuthorizationContextImpl(
        Methanol.Builder builder, Map<String, List<String>> headers, Map<String, List<String>> queryParameters)
        implements AuthorizationContext {

        @Override
        public void setHeaders(Map<String, List<String>> headers) {
            this.headers.putAll(headers);
        }

        @Override
        public void setQueryParameters(Map<String, List<String>> queryParameters) {
            this.queryParameters.putAll(queryParameters);
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
