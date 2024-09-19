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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.platform.component.domain.ComponentConnection;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.file.storage.FilesFileStorage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.internal.extensions.MimeBodyPublisherAdapter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

/**
 * @author Ivica Cardic
 */
public class HttpClientExecutorTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ActionContext context = Mockito.mock(ActionContext.class);
    private final Http.Configuration configuration = Http.Configuration.newConfiguration()
        .build();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final HttpClientExecutor httpClientExecutor =
        new HttpClientExecutor(
            Mockito.mock(ApplicationContext.class), Mockito.mock(ConnectionDefinitionService.class),
            Mockito.mock(FilesFileStorage.class));

    @Test
    public void testCreateBodyHandler() {
        HttpResponse.BodyHandler<?> bodyHandler = httpClientExecutor.createBodyHandler(
            configuration);

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.discarding());

        //

        bodyHandler = httpClientExecutor.createBodyHandler(
            Http.responseType(Http.ResponseType.BINARY)
                .build());

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofInputStream());

        //

        bodyHandler = httpClientExecutor.createBodyHandler(
            Http.responseType(Http.ResponseType.XML)
                .build());

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testCreateBodyPublisher() {
        FileEntry fileEntry = Mockito.mock(FileEntry.class);

        Mockito.when(fileEntry.getName())
            .thenReturn("fileName");
        Mockito.when(fileEntry.getExtension())
            .thenReturn("txt");
        Mockito.when(fileEntry.getMimeType())
            .thenReturn("text/plain");

        MultipartBodyPublisher multipartBodyPublisher =
            (MultipartBodyPublisher) httpClientExecutor.createBodyPublisher(
                Http.Body.of(
                    Map.of("key1", "value1", "key2", fileEntry), Http.BodyContentType.FORM_DATA));

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

        FormBodyPublisher formBodyPublisher = (FormBodyPublisher) httpClientExecutor.createBodyPublisher(
            Http.Body.of(
                Map.of("key1", "value1", "key2", "value2"), Http.BodyContentType.FORM_URL_ENCODED));

        Assertions.assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());

        Assertions.assertTrue(formBodyPublisher.encodedString()
            .contains("key1=value1"));
        Assertions.assertTrue(formBodyPublisher.encodedString()
            .contains("key2=value2"));

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
            Http.Body.of(Map.of("key1", "value1"), Http.BodyContentType.JSON));

        Assertions.assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
            Http.Body.of(Map.of("key1", "value1"), Http.BodyContentType.XML));

        Assertions.assertEquals(MediaType.APPLICATION_XML, mimeBodyPublisherAdapter.mediaType());

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
            Http.Body.of("text"));

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        HttpRequest.BodyPublisher emptyBodyPublisher = httpClientExecutor.createBodyPublisher(null);

        Assertions.assertEquals(0, emptyBodyPublisher.contentLength());

        //

        fileEntry = Mockito.mock(FileEntry.class);

        Mockito.when(fileEntry.getMimeType())
            .thenReturn("text/plain");
        Mockito.when(fileEntry.getName())
            .thenReturn("fileName");
        Mockito.when(fileEntry.getUrl())
            .thenReturn("base64:text");

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
            Http.Body.of(fileEntry));

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        //

        HttpRequest.BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(null);

        Assertions.assertEquals(0, bodyPublisher.contentLength());
    }

    @Disabled
    @Test
    @SuppressFBWarnings("RV")
    @SuppressWarnings("checkstyle:methodlengthcheck")
    public void testCreateHTTPClient() {
        HttpClient httpClient = httpClientExecutor.createHttpClient(
            new HashMap<>(), new HashMap<>(), Http.allowUnauthorizedCerts(true)
                .build(),
            "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertTrue(httpClient.authenticator()
            .isEmpty());

        Assertions.assertNotNull(httpClient.sslContext());

        //

//        Mockito.when(context.fetchConnection())
//            .thenReturn(
//                Optional.of(
//                    new MockConnection(
//                        ComponentDSL.authorization(AuthorizationType.API_KEY))
//                            .parameters(
//                                Map.of(
//                                    AuthorizationConstants.KEY, AuthorizationConstants.API_TOKEN,
//                                    AuthorizationConstants.VALUE,
//                                    "token_value"))));

        Map<String, List<String>> headers = new HashMap<>();

        httpClientExecutor.createHttpClient(
            headers, new HashMap<>(), configuration, "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertEquals(Map.of(Authorization.API_TOKEN, List.of("token_value")), headers);

//        Mockito.when(context.fetchConnection())
//            .thenReturn(
//                Optional.of(
//                    new MockConnection(
//                        ComponentDSL.authorization(AuthorizationType.API_KEY))
//                            .parameters(
//                                Map.of(
//                                    AuthorizationConstants.KEY, AuthorizationConstants.API_TOKEN,
//                                    AuthorizationConstants.VALUE, "token_value",
//                                    AuthorizationConstants.ADD_TO,
//                                    Authorization.ApiTokenLocation.QUERY_PARAMETERS.name()))));

        Map<String, List<String>> queryParameters = new HashMap<>();

        httpClientExecutor.createHttpClient(
            new HashMap<>(), queryParameters, configuration, "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertEquals(Map.of(Authorization.API_TOKEN, List.of("token_value")), queryParameters);

//        Mockito.when(context.fetchConnection())
//            .thenReturn(
//                Optional.of(
//                    new MockConnection(
//                        ComponentDSL.authorization(Authorization.AuthorizationType.BASIC_AUTH))
//                                .parameters(
//                                    Map.of(AuthorizationConstants.USERNAME, "username", AuthorizationConstants.PASSWORD,
//                                        "password"))));

        headers = new HashMap<>();

        httpClientExecutor.createHttpClient(
            headers, new HashMap<>(), configuration, "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertEquals(
            Map.of(
                "Authorization",
                List.of("Basic " + encoder
                    .encodeToString("username:password".getBytes(StandardCharsets.UTF_8)))),
            headers);

//        Mockito.when(context.fetchConnection())
//            .thenReturn(
//                Optional.of(
//                    new MockConnection(
//                        ComponentDSL.authorization(
//                            Authorization.AuthorizationType.BEARER_TOKEN.name(),
//                            Authorization.AuthorizationType.BEARER_TOKEN))
//                                .parameters(Map.of(AuthorizationConstants.TOKEN, "token"))));

        headers = new HashMap<>();

        httpClientExecutor.createHttpClient(
            headers, new HashMap<>(), configuration, "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer token")), headers);

//        Mockito.when(context.fetchConnection())
//            .thenReturn(
//                Optional.of(
//                    new MockConnection(
//                        ComponentDSL.authorization(
//                            Authorization.AuthorizationType.DIGEST_AUTH.name(),
//                            Authorization.AuthorizationType.DIGEST_AUTH))
//                                .parameters(
//                                    Map.of(AuthorizationConstants.USERNAME, "username", AuthorizationConstants.PASSWORD,
//                                        "password"))));

        headers = new HashMap<>();

        httpClientExecutor.createHttpClient(
            headers, new HashMap<>(), configuration, "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertEquals(
            Map.of(
                "Authorization",
                List.of("Basic " + encoder.encodeToString("username:password".getBytes(StandardCharsets.UTF_8)))),
            headers);

//        Mockito.when(context.fetchConnection())
//            .thenReturn(
//                Optional.of(
//                    new MockConnection(
//                        ComponentDSL.authorization(
//                            Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE.name(),
//                            Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
//                                .parameters(Map.of(AuthorizationConstants.ACCESS_TOKEN, "access_token"))));

        headers = new HashMap<>();

        httpClientExecutor.createHttpClient(
            headers, new HashMap<>(), configuration, "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer access_token")), headers);

        //

        httpClient = httpClientExecutor.createHttpClient(
            new HashMap<>(), new HashMap<>(), Http.followRedirect(true)
                .build(),
            "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        httpClient = httpClientExecutor.createHttpClient(
            new HashMap<>(), new HashMap<>(), Http.followAllRedirects(true)
                .build(),
            "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        httpClient = httpClientExecutor.createHttpClient(
            new HashMap<>(), new HashMap<>(), Http.proxy("10.11.12.13:30")
                .build(),
            "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertTrue(httpClient.proxy()
            .isPresent());

        //

        httpClient = httpClientExecutor.createHttpClient(
            new HashMap<>(), new HashMap<>(), Http
                .timeout(Duration.ofMillis(2000))
                .build(),
            "componentName", 1, "componentOperationName",
            new ComponentConnection("componentName", 1, -1L, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertEquals(
            Duration.ofMillis(2000), httpClient.connectTimeout()
                .orElseThrow());
    }

    @Test
    public void testCreateHTTPRequest() {
        HttpRequest httpRequest = httpClientExecutor.createHTTPRequest(
            "http://localhost:8080", Http.RequestMethod.DELETE, Map.of("header1", List.of("value1")),
            Map.of("param1", List.of("value1")), null, "componentName",
            new ComponentConnection("componentName", 1, -1L, Map.of(), Authorization.AuthorizationType.NONE.name()),
            Mockito.mock(Context.class));

        Assertions.assertEquals(Http.RequestMethod.DELETE.name(), httpRequest.method());

        HttpHeaders httpHeaders = httpRequest.headers();

        Assertions.assertEquals(Map.of("header1", List.of("value1")), httpHeaders.map());
        Assertions.assertEquals(URI.create("http://localhost:8080?param1=value1"), httpRequest.uri());
    }

    @Disabled
    @Test
    public void testHandleResponse() {
        Assertions.assertNull(
            httpClientExecutor
                .handleResponse(new TestHttpResponse(null), configuration)
                .getBody());

        //

        FileEntry fileEntry = Mockito.mock(FileEntry.class);

        Mockito
            .when(context.file(file -> file.storeContent(Mockito.anyString(), (InputStream) Mockito.any())))
            .thenReturn(fileEntry);

        Assertions.assertEquals(
            fileEntry,
            httpClientExecutor
                .handleResponse(
                    new TestHttpResponse(new ByteArrayInputStream("text".getBytes(StandardCharsets.UTF_8))),
                    Http.responseType(Http.ResponseType.BINARY)
                        .build())
                .getBody());

        //

        Assertions.assertEquals(
            Map.of("key1", "value1"),
            httpClientExecutor
                .handleResponse(
                    new TestHttpResponse(
                        """
                            {
                                "key1": "value1"
                            }
                            """),
                    Http.responseType(Http.ResponseType.JSON)
                        .build())
                .getBody());

        //

        Assertions.assertEquals(
            "text",
            httpClientExecutor
                .handleResponse(
                    new TestHttpResponse("text"),
                    Http.responseType(Http.ResponseType.TEXT)
                        .build())
                .getBody());

        //

        Assertions.assertEquals(
            Map.of("object", Map.of("key1", "value1")),
            httpClientExecutor
                .handleResponse(
                    new TestHttpResponse(
                        """
                            <root>
                                <object>
                                    <key1>value1</key1>
                                </object>
                            </root>

                            """),
                    Http.responseType(Http.ResponseType.XML)
                        .build())
                .getBody());

        //

        Http.Configuration.ConfigurationBuilder configurationBuilder =
            Http.responseType(Http.ResponseType.TEXT);

        Assertions.assertEquals(
            new TestResponseImpl(Map.of(), "text", 200),
            httpClientExecutor.handleResponse(new TestHttpResponse("text"), configurationBuilder.build()));
    }

    @Test
    public void testHandleResponseWithIncompatibleResponseType() {
        TestHttpResponse testHttpResponse = new TestHttpResponse(
            "IncompatibleResponseType body - text instead of JSON",
            HttpHeaders.of(Map.of("content-type", List.of("text/html;charset=UTF-8")), (s, s2) -> true), 404);

        Http.Configuration.ConfigurationBuilder configurationBuilder =
            Http.Configuration.newConfiguration()
                .responseType(Http.ResponseType.JSON);

        Http.Response response =
            httpClientExecutor.handleResponse(testHttpResponse, configurationBuilder.build());

        Assertions.assertNull(response.getBody());

        testHttpResponse = new TestHttpResponse(
            "{\"key1\":\"value1\", \"key2\":\"value2\"}",
            HttpHeaders.of(Map.of("content-type", List.of("application/json")), (s, s2) -> true), 404);

        response = httpClientExecutor.handleResponse(testHttpResponse, configurationBuilder.build());

        Assertions.assertNotNull(response.getBody());

        Assertions.assertEquals(Map.of("key1", "value1", "key2", "value2"), response.getBody());

        testHttpResponse = new TestHttpResponse(
            "<root><key1>\"value1\"</key1><key2>\"value2\"</key2></root>",
            HttpHeaders.of(Map.of("content-type", List.of("application/xml")), (s, s2) -> true), 200);

        response = httpClientExecutor.handleResponse(testHttpResponse, configurationBuilder.build());

        Assertions.assertNull(response.getBody());

        testHttpResponse = new TestHttpResponse(
            "<root><key1>value1</key1><key2>value2</key2></root>",
            HttpHeaders.of(Map.of("content-type", List.of("application/xml")), (s, s2) -> true), 200);

        response = httpClientExecutor.handleResponse(
            testHttpResponse, configurationBuilder.responseType(Http.ResponseType.XML)
                .build());

        Assertions.assertNotNull(response.getBody());

        Assertions.assertEquals(Map.of("key1", "value1", "key2", "value2"), response.getBody());
    }

    private static class TestResponseImpl implements Http.Response {

        private final Map<String, List<String>> headers;
        private final Object body;
        private final int statusCode;

        private TestResponseImpl(Map<String, List<String>> headers, Object body, int statusCode) {
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
            return objectMapper.convertValue(body, new TypeReference<>() {});
        }

        @Override
        public <T> T getBody(Class<T> valueType) {
            return objectMapper.convertValue(body, valueType);
        }

        @Override
        public <T> T getBody(com.bytechef.component.definition.TypeReference<T> valueTypeRef) {
            return objectMapper.convertValue(body, new TypeReference<>() {

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

    private static class TestHttpResponse implements HttpResponse<Object> {

        private final Object body;
        private final int statusCode;
        private final HttpHeaders httpHeaders;

        private TestHttpResponse(Object body) {
            this(body, 200);
        }

        private TestHttpResponse(Object body, int statusCode) {
            this(body, null, statusCode);
        }

        private TestHttpResponse(Object body, HttpHeaders httpHeaders, int statusCode) {
            this.body = body;
            this.httpHeaders = httpHeaders;
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
            if (httpHeaders != null) {
                return httpHeaders;
            }

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
}
