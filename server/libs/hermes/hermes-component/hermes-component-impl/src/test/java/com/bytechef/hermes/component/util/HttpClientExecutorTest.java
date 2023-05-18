
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
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.util.HttpClientUtils.Configuration;
import com.bytechef.hermes.definition.registry.util.AuthorizationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.internal.extensions.MimeBodyPublisherAdapter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class HttpClientExecutorTest {

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    private final Context context = Mockito.mock(Context.class);
    private static final ObjectMapper objectMapper = new ObjectMapper() {
        {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
        }
    };

    private static final HttpClientExecutor HTTP_CLIENT_EXECUTOR = new HttpClientExecutor();

    @Test
    public void testCreateBodyHandler() {
        HttpResponse.BodyHandler<?> bodyHandler = HTTP_CLIENT_EXECUTOR.createBodyHandler(
            Configuration.configuration());

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.discarding());

        //

        bodyHandler = HTTP_CLIENT_EXECUTOR.createBodyHandler(
            HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.BINARY));

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofInputStream());

        //

        bodyHandler = HTTP_CLIENT_EXECUTOR.createBodyHandler(
            HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.XML));

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testCreateBodyPublisher() {
        Context.FileEntry fileEntry = Mockito.mock(Context.FileEntry.class);

        Mockito.when(fileEntry.getName())
            .thenReturn("fileName");
        Mockito.when(fileEntry.getExtension())
            .thenReturn("txt");
        Mockito.when(fileEntry.getMimeType())
            .thenReturn("text/plain");

        MultipartBodyPublisher multipartBodyPublisher =
            (MultipartBodyPublisher) HTTP_CLIENT_EXECUTOR.createBodyPublisher(
                context,
                HttpClientUtils.Body.of(
                    Map.of("key1", "value1", "key2", fileEntry), HttpClientUtils.BodyContentType.FORM_DATA));

        Assertions.assertTrue(multipartBodyPublisher.mediaType()
            .toString()
            .startsWith("multipart/form-data"));

        MimeBodyPublisherAdapter mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) multipartBodyPublisher.parts()
            .stream()
            .map(MultipartBodyPublisher.Part::bodyPublisher)
            .filter(bodyPublisher -> bodyPublisher instanceof MimeBodyPublisherAdapter)
            .findFirst()
            .orElseThrow();

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        //

        FormBodyPublisher formBodyPublisher = (FormBodyPublisher) HTTP_CLIENT_EXECUTOR.createBodyPublisher(
            context,
            HttpClientUtils.Body.of(
                Map.of("key1", "value1", "key2", "value2"), HttpClientUtils.BodyContentType.FORM_URL_ENCODED));

        Assertions.assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());

        Assertions.assertTrue(formBodyPublisher.encodedString()
            .contains("key1=value1"));
        Assertions.assertTrue(formBodyPublisher.encodedString()
            .contains("key2=value2"));

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) HTTP_CLIENT_EXECUTOR.createBodyPublisher(
            context,
            HttpClientUtils.Body.of(Map.of("key1", "value1"), HttpClientUtils.BodyContentType.JSON));

        Assertions.assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) HTTP_CLIENT_EXECUTOR.createBodyPublisher(
            context,
            HttpClientUtils.Body.of(Map.of("key1", "value1"), HttpClientUtils.BodyContentType.XML));

        Assertions.assertEquals(MediaType.APPLICATION_XML, mimeBodyPublisherAdapter.mediaType());

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) HTTP_CLIENT_EXECUTOR.createBodyPublisher(
            context, HttpClientUtils.Body.of("text"));

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        HttpRequest.BodyPublisher emptyBodyPublisher = HTTP_CLIENT_EXECUTOR.createBodyPublisher(context, null);

        Assertions.assertEquals(0, emptyBodyPublisher.contentLength());

        //

        fileEntry = Mockito.mock(Context.FileEntry.class);

        Mockito.when(fileEntry.getMimeType())
            .thenReturn("text/plain");
        Mockito.when(fileEntry.getName())
            .thenReturn("fileName");
        Mockito.when(fileEntry.getUrl())
            .thenReturn("base64:text");

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) HTTP_CLIENT_EXECUTOR.createBodyPublisher(
            context, HttpClientUtils.Body.of(fileEntry));

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        //

        HttpRequest.BodyPublisher bodyPublisher = HTTP_CLIENT_EXECUTOR.createBodyPublisher(context, null);

        Assertions.assertEquals(0, bodyPublisher.contentLength());
    }

    @Test
    @SuppressFBWarnings("RV")
    @SuppressWarnings("checkstyle:methodlengthcheck")
    public void testCreateHTTPClient() {
        HttpClient httpClient = HTTP_CLIENT_EXECUTOR.createHttpClient(
            context, null, new HashMap<>(), new HashMap<>(), HttpClientUtils.allowUnauthorizedCerts(true));

        Assertions.assertTrue(httpClient.authenticator()
            .isEmpty());

        Assertions.assertNotNull(httpClient.sslContext());

        //

        Mockito
            .when(context.fetchConnection())
            .thenReturn(
                Optional.of(
                    new MockConnection(
                        ComponentDSL.authorization(AuthorizationType.API_KEY.name(), AuthorizationType.API_KEY))
                            .parameters(
                                Map.of(Authorization.KEY, Authorization.API_TOKEN, Authorization.VALUE,
                                    "token_value"))));

        Map<String, List<String>> headers = new HashMap<>();

        HTTP_CLIENT_EXECUTOR.createHttpClient(context, null, headers, new HashMap<>(), Configuration.configuration());

        Assertions.assertEquals(Map.of(Authorization.API_TOKEN, List.of("token_value")), headers);

        Mockito
            .when(context.fetchConnection())
            .thenReturn(
                Optional.of(
                    new MockConnection(
                        ComponentDSL.authorization(AuthorizationType.API_KEY.name(), AuthorizationType.API_KEY))
                            .parameters(
                                Map.of(
                                    Authorization.KEY, Authorization.API_TOKEN,
                                    Authorization.VALUE, "token_value",
                                    Authorization.ADD_TO, Authorization.ApiTokenLocation.QUERY_PARAMETERS.name()))));

        Map<String, List<String>> queryParameters = new HashMap<>();

        HTTP_CLIENT_EXECUTOR.createHttpClient(
            context, null, new HashMap<>(), queryParameters, Configuration.configuration());

        Assertions.assertEquals(Map.of(Authorization.API_TOKEN, List.of("token_value")), queryParameters);

        Mockito
            .when(context.fetchConnection())
            .thenReturn(
                Optional.of(
                    new MockConnection(
                        ComponentDSL.authorization(
                            Authorization.AuthorizationType.BASIC_AUTH.name(),
                            Authorization.AuthorizationType.BASIC_AUTH))
                                .parameters(
                                    Map.of(Authorization.USERNAME, "username", Authorization.PASSWORD, "password"))));

        headers = new HashMap<>();

        HTTP_CLIENT_EXECUTOR.createHttpClient(context, null, headers, new HashMap<>(), Configuration.configuration());

        Assertions.assertEquals(
            Map.of(
                "Authorization",
                List.of("Basic " + ENCODER
                    .encodeToString("username:password".getBytes(StandardCharsets.UTF_8)))),
            headers);

        Mockito
            .when(context.fetchConnection())
            .thenReturn(
                Optional.of(
                    new MockConnection(
                        ComponentDSL.authorization(
                            Authorization.AuthorizationType.BEARER_TOKEN.name(),
                            Authorization.AuthorizationType.BEARER_TOKEN))
                                .parameters(Map.of(Authorization.TOKEN, "token"))));

        headers = new HashMap<>();

        HTTP_CLIENT_EXECUTOR.createHttpClient(context, null, headers, new HashMap<>(), Configuration.configuration());

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer token")), headers);

        Mockito
            .when(context.fetchConnection())
            .thenReturn(
                Optional.of(
                    new MockConnection(
                        ComponentDSL.authorization(
                            Authorization.AuthorizationType.DIGEST_AUTH.name(),
                            Authorization.AuthorizationType.DIGEST_AUTH))
                                .parameters(
                                    Map.of(Authorization.USERNAME, "username", Authorization.PASSWORD, "password"))));

        headers = new HashMap<>();

        HTTP_CLIENT_EXECUTOR.createHttpClient(context, null, headers, new HashMap<>(), Configuration.configuration());

        Assertions.assertEquals(
            Map.of(
                "Authorization",
                List.of("Basic " + ENCODER.encodeToString("username:password".getBytes(StandardCharsets.UTF_8)))),
            headers);

        Mockito
            .when(context.fetchConnection())
            .thenReturn(
                Optional.of(
                    new MockConnection(
                        ComponentDSL.authorization(
                            Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE.name(),
                            Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
                                .parameters(Map.of(Authorization.ACCESS_TOKEN, "access_token"))));

        headers = new HashMap<>();

        HTTP_CLIENT_EXECUTOR.createHttpClient(context, null, headers, new HashMap<>(), Configuration.configuration());

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer access_token")), headers);

        //

        httpClient = HTTP_CLIENT_EXECUTOR.createHttpClient(
            context, null, new HashMap<>(), new HashMap<>(), HttpClientUtils.followRedirect(true));

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        httpClient = HTTP_CLIENT_EXECUTOR.createHttpClient(
            context, null, new HashMap<>(), new HashMap<>(), HttpClientUtils.followAllRedirects(true));

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        httpClient = HTTP_CLIENT_EXECUTOR.createHttpClient(
            context, null, new HashMap<>(), new HashMap<>(), HttpClientUtils.proxy("10.11.12.13:30"));

        Assertions.assertTrue(httpClient.proxy()
            .isPresent());

        //

        httpClient = HTTP_CLIENT_EXECUTOR.createHttpClient(
            context, null, new HashMap<>(), new HashMap<>(), HttpClientUtils.timeout(Duration.ofMillis(2000)));

        Assertions.assertEquals(
            Duration.ofMillis(2000), httpClient.connectTimeout()
                .orElseThrow());
    }

    @Test
    public void testCreateHTTPRequest() {
        HttpRequest httpRequest = HTTP_CLIENT_EXECUTOR.createHTTPRequest(
            context, "http://localhost:8080", HttpClientUtils.RequestMethod.DELETE,
            Map.of("header1", List.of("value1")), Map.of("param1", List.of("value1")), null);

        Assertions.assertEquals(HttpClientUtils.RequestMethod.DELETE.name(), httpRequest.method());
        Assertions.assertEquals(
            Map.of("header1", List.of("value1")), httpRequest.headers()
                .map());
        Assertions.assertEquals(URI.create("http://localhost:8080?param1=value1"), httpRequest.uri());
    }

    @Test
    public void testHandleResponse() throws Exception {
        Assertions.assertNull(
            HTTP_CLIENT_EXECUTOR.handleResponse(context, new TestHttpResponse(null), Configuration.configuration())
                .body());

        //

        Context.FileEntry fileEntry = Mockito.mock(Context.FileEntry.class);

        Mockito.when(context.storeFileContent(Mockito.anyString(), (InputStream) Mockito.any()))
            .thenReturn(fileEntry);

        Assertions.assertEquals(
            fileEntry,
            HTTP_CLIENT_EXECUTOR.handleResponse(
                context, new TestHttpResponse(new ByteArrayInputStream("text".getBytes(StandardCharsets.UTF_8))),
                HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.BINARY))
                .body());

        //

        Assertions.assertEquals(
            Map.of("key1", "value1"),
            HTTP_CLIENT_EXECUTOR.handleResponse(
                context,
                new TestHttpResponse(
                    """
                        {
                            "key1": "value1"
                        }
                        """),
                HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.JSON))
                .body());

        //

        Assertions.assertEquals(
            "text",
            HTTP_CLIENT_EXECUTOR.handleResponse(
                context, new TestHttpResponse("text"),
                HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.TEXT))
                .body());

        //

        Assertions.assertEquals(
            Map.of("object", Map.of("key1", "value1")),
            HTTP_CLIENT_EXECUTOR.handleResponse(
                context,
                new TestHttpResponse(
                    """
                        <root>
                            <object>
                                <key1>value1</key1>
                            </object>
                        </root>

                        """),
                HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.XML))
                .body());

        //

        Assertions.assertEquals(
            new HttpClientUtils.Response(Map.of(), "text", 200),
            HTTP_CLIENT_EXECUTOR.handleResponse(
                context, new TestHttpResponse("text"),
                HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.TEXT)));
    }

    private static class TestHttpResponse implements HttpResponse<Object> {

        private final Object body;
        private final int statusCode;

        private TestHttpResponse(Object body) {
            this(body, 200);
        }

        private TestHttpResponse(Object body, int statusCode) {
            this.body = body;
            this.statusCode = statusCode;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<Object>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (n, v) -> true);
        }

        @Override
        public Object body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return null;
        }

        @Override
        public HttpClient.Version version() {
            return null;
        }
    }

    @SuppressFBWarnings("NP")
    private static class MockConnection implements Context.Connection {

        private final Authorization authorization;
        private final Map<String, Object> parameters = new HashMap<>();

        public MockConnection(Authorization authorization) {
            this.authorization = authorization;
        }

        @Override
        public void applyAuthorization(AuthorizationContext authorizationContext) {
            // TODO mock ConnectionDefinitionService
            Authorization.ApplyConsumer applyConsumer = AuthorizationUtils.getDefaultApply(authorization.getType());

            applyConsumer.accept(parameters, authorizationContext);
        }

        @Override
        public Optional<String> fetchBaseUri() {
            return Optional.empty();
        }

        @Override
        public String getBaseUri() {
            return null;
        }

        @Override
        public Map<String, Object> getParameters() {
            return null;
        }

        public MockConnection parameters(Map<String, Object> parameters) {
            this.parameters.putAll(parameters);

            return this;
        }

    }
}
