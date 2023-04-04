
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
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.Authorization;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationContext;
import com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.util.HttpClientUtils.Configuration;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
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
public class HttpClientUtilsTest {

    public static final Base64.Encoder ENCODER = Base64.getEncoder();
    private final Context context = Mockito.mock(Context.class);

    @Test
    public void testCreateBodyHandler() {
        HttpResponse.BodyHandler<?> bodyHandler = HttpClientUtils.createBodyHandler(
            Configuration.configuration());

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.discarding());

        //

        bodyHandler = HttpClientUtils.createBodyHandler(
            HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.BINARY));

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofInputStream());

        //

        bodyHandler = HttpClientUtils.createBodyHandler(
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

        MultipartBodyPublisher multipartBodyPublisher = (MultipartBodyPublisher) HttpClientUtils.createBodyPublisher(
            context,
            HttpClientUtils.Payload.of(
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

        FormBodyPublisher formBodyPublisher = (FormBodyPublisher) HttpClientUtils.createBodyPublisher(
            context,
            HttpClientUtils.Payload.of(
                Map.of("key1", "value1", "key2", "value2"), HttpClientUtils.BodyContentType.FORM_URL_ENCODED));

        Assertions.assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());

        Assertions.assertTrue(formBodyPublisher.encodedString()
            .contains("key1=value1"));
        Assertions.assertTrue(formBodyPublisher.encodedString()
            .contains("key2=value2"));

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) HttpClientUtils.createBodyPublisher(
            context,
            HttpClientUtils.Payload.of(Map.of("key1", "value1"), HttpClientUtils.BodyContentType.JSON));

        Assertions.assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) HttpClientUtils.createBodyPublisher(
            context,
            HttpClientUtils.Payload.of(Map.of("key1", "value1"), HttpClientUtils.BodyContentType.XML));

        Assertions.assertEquals(MediaType.APPLICATION_XML, mimeBodyPublisherAdapter.mediaType());

        //

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) HttpClientUtils.createBodyPublisher(
            context, HttpClientUtils.Payload.of("text"));

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        HttpRequest.BodyPublisher emptyBodyPublisher = HttpClientUtils.createBodyPublisher(context, null);

        Assertions.assertEquals(0, emptyBodyPublisher.contentLength());

        //

        fileEntry = Mockito.mock(Context.FileEntry.class);

        Mockito.when(fileEntry.getMimeType())
            .thenReturn("text/plain");
        Mockito.when(fileEntry.getName())
            .thenReturn("fileName");
        Mockito.when(fileEntry.getUrl())
            .thenReturn("base64:text");

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) HttpClientUtils.createBodyPublisher(
            context, HttpClientUtils.Payload.of(fileEntry));

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        //

        HttpRequest.BodyPublisher bodyPublisher = HttpClientUtils.createBodyPublisher(context, null);

        Assertions.assertEquals(0, bodyPublisher.contentLength());
    }

    @Test
    @SuppressFBWarnings("RV")
    @SuppressWarnings("checkstyle:methodlengthcheck")
    public void testCreateHTTPClient() {
        HttpClient httpClient = HttpClientUtils.createHttpClient(
            context, new HashMap<>(), new HashMap<>(), HttpClientUtils.allowUnauthorizedCerts(true));

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
                            Map.of(Authorization.KEY, Authorization.API_TOKEN, Authorization.VALUE, "token_value"))));

        Map<String, List<String>> headers = new HashMap<>();

        HttpClientUtils.createHttpClient(context, headers, new HashMap<>(), Configuration.configuration());

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

        HttpClientUtils.createHttpClient(
            context, new HashMap<>(), queryParameters, Configuration.configuration());

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

        HttpClientUtils.createHttpClient(context, headers, new HashMap<>(), Configuration.configuration());

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

        HttpClientUtils.createHttpClient(context, headers, new HashMap<>(), Configuration.configuration());

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer token")), headers);

        Mockito
            .when(context.fetchConnection())
            .thenReturn(
                Optional.of(
                    new MockConnection(
                        ComponentDSL.authorization(
                            Authorization.AuthorizationType.DIGEST_AUTH.name(),
                            Authorization.AuthorizationType.DIGEST_AUTH))
                        .parameters(Map.of(Authorization.USERNAME, "username", Authorization.PASSWORD, "password"))));

        headers = new HashMap<>();

        HttpClientUtils.createHttpClient(context, headers, new HashMap<>(), Configuration.configuration());

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

        HttpClientUtils.createHttpClient(context, headers, new HashMap<>(), Configuration.configuration());

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer access_token")), headers);

        //

        httpClient = HttpClientUtils.createHttpClient(
            context, new HashMap<>(), new HashMap<>(), HttpClientUtils.followRedirect(true));

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        httpClient = HttpClientUtils.createHttpClient(
            context, new HashMap<>(), new HashMap<>(), HttpClientUtils.followAllRedirects(true));

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        httpClient = HttpClientUtils.createHttpClient(
            context, new HashMap<>(), new HashMap<>(), HttpClientUtils.proxy("10.11.12.13:30"));

        Assertions.assertTrue(httpClient.proxy()
            .isPresent());

        //

        httpClient = HttpClientUtils.createHttpClient(
            context, new HashMap<>(), new HashMap<>(), HttpClientUtils.timeout(Duration.ofMillis(2000)));

        Assertions.assertEquals(
            Duration.ofMillis(2000), httpClient.connectTimeout()
                .orElseThrow());
    }

    @Test
    public void testCreateHTTPRequest() {
        HttpRequest httpRequest = HttpClientUtils.createHTTPRequest(
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
            HttpClientUtils.handleResponse(context, new TestHttpResponse(null), Configuration.configuration())
                .body());

        //

        Context.FileEntry fileEntry = Mockito.mock(Context.FileEntry.class);

        Mockito.when(context.storeFileContent(Mockito.anyString(), (InputStream) Mockito.any()))
            .thenReturn(fileEntry);

        Assertions.assertEquals(
            fileEntry,
            HttpClientUtils.handleResponse(
                context, new TestHttpResponse(new ByteArrayInputStream("text".getBytes(StandardCharsets.UTF_8))),
                HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.BINARY))
                .body());

        //

        Assertions.assertEquals(
            Map.of("key1", "value1"),
            HttpClientUtils.handleResponse(
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
            HttpClientUtils.handleResponse(
                context, new TestHttpResponse("text"),
                HttpClientUtils.responseFormat(HttpClientUtils.ResponseFormat.TEXT))
                .body());

        //

        Assertions.assertEquals(
            Map.of("object", Map.of("key1", "value1")),
            HttpClientUtils.handleResponse(
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
            new HttpClientUtils.Response("text", Map.of(), 200),
            HttpClientUtils.handleResponse(
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

    private static class MockConnection implements Context.Connection {

        private final Authorization authorization;
        private final Map<String, Object> parameters = new HashMap<>();

        public MockConnection(Authorization authorization) {
            this.authorization = authorization;
        }

        @Override
        public void applyAuthorization(AuthorizationContext authorizationContext) {
            authorization.getApply()
                .accept(
                    new HttpClientUtilsTest.MockInputParameters(parameters), authorizationContext, null, null);
        }

        @Override
        public Optional<String> fetchBaseUri() {
            return Optional.empty();
        }

        @Override
        public InputParameters getParameters() {
            return null;
        }

        public MockConnection parameters(Map<String, Object> parameters) {
            this.parameters.putAll(parameters);

            return this;
        }

    }

    @SuppressFBWarnings("NP")
    private static class MockInputParameters implements InputParameters {

        private final Map<String, Object> parameters;

        private MockInputParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        @Override
        public boolean containsKey(String key) {
            return false;
        }

        @Override
        public Object get(String key) {
            return null;
        }

        @Override
        public <T> T get(String key, Class<T> returnType) {
            return null;
        }

        @Override
        public <T> T get(String key, Class<T> returnType, T defaultValue) {
            return null;
        }

        @Override
        public <T> T[] getArray(String key, Class<T> elementType) {
            return null;
        }

        @Override
        public Boolean getBoolean(String key) {
            return null;
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return false;
        }

        @Override
        public Date getDate(String key) {
            return null;
        }

        @Override
        public Date getDate(String key, Date defaultValue) {
            return null;
        }

        @Override
        public Double getDouble(String key) {
            return null;
        }

        @Override
        public double getDouble(String key, double defaultValue) {
            return 0;
        }

        @Override
        public Duration getDuration(String key) {
            return null;
        }

        @Override
        public Duration getDuration(String key, Duration defaultDuration) {
            return null;
        }

        @Override
        public Float getFloat(String key) {
            return null;
        }

        @Override
        public float getFloat(String key, float defaultValue) {
            return 0;
        }

        @Override
        public Integer getInteger(String key) {
            return null;
        }

        @Override
        public int getInteger(String key, int defaultValue) {
            return 0;
        }

        @Override
        public <T> List<T> getList(String key, Class<T> elementType) {
            return null;
        }

        @Override
        public <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue) {
            return null;
        }

        @Override
        public List<Object> getList(String key, List<Class<?>> elementTypes, List<Object> defaultValue) {
            return null;
        }

        @Override
        public LocalDate getLocalDate(String key) {
            return null;
        }

        @Override
        public LocalDate getLocalDate(String key, LocalDate defaultValue) {
            return null;
        }

        @Override
        public LocalDateTime getLocalDateTime(String key) {
            return null;
        }

        @Override
        public LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue) {
            return null;
        }

        @Override
        public Long getLong(String key) {
            return null;
        }

        @Override
        public long getLong(String key, long defaultValue) {
            return 0;
        }

        @Override
        public <V> Map<String, V> getMap(String key) {
            return null;
        }

        @Override
        public <V> Map<String, V> getMap(String key, Map<String, V> defaultValue) {
            return null;
        }

        @Override
        public Map<String, Object> getMap(String key, List<Class<?>> valueTypes, Map<String, Object> defaultValue) {
            return null;
        }

        @Override
        public <T> T getRequired(String key) {
            return null;
        }

        @Override
        public <T> T getRequired(String key, Class<T> returnType) {
            return null;
        }

        @Override
        public Boolean getRequiredBoolean(String key) {
            return null;
        }

        @Override
        public Date getRequiredDate(String key) {
            return null;
        }

        @Override
        public Double getRequiredDouble(String key) {
            return null;
        }

        @Override
        public Float getRequiredFloat(String key) {
            return null;
        }

        @Override
        public Integer getRequiredInteger(String key) {
            return null;
        }

        @Override
        public LocalDate getRequiredLocalDate(String key) {
            return null;
        }

        @Override
        public LocalDateTime getRequiredLocalDateTime(String key) {
            return null;
        }

        @Override
        public String getRequiredString(String key) {
            return null;
        }

        @Override
        public String getString(String key) {
            return (String) parameters.get(key);
        }

        @Override
        public String getString(String key, String defaultValue) {
            return (String) parameters.getOrDefault(key, defaultValue);
        }
    }
}
