/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.exception.HttpResponseTooLargeException;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppConnection;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.file.storage.TempFileStorage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppProxyExecutorTest {

    private static final String BASE_URI = "https://api.example.com/v2";
    private static final String COMPONENT_NAME = "crm";

    private final AtomicInteger spoolCounter = new AtomicInteger();

    @Mock
    private Connection connection;

    @Mock
    private ConnectionDefinitionService connectionDefinitionService;

    @Mock
    private Context context;

    @Mock
    private ContextFactory contextFactory;

    @Mock
    private Context.File file;

    @Mock
    private Http http;

    @Mock
    private TempFileStorage tempFileStorage;

    private final ApplicationProperties applicationProperties = new ApplicationProperties();

    private AppConnection binding;
    private Http.Executor executor;
    private AppProxyExecutor appProxyExecutor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() throws Exception {
        ApplicationProperties.App.Proxy proxy = applicationProperties.getApp()
            .getProxy();

        proxy.setMaxRequestBytes(1024);
        proxy.setMaxResponseBytes(1024);
        proxy.setTimeout(Duration.ofSeconds(30));

        binding = new AppConnection();

        binding.setAppId(1L);
        binding.setBindingKey("crm");
        binding.setProxyEnabled(true);
        binding.setProxyAllowedMethodList(List.of("GET", "POST"));

        when(connection.getAuthorizationType()).thenReturn(AuthorizationType.BEARER_TOKEN);
        when(connection.getComponentName()).thenReturn(COMPONENT_NAME);
        when(connection.getConnectionVersion()).thenReturn(1);
        when(connection.getId()).thenReturn(7L);
        doReturn(Map.of("token", "secret")).when(connection)
            .getParameters();

        when(contextFactory.createContext(eq(COMPONENT_NAME), any(ComponentConnection.class))).thenReturn(context);

        when(connectionDefinitionService.executeBaseUri(
            eq(COMPONENT_NAME), any(ComponentConnection.class), eq(context)))
                .thenReturn(Optional.of(BASE_URI));

        when(connectionDefinitionService.executeAuthorizationApply(
            eq(COMPONENT_NAME), anyInt(), any(AuthorizationType.class), anyMap(), eq(context)))
                .thenAnswer(invocation -> ApplyResponse.ofHeaders(
                    Map.of(
                        "Authorization",
                        List.of("Bearer " + ((Map<String, ?>) invocation.getArgument(3)).get("token")))));

        when(connectionDefinitionService.getAuthorizationRefreshOn(eq(COMPONENT_NAME), anyInt(), any()))
            .thenReturn(List.of(401));

        when(context.http(any())).thenAnswer(
            invocation -> ((ContextFunction<Http, ?>) invocation.getArgument(0)).apply(http));
        when(context.file(any())).thenAnswer(
            invocation -> ((ContextFunction<Context.File, ?>) invocation.getArgument(0)).apply(file));

        executor = mock(Http.Executor.class, RETURNS_SELF);

        when(http.exchange(anyString(), any(Http.RequestMethod.class))).thenReturn(executor);

        appProxyExecutor = new AppProxyExecutor(
            applicationProperties, connectionDefinitionService, contextFactory,
            tempFileStorage);
    }

    @Test
    void testRejectsMethodNotAllowedOnBinding() {
        binding.setProxyAllowedMethodList(List.of("GET"));

        assertThatThrownBy(() -> appProxyExecutor.execute(binding, connection, request("POST", "/items")))
            .isInstanceOfSatisfying(ConfigurationException.class, exception -> {
                assertThat(exception.getErrorKey()).isEqualTo(AppErrorType.APP_OPERATION_NOT_ALLOWED.getErrorKey());
                assertThat(exception.getMessage())
                    .isEqualTo("POST is not allowed on binding 'crm'; allowed proxy methods: GET");
            });

        verifyNoInteractions(contextFactory);
    }

    @Test
    void testRejectsEveryMethodWhenProxyIsDisabled() {
        binding.setProxyEnabled(false);

        assertThatThrownBy(() -> appProxyExecutor.execute(binding, connection, request("get", "/items")))
            .isInstanceOfSatisfying(ConfigurationException.class, exception -> {
                assertThat(exception.getErrorKey()).isEqualTo(AppErrorType.APP_OPERATION_NOT_ALLOWED.getErrorKey());
                assertThat(exception.getMessage())
                    .isEqualTo("GET is not allowed on binding 'crm'; allowed proxy methods: none");
            });

        verifyNoInteractions(contextFactory);
    }

    @Test
    void testRejectsMalformedMethodWithoutEchoingIt() {
        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request("GET\r\nX-Evil: 1", "/items")),
            AppErrorType.APP_PROXY_REQUEST_INVALID);

        assertThatThrownBy(() -> appProxyExecutor.execute(binding, connection, request("GET\r\nX-Evil: 1", "/items")))
            .hasMessageNotContaining("Evil");

        verifyNoInteractions(contextFactory);
    }

    @Test
    void testRefusesConnectionWithoutAuthorizationType() {
        when(connection.getAuthorizationType()).thenReturn(null);

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request("GET", "/items")),
            AppErrorType.APP_PROXY_UNSUPPORTED);

        verifyNoInteractions(contextFactory);
    }

    @Test
    void testRefusesConnectionWithoutBaseUri() {
        when(connectionDefinitionService.executeBaseUri(
            eq(COMPONENT_NAME), any(ComponentConnection.class), eq(context)))
                .thenReturn(Optional.empty());

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request("GET", "/items")),
            AppErrorType.APP_PROXY_UNSUPPORTED);

        verifyNoInteractions(http);
    }

    @Test
    void testRefusesBaseUriThatCannotBePinned() {
        when(connectionDefinitionService.executeBaseUri(
            eq(COMPONENT_NAME), any(ComponentConnection.class), eq(context)))
                .thenReturn(Optional.of("https://user@api.example.com/v2"));

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request("GET", "/items")),
            AppErrorType.APP_PROXY_PATH_REJECTED);

        verifyNoInteractions(http);
    }

    @Test
    void testRejectsPathEscapeBeforeAnyCall() {
        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request("GET", "/a/../../admin")),
            AppErrorType.APP_PROXY_PATH_REJECTED);

        verifyNoInteractions(contextFactory, http);
    }

    @Test
    void testRejectsQueryValueWithLineBreak() {
        ProxyRequest request = new ProxyRequest(
            "GET", "/items", Map.of("x", List.of("a\r\nb")), Map.of(), null, BodyEncoding.UTF8, null);

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request), AppErrorType.APP_PROXY_PATH_REJECTED);

        verifyNoInteractions(contextFactory, http);
    }

    @Test
    void testRejectsQueryNameWithLineBreak() {
        ProxyRequest request = new ProxyRequest(
            "GET", "/items", Map.of("x\ny", List.of("a")), Map.of(), null, BodyEncoding.UTF8, null);

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request), AppErrorType.APP_PROXY_PATH_REJECTED);

        verifyNoInteractions(contextFactory, http);
    }

    @Test
    void testRefusesOversizedRequestBody() {
        applicationProperties.getApp()
            .getProxy()
            .setMaxRequestBytes(4);

        ProxyRequest request = new ProxyRequest(
            "POST", "/items", Map.of(), Map.of(), "12345", BodyEncoding.UTF8, "text/plain");

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request),
            AppErrorType.APP_PROXY_REQUEST_TOO_LARGE);

        verifyNoInteractions(contextFactory, http);
    }

    @Test
    void testMeasuresDecodedBase64BodySize() throws Exception {
        applicationProperties.getApp()
            .getProxy()
            .setMaxRequestBytes(4);

        String fourBytes = Base64.getEncoder()
            .encodeToString(new byte[] {
                1, 2, 3, 4
            });
        String fiveBytes = Base64.getEncoder()
            .encodeToString(new byte[] {
                1, 2, 3, 4, 5
            });

        assertAppError(
            () -> appProxyExecutor.execute(
                binding, connection,
                new ProxyRequest("POST", "/items", Map.of(), Map.of(), fiveBytes, BodyEncoding.BASE64, null)),
            AppErrorType.APP_PROXY_REQUEST_TOO_LARGE);

        FileEntry requestSpool = spoolFileEntry();

        when(file.storeContent(anyString(), any(InputStream.class))).thenReturn(requestSpool);

        stubExecute(response(200, Map.of(), null));

        ProxyResponse proxyResponse = appProxyExecutor.execute(
            binding, connection,
            new ProxyRequest("POST", "/items", Map.of(), Map.of(), fourBytes, BodyEncoding.BASE64, null));

        assertThat(proxyResponse.status()).isEqualTo(200);
    }

    @Test
    void testRejectsInvalidBase64Body() {
        ProxyRequest request = new ProxyRequest(
            "POST", "/items", Map.of(), Map.of(), "not base64!", BodyEncoding.BASE64, null);

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request), AppErrorType.APP_PROXY_REQUEST_INVALID);

        verifyNoInteractions(contextFactory, http);
    }

    @Test
    void testRejectsMalformedContentTypeBeforeAnyWork() throws Exception {
        for (String contentType : List.of(
            "application/json\r\nX-Evil: 1", "json", "text/plain; charset", "application/json; a=\"b", "/json",
            "text/ plain")) {

            ProxyRequest request = new ProxyRequest(
                "POST", "/items", Map.of(), Map.of(), "e30=", BodyEncoding.BASE64, contentType);

            assertThatThrownBy(() -> appProxyExecutor.execute(binding, connection, request))
                .as(contentType)
                .isInstanceOfSatisfying(ConfigurationException.class, exception -> {
                    assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_PROXY_REQUEST_INVALID.getErrorKey());
                    assertThat(exception.getMessage()).doesNotContain("Evil");
                });
        }

        ProxyRequest malformedHeader = new ProxyRequest(
            "POST", "/items", Map.of(), Map.of("Content-Type", List.of("not a media type")), "{}", BodyEncoding.UTF8,
            null);

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, malformedHeader),
            AppErrorType.APP_PROXY_REQUEST_INVALID);

        verifyNoInteractions(contextFactory, http, file);
    }

    @Test
    void testRejectsMalformedHeadersBeforeAnyWork() {
        for (String name : List.of("X Bad", "X:Y", "", "X-\u00e9", "X\tTab", "X\r\nEvil", "X(1)")) {
            ProxyRequest request = new ProxyRequest(
                "POST", "/items", Map.of(), Map.of(name, List.of("1")), "e30=", BodyEncoding.BASE64, null);

            assertThatThrownBy(() -> appProxyExecutor.execute(binding, connection, request))
                .as("header name '%s'", name)
                .isInstanceOfSatisfying(ConfigurationException.class, exception -> {
                    assertThat(exception.getErrorKey())
                        .isEqualTo(AppErrorType.APP_PROXY_REQUEST_INVALID.getErrorKey());
                    assertThat(exception.getMessage()).doesNotContain("Evil")
                        .doesNotContain("Bad");
                });
        }

        for (String value : List.of("a\u0001b", "a\u007fb", "a\nb", "a\u0000b")) {
            ProxyRequest request = new ProxyRequest(
                "GET", "/items", Map.of(), Map.of("X-Custom", List.of("fine", value)), null, BodyEncoding.UTF8, null);

            assertThatThrownBy(() -> appProxyExecutor.execute(binding, connection, request))
                .as("header value")
                .isInstanceOfSatisfying(ConfigurationException.class, exception -> assertThat(
                    exception.getErrorKey()).isEqualTo(AppErrorType.APP_PROXY_REQUEST_INVALID.getErrorKey()));
        }

        verifyNoInteractions(contextFactory, http, file);
    }

    @Test
    void testAcceptsEveryTokenCharacterInAHeaderNameAndATabInAValue() throws Exception {
        stubExecute(response(200, Map.of(), null));

        ProxyRequest request = new ProxyRequest(
            "GET", "/items", Map.of(), Map.of("X-A1!#$%&'*+.^_`|~", List.of("a\tb c", "\u00e9")), null,
            BodyEncoding.UTF8, null);

        assertThat(appProxyExecutor.execute(binding, connection, request)
            .status()).isEqualTo(200);
    }

    @Test
    void testFailureReadingTheResponseSpoolIsALocalStorageFailureAndTheSpoolIsDeleted() throws Exception {
        FileEntry responseSpool = spoolFileEntry();

        when(file.getContentLength(responseSpool)).thenReturn(3L);
        when(file.getInputStream(responseSpool)).thenReturn(new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("disk gone: secret-path");
            }
        });

        stubExecute(response(200, Map.of("content-type", List.of("text/plain")), responseSpool));

        assertThatThrownBy(() -> appProxyExecutor.execute(binding, connection, request("GET", "/items")))
            .isInstanceOf(ProxyStorageException.class)
            .hasMessage("Unable to read the proxied response body")
            .hasRootCauseInstanceOf(IOException.class);

        verifyDeleted(responseSpool);
    }

    @Test
    void testFailureOpeningTheResponseSpoolIsALocalStorageFailure() throws Exception {
        FileEntry responseSpool = spoolFileEntry();

        when(file.getContentLength(responseSpool)).thenReturn(3L);
        when(file.getInputStream(responseSpool)).thenThrow(new RuntimeException("storage unavailable"));

        stubExecute(response(200, Map.of("content-type", List.of("text/plain")), responseSpool));

        assertThatThrownBy(() -> appProxyExecutor.execute(binding, connection, request("GET", "/items")))
            .isInstanceOf(ProxyStorageException.class);

        verifyDeleted(responseSpool);
    }

    @Test
    void testAcceptsContentTypeWithParameters() throws Exception {
        stubExecute(response(200, Map.of(), null));

        ProxyRequest request = new ProxyRequest(
            "POST", "/items", Map.of(), Map.of(), "a", BodyEncoding.UTF8,
            "multipart/form-data; boundary=\"x y\"; charset=utf-8");

        appProxyExecutor.execute(binding, connection, request);

        verify(executor).body(Http.Body.of("a", "multipart/form-data; boundary=\"x y\"; charset=utf-8"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSendsSanitizedHeadersWithAppliedCredentialsToPinnedUrl() throws Exception {
        Map<String, List<String>> requestHeaders = new LinkedHashMap<>();

        requestHeaders.put("Authorization", List.of("evil"));
        requestHeaders.put("Accept", List.of("application/json"));

        ProxyRequest request = new ProxyRequest(
            "get", "/items", Map.of("x", List.of("1")), requestHeaders, null, BodyEncoding.UTF8, null);

        Map<String, List<String>> responseHeaders = new LinkedHashMap<>();

        responseHeaders.put("content-type", List.of("application/json"));
        responseHeaders.put("set-cookie", List.of("s"));

        FileEntry responseSpool = storedBody("{}");

        stubExecute(response(200, responseHeaders, responseSpool));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request);

        ArgumentCaptor<ComponentConnection> componentConnectionCaptor = ArgumentCaptor.forClass(
            ComponentConnection.class);

        verify(contextFactory).createContext(eq(COMPONENT_NAME), componentConnectionCaptor.capture());

        assertThat(componentConnectionCaptor.getValue()).isEqualTo(
            new ComponentConnection(
                COMPONENT_NAME, 1, 7L, Map.of("token", "secret"), AuthorizationType.BEARER_TOKEN));

        verify(http).exchange(
            ProxyPathValidator.pin(BASE_URI, "/items")
                .toString(),
            Http.RequestMethod.GET);
        verify(http).exchange("https://api.example.com/v2/items", Http.RequestMethod.GET);

        ArgumentCaptor<Map<String, List<String>>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, List<String>>> queryCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationCaptor = ArgumentCaptor.forClass(
            Http.Configuration.ConfigurationBuilder.class);

        verify(executor).headers(headersCaptor.capture());
        verify(executor).queryParameters(queryCaptor.capture());
        verify(executor).configuration(configurationCaptor.capture());
        verify(executor, never()).body(any());

        assertThat(headersCaptor.getValue()).containsOnly(
            Map.entry("Accept", List.of("application/json")), Map.entry("Authorization", List.of("Bearer secret")));
        assertThat(queryCaptor.getValue()).containsExactly(Map.entry("x", List.of("1")));

        Http.Configuration configuration = configurationCaptor.getValue()
            .build();

        assertThat(configuration.getResponseType()).isEqualTo(Http.ResponseType.BINARY);
        assertThat(configuration.getRequestTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(configuration.isFollowRedirect()).isFalse();
        assertThat(configuration.isFollowAllRedirects()).isFalse();
        assertThat(configuration.isDisableAuthorization()).isTrue();

        assertThat(proxyResponse.status()).isEqualTo(200);
        assertThat(proxyResponse.headers()).containsExactly(Map.entry("content-type", List.of("application/json")));
        assertThat(proxyResponse.body()).isEqualTo("{}");
        assertThat(proxyResponse.bodyEncoding()).isEqualTo(BodyEncoding.UTF8);

        verifyDeleted(responseSpool);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAppliedCredentialsReplaceCollidingAppHeadersAndQueryParameters() throws Exception {
        ApplyResponse applyResponse = mock(ApplyResponse.class);

        when(applyResponse.getHeaders()).thenReturn(Map.of("X-Api-Key", List.of("connection-key")));
        when(applyResponse.getQueryParameters()).thenReturn(Map.of("api_key", List.of("connection-key")));
        when(connectionDefinitionService.executeAuthorizationApply(
            eq(COMPONENT_NAME), anyInt(), any(AuthorizationType.class), anyMap(), eq(context)))
                .thenReturn(applyResponse);

        Map<String, List<String>> requestHeaders = new LinkedHashMap<>();

        requestHeaders.put("x-api-key", List.of("app-key"));
        requestHeaders.put("X-API-KEY", List.of("other-app-key"));
        requestHeaders.put("Accept", List.of("*/*"));

        Map<String, List<String>> query = new LinkedHashMap<>();

        query.put("API_KEY", List.of("app-key"));
        query.put("page", List.of("2"));

        stubExecute(response(200, Map.of(), null));

        appProxyExecutor.execute(
            binding, connection,
            new ProxyRequest("GET", "/items", query, requestHeaders, null, BodyEncoding.UTF8, null));

        ArgumentCaptor<Map<String, List<String>>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, List<String>>> queryCaptor = ArgumentCaptor.forClass(Map.class);

        verify(executor).headers(headersCaptor.capture());
        verify(executor).queryParameters(queryCaptor.capture());

        assertThat(headersCaptor.getValue()).containsOnly(
            Map.entry("Accept", List.of("*/*")), Map.entry("X-Api-Key", List.of("connection-key")));
        assertThat(queryCaptor.getValue()).containsOnly(
            Map.entry("page", List.of("2")), Map.entry("api_key", List.of("connection-key")));
    }

    @Test
    void testReturnsUpstreamErrorBodyAndContentType() throws Exception {
        FileEntry responseSpool = storedBody("{\"error\":\"invalid\"}");

        stubExecute(response(422, Map.of("content-type", List.of("application/json")), responseSpool));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.status()).isEqualTo(422);
        assertThat(proxyResponse.headers()).containsEntry("content-type", List.of("application/json"));
        assertThat(proxyResponse.body()).isEqualTo("{\"error\":\"invalid\"}");
        assertThat(proxyResponse.bodyEncoding()).isEqualTo(BodyEncoding.UTF8);

        verifyDeleted(responseSpool);
    }

    @Test
    void testReturnsUpstreamNotFoundBody() throws Exception {
        stubExecute(response(404, Map.of("content-type", List.of("text/plain")), storedBody("no such item")));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.status()).isEqualTo(404);
        assertThat(proxyResponse.body()).isEqualTo("no such item");
    }

    @Test
    void testReturnsUpstreamRetryAfter() throws Exception {
        stubExecute(response(429, Map.of("retry-after", List.of("5")), storedBody("")));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.status()).isEqualTo(429);
        assertThat(proxyResponse.headers()).containsExactly(Map.entry("retry-after", List.of("5")));
        assertThat(proxyResponse.body()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testRefreshesRefreshableCredentialsOnceAndResends() throws Exception {
        when(connection.getAuthorizationType()).thenReturn(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        ComponentConnection refreshedConnection = new ComponentConnection(
            COMPONENT_NAME, 1, 7L, Map.of("token", "refreshed"), AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        when(connectionDefinitionService.executeConnectionRefresh(any(ComponentConnection.class)))
            .thenReturn(refreshedConnection);

        FileEntry unauthorizedSpool = storedBody("{\"error\":\"expired\"}");
        FileEntry successSpool = storedBody("{\"ok\":true}");

        stubExecute(
            response(401, Map.of("content-type", List.of("application/json")), unauthorizedSpool),
            response(200, Map.of("content-type", List.of("application/json")), successSpool));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.status()).isEqualTo(200);
        assertThat(proxyResponse.body()).isEqualTo("{\"ok\":true}");

        verify(connectionDefinitionService).executeConnectionRefresh(
            new ComponentConnection(
                COMPONENT_NAME, 1, 7L, Map.of("token", "secret"), AuthorizationType.OAUTH2_AUTHORIZATION_CODE));
        verify(contextFactory).createContext(COMPONENT_NAME, refreshedConnection);
        verify(http, times(2)).exchange("https://api.example.com/v2/items", Http.RequestMethod.GET);

        ArgumentCaptor<Map<String, List<String>>> headersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(executor, times(2)).headers(headersCaptor.capture());

        assertThat(headersCaptor.getAllValues()).extracting(headers -> headers.get("Authorization"))
            .containsExactly(List.of("Bearer secret"), List.of("Bearer refreshed"));

        verifyDeleted(unauthorizedSpool);
        verifyDeleted(successSpool);
    }

    @Test
    void testDoesNotRefreshNonRefreshableCredentials() throws Exception {
        stubExecute(response(401, Map.of(), storedBody("")));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.status()).isEqualTo(401);

        verify(connectionDefinitionService, never()).executeConnectionRefresh(any());
        verify(http, times(1)).exchange(anyString(), any(Http.RequestMethod.class));
    }

    @Test
    void testDoesNotRefreshOnStatusOutsideRefreshOn() throws Exception {
        when(connection.getAuthorizationType()).thenReturn(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);

        stubExecute(response(403, Map.of(), storedBody("")));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.status()).isEqualTo(403);

        verify(connectionDefinitionService, never()).executeConnectionRefresh(any());
    }

    @Test
    void testRefreshesAtMostOnce() throws Exception {
        when(connection.getAuthorizationType()).thenReturn(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        when(connectionDefinitionService.executeConnectionRefresh(any(ComponentConnection.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        stubExecute(response(401, Map.of(), storedBody("")), response(401, Map.of(), storedBody("still expired")));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.status()).isEqualTo(401);
        assertThat(Base64.getDecoder()
            .decode(proxyResponse.body())).isEqualTo("still expired".getBytes(StandardCharsets.UTF_8));

        verify(connectionDefinitionService, times(1)).executeConnectionRefresh(any());
        verify(http, times(2)).exchange(anyString(), any(Http.RequestMethod.class));
    }

    @Test
    void testFailedRefreshIsReportedAsUnavailableConnectionWithAFixedMessage() throws Exception {
        when(connection.getAuthorizationType()).thenReturn(AuthorizationType.OAUTH2_AUTHORIZATION_CODE);
        when(connectionDefinitionService.executeConnectionRefresh(any(ComponentConnection.class)))
            .thenThrow(new IllegalStateException("token endpoint said: secret-refresh-token is revoked"));

        FileEntry unauthorizedSpool = storedBody("{\"error\":\"expired\"}");

        stubExecute(response(401, Map.of("content-type", List.of("application/json")), unauthorizedSpool));

        assertThatThrownBy(() -> appProxyExecutor.execute(binding, connection, request("GET", "/items")))
            .isInstanceOf(ConfigurationException.class)
            .hasMessage("Connection of binding 'crm' is unavailable")
            .extracting(exception -> ((ConfigurationException) exception).getErrorKey())
            .isEqualTo(AppErrorType.APP_CONNECTION_UNAVAILABLE.getErrorKey());

        verify(http, times(1)).exchange(anyString(), any(Http.RequestMethod.class));

        verifyDeleted(unauthorizedSpool);
    }

    @Test
    void testPropagatesUnreachableUpstreamAndDeletesRequestSpool() throws Exception {
        FileEntry requestSpool = spoolFileEntry();

        when(file.storeContent(anyString(), any(InputStream.class))).thenReturn(requestSpool);

        RuntimeException unreachable = new RuntimeException("Unable to execute HTTP request");

        when(executor.execute()).thenThrow(unreachable);

        assertThatThrownBy(() -> appProxyExecutor.execute(
            binding, connection,
            new ProxyRequest("POST", "/items", Map.of(), Map.of(), "AAEC", BodyEncoding.BASE64, null)))
                .isSameAs(unreachable);

        verifyDeleted(requestSpool);
    }

    @Test
    void testDeletesBothSpoolsOnUpstreamError() throws Exception {
        FileEntry requestSpool = spoolFileEntry();
        FileEntry responseSpool = storedBody("boom");

        when(file.storeContent(anyString(), any(InputStream.class))).thenReturn(requestSpool);

        stubExecute(response(500, Map.of("content-type", List.of("text/plain")), responseSpool));

        ProxyResponse proxyResponse = appProxyExecutor.execute(
            binding, connection,
            new ProxyRequest("POST", "/items", Map.of(), Map.of(), "AAEC", BodyEncoding.BASE64, null));

        assertThat(proxyResponse.status()).isEqualTo(500);
        assertThat(proxyResponse.body()).isEqualTo("boom");

        verifyDeleted(requestSpool);
        verifyDeleted(responseSpool);
    }

    @Test
    void testDeleteFailureDoesNotMaskResponse() throws Exception {
        doThrow(new IllegalStateException("storage down")).when(tempFileStorage)
            .deleteFile(any());

        stubExecute(response(200, Map.of("content-type", List.of("text/plain")), storedBody("ok")));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.body()).isEqualTo("ok");
    }

    @Test
    void testReturnsRedirectWithoutFollowingIt() throws Exception {
        stubExecute(response(302, Map.of("location", List.of("https://evil.example.com/steal")), storedBody("")));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.status()).isEqualTo(302);
        assertThat(proxyResponse.headers())
            .containsExactly(Map.entry("location", List.of("https://evil.example.com/steal")));
        assertThat(proxyResponse.body()).isNull();

        verify(http).exchange(anyString(), any(Http.RequestMethod.class));
    }

    @Test
    void testReturnsNullBodyForNoContentAndDeletesItsSpool() throws Exception {
        FileEntry emptySpool = storedBody("");

        stubExecute(response(204, Map.of(), emptySpool));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.status()).isEqualTo(204);
        assertThat(proxyResponse.body()).isNull();

        verify(file, never()).getInputStream(any());

        verifyDeleted(emptySpool);
    }

    @Test
    void testBase64EncodesNonTextBodies() throws Exception {
        byte[] bytes = {
            (byte) 0x89, 'P', 'N', 'G', 0, (byte) 0xff
        };

        stubExecute(response(200, Map.of("content-type", List.of("application/octet-stream")), storedBody(bytes)));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.bodyEncoding()).isEqualTo(BodyEncoding.BASE64);
        assertThat(Base64.getDecoder()
            .decode(proxyResponse.body())).isEqualTo(bytes);
    }

    @Test
    void testTreatsStructuredSuffixAndCharsetParameterAsText() throws Exception {
        stubExecute(
            response(
                200, Map.of("Content-Type", List.of("application/problem+json; charset=utf-8")),
                storedBody("{\"title\":\"ünïcode\"}")));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.bodyEncoding()).isEqualTo(BodyEncoding.UTF8);
        assertThat(proxyResponse.body()).isEqualTo("{\"title\":\"ünïcode\"}");
    }

    @Test
    void testFallsBackToBase64ForTextThatIsNotUtf8() throws Exception {
        byte[] latin1 = "café".getBytes(StandardCharsets.ISO_8859_1);

        stubExecute(
            response(200, Map.of("content-type", List.of("text/plain; charset=iso-8859-1")), storedBody(latin1)));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        assertThat(proxyResponse.bodyEncoding()).isEqualTo(BodyEncoding.BASE64);
        assertThat(Base64.getDecoder()
            .decode(proxyResponse.body())).isEqualTo(latin1);
    }

    @Test
    void testRefusesOversizedResponseAndDeletesItsSpool() throws Exception {
        applicationProperties.getApp()
            .getProxy()
            .setMaxResponseBytes(4);

        FileEntry responseSpool = storedBody("12345");

        stubExecute(response(200, Map.of("content-type", List.of("text/plain")), responseSpool));

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request("GET", "/items")),
            AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE);

        verifyDeleted(responseSpool);
    }

    @Test
    void testRefusesOversizedResponseWhoseStoredLengthIsUnderReported() throws Exception {
        applicationProperties.getApp()
            .getProxy()
            .setMaxResponseBytes(4);

        FileEntry responseSpool = storedBody("12345");

        when(file.getContentLength(responseSpool)).thenReturn(1L);

        stubExecute(response(200, Map.of("content-type", List.of("text/plain")), responseSpool));

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request("GET", "/items")),
            AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE);

        verifyDeleted(responseSpool);
    }

    @Test
    void testSendsTheResponseCapToTheHttpClient() throws Exception {
        FileEntry responseSpool = storedBody("{}");

        stubExecute(response(200, Map.of("content-type", List.of("application/json")), responseSpool));

        appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationCaptor = ArgumentCaptor.forClass(
            Http.Configuration.ConfigurationBuilder.class);

        verify(executor).configuration(configurationCaptor.capture());

        Http.Configuration configuration = configurationCaptor.getValue()
            .build();

        assertThat(configuration.getMaxResponseBytes()).isEqualTo(1024L);
    }

    @Test
    void testAbortedOversizedResponseIsRefusedAsTooLarge() {
        when(executor.execute()).thenThrow(
            new RuntimeException("Unable to execute HTTP request", new HttpResponseTooLargeException(1024)));

        assertAppError(
            () -> appProxyExecutor.execute(binding, connection, request("GET", "/items")),
            AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE);
    }

    @Test
    void testAbortedOversizedResponseStillDeletesTheRequestSpool() throws Exception {
        FileEntry requestSpool = spoolFileEntry();

        when(file.storeContent(anyString(), any(InputStream.class))).thenReturn(requestSpool);
        when(executor.execute()).thenThrow(
            new RuntimeException("Unable to execute HTTP request", new HttpResponseTooLargeException(1024)));

        assertAppError(
            () -> appProxyExecutor.execute(
                binding, connection,
                new ProxyRequest("POST", "/items", Map.of(), Map.of(), "AAEC", BodyEncoding.BASE64, null)),
            AppErrorType.APP_PROXY_RESPONSE_TOO_LARGE);

        verifyDeleted(requestSpool);
    }

    @Test
    void testSendsUtf8BodyWithItsContentType() throws Exception {
        Map<String, List<String>> requestHeaders = new LinkedHashMap<>();

        requestHeaders.put("Content-Type", List.of("text/html"));
        requestHeaders.put("Accept", List.of("*/*"));

        ProxyRequest request = new ProxyRequest(
            "POST", "/items", Map.of(), requestHeaders, "{\"a\":1}", BodyEncoding.UTF8, "application/json");

        stubExecute(response(201, Map.of(), null));

        ProxyResponse proxyResponse = appProxyExecutor.execute(binding, connection, request);

        verify(executor).body(Http.Body.of("{\"a\":1}", "application/json"));
        verify(executor).headers(Map.of("Accept", List.of("*/*"), "Authorization", List.of("Bearer secret")));

        assertThat(proxyResponse.status()).isEqualTo(201);
        assertThat(proxyResponse.body()).isNull();

        verify(file, never()).storeContent(anyString(), any(InputStream.class));
    }

    @Test
    void testSendsBase64BodyAsBinaryFileAndDeletesIt() throws Exception {
        byte[] bytes = {
            0, 1, 2, (byte) 0xfe
        };

        FileEntry requestSpool = spoolFileEntry();
        ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);

        when(file.storeContent(anyString(), inputStreamCaptor.capture())).thenReturn(requestSpool);

        stubExecute(response(200, Map.of(), null));

        appProxyExecutor.execute(
            binding, connection,
            new ProxyRequest(
                "POST", "/items", Map.of(), Map.of(), Base64.getEncoder()
                    .encodeToString(bytes),
                BodyEncoding.BASE64, "image/png"));

        assertThat(inputStreamCaptor.getValue()
            .readAllBytes()).isEqualTo(bytes);

        verify(executor).body(Http.Body.of(requestSpool, "image/png"));

        verifyDeleted(requestSpool);
    }

    @Test
    void testNamesEveryRequestSpoolUniquely() throws Exception {
        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

        when(file.storeContent(nameCaptor.capture(), any(InputStream.class))).thenAnswer(
            invocation -> spoolFileEntry());

        when(executor.execute()).thenAnswer(invocation -> response(200, Map.of(), null));

        ProxyRequest request = new ProxyRequest(
            "POST", "/items", Map.of(), Map.of(), "AAEC", BodyEncoding.BASE64, null);

        appProxyExecutor.execute(binding, connection, request);
        appProxyExecutor.execute(binding, connection, request);

        List<String> names = nameCaptor.getAllValues();

        assertThat(names).hasSize(2)
            .doesNotHaveDuplicates()
            .allSatisfy(name -> assertThat(name).startsWith("proxy-request-")
                .isNotEqualTo("proxy-request-body"));
    }

    @Test
    void testNamesEveryResponseSpoolUniquelyRegardlessOfContentDisposition() throws Exception {
        when(executor.execute()).thenAnswer(
            invocation -> response(
                200, Map.of("content-disposition", List.of("attachment; filename=\"report.pdf\"")), null));

        appProxyExecutor.execute(binding, connection, request("GET", "/items"));
        appProxyExecutor.execute(binding, connection, request("GET", "/items"));

        ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationCaptor = ArgumentCaptor.forClass(
            Http.Configuration.ConfigurationBuilder.class);

        verify(executor, times(2)).configuration(configurationCaptor.capture());

        List<String> filenames = configurationCaptor.getAllValues()
            .stream()
            .map(configurationBuilder -> configurationBuilder.build()
                .getFilename())
            .toList();

        assertThat(filenames).hasSize(2)
            .doesNotHaveDuplicates()
            .allSatisfy(filename -> assertThat(filename).startsWith("proxy-response-")
                .isNotEqualTo("report.pdf"));
    }

    private void assertAppError(ThrowingCallable callable, AppErrorType appErrorType) {
        assertThatThrownBy(callable)
            .isInstanceOfSatisfying(ConfigurationException.class, exception -> {
                assertThat(exception.getEntityClass()).isEqualTo(App.class);
                assertThat(exception.getErrorKey()).isEqualTo(appErrorType.getErrorKey());
            });
    }

    private static ProxyRequest request(String method, String path) {
        return new ProxyRequest(method, path, Map.of(), Map.of(), null, BodyEncoding.UTF8, null);
    }

    private static Http.Response response(
        int status, Map<String, List<String>> headers, @Nullable FileEntry fileEntry) {

        Http.Response response = mock(Http.Response.class);

        when(response.getStatusCode()).thenReturn(status);
        when(response.getHeaders()).thenReturn(headers);
        when(response.getBody(FileEntry.class)).thenReturn(fileEntry);

        return response;
    }

    private void stubExecute(Http.Response response, Http.Response... responses) {
        when(executor.execute()).thenReturn(response, responses);
    }

    private FileEntry spoolFileEntry() {
        FileEntry fileEntry = mock(FileEntry.class);

        int index = spoolCounter.incrementAndGet();

        when(fileEntry.getName()).thenReturn("spool-" + index + ".bin");
        when(fileEntry.getExtension()).thenReturn("bin");
        when(fileEntry.getUrl()).thenReturn("base64:///temp/spool-" + index + ".bin");

        return fileEntry;
    }

    private FileEntry storedBody(String content) {
        return storedBody(content.getBytes(StandardCharsets.UTF_8));
    }

    private FileEntry storedBody(byte[] bytes) {
        FileEntry fileEntry = spoolFileEntry();

        when(file.getContentLength(fileEntry)).thenReturn((long) bytes.length);
        when(file.getInputStream(fileEntry)).thenReturn(new ByteArrayInputStream(bytes));

        return fileEntry;
    }

    private void verifyDeleted(FileEntry fileEntry) {
        String url = fileEntry.getUrl();

        verify(tempFileStorage).deleteFile(argThat(deleted -> deleted.getUrl()
            .equals(url)));
    }
}
