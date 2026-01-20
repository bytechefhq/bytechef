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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.RequestMethod;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * Integration tests for {@link HttpClientExecutor} that test the execute method with actual HTTP calls using WireMock
 * to mock server responses.
 *
 * @author Ivica Cardic
 */
class HttpClientExecutorIntTest {

    private static final int WIREMOCK_PORT = 9998;
    private static final String BASE_URL = "http://localhost:" + WIREMOCK_PORT;

    static {
        ObjectMapper objectMapper = JsonMapper.builder()
            .build();

        ConvertUtils.setObjectMapper(objectMapper);
        JsonUtils.setObjectMapper(objectMapper);
        XmlUtils.setXmlMapper(
            XmlMapper.builder()
                .build());
    }

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .port(WIREMOCK_PORT)
            .http2PlainDisabled(true))
        .configureStaticDsl(true)
        .build();

    private HttpClientExecutor httpClientExecutor;
    private ApplicationContext applicationContext;
    private TempFileStorage tempFileStorage;
    private ActionContext actionContext;
    private ConnectionDefinitionService connectionDefinitionService;

    @BeforeEach
    void setUp() {
        applicationContext = mock(ApplicationContext.class);
        tempFileStorage = mock(TempFileStorage.class);
        actionContext = mock(ActionContext.class);
        connectionDefinitionService = mock(ConnectionDefinitionService.class);

        when(applicationContext.getBean(ActionDefinitionService.class))
            .thenReturn(mock(ActionDefinitionService.class));
        when(applicationContext.getBean(TriggerDefinitionService.class))
            .thenReturn(mock(TriggerDefinitionService.class));
        when(applicationContext.getBean(ConnectionDefinitionService.class))
            .thenReturn(connectionDefinitionService);

        httpClientExecutor = new HttpClientExecutor(applicationContext, tempFileStorage);
    }

    @Nested
    @DisplayName("GET Request Tests")
    class GetRequestTests {

        @Test
        @DisplayName("Should execute GET request and return JSON response")
        void testGetRequestWithJsonResponse() throws Exception {
            String responseBody = "{\"id\": 1, \"name\": \"Test\"}";

            stubFor(get(urlPathEqualTo("/api/test"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody)));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/test",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();

            assertEquals(1, body.get("id"));
            assertEquals("Test", body.get("name"));
        }

        @Test
        @DisplayName("Should execute GET request with query parameters")
        void testGetRequestWithQueryParameters() throws Exception {
            stubFor(get(urlPathEqualTo("/api/search"))
                .withQueryParam("query", equalTo("test"))
                .withQueryParam("page", equalTo("1"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"results\": []}")));

            Map<String, List<String>> queryParameters = Map.of(
                "query", List.of("test"),
                "page", List.of("1"));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/search",
                Collections.emptyMap(),
                queryParameters,
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }

        @Test
        @DisplayName("Should execute GET request with custom headers")
        void testGetRequestWithCustomHeaders() throws Exception {
            stubFor(get(urlPathEqualTo("/api/protected"))
                .withHeader("X-Custom-Header", equalTo("custom-value"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));

            Map<String, List<String>> headers = Map.of(
                "X-Custom-Header", List.of("custom-value"));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/protected",
                headers,
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }

        @Test
        @DisplayName("Should execute GET request and return TEXT response")
        void testGetRequestWithTextResponse() throws Exception {
            String responseBody = "Plain text response";

            stubFor(get(urlPathEqualTo("/api/text"))
                .willReturn(ok()
                    .withHeader("Content-Type", "text/plain")
                    .withBody(responseBody)));

            Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/text",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
            assertEquals(responseBody, response.getBody());
        }

        @Test
        @DisplayName("Should execute GET request and return XML response")
        void testGetRequestWithXmlResponse() throws Exception {
            String responseBody = "<root><name>Test</name></root>";

            stubFor(get(urlPathEqualTo("/api/xml"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/xml")
                    .withBody(responseBody)));

            Configuration configuration = Http.responseType(Http.ResponseType.XML)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/xml",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }

        @Test
        @DisplayName("Should execute GET request and return binary response")
        void testGetRequestWithBinaryResponse() throws Exception {
            byte[] binaryData = new byte[] {
                0x00, 0x01, 0x02, 0x03
            };

            stubFor(get(urlPathEqualTo("/api/binary"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/octet-stream")
                    .withBody(binaryData)));

            FileEntry mockFileEntry = mock(FileEntry.class);

            when(mockFileEntry.getName()).thenReturn("binary.dat");
            when(mockFileEntry.getMimeType()).thenReturn("application/octet-stream");
            when(mockFileEntry.getUrl()).thenReturn("file:///tmp/binary.dat");

            when(tempFileStorage.storeFileContent(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(InputStream.class)))
                    .thenReturn(mockFileEntry);

            Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/binary",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("POST Request Tests")
    class PostRequestTests {

        @Test
        @DisplayName("Should execute POST request with JSON body")
        void testPostRequestWithJsonBody() throws Exception {
            String requestBody = "{\"name\":\"Test\",\"value\":123}";
            String responseBody = "{\"id\": 1, \"created\": true}";

            stubFor(post(urlPathEqualTo("/api/create"))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody)));

            Map<String, Object> bodyContent = new HashMap<>();
            bodyContent.put("name", "Test");
            bodyContent.put("value", 123);

            Body body = Http.Body.of(bodyContent, Http.BodyContentType.JSON);

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/create",
                Collections.emptyMap(),
                Collections.emptyMap(),
                body,
                configuration,
                RequestMethod.POST,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = (Map<String, Object>) response.getBody();

            assertEquals(1, responseMap.get("id"));
            assertEquals(true, responseMap.get("created"));
        }

        @SuppressFBWarnings("HARD_CODE_PASSWORD")
        @Test
        @DisplayName("Should execute POST request with form URL encoded body")
        void testPostRequestWithFormUrlEncodedBody() throws Exception {
            stubFor(post(urlPathEqualTo("/api/form"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"received\": true}")));

            Map<String, Object> formData = new HashMap<>();
            formData.put("username", "testuser");
            formData.put("password", "testpassword");

            Body body = Http.Body.of(formData, Http.BodyContentType.FORM_URL_ENCODED);

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/form",
                Collections.emptyMap(),
                Collections.emptyMap(),
                body,
                configuration,
                RequestMethod.POST,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }

        @Test
        @DisplayName("Should execute POST request with XML body")
        void testPostRequestWithXmlBody() throws Exception {
            stubFor(post(urlPathEqualTo("/api/xml"))
                .withHeader("Content-Type", containing("application/xml"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/xml")
                    .withBody("<response><status>ok</status></response>")));

            Map<String, Object> xmlContent = new HashMap<>();
            xmlContent.put("name", "Test");

            Body body = Http.Body.of(xmlContent, Http.BodyContentType.XML);

            Configuration configuration = Http.responseType(Http.ResponseType.XML)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/xml",
                Collections.emptyMap(),
                Collections.emptyMap(),
                body,
                configuration,
                RequestMethod.POST,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }

        @Test
        @DisplayName("Should execute POST request without body")
        void testPostRequestWithoutBody() throws Exception {
            stubFor(post(urlPathEqualTo("/api/trigger"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"triggered\": true}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/trigger",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.POST,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("PUT Request Tests")
    class PutRequestTests {

        @Test
        @DisplayName("Should execute PUT request with JSON body")
        void testPutRequestWithJsonBody() throws Exception {
            String responseBody = "{\"updated\": true}";

            stubFor(put(urlPathEqualTo("/api/update/1"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody)));

            Map<String, Object> bodyContent = new HashMap<>();
            bodyContent.put("name", "Updated Name");

            Body body = Http.Body.of(bodyContent, Http.BodyContentType.JSON);

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/update/1",
                Collections.emptyMap(),
                Collections.emptyMap(),
                body,
                configuration,
                RequestMethod.PUT,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("PATCH Request Tests")
    class PatchRequestTests {

        @Test
        @DisplayName("Should execute PATCH request with JSON body")
        void testPatchRequestWithJsonBody() throws Exception {
            String responseBody = "{\"patched\": true}";

            stubFor(patch(urlPathEqualTo("/api/patch/1"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody)));

            Map<String, Object> bodyContent = new HashMap<>();
            bodyContent.put("field", "newValue");

            Body body = Http.Body.of(bodyContent, Http.BodyContentType.JSON);

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/patch/1",
                Collections.emptyMap(),
                Collections.emptyMap(),
                body,
                configuration,
                RequestMethod.PATCH,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("DELETE Request Tests")
    class DeleteRequestTests {

        @Test
        @DisplayName("Should execute DELETE request")
        void testDeleteRequest() throws Exception {
            stubFor(delete(urlPathEqualTo("/api/delete/1"))
                .willReturn(ok()));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/delete/1",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.DELETE,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }

        @Test
        @DisplayName("Should execute DELETE request with body")
        void testDeleteRequestWithBody() throws Exception {
            stubFor(delete(urlPathEqualTo("/api/delete"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"deleted\": true}")));

            Map<String, Object> bodyContent = new HashMap<>();
            bodyContent.put("ids", List.of(1, 2, 3));

            Body body = Http.Body.of(bodyContent, Http.BodyContentType.JSON);

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/delete",
                Collections.emptyMap(),
                Collections.emptyMap(),
                body,
                configuration,
                RequestMethod.DELETE,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Error Response Tests")
    class ErrorResponseTests {

        @Test
        @DisplayName("Should handle 400 Bad Request response")
        void testBadRequestResponse() throws Exception {
            stubFor(get(urlPathEqualTo("/api/bad-request"))
                .willReturn(aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Bad Request\"}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/bad-request",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(400, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle 401 Unauthorized response")
        void testUnauthorizedResponse() throws Exception {
            stubFor(get(urlPathEqualTo("/api/unauthorized"))
                .willReturn(aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Unauthorized\"}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/unauthorized",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(401, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle 404 Not Found response")
        void testNotFoundResponse() throws Exception {
            stubFor(get(urlPathEqualTo("/api/not-found"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Not Found\"}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/not-found",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(404, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle 500 Internal Server Error response")
        void testInternalServerErrorResponse() throws Exception {
            stubFor(get(urlPathEqualTo("/api/server-error"))
                .willReturn(aResponse()
                    .withStatus(500)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Internal Server Error\"}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/server-error",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(500, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle 204 No Content response")
        void testNoContentResponse() throws Exception {
            stubFor(delete(urlPathEqualTo("/api/no-content"))
                .willReturn(aResponse()
                    .withStatus(204)));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/no-content",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.DELETE,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(204, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Response Headers Tests")
    class ResponseHeadersTests {

        @Test
        @DisplayName("Should capture response headers")
        void testResponseHeaders() throws Exception {
            stubFor(get(urlPathEqualTo("/api/with-headers"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withHeader("X-Custom-Header", "custom-value")
                    .withHeader("X-Request-Id", "12345")
                    .withBody("{\"status\": \"ok\"}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/with-headers",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());

            Map<String, List<String>> headers = response.getHeaders();

            assertNotNull(headers);
            assertTrue(headers.containsKey("X-Custom-Header") || headers.containsKey("x-custom-header"));
        }
    }

    @Nested
    @DisplayName("Configuration Options Tests")
    class ConfigurationOptionsTests {

        @Test
        @DisplayName("Should follow redirects when configured")
        void testFollowRedirects() throws Exception {
            stubFor(get(urlPathEqualTo("/api/redirect"))
                .willReturn(aResponse()
                    .withStatus(302)
                    .withHeader("Location", BASE_URL + "/api/final")));

            stubFor(get(urlPathEqualTo("/api/final"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"redirected\": true}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .followRedirect(true)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/redirect",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }

        @Test
        @DisplayName("Should not follow redirects when not configured")
        void testNoRedirectFollow() throws Exception {
            stubFor(get(urlPathEqualTo("/api/redirect-no-follow"))
                .willReturn(aResponse()
                    .withStatus(302)
                    .withHeader("Location", BASE_URL + "/api/final")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .followRedirect(false)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/redirect-no-follow",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(302, response.getStatusCode());
        }

        @Test
        @DisplayName("Should disable authorization when configured")
        void testDisableAuthorization() throws Exception {
            stubFor(get(urlPathEqualTo("/api/no-auth"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));

            ComponentConnection connection = new ComponentConnection(
                "testComponent",
                1,
                1L,
                Map.of("token", "secret-token"),
                AuthorizationType.BEARER_TOKEN);

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .disableAuthorization(true)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/no-auth",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                connection,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Multiple Query Parameter Values Tests")
    class MultipleQueryParameterValuesTests {

        @Test
        @DisplayName("Should handle multiple values for same query parameter")
        void testMultipleQueryParameterValues() throws Exception {
            stubFor(get(urlPathEqualTo("/api/multi-query"))
                .withQueryParam("status", equalTo("active"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"results\": []}")));

            Map<String, List<String>> queryParameters = Map.of(
                "status", List.of("active", "pending"));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/multi-query",
                Collections.emptyMap(),
                queryParameters,
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Binary Response Tests")
    class BinaryResponseTests {

        @Test
        @DisplayName("Should handle binary response with content-disposition header")
        void testBinaryResponseWithContentDisposition() throws Exception {
            byte[] pngData = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47
            };

            stubFor(get(urlPathEqualTo("/api/download"))
                .willReturn(ok()
                    .withHeader("Content-Type", "image/png")
                    .withHeader("Content-Disposition", "attachment; filename=\"image.png\"")
                    .withBody(pngData)));

            FileEntry mockFileEntry = mock(FileEntry.class);

            when(mockFileEntry.getName()).thenReturn("image.png");
            when(mockFileEntry.getMimeType()).thenReturn("image/png");
            when(mockFileEntry.getUrl()).thenReturn("file:///tmp/image.png");

            when(tempFileStorage.storeFileContent(
                ArgumentMatchers.eq("image.png"),
                ArgumentMatchers.any(InputStream.class)))
                    .thenReturn(mockFileEntry);

            Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/download",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("HEAD Request Tests")
    class HeadRequestTests {

        @Test
        @DisplayName("Should execute HEAD request")
        void testHeadRequest() throws Exception {
            stubFor(head(urlPathEqualTo("/api/head-test"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Content-Length", "100")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/head-test",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.HEAD,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Content Type Handling Tests")
    class ContentTypeHandlingTests {

        @Test
        @DisplayName("Should handle JSON content type with charset")
        void testJsonContentTypeWithCharset() throws Exception {
            stubFor(get(urlPathEqualTo("/api/json-charset"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody("{\"message\": \"Hello\"}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/json-charset",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle vendor-specific JSON content type")
        void testVendorSpecificJsonContentType() throws Exception {
            stubFor(get(urlPathEqualTo("/api/vendor-json"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/vnd.api+json")
                    .withBody("{\"data\": []}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/vendor-json",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Timeout Tests")
    class TimeoutTests {

        @Test
        @DisplayName("Should use configured timeout")
        void testConfiguredTimeout() throws Exception {
            stubFor(get(urlPathEqualTo("/api/slow"))
                .willReturn(ok()
                    .withFixedDelay(100)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"status\": \"ok\"}")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .timeout(Duration.ofMillis(5000))
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/slow",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Empty Response Tests")
    class EmptyResponseTests {

        @Test
        @DisplayName("Should handle empty JSON response body")
        void testEmptyJsonResponse() throws Exception {
            stubFor(get(urlPathEqualTo("/api/empty"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/empty",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }

        @Test
        @DisplayName("Should handle whitespace-only response body")
        void testWhitespaceOnlyResponse() throws Exception {
            stubFor(get(urlPathEqualTo("/api/whitespace"))
                .willReturn(ok()
                    .withHeader("Content-Type", "text/plain")
                    .withBody("   ")));

            Configuration configuration = Http.responseType(Http.ResponseType.TEXT)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/whitespace",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Array Response Tests")
    class ArrayResponseTests {

        @Test
        @DisplayName("Should handle JSON array response")
        void testJsonArrayResponse() throws Exception {
            stubFor(get(urlPathEqualTo("/api/array"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody("[{\"id\": 1}, {\"id\": 2}, {\"id\": 3}]")));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/array",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());
            assertInstanceOf(List.class, response.getBody());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> bodyList = (List<Map<String, Object>>) response.getBody();

            assertEquals(3, bodyList.size());
        }
    }

    @Nested
    @DisplayName("Unicode Content Tests")
    class UnicodeContentTests {

        @Test
        @DisplayName("Should handle Unicode characters in response")
        void testUnicodeResponse() throws Exception {
            String unicodeBody = "{\"message\": \"Hello, 世界! 🌍\"}";

            stubFor(get(urlPathEqualTo("/api/unicode"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json; charset=utf-8")
                    .withBody(unicodeBody.getBytes(StandardCharsets.UTF_8))));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/unicode",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();

            assertTrue(body.get("message")
                .toString()
                .contains("世界"));
        }
    }

    @Nested
    @DisplayName("Large Response Tests")
    class LargeResponseTests {

        @Test
        @DisplayName("Should handle large JSON response")
        void testLargeJsonResponse() throws Exception {
            StringBuilder largeJson = new StringBuilder("{\"items\": [");

            for (int i = 0; i < 1000; i++) {
                if (i > 0) {
                    largeJson.append(",");
                }

                largeJson.append("{\"id\": ")
                    .append(i)
                    .append(", \"name\": \"Item ")
                    .append(i)
                    .append("\"}");
            }

            largeJson.append("]}");

            stubFor(get(urlPathEqualTo("/api/large"))
                .willReturn(ok()
                    .withHeader("Content-Type", "application/json")
                    .withBody(largeJson.toString())));

            Configuration configuration = Http.responseType(Http.ResponseType.JSON)
                .build();

            Response response = httpClientExecutor.execute(
                BASE_URL + "/api/large",
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                configuration,
                RequestMethod.GET,
                "testComponent",
                1,
                null,
                null,
                actionContext);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode());

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();

            @SuppressWarnings("unchecked")
            List<Object> items = (List<Object>) body.get("items");

            assertEquals(1000, items.size());
        }
    }
}
