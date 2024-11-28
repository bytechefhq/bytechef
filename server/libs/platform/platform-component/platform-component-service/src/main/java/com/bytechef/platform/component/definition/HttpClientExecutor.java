/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.definition;

import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.RequestMethod;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.platform.component.domain.ComponentConnection;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.OperationDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.util.RefreshCredentialsUtils;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MoreBodyPublishers;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
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
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
class HttpClientExecutor {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientExecutor.class);

    private final ApplicationContext applicationContext;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final FilesFileStorage filesFileStorage;

    @SuppressFBWarnings("EI")
    public HttpClientExecutor(
        ApplicationContext applicationContext, ConnectionDefinitionService connectionDefinitionService,
        FilesFileStorage filesFileStorage) {

        this.applicationContext = applicationContext;
        this.connectionDefinitionService = connectionDefinitionService;
        this.filesFileStorage = filesFileStorage;
    }

    public Response execute(
        String urlString, Map<String, List<String>> headers, Map<String, List<String>> queryParameters, Body body,
        Configuration configuration, RequestMethod requestMethod, String componentName, int componentVersion,
        String componentOperationName, ComponentConnection componentConnection, Context context)
        throws Exception {

        HttpResponse<?> httpResponse;

        try (HttpClient httpClient = createHttpClient(
            headers, queryParameters, configuration, componentName, componentVersion, componentOperationName,
            componentConnection, context)) {

            HttpRequest httpRequest = createHTTPRequest(
                urlString, requestMethod, headers, queryParameters, body, componentName, componentConnection, context);

            if (logger.isDebugEnabled()) {
                logger.debug(
                    "uri: {}, requestMethod: {}, headers: {}, queryParameters: {}, responseType: {}",
                    httpRequest.uri(), requestMethod, headers, queryParameters, requestMethod);
            }

            httpResponse = httpClient.send(httpRequest, createBodyHandler(configuration));
        }

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
            if (body.getContentType() == Http.BodyContentType.BINARY &&
                body.getContent() instanceof FileEntry fileEntry) {

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
        String componentName, int componentVersion, String componentOperationName,
        ComponentConnection componentConnection, Context context) {

        Methanol.Builder builder = Methanol.newBuilder()
            .version(HttpClient.Version.HTTP_1_1);

        if (configuration.isAllowUnauthorizedCerts()) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                sslContext.init(
                    null, new TrustManager[] {
                        new UnauthorizedCertsX509ExtendedTrustManager()
                    },
                    null);

                builder.sslContext(sslContext);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (!configuration.isDisableAuthorization() && (componentConnection != null) &&
            Authorization.AuthorizationType.isApplicable(componentConnection.authorizationName())) {

            applyAuthorization(headers, queryParameters, componentName, componentConnection, context);

            boolean isAction = !(context instanceof TriggerContext);

            builder.interceptor(
                getInterceptor(
                    componentName, componentVersion, componentOperationName, componentConnection.version(),
                    componentConnection.authorizationName(), componentConnection.canCredentialsBeRefreshed(),
                    isAction));
        }

        if (configuration.isFollowRedirect()) {
            builder.followRedirects(HttpClient.Redirect.NORMAL);
        }

        if (configuration.isFollowAllRedirects()) {
            builder.followRedirects(HttpClient.Redirect.ALWAYS);
        }

        String proxy = configuration.getProxy();

        if (StringUtils.isNoneEmpty(proxy)) {
            String[] hostPortArray = proxy.split(":");

            builder.proxy(
                ProxySelector.of(new InetSocketAddress(hostPortArray[0], Integer.parseInt(hostPortArray[1]))));
        }

        if (configuration.getTimeout() == null) {
            builder.connectTimeout(Duration.ofMillis(4000));
        } else {
            builder.connectTimeout(configuration.getTimeout());
        }

        return builder.build();
    }

    HttpRequest createHTTPRequest(
        String urlString, RequestMethod requestMethod, Map<String, List<String>> headers,
        Map<String, List<String>> queryParameters, Body body, String componentName,
        ComponentConnection componentConnection, Context context) {

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
            .method(requestMethod.name(), createBodyPublisher(body));

        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                httpRequestBuilder.header(entry.getKey(), value);
            }
        }

        httpRequestBuilder.uri(
            createURI(
                getConnectionUrl(urlString, componentName, componentConnection, context),
                queryParameters == null ? Collections.emptyMap() : queryParameters));

        return httpRequestBuilder.build();
    }

    Response handleResponse(HttpResponse<?> httpResponse, Configuration configuration) {
        HttpHeaders httpHeaders = httpResponse.headers();
        ResponseType responseType = configuration.getResponseType();
        int statusCode = httpResponse.statusCode();

        if (statusCode != 204 &&
            ((responseType == null) || !matches(responseType, httpHeaders.firstValue("content-type")))) {

            logger.warn(
                "Unexpected response body content-type type: {} can not be converted to {}",
                httpHeaders.firstValue("content-type"), responseType);

            return new ResponseImpl(httpHeaders.map(), null, statusCode);
        }

        Object httpResponseBody = httpResponse.body();

        if (isEmpty(httpResponseBody)) {
            return new ResponseImpl(httpHeaders.map(), null, statusCode);
        }

        return switch (responseType) {
            case BINARY -> new ResponseImpl(
                httpHeaders.map(),
                storeBinaryResponseBody(configuration, httpHeaders.map(), (InputStream) httpResponseBody), statusCode);
            case JSON -> new ResponseImpl(httpHeaders.map(), JsonUtils.read(httpResponseBody.toString()), statusCode);
            case XML -> new ResponseImpl(httpHeaders.map(), XmlUtils.read(httpResponseBody.toString()), statusCode);
            default -> new ResponseImpl(httpHeaders.map(), httpResponseBody.toString(), statusCode);
        };
    }

    private void addFileEntry(MultipartBodyPublisher.Builder builder, String name, FileEntry fileEntry) {
        builder.formPart(
            name, fileEntry.getName(),
            MoreBodyPublishers.ofMediaType(
                BodyPublishers.ofInputStream(() -> filesFileStorage.getFileStream(
                    ((FileEntryImpl) fileEntry).getFileEntry())),
                MediaType.parse(fileEntry.getMimeType())));
    }

    private void applyAuthorization(
        Map<String, List<String>> headers, Map<String, List<String>> queryParameters, String componentName,
        ComponentConnection componentConnection, Context context) {

        if ((componentConnection == null) || Objects.equals(componentConnection.getAuthorizationName(), "none")) {
            return;
        }

        ApplyResponse applyResponse = connectionDefinitionService.executeAuthorizationApply(
            componentName, componentConnection.version(),
            Objects.requireNonNull(componentConnection.authorizationName()), componentConnection.getParameters(),
            context);

        if (applyResponse != null) {
            headers.putAll(applyResponse.getHeaders());
            queryParameters.putAll(applyResponse.getQueryParameters());
        }
    }

    private URI createURI(String urlString, @NonNull Map<String, List<String>> queryParameters) {
        URI uri;

        if (queryParameters.isEmpty()) {
            uri = URI.create(urlString);
        } else {
            String parameter = queryParameters
                .entrySet()
                .stream()
                .flatMap(entry -> entry.getValue()
                    .stream()
                    .map(value -> entry.getKey() + "=" + value))
                .collect(Collectors.joining("&"));

            uri = URI.create(urlString + '?' + parameter);
        }

        return uri;
    }

    private BodyPublisher getBinaryBodyPublisher(Body body, FileEntry fileEntry) {
        return MoreBodyPublishers.ofMediaType(
            BodyPublishers.ofInputStream(
                () -> filesFileStorage.getFileStream(((FileEntryImpl) fileEntry).getFileEntry())),
            MediaType.parse(body.getMimeType() == null ? fileEntry.getMimeType() : body.getMimeType()));
    }

    private String getConnectionUrl(
        String urlString, String componentName, ComponentConnection componentConnection, Context context) {

        if (urlString.contains("://") || (componentConnection == null)) {
            return urlString;
        }

        return OptionalUtils.map(
            connectionDefinitionService.executeBaseUri(componentName, componentConnection, context),
            baseUri -> baseUri + urlString);

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

        processParameters("", bodyParameters, builder);

        return builder.build();
    }

    private void processParameters(String prefix, Map<?, ?> parameters, FormBodyPublisher.Builder builder) {
        parameters.forEach((key, value) -> {
            Validate.notNull(value, "Expected value for " + key);

            String newKey = prefix.isEmpty() ? key.toString() : prefix + "[" + key + "]";

            if (value instanceof Map<?, ?> nestedMap) {
                processParameters(newKey, nestedMap, builder);
            } else if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);

                    builder.query(newKey + "[" + i + "]", item.toString());
                }
            } else {
                builder.query(newKey, value.toString());
            }
        });
    }

    /**
     * Gets interceptor that scans valid response bodyContent for information on token refresh errors.
     *
     * @param componentName
     * @param componentVersion
     * @param componentOperationName
     * @param connectionVersion
     * @param authorizationName
     * @param credentialsBeRefreshed
     * @param isAction
     * @return
     */
    private Methanol.Interceptor getInterceptor(
        String componentName, int componentVersion, String componentOperationName, int connectionVersion,
        String authorizationName, boolean credentialsBeRefreshed, boolean isAction) {

        return new Methanol.Interceptor() {
            @Override
            public <T> HttpResponse<T> intercept(HttpRequest httpRequest, Chain<T> chain)
                throws IOException, InterruptedException {

                logger.trace("Intercepting request to analyze response");

                HttpResponse<T> httpResponse = chain.forward(httpRequest);

                OperationDefinitionFacade operationDefinitionFacade = getOperationDefinitionFacade(isAction);

                if ((httpResponse.statusCode() > 199) && (httpResponse.statusCode() < 400)) {
                    List<String> detectOn = connectionDefinitionService.getAuthorizationDetectOn(
                        componentName, connectionVersion, authorizationName);

                    if (credentialsBeRefreshed && !detectOn.isEmpty()) {
                        Object body = httpResponse.body();

                        if (body != null && RefreshCredentialsUtils.matches(body.toString(), detectOn)) {
                            throw operationDefinitionFacade.executeProcessErrorResponse(
                                componentName, componentVersion, componentOperationName, httpResponse.statusCode(),
                                body);
                        }
                    }

                    return httpResponse;
                }

                Object body = httpResponse.body();

                throw operationDefinitionFacade.executeProcessErrorResponse(
                    componentName, componentVersion, componentOperationName, httpResponse.statusCode(), body);
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> interceptAsync(HttpRequest httpRequest, Chain<T> chain) {
                logger.trace("Intercepting ASYNC request to analyze response");

                return chain.forwardAsync(httpRequest);
            }
        };
    }

    private BodyPublisher getJsonBodyPublisher(Body body) {
        return MoreBodyPublishers.ofMediaType(
            BodyPublishers.ofString(JsonUtils.write(body.getContent())), MediaType.APPLICATION_JSON);
    }

    private OperationDefinitionFacade getOperationDefinitionFacade(boolean isAction) {
        OperationDefinitionFacade operationDefinitionFacade;

        if (isAction) {
            operationDefinitionFacade = applicationContext.getBean(ActionDefinitionFacade.class);
        } else {
            operationDefinitionFacade = applicationContext.getBean(TriggerDefinitionFacade.class);
        }

        return operationDefinitionFacade;
    }

    private BodyPublisher getStringBodyPublisher(Body body) {
        Object content = body.getContent();

        return MoreBodyPublishers.ofMediaType(
            BodyPublishers.ofString(content.toString()), MediaType.parse(body.getMimeType()));
    }

    private BodyPublisher getXmlBodyPublisher(Body body) {
        return MoreBodyPublishers.ofMediaType(
            BodyPublishers.ofString(XmlUtils.write(body.getContent())), MediaType.APPLICATION_XML);
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

    private boolean matches(ResponseType responseType, Optional<String> contentTypeValueOptional) {
        return contentTypeValueOptional.isPresent() &&
            StringUtils.containsIgnoreCase(contentTypeValueOptional.get(), responseType.name());
    }

    private FileEntry storeBinaryResponseBody(
        Configuration configuration, Map<String, List<String>> headers, InputStream httpResponseBody) {

        String filename = configuration.getFilename();

        if (filename == null || filename.isEmpty()) {
            if (headers.containsKey("Content-Type")) {
                List<String> values = headers.get("Content-Type");

                filename = "file." + MimeTypeUtils.getDefaultExt(values.getFirst());
            } else {
                filename = "file.txt";
            }
        }

        return new FileEntryImpl(filesFileStorage.storeFileContent(filename, httpResponseBody));
    }

    private static class ResponseImpl implements Response {

        private final Map<String, List<String>> headers;
        private final Object body;
        private final int statusCode;

        private ResponseImpl(Map<String, List<String>> headers, Object body, int statusCode) {
            this.headers = headers;
            this.body = body;
            this.statusCode = statusCode;
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        @Override
        public Object getBody() {
            return ConvertUtils.convertValue(body, new TypeReference<>() {});
        }

        @Override
        public <T> T getBody(Class<T> valueType) {
            return ConvertUtils.convertValue(body, valueType);
        }

        @Override
        public <T> T getBody(com.bytechef.component.definition.TypeReference<T> valueTypeRef) {
            return ConvertUtils.convertValue(body, new TypeReference<>() {

                @Override
                public Type getType() {
                    return valueTypeRef.getType();
                }
            });
        }

        @Override
        public String getFirstHeader(String name) {
            List<String> values = headers.get(name);

            return values.getFirst();
        }

        @Override
        public List<String> getHeader(String name) {
            return headers.get(name);
        }

        @Override
        public int getStatusCode() {
            return statusCode;
        }
    }

    private static class UnauthorizedCertsX509ExtendedTrustManager extends X509ExtendedTrustManager {

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(final X509Certificate[] x509Certificates, final String authType) {
        }

        public void checkServerTrusted(final X509Certificate[] x509Certificates, final String authType) {
        }

        public void checkClientTrusted(
            final X509Certificate[] x509Certificates, final String certificates, final Socket socket) {
        }

        public void checkServerTrusted(
            final X509Certificate[] x509Certificates, final String authType, final Socket socket) {
        }

        public void checkClientTrusted(
            final X509Certificate[] x509Certificates, final String authType, final SSLEngine engine) {
        }

        public void checkServerTrusted(
            final X509Certificate[] x509Certificates, final String authType, final SSLEngine engine) {
        }
    }
}
