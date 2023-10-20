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
import static com.bytechef.hermes.connection.ConnectionConstants.CONNECTION_ID;

import com.bytechef.commons.collection.MapUtils;
import com.bytechef.hermes.component.ConnectionParameters;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.http.client.constants.HttpClientConstants;
import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.hermes.connection.domain.Connection;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.internal.extensions.MimeBodyPublisherAdapter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
public class HttpClientTest {

    private final Context context = Mockito.spy(new MockContext());
    private final HttpClient httpClient = new HttpClient();

    @Test
    public void testCreateBodyHandler() {
        MockExecutionParameters parameters = new MockExecutionParameters();

        HttpResponse.BodyHandler<?> bodyHandler = httpClient.createBodyHandler(parameters);

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.discarding());

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.RESPONSE_FORMAT, HttpClientConstants.ResponseFormat.FILE.name());

        bodyHandler = httpClient.createBodyHandler(parameters);

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofInputStream());

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.RESPONSE_FORMAT, HttpClientConstants.ResponseFormat.XML.name());

        bodyHandler = httpClient.createBodyHandler(parameters);

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testCreateBodyPublisher() {
        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.BODY_CONTENT_TYPE, HttpClientConstants.BodyContentType.FORM_DATA.name());
        parameters.set(
                HttpClientConstants.BODY_PARAMETERS,
                List.of(
                        Map.of(HttpClientConstants.KEY, "key1", HttpClientConstants.VALUE, "value1"),
                        Map.of(
                                HttpClientConstants.KEY,
                                "key2",
                                HttpClientConstants.VALUE,
                                com.bytechef.hermes.file.storage.domain.FileEntry.of("fileName.txt")
                                        .toMap())));

        MultipartBodyPublisher multipartBodyPublisher =
                (MultipartBodyPublisher) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertTrue(multipartBodyPublisher.mediaType().toString().startsWith("multipart/form-data"));

        List<MultipartBodyPublisher.Part> parts = multipartBodyPublisher.parts();

        Assertions.assertEquals(
                "StringPublisher", parts.get(0).bodyPublisher().getClass().getSimpleName());
        Assertions.assertEquals(
                MediaType.TEXT_PLAIN, ((MimeBodyPublisherAdapter) parts.get(1).bodyPublisher()).mediaType());

        //

        parameters = new MockExecutionParameters();

        parameters.set(
                HttpClientConstants.BODY_CONTENT_TYPE, HttpClientConstants.BodyContentType.FORM_URLENCODED.name());
        parameters.set(
                HttpClientConstants.BODY_PARAMETERS,
                List.of(
                        Map.of(HttpClientConstants.KEY, "key1", HttpClientConstants.VALUE, "value1"),
                        Map.of(HttpClientConstants.KEY, "key2", HttpClientConstants.VALUE, "value2")));

        FormBodyPublisher formBodyPublisher = (FormBodyPublisher) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());

        Assertions.assertEquals("key1=value1&key2=value2", formBodyPublisher.encodedString());

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.BODY_CONTENT_TYPE, HttpClientConstants.BodyContentType.JSON.name());
        parameters.set(HttpClientConstants.BODY_PARAMETERS, Map.of("key1", "value1"));

        MimeBodyPublisherAdapter mimeBodyPublisherAdapter =
                (MimeBodyPublisherAdapter) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.BODY_CONTENT_TYPE, HttpClientConstants.BodyContentType.XML.name());
        parameters.set(HttpClientConstants.BODY_PARAMETERS, Map.of("key1", "value1"));

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(MediaType.APPLICATION_XML, mimeBodyPublisherAdapter.mediaType());

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.BODY_CONTENT_TYPE, HttpClientConstants.BodyContentType.RAW.name());
        parameters.set(HttpClientConstants.BODY_PARAMETERS, "text");

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        parameters.set(HttpClientConstants.MIME_TYPE, "text/html");

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(MediaType.TEXT_HTML, mimeBodyPublisherAdapter.mediaType());

        //

        parameters = new MockExecutionParameters();

        parameters.set(
                FILE_ENTRY,
                com.bytechef.hermes.file.storage.domain.FileEntry.of("filename", "base64:text")
                        .toMap());

        parameters.set(HttpClientConstants.BODY_CONTENT_TYPE, HttpClientConstants.BodyContentType.BINARY.name());

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(MediaType.APPLICATION_OCTET_STREAM, mimeBodyPublisherAdapter.mediaType());

        parameters.set(HttpClientConstants.BODY_CONTENT_TYPE, HttpClientConstants.BodyContentType.JSON.name());

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());

        parameters.set(HttpClientConstants.BODY_CONTENT_TYPE, HttpClientConstants.BodyContentType.XML.name());

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(MediaType.APPLICATION_XML, mimeBodyPublisherAdapter.mediaType());

        parameters.set(HttpClientConstants.BODY_CONTENT_TYPE, HttpClientConstants.BodyContentType.RAW.name());

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        //

        parameters = new MockExecutionParameters();

        HttpRequest.BodyPublisher bodyPublisher = httpClient.createBodyPublisher(context, parameters);

        Assertions.assertEquals(0, bodyPublisher.contentLength());
    }

    @Test
    @SuppressFBWarnings("RV")
    @SuppressWarnings("checkstyle:methodlengthcheck")
    public void testCreateHTTPClient() {
        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.ALLOW_UNAUTHORIZED_CERTS, true);

        java.net.http.HttpClient httpClient = this.httpClient.createHTTPClient(context, parameters, Map.of(), Map.of());

        Assertions.assertTrue(httpClient.authenticator().isEmpty());

        Assertions.assertNotNull(httpClient.sslContext());

        //

        parameters = new MockExecutionParameters();

        parameters.set(CONNECTION_ID, "connectionId");

        Connection connection = new Connection();

        connection.setName(HttpClientConstants.AuthType.API_KEY.name());
        connection.setParameters(Map.of(
                HttpClientConstants.KEY, HttpClientConstants.API_TOKEN, HttpClientConstants.VALUE, "token_value"));

        Mockito.doReturn(Optional.of(new ConnectionParametersImpl(connection)))
                .when(context)
                .fetchConnection();

        Map<String, List<String>> headers = new HashMap<>();

        this.httpClient.createHTTPClient(context, parameters, headers, Map.of());

        Assertions.assertEquals(Map.of(HttpClientConstants.API_TOKEN, List.of("token_value")), headers);

        connection = new Connection();

        connection.setName(HttpClientConstants.AuthType.API_KEY.name());
        connection.setParameters(Map.of(
                HttpClientConstants.KEY,
                HttpClientConstants.API_TOKEN,
                HttpClientConstants.VALUE,
                "token_value",
                HttpClientConstants.ADD_TO,
                HttpClientConstants.ApiTokenLocation.QUERY_PARAMS));

        Mockito.doReturn(Optional.of(new ConnectionParametersImpl(connection)))
                .when(context)
                .fetchConnection();

        Map<String, List<String>> queryParams = new HashMap<>();

        this.httpClient.createHTTPClient(context, parameters, Map.of(), queryParams);

        Assertions.assertEquals(Map.of(HttpClientConstants.API_TOKEN, List.of("token_value")), queryParams);

        connection = new Connection();

        connection.setParameters(
                Map.of(HttpClientConstants.USERNAME, "username", HttpClientConstants.PASSWORD, "password"));
        connection.setName(HttpClientConstants.AuthType.BASIC_AUTH.name());

        Mockito.doReturn(Optional.of(new ConnectionParametersImpl(connection)))
                .when(context)
                .fetchConnection();

        httpClient = this.httpClient.createHTTPClient(context, parameters, Map.of(), Map.of());

        PasswordAuthentication passwordAuthentication = httpClient
                .authenticator()
                .get()
                .requestPasswordAuthenticationInstance(null, null, 0, null, null, null, null, null);

        Assertions.assertArrayEquals(passwordAuthentication.getPassword(), "password".toCharArray());
        Assertions.assertEquals(passwordAuthentication.getUserName(), "username");

        connection = new Connection();

        connection.setParameters(Map.of(HttpClientConstants.TOKEN, "token"));
        connection.setName(HttpClientConstants.AuthType.BEARER_TOKEN.name());

        Mockito.doReturn(Optional.of(new ConnectionParametersImpl(connection)))
                .when(context)
                .fetchConnection();

        headers = new HashMap<>();

        this.httpClient.createHTTPClient(context, parameters, headers, Map.of());

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer token")), headers);

        connection = new Connection();

        connection.setParameters(
                Map.of(HttpClientConstants.USERNAME, "username", HttpClientConstants.PASSWORD, "password"));
        connection.setName(HttpClientConstants.AuthType.DIGEST_AUTH.name());

        Mockito.doReturn(Optional.of(new ConnectionParametersImpl(connection)))
                .when(context)
                .fetchConnection();

        httpClient = this.httpClient.createHTTPClient(context, parameters, Map.of(), Map.of());

        passwordAuthentication = httpClient
                .authenticator()
                .get()
                .requestPasswordAuthenticationInstance(null, null, 0, null, null, null, null, null);

        Assertions.assertArrayEquals(passwordAuthentication.getPassword(), "password".toCharArray());
        Assertions.assertEquals(passwordAuthentication.getUserName(), "username");

        connection = new Connection();

        connection.setParameters(Map.of(HttpClientConstants.ACCESS_TOKEN, "access_token"));
        connection.setName(HttpClientConstants.AuthType.OAUTH2.name());

        Mockito.doReturn(Optional.of(new ConnectionParametersImpl(connection)))
                .when(context)
                .fetchConnection();

        headers = new HashMap<>();

        this.httpClient.createHTTPClient(context, parameters, headers, Map.of());

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer access_token")), headers);

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.FOLLOW_REDIRECT, true);

        httpClient = this.httpClient.createHTTPClient(context, parameters, new HashMap<>(), new HashMap<>());

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.FOLLOW_ALL_REDIRECTS, true);

        httpClient = this.httpClient.createHTTPClient(context, parameters, new HashMap<>(), new HashMap<>());

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.PROXY, "10.11.12.13:30");

        httpClient = this.httpClient.createHTTPClient(context, parameters, new HashMap<>(), new HashMap<>());

        Assertions.assertTrue(httpClient.proxy().isPresent());

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.TIMEOUT, 20000);

        httpClient = this.httpClient.createHTTPClient(context, parameters, new HashMap<>(), new HashMap<>());

        Assertions.assertEquals(
                Duration.ofMillis(20000), httpClient.connectTimeout().orElseThrow());
    }

    @Test
    public void testCreateHTTPRequest() {
        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.URI, "http://localhost:8080");

        HttpRequest httpRequest = httpClient.createHTTPRequest(
                context,
                parameters,
                HttpClientConstants.RequestMethod.DELETE,
                Map.of("header1", List.of("value1")),
                Map.of("param1", List.of("value1")));

        Assertions.assertEquals(HttpClientConstants.RequestMethod.DELETE.name(), httpRequest.method());
        Assertions.assertEquals(
                Map.of("header1", List.of("value1")), httpRequest.headers().map());
        Assertions.assertEquals(URI.create("http://localhost:8080?param1=value1"), httpRequest.uri());
    }

    @Test
    public void testHandleResponse() throws Exception {
        MockExecutionParameters parameters = new MockExecutionParameters();

        Assertions.assertNull(httpClient.handleResponse(context, parameters, new TestHttpResponse(null)));

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.RESPONSE_FORMAT, HttpClientConstants.ResponseFormat.FILE.name());

        Assertions.assertEquals(
                com.bytechef.hermes.file.storage.domain.FileEntry.of("file.txt", "base64:dGV4dA==")
                        .toMap(),
                ((FileEntry) httpClient.handleResponse(
                                context,
                                parameters,
                                new TestHttpResponse(
                                        new ByteArrayInputStream("text".getBytes(StandardCharsets.UTF_8)))))
                        .toMap());

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.RESPONSE_FORMAT, HttpClientConstants.ResponseFormat.JSON.name());

        Assertions.assertEquals(
                Map.of("key1", "value1"),
                httpClient.handleResponse(
                        context,
                        parameters,
                        new TestHttpResponse(
                                """
                                {
                                    "key1": "value1"
                                }
                                """)));

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.RESPONSE_FORMAT, HttpClientConstants.ResponseFormat.TEXT.name());

        Assertions.assertEquals("text", httpClient.handleResponse(context, parameters, new TestHttpResponse("text")));

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.RESPONSE_FORMAT, HttpClientConstants.ResponseFormat.XML.name());

        Assertions.assertEquals(
                Map.of("object", Map.of("key1", "value1")),
                httpClient.handleResponse(
                        context,
                        parameters,
                        new TestHttpResponse(
                                """
                                <root>
                                    <object>
                                        <key1>value1</key1>
                                    </object>
                                </root>

                                """)));

        //

        parameters = new MockExecutionParameters();

        parameters.set(HttpClientConstants.FULL_RESPONSE, true);

        parameters.set(HttpClientConstants.RESPONSE_FORMAT, HttpClientConstants.ResponseFormat.TEXT.name());

        Assertions.assertEquals(
                new HttpClient.HttpResponseEntry("text", Map.of(), 200),
                httpClient.handleResponse(context, parameters, new TestHttpResponse("text")));
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
        public java.net.http.HttpClient.Version version() {
            return null;
        }
    }

    private static class ConnectionParametersImpl implements ConnectionParameters {
        private final String name;
        private final Map<String, Object> parameters;

        public ConnectionParametersImpl(Connection connection) {
            this.name = connection.getName();
            this.parameters = connection.getParameters();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getParameter(String name) {
            return (T) parameters.get(name);
        }

        @Override
        public String getParameter(String name, String defaultValue) {
            return MapUtils.getString(parameters, name, defaultValue);
        }
    }
}
