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

package com.bytechef.automation.ai.a2a.server.security.web.configurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.server.config.AutomationA2AServerSecurityIntTestConfiguration;
import com.bytechef.automation.ai.a2a.server.facade.AutomationA2AServerFacade;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.platform.ai.a2a.A2AAgentDescriptor;
import com.bytechef.platform.ai.a2a.A2AProtocolHandler;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.security.web.mcp.McpApiKeyEntity;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.TaskNotFoundError;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.security.server.apikey.authentication.ApiKeyAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationA2AServerSecurityIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
class AutomationA2AServerSecurityConfigurerIntTest {

    private static final long A2A_SERVER_ID = 42L;
    private static final String A2A_SERVER_SECRET_KEY = String.valueOf(TenantKey.of());
    private static final String JSON_RPC_REQUEST = """
        {"jsonrpc":"2.0","id":1,"method":"tasks/get","params":{"id":"task-1"}}""";
    private static final String OTHER_TENANT_ID = "tenantb";
    private static final String OTHER_TENANT_A2A_SERVER_SECRET_KEY = String.valueOf(TenantKey.of(OTHER_TENANT_ID));
    private static final String TASK_ID = "task-1";
    private static final long USER_ID = 1050L;

    @Autowired
    private A2aServerService a2aServerService;

    @MockitoBean
    private A2AProtocolHandler a2aProtocolHandler;

    @MockitoSpyBean
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private AuthorityService authorityService;

    @MockitoBean
    private AutomationA2AServerFacade automationA2AServerFacade;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserService userService;

    @LocalServerPort
    private int port;

    private final List<Authentication> handlerAuthentications = new ArrayList<>();
    private final List<String> handlerTenantIds = new ArrayList<>();
    private final List<String> a2aServerLookupTenantIds = new ArrayList<>();

    @BeforeEach
    void beforeEach() {
        reset(a2aServerService, authorityService, userService);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM api_key");

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of());
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));

        when(automationA2AServerFacade.getAgentDescriptor(anyString(), anyString())).thenAnswer(invocation -> {
            recordHandlerInvocation();

            return new A2AAgentDescriptor("agent", "description", invocation.getArgument(1), "1.0.0", List.of());
        });
        when(a2aProtocolHandler.handleGetTask(anyString(), any(), any())).thenAnswer(invocation -> {
            recordHandlerInvocation();

            return new JSONRPCErrorResponse(invocation.getArgument(1), new TaskNotFoundError());
        });
    }

    @AfterEach
    void afterEach() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("DELETE FROM api_key");
    }

    @Test
    void testAgentCardRequestWithoutBearerTokenIsRejected() throws Exception {
        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, true);

        HttpResponse<String> httpResponse = getAgentCard(A2A_SERVER_SECRET_KEY, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        verify(a2aServerService).fetchA2aServer(A2A_SERVER_SECRET_KEY);
        verifyNoInteractions(apiKeyService, automationA2AServerFacade, a2aProtocolHandler);
    }

    @Test
    void testJsonRpcRequestWithoutBearerTokenIsRejected() throws Exception {
        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, true);

        HttpResponse<String> httpResponse = postJsonRpc(A2A_SERVER_SECRET_KEY, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        verify(a2aServerService).fetchA2aServer(A2A_SERVER_SECRET_KEY);
        verifyNoInteractions(apiKeyService, automationA2AServerFacade, a2aProtocolHandler);
    }

    @Test
    void testAgentCardRequestWithValidApiKeySucceeds() throws Exception {
        ApiKey apiKey = seedApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, true);

        HttpResponse<String> httpResponse = getAgentCard(A2A_SERVER_SECRET_KEY, apiKey.getSecretKey());

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.body()).contains("\"name\":\"agent\"");

        verify(automationA2AServerFacade).getAgentDescriptor(anyString(), anyString());
        verify(apiKeyService).updateLastUsedDate(apiKey.getId());

        assertApiKeyAuthentication(apiKey);
    }

    @Test
    void testJsonRpcRequestWithValidApiKeySucceeds() throws Exception {
        ApiKey apiKey = seedApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, true);

        HttpResponse<String> httpResponse = postJsonRpc(A2A_SERVER_SECRET_KEY, apiKey.getSecretKey());

        assertThat(httpResponse.statusCode()).isEqualTo(200);

        verify(a2aProtocolHandler).handleGetTask(A2A_SERVER_SECRET_KEY, 1, TASK_ID);
        verify(apiKeyService).updateLastUsedDate(apiKey.getId());

        assertApiKeyAuthentication(apiKey);
    }

    @Test
    void testApiKeyFromAnotherEnvironmentIsRejected() throws Exception {
        ApiKey apiKey = seedApiKey(PlatformType.AUTOMATION, Environment.STAGING);

        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, true);

        HttpResponse<String> httpResponse = postJsonRpc(A2A_SERVER_SECRET_KEY, apiKey.getSecretKey());

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        assertApiKeyLookedUpButNotUsed(apiKey);
    }

    @Test
    void testEmbeddedApiKeyIsRejected() throws Exception {
        ApiKey apiKey = seedApiKey(PlatformType.EMBEDDED, Environment.PRODUCTION);

        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, true);

        HttpResponse<String> httpResponse = postJsonRpc(A2A_SERVER_SECRET_KEY, apiKey.getSecretKey());

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        assertApiKeyLookedUpButNotUsed(apiKey);
    }

    @Test
    void testUnknownApiKeyIsRejected() throws Exception {
        String unknownApiSecretKey = String.valueOf(TenantKey.of());

        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, true);

        HttpResponse<String> httpResponse = postJsonRpc(A2A_SERVER_SECRET_KEY, unknownApiSecretKey);

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        verify(a2aServerService).fetchA2aServer(A2A_SERVER_SECRET_KEY);
        verify(apiKeyService).fetchApiKey(unknownApiSecretKey);
        verify(apiKeyService, never()).updateLastUsedDate(anyLong());
        verifyNoInteractions(automationA2AServerFacade, a2aProtocolHandler);
    }

    @Test
    void testAnonymousAgentCardRequestIsAuthenticatedAsTheA2aServer() throws Exception {
        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, false);

        HttpResponse<String> httpResponse = getAgentCard(A2A_SERVER_SECRET_KEY, null);

        assertThat(httpResponse.statusCode()).isEqualTo(200);

        verifyNoInteractions(apiKeyService);

        assertAnonymousAuthentication();
    }

    @Test
    void testAnonymousJsonRpcRequestIsAuthenticatedAsTheA2aServer() throws Exception {
        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, false);

        HttpResponse<String> httpResponse = postJsonRpc(A2A_SERVER_SECRET_KEY, null);

        assertThat(httpResponse.statusCode()).isEqualTo(200);

        verify(a2aProtocolHandler).handleGetTask(A2A_SERVER_SECRET_KEY, 1, TASK_ID);
        verifyNoInteractions(apiKeyService);

        assertAnonymousAuthentication();
    }

    @Test
    void testPresentedApiKeyIsIgnoredWhenAuthenticationIsNotRequired() throws Exception {
        ApiKey apiKey = seedApiKey(PlatformType.EMBEDDED, Environment.STAGING);

        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, false);

        HttpResponse<String> httpResponse = postJsonRpc(A2A_SERVER_SECRET_KEY, apiKey.getSecretKey());

        assertThat(httpResponse.statusCode()).isEqualTo(200);

        verifyNoInteractions(apiKeyService);

        assertAnonymousAuthentication();
    }

    @Test
    void testDisabledA2aServerIsRejected() throws Exception {
        ApiKey apiKey = seedApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, false, true);

        HttpResponse<String> httpResponse = postJsonRpc(A2A_SERVER_SECRET_KEY, apiKey.getSecretKey());

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        verify(a2aServerService).fetchA2aServer(A2A_SERVER_SECRET_KEY);
        verifyNoInteractions(apiKeyService, automationA2AServerFacade, a2aProtocolHandler);
    }

    @Test
    void testDisabledA2aServerRejectsAnonymousRequest() throws Exception {
        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, false, false);

        HttpResponse<String> httpResponse = getAgentCard(A2A_SERVER_SECRET_KEY, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        verify(a2aServerService).fetchA2aServer(A2A_SERVER_SECRET_KEY);
        verifyNoInteractions(apiKeyService, automationA2AServerFacade, a2aProtocolHandler);
    }

    @Test
    void testUnknownA2aServerSecretKeyIsRejected() throws Exception {
        ApiKey apiKey = seedApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);
        String unknownA2aServerSecretKey = String.valueOf(TenantKey.of());

        when(a2aServerService.fetchA2aServer(unknownA2aServerSecretKey)).thenReturn(Optional.empty());

        HttpResponse<String> httpResponse = postJsonRpc(unknownA2aServerSecretKey, apiKey.getSecretKey());

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        verify(a2aServerService).fetchA2aServer(unknownA2aServerSecretKey);
        verifyNoInteractions(apiKeyService, automationA2AServerFacade, a2aProtocolHandler);
    }

    @Test
    void testUnparseableA2aServerSecretKeyIsRejected() throws Exception {
        ApiKey apiKey = seedApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        HttpResponse<String> httpResponse = getAgentCard("server-secret", apiKey.getSecretKey());

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        verifyNoInteractions(a2aServerService, apiKeyService, automationA2AServerFacade, a2aProtocolHandler);
    }

    @Test
    void testAgentCardRequestResolvesTheA2aServerInsideThePathTenant() throws Exception {
        stubA2aServer(OTHER_TENANT_A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, false);

        HttpResponse<String> httpResponse = getAgentCard(OTHER_TENANT_A2A_SERVER_SECRET_KEY, null);

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(a2aServerLookupTenantIds).containsExactly(OTHER_TENANT_ID);
        assertThat(handlerTenantIds).containsExactly(OTHER_TENANT_ID);
    }

    @Test
    void testJsonRpcRequestResolvesTheA2aServerInsideThePathTenant() throws Exception {
        stubA2aServer(OTHER_TENANT_A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, false);

        HttpResponse<String> httpResponse = postJsonRpc(OTHER_TENANT_A2A_SERVER_SECRET_KEY, null);

        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(a2aServerLookupTenantIds).containsExactly(OTHER_TENANT_ID);
        assertThat(handlerTenantIds).containsExactly(OTHER_TENANT_ID);
    }

    @Test
    void testApiKeyFromAnotherTenantIsRejectedWhenAuthenticationIsRequired() throws Exception {
        ApiKey apiKey = seedApiKey(PlatformType.AUTOMATION, Environment.PRODUCTION);

        stubA2aServer(OTHER_TENANT_A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, true);

        HttpResponse<String> httpResponse = postJsonRpc(OTHER_TENANT_A2A_SERVER_SECRET_KEY, apiKey.getSecretKey());

        assertThat(httpResponse.statusCode()).isEqualTo(401);
        assertThat(a2aServerLookupTenantIds).containsExactly(OTHER_TENANT_ID);

        verify(a2aServerService).fetchA2aServer(OTHER_TENANT_A2A_SERVER_SECRET_KEY);
        verifyNoInteractions(apiKeyService, automationA2AServerFacade, a2aProtocolHandler);
    }

    @Test
    void testOtherPathUnderTheA2aServerIsRejectedEvenWhenAuthenticationIsNotRequired() throws Exception {
        stubA2aServer(A2A_SERVER_SECRET_KEY, Environment.PRODUCTION, true, false);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:%d/api/automation/a2a/%s/tasks".formatted(port, A2A_SERVER_SECRET_KEY)))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON_RPC_REQUEST));

        HttpResponse<String> httpResponse = send(requestBuilder, null);

        assertThat(httpResponse.statusCode()).isEqualTo(401);

        verifyNoInteractions(a2aServerService, apiKeyService, automationA2AServerFacade, a2aProtocolHandler);
    }

    private void assertAnonymousAuthentication() {
        assertThat(handlerAuthentications).singleElement()
            .isInstanceOfSatisfying(McpAnonymousAuthenticationToken.class, authentication -> {
                assertThat(authentication.isAuthenticated()).isTrue();
                assertThat(authentication.getPrincipal()).isEqualTo("a2a-anonymous:automation:" + A2A_SERVER_ID);
                assertThat(authentication.getAuthorities()).isEmpty();
            });
    }

    private void assertApiKeyAuthentication(ApiKey apiKey) {
        assertThat(handlerAuthentications).singleElement()
            .isInstanceOfSatisfying(ApiKeyAuthenticationToken.class, authentication -> {
                assertThat(authentication.isAuthenticated()).isTrue();
                assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(
                    McpApiKeyEntity.class,
                    mcpApiKeyEntity -> assertThat(mcpApiKeyEntity.getApiKeyId()).isEqualTo(apiKey.getId()));
            });
    }

    private void assertApiKeyLookedUpButNotUsed(ApiKey apiKey) {
        verify(a2aServerService).fetchA2aServer(A2A_SERVER_SECRET_KEY);
        verify(apiKeyService).fetchApiKey(apiKey.getSecretKey());
        verify(apiKeyService, never()).updateLastUsedDate(anyLong());
        verifyNoInteractions(automationA2AServerFacade, a2aProtocolHandler);
    }

    private HttpResponse<String> getAgentCard(String a2aServerSecretKey, String bearerSecretKey) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(
                "http://localhost:%d/api/automation/a2a/%s/.well-known/agent-card.json".formatted(
                    port, a2aServerSecretKey)))
            .GET();

        return send(requestBuilder, bearerSecretKey);
    }

    private HttpResponse<String> postJsonRpc(String a2aServerSecretKey, String bearerSecretKey) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:%d/api/automation/a2a/%s".formatted(port, a2aServerSecretKey)))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JSON_RPC_REQUEST));

        return send(requestBuilder, bearerSecretKey);
    }

    private void recordHandlerInvocation() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        handlerAuthentications.add(securityContext.getAuthentication());
        handlerTenantIds.add(TenantContext.getCurrentTenantId());
    }

    private ApiKey seedApiKey(PlatformType type, Environment environment) {
        ApiKey apiKey = new ApiKey();

        apiKey.setName("test");
        apiKey.setSecretKey(String.valueOf(TenantKey.of()));
        apiKey.setType(type);
        apiKey.setEnvironment(environment);
        apiKey.setUserId(USER_ID);

        return apiKeyRepository.save(apiKey);
    }

    private HttpResponse<String> send(HttpRequest.Builder requestBuilder, String bearerSecretKey) throws Exception {
        if (bearerSecretKey != null) {
            requestBuilder.header("Authorization", "Bearer " + bearerSecretKey);
        }

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private void stubA2aServer(
        String a2aServerSecretKey, Environment environment, boolean enabled, boolean authenticationRequired) {

        A2aServer a2aServer = new A2aServer("server", "description", environment);

        a2aServer.setId(A2A_SERVER_ID);
        a2aServer.setEnabled(enabled);
        a2aServer.setAuthenticationRequired(authenticationRequired);

        when(a2aServerService.fetchA2aServer(a2aServerSecretKey)).thenAnswer(invocation -> {
            a2aServerLookupTenantIds.add(TenantContext.getCurrentTenantId());

            return Optional.of(a2aServer);
        });
    }
}
