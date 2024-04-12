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

package com.bytechef.platform.component.registry.definition;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.registry.service.ConnectionDefinitionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class HttpClientExecutorTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ActionContext context = Mockito.mock(ActionContext.class);
    private final Context.Http.Configuration configuration = Context.Http.Configuration.newConfiguration()
        .build();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final HttpClientExecutor httpClientExecutor =
        new HttpClientExecutor(
            Mockito.mock(ConnectionDefinitionService.class), Mockito.mock(FileStorageService.class), objectMapper);

    @BeforeAll
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public static void beforeAll() {
        class JsonUtilsMock extends JsonUtils {
            static {
                objectMapper = HttpClientExecutorTest.objectMapper;
            }
        }

        new JsonUtilsMock();

        class XmlUtilsMock extends XmlUtils {
            static {
                xmlMapper = new XmlMapper();
            }
        }

        new XmlUtilsMock();
    }

    @Test
    public void testCreateBodyHandler() {
        HttpResponse.BodyHandler<?> bodyHandler = httpClientExecutor.createBodyHandler(
            configuration);

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.discarding());

        //

        bodyHandler = httpClientExecutor.createBodyHandler(
            Context.Http.responseType(Context.Http.ResponseType.BINARY)
                .build());

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofInputStream());

        //

        bodyHandler = httpClientExecutor.createBodyHandler(
            Context.Http.responseType(Context.Http.ResponseType.XML)
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
                Context.Http.Body.of(
                    Map.of("key1", "value1", "key2", fileEntry), Context.Http.BodyContentType.FORM_DATA));

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
            Context.Http.Body.of(
                Map.of("key1", "value1", "key2", "value2"), Context.Http.BodyContentType.FORM_URL_ENCODED));

        Assertions.assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());

        Assertions.assertTrue(formBodyPublisher.encodedString()
            .contains("key1=value1"));
        Assertions.assertTrue(formBodyPublisher.encodedString()
            .contains("key2=value2"));

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
            Context.Http.Body.of(Map.of("key1", "value1"), Context.Http.BodyContentType.JSON));

        Assertions.assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
            Context.Http.Body.of(Map.of("key1", "value1"), Context.Http.BodyContentType.XML));

        Assertions.assertEquals(MediaType.APPLICATION_XML, mimeBodyPublisherAdapter.mediaType());

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
            Context.Http.Body.of("text"));

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
            Context.Http.Body.of(fileEntry));

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
            new HashMap<>(), new HashMap<>(), Context.Http.allowUnauthorizedCerts(true)
                .build(),
            "componentName", new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

        Assertions.assertTrue(httpClient.authenticator()
            .isEmpty());

        Assertions.assertNotNull(httpClient.sslContext());

        //

//        Mockito.when(context.fetchConnection())
//            .thenReturn(
//                Optional.of(
//                    new MockConnection(
//                        ComponentDSL.authorization(AuthorizationType.API_KEY.name(), AuthorizationType.API_KEY))
//                            .parameters(
//                                Map.of(
//                                    AuthorizationConstants.KEY, AuthorizationConstants.API_TOKEN,
//                                    AuthorizationConstants.VALUE,
//                                    "token_value"))));

        Map<String, List<String>> headers = new HashMap<>();

        httpClientExecutor.createHttpClient(
            headers, new HashMap<>(), configuration, "componentName",
            new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

        Assertions.assertEquals(Map.of(Authorization.API_TOKEN, List.of("token_value")), headers);

//        Mockito.when(context.fetchConnection())
//            .thenReturn(
//                Optional.of(
//                    new MockConnection(
//                        ComponentDSL.authorization(AuthorizationType.API_KEY.name(), AuthorizationType.API_KEY))
//                            .parameters(
//                                Map.of(
//                                    AuthorizationConstants.KEY, AuthorizationConstants.API_TOKEN,
//                                    AuthorizationConstants.VALUE, "token_value",
//                                    AuthorizationConstants.ADD_TO,
//                                    Authorization.ApiTokenLocation.QUERY_PARAMETERS.name()))));

        Map<String, List<String>> queryParameters = new HashMap<>();

        httpClientExecutor.createHttpClient(
            new HashMap<>(), queryParameters, configuration, "componentName",
            new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

        Assertions.assertEquals(Map.of(Authorization.API_TOKEN, List.of("token_value")), queryParameters);

//        Mockito.when(context.fetchConnection())
//            .thenReturn(
//                Optional.of(
//                    new MockConnection(
//                        ComponentDSL.authorization(
//                            Authorization.AuthorizationType.BASIC_AUTH.name(),
//                            Authorization.AuthorizationType.BASIC_AUTH))
//                                .parameters(
//                                    Map.of(AuthorizationConstants.USERNAME, "username", AuthorizationConstants.PASSWORD,
//                                        "password"))));

        headers = new HashMap<>();

        httpClientExecutor.createHttpClient(
            headers, new HashMap<>(), configuration, "componentName",
            new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

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
            headers, new HashMap<>(), configuration, "componentName",
            new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

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
            headers, new HashMap<>(), configuration, "componentName",
            new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

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
            headers, new HashMap<>(), configuration, "componentName",
            new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer access_token")), headers);

        //

        httpClient = httpClientExecutor.createHttpClient(
            new HashMap<>(), new HashMap<>(), Context.Http.followRedirect(true)
                .build(),
            "componentName", new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        httpClient = httpClientExecutor.createHttpClient(
            new HashMap<>(), new HashMap<>(), Context.Http.followAllRedirects(true)
                .build(),
            "componentName", new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        httpClient = httpClientExecutor.createHttpClient(
            new HashMap<>(), new HashMap<>(), Context.Http.proxy("10.11.12.13:30")
                .build(),
            "componentName", new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

        Assertions.assertTrue(httpClient.proxy()
            .isPresent());

        //

        httpClient = httpClientExecutor.createHttpClient(
            new HashMap<>(), new HashMap<>(), Context.Http
                .timeout(Duration.ofMillis(2000))
                .build(),
            "componentName", new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

        Assertions.assertEquals(
            Duration.ofMillis(2000), httpClient.connectTimeout()
                .orElseThrow());
    }

    @Test
    public void testCreateHTTPRequest() {
        HttpRequest httpRequest = httpClientExecutor.createHTTPRequest(
            "http://localhost:8080", Context.Http.RequestMethod.DELETE, Map.of("header1", List.of("value1")),
            Map.of("param1", List.of("value1")), null, "componentName",
            new ComponentConnection("componentName", 1, Map.of(), null), Mockito.mock(Context.class));

        Assertions.assertEquals(Context.Http.RequestMethod.DELETE.name(), httpRequest.method());

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
                    Context.Http
                        .responseType(Context.Http.ResponseType.BINARY)
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
                    Context.Http
                        .responseType(Context.Http.ResponseType.JSON)
                        .build())
                .getBody());

        //

        Assertions.assertEquals(
            "text",
            httpClientExecutor
                .handleResponse(
                    new TestHttpResponse("text"),
                    Context.Http
                        .responseType(Context.Http.ResponseType.TEXT)
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
                    Context.Http
                        .responseType(Context.Http.ResponseType.XML)
                        .build())
                .getBody());

        //

        Context.Http.Configuration.ConfigurationBuilder configurationBuilder =
            Context.Http.responseType(Context.Http.ResponseType.TEXT);

        Assertions.assertEquals(
            new TestResponseImpl(Map.of(), "text", 200),
            httpClientExecutor.handleResponse(new TestHttpResponse("text"), configurationBuilder.build()));
    }

    private static class TestResponseImpl implements Context.Http.Response {

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
        public <T> T getBody(Context.TypeReference<T> valueTypeRef) {
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
}
