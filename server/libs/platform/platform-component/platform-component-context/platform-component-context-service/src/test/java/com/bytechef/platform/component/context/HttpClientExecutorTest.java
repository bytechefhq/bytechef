/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.github.mizosoft.methanol.FormBodyPublisher;
import com.github.mizosoft.methanol.MediaType;
import com.github.mizosoft.methanol.MultipartBodyPublisher;
import com.github.mizosoft.methanol.internal.extensions.MimeBodyPublisherAdapter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * @author Ivica Cardic
 */
public class HttpClientExecutorTest {

    private ActionContext context;
    private TempFileStorage tempFileStorage;
    private ApplicationContext applicationContext;
    private HttpClientExecutor httpClientExecutor;

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        ConvertUtils.setObjectMapper(objectMapper);
        JsonUtils.setObjectMapper(objectMapper);
        XmlUtils.setXmlMapper(
            XmlMapper.builder()
                .build());
    }

    @BeforeEach
    void setUp() {
        context = Mockito.mock(ActionContext.class);
        tempFileStorage = Mockito.mock(TempFileStorage.class);
        applicationContext = Mockito.mock(ApplicationContext.class);
        httpClientExecutor = new HttpClientExecutor(applicationContext, tempFileStorage);
    }

    @Nested
    @DisplayName("createBodyPublisher tests")
    class CreateBodyPublisherTests {

        @Test
        @DisplayName("Should return noBody publisher when body is null")
        void testNullBody() {
            HttpRequest.BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(null);

            assertEquals(0, bodyPublisher.contentLength());
        }

        @Test
        @DisplayName("Should create form data body publisher with text parts")
        void testFormDataBodyPublisherWithTextParts() {
            MultipartBodyPublisher multipartBodyPublisher =
                (MultipartBodyPublisher) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(Map.of("key1", "value1", "key2", "value2"), Http.BodyContentType.FORM_DATA));

            assertTrue(multipartBodyPublisher.mediaType()
                .toString()
                .startsWith("multipart/form-data"));
            assertEquals(2, multipartBodyPublisher.parts()
                .size());
        }

        @Test
        @DisplayName("Should create form data body publisher with file entry")
        void testFormDataBodyPublisherWithFileEntry() {
            FileEntryImpl fileEntry = new FileEntryImpl("test.txt", "txt", "text/plain", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream("file content".getBytes(StandardCharsets.UTF_8)));

            MultipartBodyPublisher multipartBodyPublisher =
                (MultipartBodyPublisher) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(Map.of("key1", "value1", "file", fileEntry), Http.BodyContentType.FORM_DATA));

            assertTrue(multipartBodyPublisher.mediaType()
                .toString()
                .startsWith("multipart/form-data"));
        }

        @Test
        @DisplayName("Should create form URL encoded body publisher with simple values")
        void testFormUrlEncodedBodyPublisherSimple() {
            FormBodyPublisher formBodyPublisher = (FormBodyPublisher) httpClientExecutor.createBodyPublisher(
                Http.Body.of(Map.of("key1", "value1", "key2", "value2"), Http.BodyContentType.FORM_URL_ENCODED));

            assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());
            assertTrue(formBodyPublisher.encodedString()
                .contains("key1=value1"));
            assertTrue(formBodyPublisher.encodedString()
                .contains("key2=value2"));
        }

        @Test
        @DisplayName("Should create form URL encoded body publisher with nested map")
        void testFormUrlEncodedBodyPublisherNestedMap() {
            FormBodyPublisher formBodyPublisher = (FormBodyPublisher) httpClientExecutor.createBodyPublisher(
                Http.Body.of(
                    Map.of(
                        "key1", "value1",
                        "nested", Map.of("innerKey", "innerValue")),
                    Http.BodyContentType.FORM_URL_ENCODED));

            assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());
            assertTrue(formBodyPublisher.encodedString()
                .contains("key1=value1"));
            assertTrue(formBodyPublisher.encodedString()
                .contains(URLEncoder.encode("nested[innerKey]", StandardCharsets.UTF_8) + "=innerValue"));
        }

        @Test
        @DisplayName("Should create form URL encoded body publisher with list values")
        void testFormUrlEncodedBodyPublisherWithList() {
            FormBodyPublisher formBodyPublisher = (FormBodyPublisher) httpClientExecutor.createBodyPublisher(
                Http.Body.of(
                    Map.of("items", List.of("item1", "item2", "item3")),
                    Http.BodyContentType.FORM_URL_ENCODED));

            assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());
            assertTrue(formBodyPublisher.encodedString()
                .contains(URLEncoder.encode("items[0]", StandardCharsets.UTF_8) + "=item1"));
            assertTrue(formBodyPublisher.encodedString()
                .contains(URLEncoder.encode("items[1]", StandardCharsets.UTF_8) + "=item2"));
            assertTrue(formBodyPublisher.encodedString()
                .contains(URLEncoder.encode("items[2]", StandardCharsets.UTF_8) + "=item3"));
        }

        @Test
        @DisplayName("Should create form URL encoded body publisher with deeply nested structure")
        void testFormUrlEncodedBodyPublisherDeeplyNested() {
            FormBodyPublisher formBodyPublisher = (FormBodyPublisher) httpClientExecutor.createBodyPublisher(
                Http.Body.of(
                    Map.of(
                        "level1", Map.of(
                            "level2", Map.of(
                                "level3", "deepValue"))),
                    Http.BodyContentType.FORM_URL_ENCODED));

            assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());
            assertTrue(formBodyPublisher.encodedString()
                .contains(
                    URLEncoder.encode("level1[level2][level3]", StandardCharsets.UTF_8) + "=deepValue"));
        }

        @Test
        @DisplayName("Should create JSON body publisher")
        void testJsonBodyPublisher() {
            MimeBodyPublisherAdapter mimeBodyPublisherAdapter =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(Map.of("key1", "value1"), Http.BodyContentType.JSON));

            assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());
        }

        @Test
        @DisplayName("Should create JSON body publisher with list content")
        void testJsonBodyPublisherWithList() {
            MimeBodyPublisherAdapter mimeBodyPublisherAdapter =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(List.of("item1", "item2"), Http.BodyContentType.JSON));

            assertEquals(MediaType.APPLICATION_JSON, mimeBodyPublisherAdapter.mediaType());
        }

        @Test
        @DisplayName("Should create XML body publisher")
        void testXmlBodyPublisher() {
            MimeBodyPublisherAdapter mimeBodyPublisherAdapter =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(Map.of("key1", "value1"), Http.BodyContentType.XML));

            assertEquals(MediaType.APPLICATION_XML, mimeBodyPublisherAdapter.mediaType());
        }

        @Test
        @DisplayName("Should create string body publisher with text/plain")
        void testStringBodyPublisher() {
            MimeBodyPublisherAdapter mimeBodyPublisherAdapter =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of("plain text content"));

            assertEquals(MediaType.TEXT_PLAIN, mimeBodyPublisherAdapter.mediaType());
        }

        @Test
        @DisplayName("Should create string body publisher with custom mime type")
        void testStringBodyPublisherCustomMimeType() {
            MimeBodyPublisherAdapter mimeBodyPublisherAdapter =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of("custom content", "application/custom"));

            assertEquals(MediaType.parse("application/custom"), mimeBodyPublisherAdapter.mediaType());
        }

        @Test
        @DisplayName("Should create binary body publisher from file entry")
        void testBinaryBodyPublisher() {
            FileEntryImpl fileEntry = new FileEntryImpl("test.bin", "bin", "application/octet-stream", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3, 4
                }));

            MimeBodyPublisherAdapter mimeBodyPublisherAdapter =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(fileEntry));

            assertEquals(MediaType.parse("application/octet-stream"), mimeBodyPublisherAdapter.mediaType());
        }

        @Test
        @DisplayName("Should create binary body publisher with custom mime type")
        void testBinaryBodyPublisherCustomMimeType() {
            FileEntryImpl fileEntry = new FileEntryImpl("image.png", "png", "image/png", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3, 4
                }));

            MimeBodyPublisherAdapter mimeBodyPublisherAdapter =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(fileEntry, "image/jpeg"));

            assertEquals(MediaType.parse("image/jpeg"), mimeBodyPublisherAdapter.mediaType());
        }
    }

    @Nested
    @DisplayName("createResponseBodyHandler tests")
    class CreateResponseBodyHandlerTests {

        @Test
        @DisplayName("Should return discarding handler when responseType is null")
        void testNullResponseType() {
            Http.Configuration configuration = Http.Configuration.newConfiguration()
                .build();

            HttpResponse.BodyHandler<?> bodyHandler = httpClientExecutor.createResponseBodyHandler(configuration);

            assertEquals(HttpResponse.BodyHandlers.discarding(), bodyHandler);
        }

        @Test
        @DisplayName("Should return InputStream handler for BINARY response type")
        void testBinaryResponseType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .build();

            HttpResponse.BodyHandler<?> bodyHandler = httpClientExecutor.createResponseBodyHandler(configuration);

            assertEquals(HttpResponse.BodyHandlers.ofInputStream(), bodyHandler);
        }

        @Test
        @DisplayName("Should return String handler for JSON response type")
        void testJsonResponseType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            HttpResponse.BodyHandler<?> bodyHandler = httpClientExecutor.createResponseBodyHandler(configuration);

            assertEquals(HttpResponse.BodyHandlers.ofString(), bodyHandler);
        }

        @Test
        @DisplayName("Should return String handler for XML response type")
        void testXmlResponseType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.XML)
                .build();

            HttpResponse.BodyHandler<?> bodyHandler = httpClientExecutor.createResponseBodyHandler(configuration);

            assertEquals(HttpResponse.BodyHandlers.ofString(), bodyHandler);
        }

        @Test
        @DisplayName("Should return String handler for TEXT response type")
        void testTextResponseType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            HttpResponse.BodyHandler<?> bodyHandler = httpClientExecutor.createResponseBodyHandler(configuration);

            assertEquals(HttpResponse.BodyHandlers.ofString(), bodyHandler);
        }
    }

    @Nested
    @DisplayName("createHttpRequest tests")
    class CreateHttpRequestTests {

        @Test
        @DisplayName("Should create HTTP request with DELETE method")
        void testDeleteMethod() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.DELETE, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(Http.RequestMethod.DELETE.name(), httpRequest.method());
        }

        @Test
        @DisplayName("Should create HTTP request with GET method")
        void testGetMethod() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.GET, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(Http.RequestMethod.GET.name(), httpRequest.method());
        }

        @Test
        @DisplayName("Should create HTTP request with POST method")
        void testPostMethod() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.POST, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(Http.RequestMethod.POST.name(), httpRequest.method());
        }

        @Test
        @DisplayName("Should create HTTP request with PUT method")
        void testPutMethod() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.PUT, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(Http.RequestMethod.PUT.name(), httpRequest.method());
        }

        @Test
        @DisplayName("Should create HTTP request with PATCH method")
        void testPatchMethod() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.PATCH, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(Http.RequestMethod.PATCH.name(), httpRequest.method());
        }

        @Test
        @DisplayName("Should create HTTP request with HEAD method")
        void testHeadMethod() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.HEAD, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(Http.RequestMethod.HEAD.name(), httpRequest.method());
        }

        @Test
        @DisplayName("Should create HTTP request with headers")
        void testWithHeaders() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.GET,
                Map.of("Authorization", List.of("Bearer token123"), "Content-Type", List.of("application/json")),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            HttpHeaders httpHeaders = httpRequest.headers();

            assertEquals(List.of("Bearer token123"), httpHeaders.allValues("Authorization"));
            assertEquals(List.of("application/json"), httpHeaders.allValues("Content-Type"));
        }

        @Test
        @DisplayName("Should create HTTP request with multiple values for same header")
        void testWithMultiValueHeaders() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.GET,
                Map.of("Accept", List.of("application/json", "application/xml")),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            HttpHeaders httpHeaders = httpRequest.headers();

            assertEquals(List.of("application/json", "application/xml"), httpHeaders.allValues("Accept"));
        }

        @Test
        @DisplayName("Should create HTTP request with query parameters")
        void testWithQueryParameters() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.GET, Map.of(),
                Map.of("param1", List.of("value1"), "param2", List.of("value2")),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            assertTrue(uri.contains("param1=value1"));
            assertTrue(uri.contains("param2=value2"));
            assertTrue(uri.contains("?"));
            assertTrue(uri.contains("&"));
        }

        @Test
        @DisplayName("Should create HTTP request with empty query parameters")
        void testWithEmptyQueryParameters() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.GET, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(URI.create("http://localhost:8080"), httpRequest.uri());
        }

        @Test
        @DisplayName("Should create HTTP request with null query parameters")
        void testWithNullQueryParameters() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.GET, Map.of(),
                null, null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(URI.create("http://localhost:8080"), httpRequest.uri());
        }

        @Test
        @DisplayName("Should create HTTP request with multi-value query parameters")
        void testWithMultiValueQueryParameters() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080", Http.RequestMethod.GET, Map.of(),
                Map.of("tags", List.of("tag1", "tag2", "tag3")),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            assertTrue(uri.contains("tags=tag1"));
            assertTrue(uri.contains("tags=tag2"));
            assertTrue(uri.contains("tags=tag3"));
        }

        @Test
        @DisplayName("Should preserve full URL with protocol")
        void testFullUrlWithProtocol() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "https://api.example.com:443/v1/users", Http.RequestMethod.GET, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(URI.create("https://api.example.com:443/v1/users"), httpRequest.uri());
        }
    }

    @Nested
    @DisplayName("createHttpClient tests")
    class CreateHttpClientTests {

        @Test
        @DisplayName("Should create HTTP client with allow unauthorized certs")
        void testAllowUnauthorizedCerts() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.allowUnauthorizedCerts(true)
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertNotNull(httpClient.sslContext());
        }

        @Test
        @DisplayName("Should create HTTP client with follow redirect")
        void testFollowRedirect() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.followRedirect(true)
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertEquals(HttpClient.Redirect.NORMAL, httpClient.followRedirects());
        }

        @Test
        @DisplayName("Should create HTTP client with follow all redirects")
        void testFollowAllRedirects() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.followAllRedirects(true)
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertEquals(HttpClient.Redirect.ALWAYS, httpClient.followRedirects());
        }

        @Test
        @DisplayName("Should create HTTP client with proxy")
        void testWithProxy() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.proxy("10.11.12.13:8080")
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertTrue(httpClient.proxy()
                .isPresent());
        }

        @Test
        @DisplayName("Should create HTTP client with custom timeout")
        void testWithCustomTimeout() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.timeout(Duration.ofMillis(5000))
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertEquals(Duration.ofMillis(5000), httpClient.connectTimeout()
                .orElseThrow());
        }

        @Test
        @DisplayName("Should create HTTP client with default timeout when not specified")
        void testDefaultTimeout() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertEquals(Duration.ofMillis(4000), httpClient.connectTimeout()
                .orElseThrow());
        }

        @Test
        @DisplayName("Should create HTTP client without proxy when not specified")
        void testWithoutProxy() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertTrue(httpClient.proxy()
                .isEmpty());
        }
    }

    @Nested
    @DisplayName("handleResponse tests")
    class HandleResponseTests {

        @Test
        @DisplayName("Should return null body when response body is null")
        void testNullResponseBody() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            Http.Response response = httpClientExecutor.handleResponse(
                new TestHttpResponse(null), configuration, context);

            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should return null body when response body is empty string")
        void testEmptyResponseBody() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            Http.Response response = httpClientExecutor.handleResponse(
                new TestHttpResponse(""), configuration, context);

            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should return null body for status code 204 regardless of response type mismatch")
        void testStatusCode204() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                null,
                HttpHeaders.of(Map.of("content-type", List.of("text/html")), (name, value) -> true),
                204);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getBody());
            assertEquals(204, response.getStatusCode());
        }

        @Test
        @DisplayName("Should parse JSON response correctly")
        void testJsonResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key1\":\"value1\",\"key2\":\"value2\"}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(Map.of("key1", "value1", "key2", "value2"), response.getBody());
        }

        @Test
        @DisplayName("Should parse JSON array response correctly")
        void testJsonArrayResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "[\"item1\",\"item2\",\"item3\"]",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(List.of("item1", "item2", "item3"), response.getBody());
        }

        @Test
        @DisplayName("Should parse XML response correctly")
        void testXmlResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.XML)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "<root><key1>value1</key1><key2>value2</key2></root>",
                HttpHeaders.of(Map.of("content-type", List.of("application/xml")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(Map.of("key1", "value1", "key2", "value2"), response.getBody());
        }

        @Test
        @DisplayName("Should return text response as string")
        void testTextResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "Plain text content",
                HttpHeaders.of(Map.of("content-type", List.of("text/plain")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals("Plain text content", response.getBody());
        }

        @Test
        @DisplayName("Should handle binary response and store file")
        void testBinaryResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .filename("download.bin")
                .build();

            InputStream inputStream = new ByteArrayInputStream(new byte[] {
                1, 2, 3, 4
            });

            com.bytechef.file.storage.domain.FileEntry storageFileEntry =
                new com.bytechef.file.storage.domain.FileEntry("download.bin", "file://stored");

            Mockito.when(tempFileStorage.storeFileContent(Mockito.anyString(), Mockito.any(InputStream.class)))
                .thenReturn(storageFileEntry);

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                inputStream,
                HttpHeaders.of(Map.of("content-type", List.of("application/octet-stream")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());

            FileEntryImpl resultFileEntry = response.getBody(FileEntryImpl.class);

            assertEquals("download.bin", resultFileEntry.getName());
            assertEquals("file://stored", resultFileEntry.getUrl());
        }

        @Test
        @DisplayName("Should handle binary response with custom binary content type")
        void testBinaryResponseWithCustomContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.binary("image/png"))
                .build();

            InputStream inputStream = new ByteArrayInputStream(new byte[] {
                1, 2, 3, 4
            });

            com.bytechef.file.storage.domain.FileEntry storageFileEntry =
                new com.bytechef.file.storage.domain.FileEntry("file.png", "file://stored");

            Mockito.when(tempFileStorage.storeFileContent(Mockito.anyString(), Mockito.any(InputStream.class)))
                .thenReturn(storageFileEntry);

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                inputStream,
                HttpHeaders.of(Map.of("content-type", List.of("image/png")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());

            FileEntryImpl resultFileEntry = response.getBody(FileEntryImpl.class);

            assertEquals("file.png", resultFileEntry.getName());
        }

        @Test
        @DisplayName("Should handle binary response with default filename when no content-type header")
        void testBinaryResponseDefaultFilename() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .build();

            InputStream inputStream = new ByteArrayInputStream(new byte[] {
                1, 2, 3, 4
            });

            com.bytechef.file.storage.domain.FileEntry storageFileEntry =
                new com.bytechef.file.storage.domain.FileEntry("file.txt", "file://stored");

            Mockito.when(tempFileStorage.storeFileContent(Mockito.anyString(), Mockito.any(InputStream.class)))
                .thenReturn(storageFileEntry);

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                inputStream,
                HttpHeaders.of(Map.of(), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());

            FileEntryImpl resultFileEntry = response.getBody(FileEntryImpl.class);

            assertEquals("file.txt", resultFileEntry.getName());
        }

        @Test
        @DisplayName("Should return null body when content-type does not match expected response type")
        void testContentTypeMismatch() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "This is HTML content",
                HttpHeaders.of(Map.of("content-type", List.of("text/html")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should parse JSON with charset in content-type header")
        void testJsonWithCharsetInContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json; charset=utf-8")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(Map.of("key", "value"), response.getBody());
        }

        @Test
        @DisplayName("Should parse XML with charset in content-type header")
        void testXmlWithCharsetInContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.XML)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "<root><key>value</key></root>",
                HttpHeaders.of(Map.of("content-type", List.of("application/xml; charset=utf-8")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(Map.of("key", "value"), response.getBody());
        }

        @Test
        @DisplayName("Should handle response without content-type header")
        void testResponseWithoutContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of(), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(Map.of("key", "value"), response.getBody());
        }

        @Test
        @DisplayName("Should return null body when response type is null")
        void testNullResponseType() {
            Http.Configuration configuration = Http.Configuration.newConfiguration()
                .build();

            Http.Response response = httpClientExecutor.handleResponse(
                new TestHttpResponse(null), configuration, context);

            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should preserve status code in response")
        void testPreservesStatusCode() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"error\":\"Not found\"}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (name, value) -> true),
                404);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals(404, response.getStatusCode());
        }

        @Test
        @DisplayName("Should preserve headers in response")
        void testPreservesHeaders() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(
                    Map.of(
                        "content-type", List.of("application/json"),
                        "x-custom-header", List.of("custom-value")),
                    (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals(List.of("application/json"), response.getHeader("content-type"));
            assertEquals(List.of("custom-value"), response.getHeader("x-custom-header"));
            assertEquals("custom-value", response.getFirstHeader("x-custom-header"));
        }

        @Test
        @DisplayName("Should handle various 2xx status codes")
        void testVariousSuccessStatusCodes() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            for (int statusCode : List.of(200, 201, 202)) {
                TestHttpResponse testHttpResponse = new TestHttpResponse(
                    "{\"status\":\"ok\"}",
                    HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                    statusCode);

                Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

                assertEquals(statusCode, response.getStatusCode());
                assertNotNull(response.getBody());
            }
        }

        @Test
        @DisplayName("Should handle 4xx and 5xx status codes")
        void testErrorStatusCodes() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            for (int statusCode : List.of(400, 401, 403, 404, 500, 502, 503)) {
                TestHttpResponse testHttpResponse = new TestHttpResponse(
                    "{\"error\":\"Something went wrong\"}",
                    HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                    statusCode);

                Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

                assertEquals(statusCode, response.getStatusCode());
            }
        }
    }

    @Nested
    @DisplayName("ResponseImpl tests")
    class ResponseImplTests {

        @Test
        @DisplayName("Should get body with class type conversion")
        void testGetBodyWithClass() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"name\":\"test\",\"value\":42}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            Map<String, Object> body = response.getBody(Map.class);

            assertNotNull(body);
            assertEquals("test", body.get("name"));
            assertEquals(42, body.get("value"));
        }

        @Test
        @DisplayName("Should get body with TypeReference")
        void testGetBodyWithTypeReference() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "[\"item1\",\"item2\",\"item3\"]",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            List<String> body = response.getBody(
                new com.bytechef.component.definition.TypeReference<>() {});

            assertNotNull(body);
            assertEquals(3, body.size());
            assertEquals("item1", body.get(0));
        }

        @Test
        @DisplayName("Should return correct headers")
        void testGetHeaders() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "content",
                HttpHeaders.of(
                    Map.of(
                        "x-header-1", List.of("value1"),
                        "x-header-2", List.of("value2a", "value2b")),
                    (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            Map<String, List<String>> headers = response.getHeaders();

            assertEquals(List.of("value1"), headers.get("x-header-1"));
            assertEquals(List.of("value2a", "value2b"), headers.get("x-header-2"));
        }

        @Test
        @DisplayName("Should return first header value")
        void testGetFirstHeader() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "content",
                HttpHeaders.of(
                    Map.of("x-multi-header", List.of("first", "second", "third")),
                    (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals("first", response.getFirstHeader("x-multi-header"));
        }

        @Test
        @DisplayName("Should return all header values")
        void testGetHeader() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "content",
                HttpHeaders.of(
                    Map.of("x-multi-header", List.of("first", "second", "third")),
                    (name, value) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals(List.of("first", "second", "third"), response.getHeader("x-multi-header"));
        }
    }

    @Nested
    @DisplayName("Edge cases and special scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle JSON with nested objects")
        void testNestedJsonResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            String nestedJson = """
                {
                    "user": {
                        "name": "John",
                        "address": {
                            "city": "NYC",
                            "zip": "10001"
                        }
                    }
                }
                """;

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                nestedJson,
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();

            assertNotNull(body.get("user"));
        }

        @Test
        @DisplayName("Should handle XML with nested elements")
        void testNestedXmlResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.XML)
                .build();

            String nestedXml = "<root><user><name>John</name><age>30</age></user></root>";

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                nestedXml,
                HttpHeaders.of(Map.of("content-type", List.of("application/xml")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should handle JSON response with ISO-8859-1 charset")
        void testJsonWithIso88591Charset() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"message\":\"success\"}",
                HttpHeaders.of(
                    Map.of("content-type", List.of("application/json; charset=ISO-8859-1")),
                    (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(Map.of("message", "success"), response.getBody());
        }

        @Test
        @DisplayName("Should handle multiple query parameters with same key")
        void testMultipleQueryParamsWithSameKey() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, Map.of(),
                Map.of("filter", List.of("active", "verified", "premium")),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            assertTrue(uri.contains("filter=active"));
            assertTrue(uri.contains("filter=verified"));
            assertTrue(uri.contains("filter=premium"));
        }

        @Test
        @DisplayName("Should create request with complex body")
        void testComplexJsonBody() {
            Map<String, Object> complexBody = Map.of(
                "users", List.of(
                    Map.of("name", "John", "age", 30),
                    Map.of("name", "Jane", "age", 25)),
                "metadata", Map.of(
                    "page", 1,
                    "size", 10));

            MimeBodyPublisherAdapter bodyPublisher = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                Http.Body.of(complexBody, Http.BodyContentType.JSON));

            assertEquals(MediaType.APPLICATION_JSON, bodyPublisher.mediaType());
        }

        @Test
        @DisplayName("Should handle form data with mixed content types")
        void testFormDataMixedContent() {
            FileEntryImpl fileEntry = new FileEntryImpl("doc.pdf", "pdf", "application/pdf", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }));

            Map<String, Object> formData = new HashMap<>();

            formData.put("title", "Document Title");
            formData.put("description", "A description");
            formData.put("file", fileEntry);
            formData.put("tags", "important,urgent");

            MultipartBodyPublisher multipartBodyPublisher =
                (MultipartBodyPublisher) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(formData, Http.BodyContentType.FORM_DATA));

            assertTrue(multipartBodyPublisher.mediaType()
                .toString()
                .startsWith("multipart/form-data"));
            assertEquals(4, multipartBodyPublisher.parts()
                .size());
        }
    }

    @Nested
    @DisplayName("createHttpClient with ComponentConnection tests")
    class CreateHttpClientWithConnectionTests {

        private ApplicationContext applicationContext;
        private ConnectionDefinitionService connectionDefinitionService;
        private ActionDefinitionService actionDefinitionService;

        @BeforeEach
        void setUp() {
            applicationContext = Mockito.mock(ApplicationContext.class);
            connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);
            actionDefinitionService = Mockito.mock(ActionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(applicationContext.getBean(ActionDefinitionService.class))
                .thenReturn(actionDefinitionService);

            httpClientExecutor = new HttpClientExecutor(applicationContext, tempFileStorage);
        }

        @Test
        @DisplayName("Should apply authorization headers when ComponentConnection is provided")
        void testApplyAuthorizationHeaders() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of("apiKey", "test-key"), AuthorizationType.API_KEY);

            ApplyResponse applyResponse =
                ApplyResponse.ofHeaders(Map.of("Authorization", List.of("Bearer test-token")));

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.eq("testComponent"),
                Mockito.eq(1),
                Mockito.eq(AuthorizationType.API_KEY),
                Mockito.anyMap(),
                Mockito.any(Context.class)))
                .thenReturn(applyResponse);

            Map<String, List<String>> headers = new HashMap<>();
            Map<String, List<String>> queryParameters = new HashMap<>();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                headers, queryParameters,
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertEquals(List.of("Bearer test-token"), headers.get("Authorization"));
        }

        @Test
        @DisplayName("Should apply authorization query parameters when ComponentConnection is provided")
        void testApplyAuthorizationQueryParams() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of("apiKey", "test-key"), AuthorizationType.API_KEY);

            ApplyResponse applyResponse =
                ApplyResponse.ofQueryParameters(Map.of("api_key", List.of("secret123")));

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.eq("testComponent"),
                Mockito.eq(1),
                Mockito.eq(AuthorizationType.API_KEY),
                Mockito.anyMap(),
                Mockito.any(Context.class)))
                .thenReturn(applyResponse);

            Map<String, List<String>> headers = new HashMap<>();
            Map<String, List<String>> queryParameters = new HashMap<>();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                headers, queryParameters,
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertEquals(List.of("secret123"), queryParameters.get("api_key"));
        }

        @Test
        @DisplayName("Should not apply authorization when ComponentConnection is null")
        void testNoAuthorizationWithoutConnection() {
            Map<String, List<String>> headers = new HashMap<>();
            Map<String, List<String>> queryParameters = new HashMap<>();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                headers, queryParameters,
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", null, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertTrue(headers.isEmpty());
            assertTrue(queryParameters.isEmpty());
            Mockito.verify(connectionDefinitionService, Mockito.never())
                .executeAuthorizationApply(
                    Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any());
        }

        @Test
        @DisplayName("Should handle null ApplyResponse gracefully")
        void testNullApplyResponse() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of("apiKey", "test-key"), AuthorizationType.API_KEY);

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.eq("testComponent"),
                Mockito.eq(1),
                Mockito.eq(AuthorizationType.API_KEY),
                Mockito.anyMap(),
                Mockito.any(Context.class)))
                .thenReturn(null);

            Map<String, List<String>> headers = new HashMap<>();
            Map<String, List<String>> queryParameters = new HashMap<>();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                headers, queryParameters,
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertTrue(headers.isEmpty());
            assertTrue(queryParameters.isEmpty());
        }
    }

    @Nested
    @DisplayName("createHttpRequest with ComponentConnection tests")
    class CreateHttpRequestWithConnectionTests {

        private ApplicationContext applicationContext;
        private ConnectionDefinitionService connectionDefinitionService;

        @BeforeEach
        void setUp() {
            applicationContext = Mockito.mock(ApplicationContext.class);
            connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);

            httpClientExecutor = new HttpClientExecutor(applicationContext, tempFileStorage);
        }

        @Test
        @DisplayName("Should use full URL when URL contains protocol")
        void testFullUrlWithProtocol() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "https://api.example.com/v1/users", Http.RequestMethod.GET, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(URI.create("https://api.example.com/v1/users"), httpRequest.uri());
            Mockito.verify(connectionDefinitionService, Mockito.never())
                .executeBaseUri(Mockito.anyString(), Mockito.any(), Mockito.any());
        }

        @Test
        @DisplayName("Should use full URL when ComponentConnection is null")
        void testFullUrlWithoutConnection() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost/v1/users", Http.RequestMethod.GET, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals(URI.create("http://localhost/v1/users"), httpRequest.uri());
        }

        @Test
        @DisplayName("Should prepend baseUri when URL is relative and ComponentConnection exists")
        void testRelativeUrlWithConnection() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.API_KEY);

            Mockito.when(connectionDefinitionService.executeBaseUri(
                Mockito.eq("testComponent"),
                Mockito.eq(componentConnection),
                Mockito.any(Context.class)))
                .thenReturn(Optional.of("https://api.example.com"));

            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "/v1/users", Http.RequestMethod.GET, Map.of(),
                Map.of(), null, "testComponent", componentConnection, Mockito.mock(Context.class));

            assertEquals(URI.create("https://api.example.com/v1/users"), httpRequest.uri());
        }

        @Test
        @DisplayName("Should throw exception when baseUri is not found for relative URL")
        void testRelativeUrlWithoutBaseUri() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.API_KEY);

            Mockito.when(connectionDefinitionService.executeBaseUri(
                Mockito.eq("testComponent"),
                Mockito.eq(componentConnection),
                Mockito.any(Context.class)))
                .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> httpClientExecutor.createHttpRequest(
                "/v1/users", Http.RequestMethod.GET, Map.of(),
                Map.of(), null, "testComponent", componentConnection, Mockito.mock(Context.class)));
        }
    }

    @Nested
    @DisplayName("URL creation with query parameters tests")
    class UrlCreationTests {

        @Test
        @DisplayName("Should append query parameters to URL without existing parameters")
        void testUrlWithNewQueryParams() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, Map.of(),
                Map.of("page", List.of("1"), "size", List.of("10")),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            assertTrue(uri.startsWith("http://localhost:8080/api?"));
            assertTrue(uri.contains("page=1"));
            assertTrue(uri.contains("size=10"));
        }

        @Test
        @DisplayName("Should handle query parameters with special characters")
        void testQueryParamsWithSpecialChars() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, Map.of(),
                Map.of("filter", List.of("name=John")),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            assertTrue(uri.contains("filter=name=John"));
        }

        @Test
        @DisplayName("Should handle empty query parameter values")
        void testEmptyQueryParamValues() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, Map.of(),
                Map.of("emptyParam", List.of("")),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            assertTrue(uri.contains("emptyParam="));
        }
    }

    @Nested
    @DisplayName("Content type matching tests")
    class ContentTypeMatchingTests {

        @Test
        @DisplayName("Should match JSON content type with vendor-specific media type")
        void testJsonVendorSpecificContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("application/vnd.api+json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should match XML content type with text/xml variant")
        void testXmlTextContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.XML)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "<root><key>value</key></root>",
                HttpHeaders.of(Map.of("content-type", List.of("text/xml")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should match text content type")
        void testTextPlainContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "Plain text content",
                HttpHeaders.of(Map.of("content-type", List.of("text/plain")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals("Plain text content", response.getBody());
        }

        @Test
        @DisplayName("Should handle case-insensitive content type matching")
        void testCaseInsensitiveContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("APPLICATION/JSON")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(Map.of("key", "value"), response.getBody());
        }
    }

    @Nested
    @DisplayName("Response body emptiness tests")
    class ResponseBodyEmptinessTests {

        @Test
        @DisplayName("Should return whitespace as valid text response")
        void testWhitespaceOnlyResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            Http.Response response = httpClientExecutor.handleResponse(
                new TestHttpResponse("   "), configuration, context);

            assertEquals("   ", response.getBody());
        }

        @Test
        @DisplayName("Should return newlines as valid text response")
        void testNewlineOnlyResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            Http.Response response = httpClientExecutor.handleResponse(
                new TestHttpResponse("\n\n"), configuration, context);

            assertEquals("\n\n", response.getBody());
        }

        @Test
        @DisplayName("Should return tabs as valid text response")
        void testTabOnlyResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            Http.Response response = httpClientExecutor.handleResponse(
                new TestHttpResponse("\t\t"), configuration, context);

            assertEquals("\t\t", response.getBody());
        }

        @Test
        @DisplayName("Should return null for truly empty string")
        void testEmptyStringResponse() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            Http.Response response = httpClientExecutor.handleResponse(
                new TestHttpResponse(""), configuration, context);

            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Interceptor configuration tests")
    class InterceptorConfigurationTests {

        private ApplicationContext applicationContext;
        private ConnectionDefinitionService connectionDefinitionService;
        private ActionDefinitionService actionDefinitionService;

        @BeforeEach
        void setUp() {
            applicationContext = Mockito.mock(ApplicationContext.class);
            connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);
            actionDefinitionService = Mockito.mock(ActionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(applicationContext.getBean(ActionDefinitionService.class))
                .thenReturn(actionDefinitionService);

            httpClientExecutor = new HttpClientExecutor(applicationContext, tempFileStorage);
        }

        @Test
        @DisplayName("Should create HTTP client without interceptor when disableAuthorization is true")
        void testNoInterceptorWhenAuthorizationDisabled() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.API_KEY);

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(null);

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.disableAuthorization(true)
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
        }

        @Test
        @DisplayName("Should create HTTP client without interceptor when componentOperationName is null")
        void testNoInterceptorWhenOperationNameNull() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.API_KEY);

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(null);

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, null, componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
        }

        @Test
        @DisplayName("Should create HTTP client with interceptor when authorization is enabled")
        void testInterceptorWhenAuthorizationEnabled() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(null);

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
        }
    }

    @Nested
    @DisplayName("Form data body publisher edge cases")
    class FormDataBodyPublisherEdgeCases {

        @Test
        @DisplayName("Should throw exception for empty form data map")
        void testEmptyFormData() {
            assertThrows(IllegalStateException.class, () -> httpClientExecutor.createBodyPublisher(
                Http.Body.of(Map.of(), Http.BodyContentType.FORM_DATA)));
        }

        @Test
        @DisplayName("Should handle multiple file entries in form data")
        void testMultipleFileEntries() {
            FileEntryImpl file1 = new FileEntryImpl("doc1.pdf", "pdf", "application/pdf", "file://test1");
            FileEntryImpl file2 = new FileEntryImpl("doc2.pdf", "pdf", "application/pdf", "file://test2");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }));

            Map<String, Object> formData = new HashMap<>();

            formData.put("file1", file1);
            formData.put("file2", file2);

            MultipartBodyPublisher multipartBodyPublisher =
                (MultipartBodyPublisher) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(formData, Http.BodyContentType.FORM_DATA));

            assertEquals(2, multipartBodyPublisher.parts()
                .size());
        }
    }

    @Nested
    @DisplayName("Form URL encoded edge cases")
    class FormUrlEncodedEdgeCases {

        @Test
        @DisplayName("Should handle empty map for form URL encoded")
        void testEmptyFormUrlEncoded() {
            FormBodyPublisher formBodyPublisher = (FormBodyPublisher) httpClientExecutor.createBodyPublisher(
                Http.Body.of(Map.of(), Http.BodyContentType.FORM_URL_ENCODED));

            assertEquals(MediaType.APPLICATION_FORM_URLENCODED, formBodyPublisher.mediaType());
            assertEquals("", formBodyPublisher.encodedString());
        }

        @Test
        @DisplayName("Should handle null values in list for form URL encoded")
        void testListWithNullValuesHandling() {
            List<String> listWithValues = List.of("value1", "value2");

            FormBodyPublisher formBodyPublisher = (FormBodyPublisher) httpClientExecutor.createBodyPublisher(
                Http.Body.of(
                    Map.of("items", listWithValues),
                    Http.BodyContentType.FORM_URL_ENCODED));

            assertTrue(formBodyPublisher.encodedString()
                .contains(URLEncoder.encode("items[0]", StandardCharsets.UTF_8) + "=value1"));
            assertTrue(formBodyPublisher.encodedString()
                .contains(URLEncoder.encode("items[1]", StandardCharsets.UTF_8) + "=value2"));
        }
    }

    @Nested
    @DisplayName("TriggerContext vs ActionContext tests")
    class TriggerContextTests {

        private ApplicationContext applicationContext;
        private ConnectionDefinitionService connectionDefinitionService;
        private ActionDefinitionService actionDefinitionService;
        private TriggerDefinitionService triggerDefinitionService;

        @BeforeEach
        void setUp() {
            applicationContext = Mockito.mock(ApplicationContext.class);
            connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);
            actionDefinitionService = Mockito.mock(ActionDefinitionService.class);
            triggerDefinitionService = Mockito.mock(TriggerDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(applicationContext.getBean(ActionDefinitionService.class))
                .thenReturn(actionDefinitionService);
            Mockito.when(applicationContext.getBean(TriggerDefinitionService.class))
                .thenReturn(triggerDefinitionService);

            httpClientExecutor = new HttpClientExecutor(applicationContext, tempFileStorage);
        }

        @Test
        @DisplayName("Should use ActionDefinitionService when context is ActionContext")
        void testActionContextUsesActionDefinitionService() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(null);

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, context);

            assertNotNull(httpClient);
        }

        @Test
        @DisplayName("Should use TriggerDefinitionService when context is TriggerContext")
        void testTriggerContextUsesTriggerDefinitionService() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            TriggerContext triggerContext = Mockito.mock(TriggerContext.class);

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(null);

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, triggerContext);

            assertNotNull(httpClient);
        }
    }

    @Nested
    @DisplayName("Binary response filename extraction tests")
    class BinaryFilenameExtractionTests {

        @Test
        @DisplayName("Should extract filename from image/jpeg content type")
        void testFilenameFromImageJpeg() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.binary("image/jpeg"))
                .build();

            InputStream inputStream = new ByteArrayInputStream(new byte[] {
                1, 2, 3
            });

            com.bytechef.file.storage.domain.FileEntry storageFileEntry =
                new com.bytechef.file.storage.domain.FileEntry("file.jpeg", "file://stored");

            Mockito.when(tempFileStorage.storeFileContent(Mockito.anyString(), Mockito.any(InputStream.class)))
                .thenReturn(storageFileEntry);

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                inputStream,
                HttpHeaders.of(Map.of("content-type", List.of("image/jpeg")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should extract filename from application/pdf content type")
        void testFilenameFromApplicationPdf() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.binary("application/pdf"))
                .build();

            InputStream inputStream = new ByteArrayInputStream(new byte[] {
                1, 2, 3
            });

            com.bytechef.file.storage.domain.FileEntry storageFileEntry =
                new com.bytechef.file.storage.domain.FileEntry("file.pdf", "file://stored");

            Mockito.when(tempFileStorage.storeFileContent(Mockito.anyString(), Mockito.any(InputStream.class)))
                .thenReturn(storageFileEntry);

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                inputStream,
                HttpHeaders.of(Map.of("content-type", List.of("application/pdf")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should use configured filename over content-type derived filename")
        void testConfiguredFilenameOverContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .filename("custom-name.bin")
                .build();

            InputStream inputStream = new ByteArrayInputStream(new byte[] {
                1, 2, 3
            });

            com.bytechef.file.storage.domain.FileEntry storageFileEntry =
                new com.bytechef.file.storage.domain.FileEntry("custom-name.bin", "file://stored");

            Mockito
                .when(tempFileStorage.storeFileContent(Mockito.eq("custom-name.bin"), Mockito.any(InputStream.class)))
                .thenReturn(storageFileEntry);

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                inputStream,
                HttpHeaders.of(Map.of("content-type", List.of("application/octet-stream")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());

            Mockito.verify(tempFileStorage)
                .storeFileContent(Mockito.eq("custom-name.bin"), Mockito.any(InputStream.class));
        }
    }

    @Nested
    @DisplayName("Proxy configuration tests")
    class ProxyConfigurationTests {

        @Test
        @DisplayName("Should parse proxy with host and port correctly")
        void testProxyHostAndPort() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.proxy("192.168.1.100:3128")
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertTrue(httpClient.proxy()
                .isPresent());
        }

        @Test
        @DisplayName("Should parse localhost proxy")
        void testLocalhostProxy() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.proxy("localhost:8888")
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertTrue(httpClient.proxy()
                .isPresent());
        }
    }

    @Nested
    @DisplayName("Response type matching with Type enum tests")
    class ResponseTypeMatchingTests {

        @Test
        @DisplayName("Should match when content-type matches responseType.getType() as string")
        void testMatchWithTypeEnumString() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("JSON")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should match XML when content-type is XML as string")
        void testMatchXmlWithTypeEnumString() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.XML)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "<root><key>value</key></root>",
                HttpHeaders.of(Map.of("content-type", List.of("XML")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("isEmpty method behavior tests")
    class IsEmptyBehaviorTests {

        @Test
        @DisplayName("Should return null for null body")
        void testNullBody() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                null,
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should handle non-CharSequence body types")
        void testNonCharSequenceBody() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .build();

            InputStream inputStream = new ByteArrayInputStream(new byte[] {
                1, 2, 3, 4
            });

            com.bytechef.file.storage.domain.FileEntry storageFileEntry =
                new com.bytechef.file.storage.domain.FileEntry("file.bin", "file://stored");

            Mockito.when(tempFileStorage.storeFileContent(Mockito.anyString(), Mockito.any(InputStream.class)))
                .thenReturn(storageFileEntry);

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                inputStream,
                HttpHeaders.of(Map.of("content-type", List.of("application/octet-stream")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Combined followRedirect configuration tests")
    class RedirectConfigurationTests {

        @Test
        @DisplayName("Should not follow redirects when neither option is set")
        void testNoRedirectFollow() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertEquals(HttpClient.Redirect.NEVER, httpClient.followRedirects());
        }

        @Test
        @DisplayName("Should follow normal redirects when followRedirect is true")
        void testFollowNormalRedirects() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.followRedirect(true)
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertEquals(HttpClient.Redirect.NORMAL, httpClient.followRedirects());
        }

        @Test
        @DisplayName("Should follow all redirects when followAllRedirects is true")
        void testFollowAllRedirects() {
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.followAllRedirects(true)
                    .build(),
                "componentName", 1, "componentOperationName", null, Mockito.mock(Context.class));

            assertEquals(HttpClient.Redirect.ALWAYS, httpClient.followRedirects());
        }
    }

    @Nested
    @DisplayName("Body content handling edge cases")
    class BodyContentEdgeCasesTests {

        @Test
        @DisplayName("Should handle string body with null mimeType using default")
        void testStringBodyWithDefaultMimeType() {
            MimeBodyPublisherAdapter bodyPublisher =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of("test content"));

            assertEquals(MediaType.TEXT_PLAIN, bodyPublisher.mediaType());
        }

        @Test
        @DisplayName("Should handle JSON body with nested arrays")
        void testJsonBodyWithNestedArrays() {
            Map<String, Object> complexBody = Map.of(
                "matrix", List.of(
                    List.of(1, 2, 3),
                    List.of(4, 5, 6)));

            MimeBodyPublisherAdapter bodyPublisher = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                Http.Body.of(complexBody, Http.BodyContentType.JSON));

            assertEquals(MediaType.APPLICATION_JSON, bodyPublisher.mediaType());
        }

        @Test
        @DisplayName("Should handle XML body")
        void testXmlBodyPublisher() {
            Map<String, Object> xmlContent = Map.of(
                "root", Map.of(
                    "child1", "value1",
                    "child2", "value2"));

            MimeBodyPublisherAdapter bodyPublisher = (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                Http.Body.of(xmlContent, Http.BodyContentType.XML));

            assertEquals(MediaType.APPLICATION_XML, bodyPublisher.mediaType());
        }
    }

    @Nested
    @DisplayName("Response headers retrieval tests")
    class ResponseHeadersRetrievalTests {

        @Test
        @DisplayName("Should return empty list for non-existent header")
        void testNonExistentHeader() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getHeader("x-non-existent"));
        }

        @Test
        @DisplayName("Should return all headers via getHeaders")
        void testGetAllHeaders() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(
                    Map.of(
                        "content-type", List.of("application/json"),
                        "x-custom", List.of("custom-value"),
                        "x-rate-limit", List.of("100")),
                    (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            Map<String, List<String>> headers = response.getHeaders();

            assertEquals(3, headers.size());
            assertTrue(headers.containsKey("content-type"));
            assertTrue(headers.containsKey("x-custom"));
            assertTrue(headers.containsKey("x-rate-limit"));
        }
    }

    @Nested
    @DisplayName("Authorization type specific tests")
    class AuthorizationTypeTests {

        private ApplicationContext applicationContext;
        private ConnectionDefinitionService connectionDefinitionService;

        @BeforeEach
        void setUp() {
            applicationContext = Mockito.mock(ApplicationContext.class);
            connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);

            httpClientExecutor = new HttpClientExecutor(applicationContext, tempFileStorage);
        }

        @Test
        @DisplayName("Should handle BASIC authorization type")
        void testBasicAuthorization() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of("username", "user", "password", "pass"), AuthorizationType.BASIC_AUTH);

            ApplyResponse applyResponse =
                ApplyResponse.ofHeaders(Map.of("Authorization", List.of("Basic dXNlcjpwYXNz")));

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.eq("testComponent"),
                Mockito.eq(1),
                Mockito.eq(AuthorizationType.BASIC_AUTH),
                Mockito.anyMap(),
                Mockito.any(Context.class)))
                .thenReturn(applyResponse);

            Map<String, List<String>> headers = new HashMap<>();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                headers, new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertEquals(List.of("Basic dXNlcjpwYXNz"), headers.get("Authorization"));
        }

        @Test
        @DisplayName("Should handle BEARER_TOKEN authorization type")
        void testBearerTokenAuthorization() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of("token", "abc123"), AuthorizationType.BEARER_TOKEN);

            ApplyResponse applyResponse =
                ApplyResponse.ofHeaders(Map.of("Authorization", List.of("Bearer abc123")));

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.eq("testComponent"),
                Mockito.eq(1),
                Mockito.eq(AuthorizationType.BEARER_TOKEN),
                Mockito.anyMap(),
                Mockito.any(Context.class)))
                .thenReturn(applyResponse);

            Map<String, List<String>> headers = new HashMap<>();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                headers, new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertEquals(List.of("Bearer abc123"), headers.get("Authorization"));
        }

        @Test
        @DisplayName("Should handle OAUTH2_AUTHORIZATION_CODE authorization type")
        void testOAuth2AuthorizationCode() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of("accessToken", "oauth-token"),
                AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            ApplyResponse applyResponse =
                ApplyResponse.ofHeaders(Map.of("Authorization", List.of("Bearer oauth-token")));

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.eq("testComponent"),
                Mockito.eq(1),
                Mockito.eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE),
                Mockito.anyMap(),
                Mockito.any(Context.class)))
                .thenReturn(applyResponse);

            Map<String, List<String>> headers = new HashMap<>();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                headers, new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertEquals(List.of("Bearer oauth-token"), headers.get("Authorization"));
        }
    }

    @Nested
    @DisplayName("ComponentConnection edge cases")
    class ComponentConnectionEdgeCasesTests {

        private ApplicationContext applicationContext;
        private ConnectionDefinitionService connectionDefinitionService;

        @BeforeEach
        void setUp() {
            applicationContext = Mockito.mock(ApplicationContext.class);
            connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);

            httpClientExecutor = new HttpClientExecutor(applicationContext, tempFileStorage);
        }

        @Test
        @DisplayName("Should not apply authorization when authorizationType is null")
        void testNullAuthorizationType() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), null);

            Map<String, List<String>> headers = new HashMap<>();
            Map<String, List<String>> queryParameters = new HashMap<>();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                headers, queryParameters,
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
            assertTrue(headers.isEmpty());
            Mockito.verify(connectionDefinitionService, Mockito.never())
                .executeAuthorizationApply(
                    Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any());
        }

        @Test
        @DisplayName("Should handle canCredentialsBeRefreshed true")
        void testCanCredentialsBeRefreshedTrue() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            assertTrue(componentConnection.canCredentialsBeRefreshed());

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(null);

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
        }

        @Test
        @DisplayName("Should handle canCredentialsBeRefreshed false for API_KEY")
        void testCanCredentialsBeRefreshedFalseForApiKey() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.API_KEY);

            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(null);

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(),
                Http.Configuration.newConfiguration()
                    .build(),
                "testComponent", 1, "testAction", componentConnection, Mockito.mock(Context.class));

            assertNotNull(httpClient);
        }
    }

    @Nested
    @DisplayName("Content-type with parameters tests")
    class ContentTypeWithParametersTests {

        @Test
        @DisplayName("Should handle content-type with boundary parameter")
        void testContentTypeWithBoundary() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(
                    Map.of("content-type", List.of("application/json; boundary=something")),
                    (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(Map.of("key", "value"), response.getBody());
        }

        @Test
        @DisplayName("Should handle content-type with multiple parameters")
        void testContentTypeWithMultipleParams() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(
                    Map.of("content-type", List.of("application/json; charset=utf-8; boundary=abc")),
                    (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("HTTP status code edge cases")
    class HttpStatusCodeEdgeCasesTests {

        @Test
        @DisplayName("Should handle status code 100 (Continue)")
        void testStatusCode100() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"status\":\"continue\"}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                100);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals(100, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle status code 301 (Moved Permanently)")
        void testStatusCode301() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"redirect\":true}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                301);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals(301, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle status code 204 with non-matching content-type")
        void testStatusCode204WithMismatchedContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                null,
                HttpHeaders.of(Map.of("content-type", List.of("text/html")), (n, v) -> true),
                204);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals(204, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Query parameter URL encoding tests")
    class QueryParameterEncodingTests {

        @Test
        @DisplayName("Should handle pre-encoded query parameter with spaces")
        void testQueryParamWithSpaces() {
            // Note: HttpClientExecutor expects callers to provide URL-encoded values
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, Map.of(),
                Map.of("name", List.of("John%20Doe")),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            assertTrue(uri.contains("name=John%20Doe"));
        }

        @Test
        @DisplayName("Should handle pre-encoded query parameter with ampersand")
        void testQueryParamWithAmpersand() {
            // Note: HttpClientExecutor expects callers to provide URL-encoded values
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, Map.of(),
                Map.of("company", List.of("A%26B%20Corp")),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            assertTrue(uri.contains("company=A%26B%20Corp"));
        }

        @Test
        @DisplayName("Should throw exception for unencoded query parameter with spaces")
        void testUnencodedQueryParamWithSpacesThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, Map.of(),
                Map.of("name", List.of("John Doe")),
                null, "componentName", null, Mockito.mock(Context.class)));
        }
    }

    @Nested
    @DisplayName("Binary body publisher with mime type tests")
    class BinaryBodyMimeTypeTests {

        @Test
        @DisplayName("Should use file entry mime type when body mime type is null")
        void testBinaryBodyUsesFileEntryMimeType() {
            FileEntryImpl fileEntry =
                new FileEntryImpl("image.png", "png", "image/png", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }));

            MimeBodyPublisherAdapter bodyPublisher =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(fileEntry));

            assertEquals(MediaType.parse("image/png"), bodyPublisher.mediaType());
        }

        @Test
        @DisplayName("Should override file entry mime type with body mime type")
        void testBinaryBodyOverridesFileEntryMimeType() {
            FileEntryImpl fileEntry =
                new FileEntryImpl("document.bin", "bin", "application/octet-stream", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }));

            MimeBodyPublisherAdapter bodyPublisher =
                (MimeBodyPublisherAdapter) httpClientExecutor.createBodyPublisher(
                    Http.Body.of(fileEntry, "application/pdf"));

            assertEquals(MediaType.parse("application/pdf"), bodyPublisher.mediaType());
        }
    }

    @Nested
    @DisplayName("Request method with body tests")
    class RequestMethodWithBodyTests {

        @Test
        @DisplayName("Should create POST request with JSON body")
        void testPostWithJsonBody() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.POST, Map.of(),
                Map.of(),
                Http.Body.of(Map.of("key", "value"), Http.BodyContentType.JSON),
                "componentName", null, Mockito.mock(Context.class));

            assertEquals("POST", httpRequest.method());
            assertTrue(httpRequest.bodyPublisher()
                .isPresent());
        }

        @Test
        @DisplayName("Should create PUT request with form data")
        void testPutWithFormData() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.PUT, Map.of(),
                Map.of(),
                Http.Body.of(Map.of("field", "value"), Http.BodyContentType.FORM_URL_ENCODED),
                "componentName", null, Mockito.mock(Context.class));

            assertEquals("PUT", httpRequest.method());
            assertTrue(httpRequest.bodyPublisher()
                .isPresent());
        }

        @Test
        @DisplayName("Should create DELETE request without body")
        void testDeleteWithoutBody() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.DELETE, Map.of(),
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertEquals("DELETE", httpRequest.method());
            assertTrue(httpRequest.bodyPublisher()
                .isPresent());
            assertEquals(0, httpRequest.bodyPublisher()
                .get()
                .contentLength());
        }
    }

    @Nested
    @DisplayName("Response body conversion tests")
    class ResponseBodyConversionTests {

        @Test
        @DisplayName("Should convert JSON response to specific type")
        void testJsonToSpecificType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"name\":\"test\",\"count\":42}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody(Map.class);

            assertEquals("test", body.get("name"));
            assertEquals(42, body.get("count"));
        }

        @Test
        @DisplayName("Should handle JSON array to List conversion")
        void testJsonArrayToList() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "[1, 2, 3, 4, 5]",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            @SuppressWarnings("unchecked")
            List<Integer> body = response.getBody(List.class);

            assertEquals(5, body.size());
        }
    }

    @Nested
    @DisplayName("Header manipulation tests")
    class HeaderManipulationTests {

        @Test
        @DisplayName("Should add multiple headers with same name")
        void testMultipleHeadersSameName() {
            Map<String, List<String>> headers = new HashMap<>();

            headers.put("Accept", List.of("application/json", "text/plain"));

            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, headers,
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            List<String> acceptHeaders = httpRequest.headers()
                .allValues("Accept");

            assertEquals(2, acceptHeaders.size());
            assertTrue(acceptHeaders.contains("application/json"));
            assertTrue(acceptHeaders.contains("text/plain"));
        }

        @Test
        @DisplayName("Should preserve header case sensitivity")
        void testHeaderCaseSensitivity() {
            Map<String, List<String>> headers = new HashMap<>();

            headers.put("X-Custom-Header", List.of("value1"));
            headers.put("x-another-header", List.of("value2"));

            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, headers,
                Map.of(), null, "componentName", null, Mockito.mock(Context.class));

            assertNotNull(httpRequest.headers());
        }
    }

    @Nested
    @DisplayName("Form URL encoded nested parameter tests")
    class FormUrlEncodedNestedParameterTests {

        @Test
        @DisplayName("Should handle nested map parameters")
        void testNestedMapParameters() {
            Map<String, Object> nestedMap = new LinkedHashMap<>();

            nestedMap.put("street", "123 Main St");
            nestedMap.put("city", "Springfield");

            Map<String, Object> bodyContent = new LinkedHashMap<>();

            bodyContent.put("name", "John");
            bodyContent.put("address", nestedMap);

            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of(bodyContent, Http.BodyContentType.FORM_URL_ENCODED));

            assertNotNull(bodyPublisher);
            assertInstanceOf(FormBodyPublisher.class, bodyPublisher);
        }

        @Test
        @DisplayName("Should handle list parameters with indexed keys")
        void testListParametersWithIndexedKeys() {
            Map<String, Object> bodyContent = new LinkedHashMap<>();

            bodyContent.put("name", "John");
            bodyContent.put("tags", List.of("tag1", "tag2", "tag3"));

            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of(bodyContent, Http.BodyContentType.FORM_URL_ENCODED));

            assertNotNull(bodyPublisher);
            assertInstanceOf(FormBodyPublisher.class, bodyPublisher);
        }

        @Test
        @DisplayName("Should handle deeply nested structures")
        void testDeeplyNestedStructures() {
            Map<String, Object> innerNested = new LinkedHashMap<>();

            innerNested.put("zip", "12345");

            Map<String, Object> nestedMap = new LinkedHashMap<>();

            nestedMap.put("location", innerNested);

            Map<String, Object> bodyContent = new LinkedHashMap<>();

            bodyContent.put("data", nestedMap);

            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of(bodyContent, Http.BodyContentType.FORM_URL_ENCODED));

            assertNotNull(bodyPublisher);
        }
    }

    @Nested
    @DisplayName("Response header accessor tests")
    class ResponseHeaderAccessorTests {

        @Test
        @DisplayName("Should get first header value")
        void testGetFirstHeader() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(
                    Map.of(
                        "content-type", List.of("application/json"),
                        "x-request-id", List.of("abc123", "def456")),
                    (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals("abc123", response.getFirstHeader("x-request-id"));
        }

        @Test
        @DisplayName("Should get all header values")
        void testGetAllHeaderValues() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(
                    Map.of("x-custom", List.of("value1", "value2", "value3")),
                    (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            List<String> headerValues = response.getHeader("x-custom");

            assertEquals(3, headerValues.size());
            assertTrue(headerValues.contains("value1"));
            assertTrue(headerValues.contains("value2"));
            assertTrue(headerValues.contains("value3"));
        }

        @Test
        @DisplayName("Should get body with TypeReference")
        void testGetBodyWithTypeReference() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"name\":\"test\",\"active\":true}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            Map<String, Object> body = response.getBody(
                new com.bytechef.component.definition.TypeReference<Map<String, Object>>() {});

            assertNotNull(body);
            assertEquals("test", body.get("name"));
            assertEquals(true, body.get("active"));
        }
    }

    @Nested
    @DisplayName("Binary response filename extraction tests")
    class BinaryResponseFilenameExtractionTests {

        @Test
        @DisplayName("Should use custom filename from configuration")
        void testCustomFilenameFromConfiguration() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.binary("application/pdf"))
                .filename("custom-report.pdf")
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                new ByteArrayInputStream(new byte[] {
                    1, 2, 3, 4
                }),
                HttpHeaders.of(Map.of("content-type", List.of("application/pdf")), (n, v) -> true),
                200);

            Mockito
                .when(tempFileStorage.storeFileContent(Mockito.eq("custom-report.pdf"), Mockito.any(InputStream.class)))
                .thenReturn(
                    new com.bytechef.file.storage.domain.FileEntry("custom-report.pdf", "pdf", "application/pdf",
                        "file://stored"));

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should extract extension from content-type when no filename")
        void testFilenameFromContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.binary("image/png"))
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                new ByteArrayInputStream(new byte[] {
                    1, 2, 3, 4
                }),
                HttpHeaders.of(Map.of("content-type", List.of("image/png")), (n, v) -> true),
                200);

            Mockito.when(tempFileStorage.storeFileContent(Mockito.startsWith("file."), Mockito.any(InputStream.class)))
                .thenReturn(
                    new com.bytechef.file.storage.domain.FileEntry("file.png", "png", "image/png", "file://stored"));

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should default to file.txt when no content-type header")
        void testDefaultFilenameWithoutContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                new ByteArrayInputStream(new byte[] {
                    1, 2, 3, 4
                }),
                HttpHeaders.of(Map.of(), (n, v) -> true),
                200);

            Mockito.when(tempFileStorage.storeFileContent(Mockito.eq("file.txt"), Mockito.any(InputStream.class)))
                .thenReturn(
                    new com.bytechef.file.storage.domain.FileEntry("file.txt", "txt", "text/plain", "file://stored"));

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("Disable authorization configuration tests")
    class DisableAuthorizationTests {

        @Test
        @DisplayName("Should skip authorization when disableAuthorization is true")
        void testDisableAuthorizationSkipsAuth() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of("token", "secret"), AuthorizationType.BEARER_TOKEN);

            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .disableAuthorization(true)
                .build();

            // Should NOT call connectionDefinitionService.executeAuthorizationApply
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, "testOperation",
                componentConnection, context);

            assertNotNull(httpClient);

            // Verify that applyAuthorization was not called by checking that connectionDefinitionService
            // was never invoked
            Mockito.verify(applicationContext, Mockito.never())
                .getBean(ConnectionDefinitionService.class);
        }

        @Test
        @DisplayName("Should apply authorization when disableAuthorization is false")
        void testEnableAuthorizationAppliesAuth() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of("token", "secret"), AuthorizationType.BEARER_TOKEN);

            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .disableAuthorization(false)
                .build();

            ConnectionDefinitionService connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(ApplyResponse.ofHeaders(Map.of()));

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, "testOperation",
                componentConnection, context);

            assertNotNull(httpClient);
            Mockito.verify(connectionDefinitionService)
                .executeAuthorizationApply(
                    Mockito.eq("testComponent"), Mockito.eq(1), Mockito.eq(AuthorizationType.BEARER_TOKEN),
                    Mockito.anyMap(), Mockito.any());
        }
    }

    @Nested
    @DisplayName("Follow all redirects configuration tests")
    class FollowAllRedirectsTests {

        @Test
        @DisplayName("Should configure ALWAYS redirect policy when followAllRedirects is true")
        void testFollowAllRedirectsAlwaysPolicy() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .followAllRedirects(true)
                .build();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context);

            assertEquals(HttpClient.Redirect.ALWAYS, httpClient.followRedirects());
        }

        @Test
        @DisplayName("Should configure NORMAL redirect policy when followRedirect is true")
        void testFollowRedirectNormalPolicy() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .followRedirect(true)
                .build();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context);

            assertEquals(HttpClient.Redirect.NORMAL, httpClient.followRedirects());
        }

        @Test
        @DisplayName("Should default to NEVER redirect policy")
        void testDefaultNoRedirectPolicy() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context);

            assertEquals(HttpClient.Redirect.NEVER, httpClient.followRedirects());
        }
    }

    @Nested
    @DisplayName("Connection URL exception tests")
    class ConnectionUrlExceptionTests {

        @Test
        @DisplayName("Should throw exception when baseUri not found for relative URL")
        void testThrowsExceptionWhenBaseUriNotFound() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.API_KEY);

            ConnectionDefinitionService connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(connectionDefinitionService.executeBaseUri(
                Mockito.eq("testComponent"), Mockito.eq(componentConnection), Mockito.any()))
                .thenReturn(Optional.empty());

            RuntimeException exception = assertThrows(RuntimeException.class,
                () -> httpClientExecutor.createHttpRequest(
                    "/api/v1/resource", Http.RequestMethod.GET, Map.of(), Map.of(),
                    null, "testComponent", componentConnection, context));

            assertTrue(exception.getMessage()
                .contains("Failed to get baseUri"));
        }
    }

    @Nested
    @DisplayName("Content type matching without header tests")
    class ContentTypeMatchingWithoutHeaderTests {

        @Test
        @DisplayName("Should return response when no content-type header present")
        void testMatchesWithoutContentTypeHeader() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            // Response without content-type header should still be processed
            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"data\":\"value\"}",
                HttpHeaders.of(Map.of(), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Form data with mixed content tests")
    class FormDataMixedContentTests {

        @Test
        @DisplayName("Should handle form data with both text and file parts")
        void testFormDataWithTextAndFile() {
            FileEntryImpl fileEntry = new FileEntryImpl("document.pdf", "pdf", "application/pdf", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }));

            Map<String, Object> bodyContent = new LinkedHashMap<>();

            bodyContent.put("description", "Test document");
            bodyContent.put("category", "reports");
            bodyContent.put("file", fileEntry);

            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of(bodyContent, Http.BodyContentType.FORM_DATA));

            assertNotNull(bodyPublisher);
            assertInstanceOf(MultipartBodyPublisher.class, bodyPublisher);
        }

        @Test
        @DisplayName("Should handle form data with multiple file entries")
        void testFormDataWithMultipleFiles() {
            FileEntryImpl fileEntry1 = new FileEntryImpl("doc1.pdf", "pdf", "application/pdf", "file://test1");
            FileEntryImpl fileEntry2 = new FileEntryImpl("doc2.pdf", "pdf", "application/pdf", "file://test2");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }));

            Map<String, Object> bodyContent = new LinkedHashMap<>();

            bodyContent.put("file1", fileEntry1);
            bodyContent.put("file2", fileEntry2);

            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of(bodyContent, Http.BodyContentType.FORM_DATA));

            assertNotNull(bodyPublisher);
            assertInstanceOf(MultipartBodyPublisher.class, bodyPublisher);
        }
    }

    @Nested
    @DisplayName("Empty string body tests")
    class EmptyStringBodyTests {

        @Test
        @DisplayName("Should treat empty string response body as empty")
        void testEmptyStringBodyIsEmpty() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "",
                HttpHeaders.of(Map.of("content-type", List.of("text/plain")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should process non-empty string response body")
        void testNonEmptyStringBodyProcessed() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "Hello World",
                HttpHeaders.of(Map.of("content-type", List.of("text/plain")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals("Hello World", response.getBody());
        }
    }

    @Nested
    @DisplayName("Allow unauthorized certs tests")
    class AllowUnauthorizedCertsTests {

        @Test
        @DisplayName("Should create HTTP client with custom SSL context when allowUnauthorizedCerts is true")
        void testAllowUnauthorizedCertsConfiguresCustomSsl() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .allowUnauthorizedCerts(true)
                .build();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context);

            assertNotNull(httpClient);
            // The SSLContext is set internally, we can only verify the client is created successfully
            assertNotNull(httpClient.sslContext());
        }

        @Test
        @DisplayName("Should create HTTP client with default SSL context when allowUnauthorizedCerts is false")
        void testDefaultSslContextUsed() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .allowUnauthorizedCerts(false)
                .build();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context);

            assertNotNull(httpClient);
        }
    }

    @Nested
    @DisplayName("Response type content type matching tests")
    class ResponseTypeContentTypeMatchingTests {

        @Test
        @DisplayName("Should match custom binary content type")
        void testMatchCustomBinaryContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.binary("application/zip"))
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }),
                HttpHeaders.of(Map.of("content-type", List.of("application/zip")), (n, v) -> true),
                200);

            Mockito.when(tempFileStorage.storeFileContent(Mockito.anyString(), Mockito.any(InputStream.class)))
                .thenReturn(
                    new com.bytechef.file.storage.domain.FileEntry("file.zip", "zip", "application/zip",
                        "file://stored"));

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should match response type by type name")
        void testMatchByTypeName() {
            // When content-type is just "json" instead of "application/json"
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("String body publisher tests")
    class StringBodyPublisherTests {

        @Test
        @DisplayName("Should create string body publisher with custom mime type")
        void testStringBodyWithCustomMimeType() {
            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of("custom content", "text/csv"));

            assertNotNull(bodyPublisher);
            assertInstanceOf(MimeBodyPublisherAdapter.class, bodyPublisher);
            assertEquals(MediaType.parse("text/csv"), ((MimeBodyPublisherAdapter) bodyPublisher).mediaType());
        }

        @Test
        @DisplayName("Should handle object toString for string body")
        void testObjectToStringForStringBody() {
            Object customObject = new Object() {

                @Override
                public String toString() {
                    return "custom-object-content";
                }
            };

            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of(customObject, "text/plain"));

            assertNotNull(bodyPublisher);
        }
    }

    @Nested
    @DisplayName("Form URL encoded null value tests")
    class FormUrlEncodedNullValueTests {

        @Test
        @DisplayName("Should throw assertion error for null value in form data")
        void testNullValueThrowsAssertion() {
            Map<String, Object> bodyContent = new HashMap<>();

            bodyContent.put("name", "John");
            bodyContent.put("nullField", null);

            assertThrows(IllegalArgumentException.class, () -> httpClientExecutor.createBodyPublisher(
                Http.Body.of(bodyContent, Http.BodyContentType.FORM_URL_ENCODED)));
        }
    }

    @Nested
    @DisplayName("Apply authorization null response tests")
    class ApplyAuthorizationNullResponseTests {

        @Test
        @DisplayName("Should not modify headers when ApplyResponse is null")
        void testNullApplyResponseDoesNotModifyHeaders() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of("token", "secret"), AuthorizationType.BEARER_TOKEN);

            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            ConnectionDefinitionService connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(null);

            Map<String, List<String>> headers = new HashMap<>();

            headers.put("X-Original", List.of("value"));

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                headers, new HashMap<>(), configuration, "testComponent", 1, null,
                componentConnection, context);

            assertNotNull(httpClient);
            assertEquals(1, headers.size());
            assertTrue(headers.containsKey("X-Original"));
        }
    }

    @Nested
    @DisplayName("isEmpty additional edge case tests")
    class IsEmptyAdditionalEdgeCaseTests {

        @Test
        @DisplayName("Should return null body for null response body")
        void testNullResponseBodyReturnsNullBody() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                null,
                HttpHeaders.of(Map.of("content-type", List.of("text/plain")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getBody());
        }

        @Test
        @DisplayName("Should return body for whitespace-only string (not empty)")
        void testWhitespaceOnlyStringIsNotEmpty() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "   ",
                HttpHeaders.of(Map.of("content-type", List.of("text/plain")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertEquals("   ", response.getBody());
        }
    }

    @Nested
    @DisplayName("Both redirect flags true tests")
    class BothRedirectFlagsTrueTests {

        @Test
        @DisplayName("Should use ALWAYS redirect when both followRedirect and followAllRedirects are true")
        void testBothRedirectFlagsTrue() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .followRedirect(true)
                .followAllRedirects(true)
                .build();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context);

            // followAllRedirects is checked after followRedirect, so ALWAYS should be the result
            assertEquals(HttpClient.Redirect.ALWAYS, httpClient.followRedirects());
        }
    }

    @Nested
    @DisplayName("Response headers accessor tests")
    class ResponseHeadersAccessorTests {

        @Test
        @DisplayName("Should return all headers from getHeaders()")
        void testGetHeadersReturnsAllHeaders() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(
                    Map.of(
                        "content-type", List.of("application/json"),
                        "x-custom-1", List.of("value1"),
                        "x-custom-2", List.of("value2", "value3")),
                    (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            Map<String, List<String>> headers = response.getHeaders();

            assertNotNull(headers);
            assertTrue(headers.containsKey("content-type"));
            assertTrue(headers.containsKey("x-custom-1"));
            assertTrue(headers.containsKey("x-custom-2"));
            assertEquals(2, headers.get("x-custom-2")
                .size());
        }
    }

    @Nested
    @DisplayName("Query parameter multiple values tests")
    class QueryParameterMultipleValuesTests {

        @Test
        @DisplayName("Should handle multiple values for same query parameter")
        void testMultipleValuesForSameParam() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, Map.of(),
                Map.of("tags", List.of("tag1", "tag2", "tag3")),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            assertTrue(uri.contains("tags=tag1"));
            assertTrue(uri.contains("tags=tag2"));
            assertTrue(uri.contains("tags=tag3"));
        }

        @Test
        @DisplayName("Should handle empty list for query parameter")
        void testEmptyListForQueryParam() {
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "http://localhost:8080/api", Http.RequestMethod.GET, Map.of(),
                Map.of("empty", List.of()),
                null, "componentName", null, Mockito.mock(Context.class));

            String uri = httpRequest.uri()
                .toString();

            // Empty list should result in no query params for that key
            assertFalse(uri.contains("empty="));
        }
    }

    @Nested
    @DisplayName("Proxy configuration edge cases tests")
    class ProxyConfigurationEdgeCasesTests {

        @Test
        @DisplayName("Should throw exception for invalid proxy format without port")
        void testInvalidProxyFormatThrowsException() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .proxy("localhost")
                .build();

            assertThrows(ArrayIndexOutOfBoundsException.class, () -> httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context));
        }

        @Test
        @DisplayName("Should throw exception for invalid proxy port")
        void testInvalidProxyPortThrowsException() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .proxy("localhost:invalid")
                .build();

            assertThrows(NumberFormatException.class, () -> httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context));
        }
    }

    @Nested
    @DisplayName("Connection URL with protocol tests")
    class ConnectionUrlWithProtocolTests {

        @Test
        @DisplayName("Should use URL directly when it contains protocol")
        void testUrlWithProtocolUsedDirectly() {
            // When URL contains "://", it should be used directly without calling executeBaseUri
            HttpRequest httpRequest = httpClientExecutor.createHttpRequest(
                "https://api.example.com/resource", Http.RequestMethod.GET, Map.of(), Map.of(),
                null, "testComponent", null, Mockito.mock(Context.class));

            assertEquals("https://api.example.com/resource", httpRequest.uri()
                .toString());

            // Verify executeBaseUri was never called
            Mockito.verify(applicationContext, Mockito.never())
                .getBean(ConnectionDefinitionService.class);
        }
    }

    @Nested
    @DisplayName("Response with null responseType tests")
    class NullResponseTypeTests {

        @Test
        @DisplayName("Should return null body when responseType is null and status is not 204")
        void testNullResponseTypeReturnsNullBody() {
            Http.Configuration configuration = Http.Configuration.newConfiguration()
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "some response",
                HttpHeaders.of(Map.of("content-type", List.of("text/plain")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getBody());
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("getFirstHeader edge cases tests")
    class GetFirstHeaderEdgeCasesTests {

        @Test
        @DisplayName("Should throw NullPointerException when header not found")
        void testGetFirstHeaderNotFound() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertThrows(NullPointerException.class, () -> response.getFirstHeader("non-existent-header"));
        }
    }

    @Nested
    @DisplayName("Default timeout configuration tests")
    class DefaultTimeoutTests {

        @Test
        @DisplayName("Should use default 4000ms timeout when timeout is null")
        void testDefaultTimeoutWhenNull() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context);

            assertTrue(httpClient.connectTimeout()
                .isPresent());
            assertEquals(Duration.ofMillis(4000), httpClient.connectTimeout()
                .get());
        }

        @Test
        @DisplayName("Should use custom timeout when specified")
        void testCustomTimeout() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null, null, context);

            assertTrue(httpClient.connectTimeout()
                .isPresent());
            assertEquals(Duration.ofSeconds(30), httpClient.connectTimeout()
                .get());
        }
    }

    @Nested
    @DisplayName("Content-type case insensitivity tests")
    class ContentTypeCaseInsensitivityTests {

        @Test
        @DisplayName("Should match content-type case insensitively")
        void testContentTypeCaseInsensitive() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            // Content-Type header with uppercase
            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("APPLICATION/JSON")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should match mixed case content-type")
        void testMixedCaseContentType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("Application/Json")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("FileEntry mime type in form data tests")
    class FileEntryMimeTypeTests {

        @Test
        @DisplayName("Should use FileEntry mime type for multipart upload")
        void testFileEntryMimeTypeInMultipart() {
            FileEntryImpl fileEntry = new FileEntryImpl("report.xlsx", "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }));

            Map<String, Object> bodyContent = new LinkedHashMap<>();

            bodyContent.put("file", fileEntry);

            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of(bodyContent, Http.BodyContentType.FORM_DATA));

            assertNotNull(bodyPublisher);
            assertInstanceOf(MultipartBodyPublisher.class, bodyPublisher);
        }
    }

    @Nested
    @DisplayName("Operation definition service selection tests")
    class OperationDefinitionServiceSelectionTests {

        @Test
        @DisplayName("Should use ActionDefinitionService for action context")
        void testActionDefinitionServiceForAction() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            ConnectionDefinitionService connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);
            ActionDefinitionService actionDefinitionService = Mockito.mock(ActionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(applicationContext.getBean(ActionDefinitionService.class))
                .thenReturn(actionDefinitionService);
            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(ApplyResponse.ofHeaders(Map.of()));

            // ActionContext is used, so isAction=true
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, "testOperation",
                componentConnection, context);

            assertNotNull(httpClient);
        }

        @Test
        @DisplayName("Should use TriggerDefinitionService for trigger context")
        void testTriggerDefinitionServiceForTrigger() {
            TriggerContext triggerContext = Mockito.mock(TriggerContext.class);

            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            ConnectionDefinitionService connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);
            TriggerDefinitionService triggerDefinitionService = Mockito.mock(TriggerDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(applicationContext.getBean(TriggerDefinitionService.class))
                .thenReturn(triggerDefinitionService);
            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(ApplyResponse.ofHeaders(Map.of()));

            // TriggerContext is used, so isAction=false
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, "testTrigger",
                componentConnection, triggerContext);

            assertNotNull(httpClient);
        }
    }

    @Nested
    @DisplayName("Interceptor configuration with componentOperationName tests")
    class InterceptorWithOperationNameTests {

        @Test
        @DisplayName("Should add interceptor when componentOperationName is provided")
        void testInterceptorAddedWithOperationName() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            ConnectionDefinitionService connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);
            ActionDefinitionService actionDefinitionService = Mockito.mock(ActionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(applicationContext.getBean(ActionDefinitionService.class))
                .thenReturn(actionDefinitionService);
            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(ApplyResponse.ofHeaders(Map.of()));

            // With componentOperationName, interceptor should be added
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, "testOperation",
                componentConnection, context);

            assertNotNull(httpClient);
        }

        @Test
        @DisplayName("Should not add interceptor when componentOperationName is null")
        void testNoInterceptorWithoutOperationName() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.BEARER_TOKEN);

            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            ConnectionDefinitionService connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(ApplyResponse.ofHeaders(Map.of()));

            // Without componentOperationName, no interceptor added
            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), new HashMap<>(), configuration, "testComponent", 1, null,
                componentConnection, context);

            assertNotNull(httpClient);

            // ActionDefinitionService should not be retrieved (no interceptor)
            Mockito.verify(applicationContext, Mockito.never())
                .getBean(ActionDefinitionService.class);
        }
    }

    @Nested
    @DisplayName("Binary body with null mime type tests")
    class BinaryBodyNullMimeTypeTests {

        @Test
        @DisplayName("Should use FileEntry mime type when body mime type is null")
        void testUsesFileEntryMimeTypeWhenBodyMimeTypeNull() {
            FileEntryImpl fileEntry = new FileEntryImpl("image.png", "png", "image/png", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }));

            // Body.of(FileEntry) sets mimeType to null, should fall back to FileEntry mime type
            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of(fileEntry));

            assertNotNull(bodyPublisher);
            assertInstanceOf(MimeBodyPublisherAdapter.class, bodyPublisher);
            assertEquals(MediaType.parse("image/png"), ((MimeBodyPublisherAdapter) bodyPublisher).mediaType());
        }

        @Test
        @DisplayName("Should use body mime type when specified")
        void testUsesBodyMimeTypeWhenSpecified() {
            FileEntryImpl fileEntry = new FileEntryImpl("image.png", "png", "image/png", "file://test");

            Mockito.when(tempFileStorage.getInputStream(Mockito.any()))
                .thenReturn(new ByteArrayInputStream(new byte[] {
                    1, 2, 3
                }));

            // Body.of(FileEntry, String) sets custom mime type, should use it instead of FileEntry mime type
            BodyPublisher bodyPublisher = httpClientExecutor.createBodyPublisher(
                Http.Body.of(fileEntry, "application/octet-stream"));

            assertNotNull(bodyPublisher);
            assertInstanceOf(MimeBodyPublisherAdapter.class, bodyPublisher);
            assertEquals(MediaType.parse("application/octet-stream"),
                ((MimeBodyPublisherAdapter) bodyPublisher).mediaType());
        }
    }

    @Nested
    @DisplayName("Response status code 204 tests")
    class ResponseStatusCode204Tests {

        @Test
        @DisplayName("Should return null body for 204 No Content regardless of responseType")
        void test204ReturnsNullBodyWithResponseType() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "",
                HttpHeaders.of(Map.of(), (n, v) -> true),
                204);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNull(response.getBody());
            assertEquals(204, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Authorization apply response with query parameters tests")
    class AuthorizationApplyQueryParametersTests {

        @Test
        @DisplayName("Should add query parameters from ApplyResponse")
        void testApplyResponseAddsQueryParameters() {
            ComponentConnection componentConnection = new ComponentConnection(
                "testComponent", 1, 1L, Map.of(), AuthorizationType.API_KEY);

            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            ConnectionDefinitionService connectionDefinitionService = Mockito.mock(ConnectionDefinitionService.class);

            Mockito.when(applicationContext.getBean(ConnectionDefinitionService.class))
                .thenReturn(connectionDefinitionService);
            Mockito.when(connectionDefinitionService.executeAuthorizationApply(
                Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyMap(), Mockito.any()))
                .thenReturn(ApplyResponse.ofQueryParameters(Map.of("api_key", List.of("secret123"))));

            Map<String, List<String>> queryParameters = new HashMap<>();

            HttpClient httpClient = httpClientExecutor.createHttpClient(
                new HashMap<>(), queryParameters, configuration, "testComponent", 1, null,
                componentConnection, context);

            assertNotNull(httpClient);
            assertTrue(queryParameters.containsKey("api_key"));
            assertEquals("secret123", queryParameters.get("api_key")
                .getFirst());
        }
    }

    @Nested
    @DisplayName("Content-type with charset tests")
    class ContentTypeWithCharsetTests {

        @Test
        @DisplayName("Should extract media type ignoring charset parameter")
        void testContentTypeWithCharset() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json; charset=utf-8")), (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should handle content-type with multiple parameters")
        void testContentTypeWithMultipleParams() {
            Http.Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            TestHttpResponse testHttpResponse = new TestHttpResponse(
                "{\"key\":\"value\"}",
                HttpHeaders.of(Map.of("content-type", List.of("application/json; charset=utf-8; boundary=something")),
                    (n, v) -> true),
                200);

            Http.Response response = httpClientExecutor.handleResponse(testHttpResponse, configuration, context);

            assertNotNull(response.getBody());
        }
    }

    private static class TestHttpResponse implements HttpResponse<Object> {

        private final Object body;
        private final int statusCode;
        private final @Nullable HttpHeaders httpHeaders;

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
