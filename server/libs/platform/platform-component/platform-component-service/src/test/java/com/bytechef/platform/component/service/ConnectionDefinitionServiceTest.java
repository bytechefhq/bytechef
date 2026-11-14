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

package com.bytechef.platform.component.service;

import static com.bytechef.component.definition.Authorization.DEFAULT_REFRESH_ON;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.aspect.TokenRefreshHandler;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class ConnectionDefinitionServiceTest {

    @RegisterExtension
    static WireMockExtension tokenServer = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .dynamicPort()
            .http2PlainDisabled(true))
        .build();

    private final ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
    private final ConnectionDefinitionService connectionDefinitionService = new ConnectionDefinitionServiceImpl(
        componentDefinitionRegistry, mock(ContextFactory.class), mock(TokenRefreshHandler.class));

    @Test
    public void testGetAuthorizationRefreshOnIsEmptyForCustomAuthorizationWithoutAcquire() {
        when(componentDefinitionRegistry.fetchAuthorization("telegram", 1, AuthorizationType.CUSTOM))
            .thenReturn(Optional.of(ComponentDsl.authorization(AuthorizationType.CUSTOM)));

        assertThat(connectionDefinitionService.getAuthorizationRefreshOn("telegram", 1, AuthorizationType.CUSTOM))
            .isEmpty();
    }

    @Test
    public void testGetAuthorizationRefreshOnKeepsTheDefaultForCustomAuthorizationWithAcquire() {
        when(componentDefinitionRegistry.fetchAuthorization("custom", 1, AuthorizationType.CUSTOM))
            .thenReturn(
                Optional.of(
                    ComponentDsl.authorization(AuthorizationType.CUSTOM)
                        .acquire((connectionParameters, context) -> Map.of("token", "acquired"))));

        assertThat(connectionDefinitionService.getAuthorizationRefreshOn("custom", 1, AuthorizationType.CUSTOM))
            .isNotEmpty();
    }

    @Test
    public void testGetAuthorizationRefreshOnKeepsTheDefaultForOAuth2AuthorizationCode() {
        when(
            componentDefinitionRegistry.fetchAuthorization(
                "oauth", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
                    .thenReturn(Optional.of(ComponentDsl.authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)));

        assertThat(
            connectionDefinitionService.getAuthorizationRefreshOn(
                "oauth", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
                    .isEqualTo(DEFAULT_REFRESH_ON);
    }

    @Disabled
    @Test
    public void testConnectionExists() {
// TODO
    }

    @Test
    public void testExecuteAuthorizationApplySendsEmptyPasswordForUsernameOnlyBasicAuth() {
        when(componentDefinitionRegistry.getAuthorization("bamboohr", 1, AuthorizationType.BASIC_AUTH))
            .thenReturn(ComponentDsl.authorization(AuthorizationType.BASIC_AUTH));

        ApplyResponse applyResponse = connectionDefinitionService.executeAuthorizationApply(
            "bamboohr", 1, AuthorizationType.BASIC_AUTH, Map.of(Authorization.USERNAME, "apiKey"),
            mock(Context.class));

        assertThat(applyResponse.getHeaders())
            .containsEntry("Authorization", List.of("Basic " + encode("apiKey:")));
    }

    @Test
    public void testExecuteAuthorizationApplySendsUsernameAndPasswordForBasicAuth() {
        when(componentDefinitionRegistry.getAuthorization("http", 1, AuthorizationType.BASIC_AUTH))
            .thenReturn(ComponentDsl.authorization(AuthorizationType.BASIC_AUTH));

        ApplyResponse applyResponse = connectionDefinitionService.executeAuthorizationApply(
            "http", 1, AuthorizationType.BASIC_AUTH,
            Map.of(Authorization.USERNAME, "user", Authorization.PASSWORD, "secret"), mock(Context.class));

        assertThat(applyResponse.getHeaders())
            .containsEntry("Authorization", List.of("Basic " + encode("user:secret")));
    }

    @Test
    public void testExecuteAuthorizationCallbackOmitsClientSecretForPublicPkceClient() {
        stubTokenEndpoint();

        when(componentDefinitionRegistry.getAuthorization("mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE))
            .thenReturn(ComponentDsl.authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        AuthorizationCallbackResponse authorizationCallbackResponse = Objects.requireNonNull(
            connectionDefinitionService.executeAuthorizationCallback(
                "mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, getCallbackParameters(null),
                "http://localhost/callback"));

        Map<String, ?> result = authorizationCallbackResponse.result();

        assertThat(result.get(Authorization.ACCESS_TOKEN)).isEqualTo("new-access");

        tokenServer.verify(
            postRequestedFor(urlPathEqualTo("/token"))
                .withRequestBody(containing("client_id=public-client"))
                .withRequestBody(containing("code_verifier=test-verifier"))
                .withRequestBody(notContaining("client_secret")));
    }

    @Test
    public void testExecuteAuthorizationCallbackSendsClientSecretForConfidentialClient() {
        stubTokenEndpoint();

        when(componentDefinitionRegistry.getAuthorization("mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE))
            .thenReturn(ComponentDsl.authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        connectionDefinitionService.executeAuthorizationCallback(
            "mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, getCallbackParameters("top-secret"),
            "http://localhost/callback");

        tokenServer.verify(
            postRequestedFor(urlPathEqualTo("/token"))
                .withRequestBody(containing("client_secret=top-secret")));
    }

    @Test
    public void testExecuteRefreshOmitsClientSecretForPublicPkceClient() {
        stubTokenEndpoint();

        when(componentDefinitionRegistry.getAuthorization("mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE))
            .thenReturn(ComponentDsl.authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        RefreshTokenResponse refreshTokenResponse = connectionDefinitionService.executeRefresh(
            "mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, getRefreshParameters(null),
            mock(Context.class));

        assertThat(refreshTokenResponse.accessToken()).isEqualTo("new-access");

        tokenServer.verify(
            postRequestedFor(urlPathEqualTo("/token"))
                .withRequestBody(containing("grant_type=refresh_token"))
                .withRequestBody(notContaining("client_secret")));
    }

    @Test
    public void testExecuteRefreshSendsClientSecretForConfidentialClient() {
        stubTokenEndpoint();

        when(componentDefinitionRegistry.getAuthorization("mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
            .thenReturn(ComponentDsl.authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));

        connectionDefinitionService.executeRefresh(
            "mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE, getRefreshParameters("top-secret"),
            mock(Context.class));

        tokenServer.verify(
            postRequestedFor(urlPathEqualTo("/token"))
                .withRequestBody(containing("client_secret=top-secret")));
    }

    @Disabled
    @Test
    public void testPerformAuthorizationCallback() {
// TODO
    }

    @Disabled
    @Test
    public void testFetchBaseUri() {
// TODO
    }

    @Disabled
    @Test
    public void testGetAuthorizationType() {
// TODO
    }

    @Disabled
    @Test
    public void testGetConnectionDefinition() {
// TODO
    }

    @Disabled
    @Test
    public void testGetConnectionDefinitions() {
// TODO
    }

    @Disabled
    @Test
    public void testGetOAuth2Parameters() {
        // TODO
    }

    private static Map<String, Object> getCallbackParameters(String clientSecret) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(Authorization.CLIENT_ID, "public-client");
        parameters.put(Authorization.CODE, "auth-code");
        parameters.put("code_verifier", "test-verifier");
        parameters.put(Authorization.TOKEN_URL, tokenServer.baseUrl() + "/token");

        if (clientSecret != null) {
            parameters.put(Authorization.CLIENT_SECRET, clientSecret);
        }

        return parameters;
    }

    private static Map<String, Object> getRefreshParameters(String clientSecret) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(Authorization.CLIENT_ID, "public-client");
        parameters.put(Authorization.REFRESH_TOKEN, "old-refresh");
        parameters.put(Authorization.TOKEN_URL, tokenServer.baseUrl() + "/token");

        if (clientSecret != null) {
            parameters.put(Authorization.CLIENT_SECRET, clientSecret);
        }

        return parameters;
    }

    private static void stubTokenEndpoint() {
        tokenServer.stubFor(
            post(urlPathEqualTo("/token"))
                .willReturn(okJson("{\"access_token\":\"new-access\",\"refresh_token\":\"new-refresh\"}")));
    }

    private static String encode(String value) {
        Base64.Encoder encoder = Base64.getEncoder();

        return encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
