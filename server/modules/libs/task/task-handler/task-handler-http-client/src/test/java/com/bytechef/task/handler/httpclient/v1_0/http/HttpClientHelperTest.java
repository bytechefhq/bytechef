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

package com.bytechef.task.handler.httpclient.v1_0.http;

import static com.bytechef.hermes.auth.AuthenticationConstants.*;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.ACCESS_TOKEN;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.ADD_TO;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.ALLOW_UNAUTHORIZED_CERTS;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.BODY_CONTENT_TYPE;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.BODY_PARAMETERS;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.BodyContentType;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.FILE_ENTRY;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.FOLLOW_ALL_REDIRECTS;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.FOLLOW_REDIRECT;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.FULL_RESPONSE;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.KEY;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.MIME_TYPE;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.PASSWORD;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.PROXY;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.RESPONSE_FORMAT;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.TIMEOUT;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.TOKEN;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.URI;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.USERNAME;
import static com.bytechef.task.handler.httpclient.HttpClientTaskConstants.VALUE;

import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.task.execution.domain.SimpleTaskExecution;
import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.auth.service.AuthenticationService;
import com.bytechef.hermes.file.storage.base64.service.Base64FileStorageService;
import com.bytechef.hermes.file.storage.converter.FileEntryConverter;
import com.bytechef.hermes.file.storage.dto.FileEntry;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import com.bytechef.task.commons.json.JsonHelper;
import com.bytechef.task.commons.xml.XmlHelper;
import com.bytechef.task.handler.httpclient.HttpClientTaskConstants.AuthType;
import com.bytechef.task.handler.httpclient.HttpClientTaskConstants.RequestMethod;
import com.bytechef.task.handler.httpclient.HttpClientTaskConstants.ResponseFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.internal.extensions.MimeBodyPublisherAdapter;
import java.io.ByteArrayInputStream;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
public class HttpClientHelperTest {

    private final AuthenticationService authenticationService = Mockito.mock(AuthenticationService.class);
    private final FileStorageService fileStorageService = new Base64FileStorageService();
    private final HttpClientHelper httpClientHelper = new HttpClientHelper(
            authenticationService, fileStorageService, new JsonHelper(new ObjectMapper()), new XmlHelper());

    @BeforeAll
    public static void beforeAll() {
        MapObject.addConverter(new FileEntryConverter());
    }

    @Test
    public void testCreateBodyHandler() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        HttpResponse.BodyHandler<?> bodyHandler = httpClientHelper.createBodyHandler(taskExecution);

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.discarding());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(RESPONSE_FORMAT, ResponseFormat.FILE);

        bodyHandler = httpClientHelper.createBodyHandler(taskExecution);

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofInputStream());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(RESPONSE_FORMAT, ResponseFormat.XML);

        bodyHandler = httpClientHelper.createBodyHandler(taskExecution);

        Assertions.assertEquals(bodyHandler, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testCreateBodyPublisher() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put(BODY_CONTENT_TYPE, BodyContentType.FORM_DATA.name());
        taskExecution.put(
                BODY_PARAMETERS,
                List.of(
                        Map.of(KEY, "key1", VALUE, "value1"),
                        Map.of(KEY, "key2", VALUE, FileEntry.of("fileName.txt").toMap())));

        MultipartBodyPublisher multipartBodyPublisher =
                (MultipartBodyPublisher) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertTrue(multipartBodyPublisher.mediaType().toString().startsWith("multipart/form-data"));

        List<MultipartBodyPublisher.Part> parts = multipartBodyPublisher.parts();

        Assertions.assertEquals(
                "StringPublisher", parts.get(0).bodyPublisher().getClass().getSimpleName());
        Assertions.assertEquals(
                MediaType.TEXT_PLAIN, ((MimeBodyPublisherAdapter) parts.get(1).bodyPublisher()).mediaType());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(BODY_CONTENT_TYPE, BodyContentType.FORM_URLENCODED.name());
        taskExecution.put(
                BODY_PARAMETERS, List.of(Map.of(KEY, "key1", VALUE, "value1"), Map.of(KEY, "key2", VALUE, "value2")));

        FormBodyPublisher formBodyPublisher = (FormBodyPublisher) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());

        Assertions.assertEquals("key1=value1&key2=value2", formBodyPublisher.encodedString());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(BODY_CONTENT_TYPE, BodyContentType.JSON.name());
        taskExecution.put(BODY_PARAMETERS, Map.of("key1", "value1"));

        MimeBodyPublisherAdapter mimeBodyPublisherAdapter =
                (MimeBodyPublisherAdapter) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(BODY_CONTENT_TYPE, BodyContentType.XML.name());
        taskExecution.put(BODY_PARAMETERS, Map.of("key1", "value1"));

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(MediaType.APPLICATION_XML, mimeBodyPublisherAdapter.mediaType());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(BODY_CONTENT_TYPE, BodyContentType.RAW.name());
        taskExecution.put(BODY_PARAMETERS, "text");

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        taskExecution.put(MIME_TYPE, "text/html");

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(MediaType.TEXT_HTML, mimeBodyPublisherAdapter.mediaType());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(FILE_ENTRY, FileEntry.of("fileName", "base64:text").toMap());

        taskExecution.put(BODY_CONTENT_TYPE, BodyContentType.BINARY.name());

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(MediaType.APPLICATION_OCTET_STREAM, mimeBodyPublisherAdapter.mediaType());

        taskExecution.put(BODY_CONTENT_TYPE, BodyContentType.JSON.name());

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());

        taskExecution.put(BODY_CONTENT_TYPE, BodyContentType.XML.name());

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(MediaType.APPLICATION_XML, mimeBodyPublisherAdapter.mediaType());

        taskExecution.put(BODY_CONTENT_TYPE, BodyContentType.RAW.name());

        mimeBodyPublisherAdapter = (MimeBodyPublisherAdapter) httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());

        //

        taskExecution = new SimpleTaskExecution();

        HttpRequest.BodyPublisher bodyPublisher = httpClientHelper.createBodyPublisher(taskExecution);

        Assertions.assertEquals(0, bodyPublisher.contentLength());
    }

    @Test
    public void testCreateHTTPClient() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put(ALLOW_UNAUTHORIZED_CERTS, true);

        HttpClient httpClient = httpClientHelper.createHTTPClient(taskExecution, Map.of(), Map.of());

        Assertions.assertTrue(httpClient.authenticator().isEmpty());

        Assertions.assertNotNull(httpClient.sslContext());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(AUTHENTICATION_ID, "authenticationId");

        Authentication authentication = new Authentication();

        authentication.setProperties(Map.of(KEY, "api_token", VALUE, "value"));
        authentication.setType(AuthType.API_KEY.name());

        Mockito.when(authenticationService.fetchAuthentication(Mockito.anyString()))
                .thenReturn(authentication);

        Map<String, List<String>> headers = new HashMap<>();

        httpClientHelper.createHTTPClient(taskExecution, headers, Map.of());

        Assertions.assertEquals(Map.of("api_token", List.of("value")), headers);

        authentication.setProperties(Map.of(KEY, "api_token", VALUE, "value", ADD_TO, "query_params"));

        Map<String, List<String>> queryParams = new HashMap<>();

        httpClientHelper.createHTTPClient(taskExecution, Map.of(), queryParams);

        Assertions.assertEquals(Map.of("api_token", List.of("value")), queryParams);

        authentication.setProperties(Map.of(USERNAME, "username", PASSWORD, "password"));
        authentication.setType(AuthType.BASIC_AUTH.name());

        httpClient = httpClientHelper.createHTTPClient(taskExecution, Map.of(), Map.of());

        PasswordAuthentication passwordAuthentication = httpClient
                .authenticator()
                .get()
                .requestPasswordAuthenticationInstance(null, null, 0, null, null, null, null, null);

        Assertions.assertArrayEquals(passwordAuthentication.getPassword(), "password".toCharArray());
        Assertions.assertEquals(passwordAuthentication.getUserName(), "username");

        authentication.setProperties(Map.of(TOKEN, "token"));
        authentication.setType(AuthType.BEARER_TOKEN.name());

        headers = new HashMap<>();

        httpClientHelper.createHTTPClient(taskExecution, headers, Map.of());

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer token")), headers);

        authentication.setProperties(Map.of(USERNAME, "username", PASSWORD, "password"));
        authentication.setType(AuthType.DIGEST_AUTH.name());

        httpClient = httpClientHelper.createHTTPClient(taskExecution, Map.of(), Map.of());

        passwordAuthentication = httpClient
                .authenticator()
                .get()
                .requestPasswordAuthenticationInstance(null, null, 0, null, null, null, null, null);

        Assertions.assertArrayEquals(passwordAuthentication.getPassword(), "password".toCharArray());
        Assertions.assertEquals(passwordAuthentication.getUserName(), "username");

        authentication.setProperties(Map.of(ACCESS_TOKEN, "access_token"));
        authentication.setType(AuthType.OAUTH2.name());

        headers = new HashMap<>();

        httpClientHelper.createHTTPClient(taskExecution, headers, Map.of());

        Assertions.assertEquals(Map.of("Authorization", List.of("Bearer access_token")), headers);

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(FOLLOW_REDIRECT, true);

        httpClient = httpClientHelper.createHTTPClient(taskExecution, Map.of(), Map.of());

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(FOLLOW_ALL_REDIRECTS, true);

        httpClient = httpClientHelper.createHTTPClient(taskExecution, Map.of(), Map.of());

        Assertions.assertNotNull(httpClient.followRedirects());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(PROXY, "10.11.12.13:30");

        httpClient = httpClientHelper.createHTTPClient(taskExecution, Map.of(), Map.of());

        Assertions.assertTrue(httpClient.proxy().isPresent());

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(TIMEOUT, 20000);

        httpClient = httpClientHelper.createHTTPClient(taskExecution, Map.of(), Map.of());

        Assertions.assertEquals(
                Duration.ofMillis(20000), httpClient.connectTimeout().get());
    }

    @Test
    public void testCreateHTTPRequest() {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        taskExecution.put(URI, "http://localhost:8080");

        HttpRequest httpRequest = httpClientHelper.createHTTPRequest(
                taskExecution,
                RequestMethod.DELETE,
                Map.of("header1", List.of("value1")),
                Map.of("param1", List.of("value1")));

        Assertions.assertEquals(RequestMethod.DELETE.name(), httpRequest.method());
        Assertions.assertEquals(
                Map.of("header1", List.of("value1")), httpRequest.headers().map());
        Assertions.assertEquals(java.net.URI.create("http://localhost:8080?param1=value1"), httpRequest.uri());
    }

    @Test
    public void testHandleResponse() throws Exception {
        SimpleTaskExecution taskExecution = new SimpleTaskExecution();

        Assertions.assertNull(httpClientHelper.handleResponse(taskExecution, new TestHttpResponse(null)));

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(RESPONSE_FORMAT, ResponseFormat.FILE);

        Assertions.assertEquals(
                FileEntry.of("file.txt", "base64:dGV4dA=="),
                httpClientHelper.handleResponse(
                        taskExecution, new TestHttpResponse(new ByteArrayInputStream("text".getBytes()))));

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(RESPONSE_FORMAT, ResponseFormat.JSON);

        Assertions.assertEquals(
                Map.of("key1", "value1"),
                httpClientHelper.handleResponse(
                        taskExecution,
                        new TestHttpResponse(
                                """
                                {
                                    "key1": "value1"
                                }
                                """)));

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(RESPONSE_FORMAT, ResponseFormat.TEXT);

        Assertions.assertEquals("text", httpClientHelper.handleResponse(taskExecution, new TestHttpResponse("text")));

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(RESPONSE_FORMAT, ResponseFormat.XML);

        Assertions.assertEquals(
                Map.of("object", Map.of("key1", "value1")),
                httpClientHelper.handleResponse(
                        taskExecution,
                        new TestHttpResponse(
                                """
                                <root>
                                    <object>
                                        <key1>value1</key1>
                                    </object>
                                </root>

                                """)));

        //

        taskExecution = new SimpleTaskExecution();

        taskExecution.put(FULL_RESPONSE, true);

        taskExecution.put(RESPONSE_FORMAT, ResponseFormat.TEXT);

        Assertions.assertEquals(
                new HttpClientHelper.HttpResponseEntry("text", Map.of(), 200),
                httpClientHelper.handleResponse(taskExecution, new TestHttpResponse("text")));
    }

    private static class TestHttpResponse implements HttpResponse {

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
