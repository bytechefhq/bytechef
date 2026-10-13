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
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.Authorization.AuthorizationCallbackResponse;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.Authorization.RefreshTokenResponse;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.aspect.TokenRefreshHandler;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.OAuth2AuthorizationContext;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import com.bytechef.platform.oauth2.exception.OAuth2ErrorType;
import com.bytechef.platform.oauth2.service.OAuth2AuthorizationContextService;
import com.bytechef.platform.oauth2.service.OAuth2DiscoveryService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ConnectionDefinitionServiceTest {

    private static final String COMPONENT_NAME = "mcpClient";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig()
            .dynamicPort()
            .http2PlainDisabled(true))
        .build();

    private ComponentDefinitionRegistry componentDefinitionRegistry;
    private ConnectionDefinitionServiceImpl connectionDefinitionService;
    private ObjectProvider<OAuth2DiscoveryService> discoveryServiceProvider;
    private OAuth2AuthorizationContextService oAuth2AuthorizationContextService;
    private OAuth2DiscoveryService oAuth2DiscoveryService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
        oAuth2AuthorizationContextService = mock(OAuth2AuthorizationContextService.class);
        oAuth2DiscoveryService = mock(OAuth2DiscoveryService.class);

        Context context = createContext();
        ContextFactory contextFactory = mock(ContextFactory.class);

        when(contextFactory.createContext(anyString(), any())).thenReturn(context);

        discoveryServiceProvider = mock(ObjectProvider.class);

        ObjectProvider<OAuth2AuthorizationContextService> contextServiceProvider = mock(ObjectProvider.class);

        when(discoveryServiceProvider.getIfAvailable()).thenReturn(oAuth2DiscoveryService);
        when(contextServiceProvider.getIfAvailable()).thenReturn(oAuth2AuthorizationContextService);
        when(oAuth2AuthorizationContextService.seal(any())).thenReturn("sealed-context");

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Oauth2 oauth2 = applicationProperties.getOauth2();

        ApplicationProperties.Oauth2.Discovery discovery = oauth2.getDiscovery();

        discovery.setAllowedHosts(List.of("localhost"));

        Environment environment = mock(Environment.class);

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        connectionDefinitionService = new ConnectionDefinitionServiceImpl(
            applicationProperties, componentDefinitionRegistry, contextFactory, environment,
            mock(TokenRefreshHandler.class), discoveryServiceProvider, contextServiceProvider);
    }

    @Test
    void testGetAuthorizationRefreshOnIsEmptyForCustomAuthorizationWithoutAcquire() {
        when(componentDefinitionRegistry.fetchAuthorization("telegram", 1, AuthorizationType.CUSTOM))
            .thenReturn(Optional.of(authorization(AuthorizationType.CUSTOM)));

        assertThat(connectionDefinitionService.getAuthorizationRefreshOn("telegram", 1, AuthorizationType.CUSTOM))
            .isEmpty();
    }

    @Test
    void testGetAuthorizationRefreshOnKeepsTheDefaultForCustomAuthorizationWithAcquire() {
        when(componentDefinitionRegistry.fetchAuthorization("custom", 1, AuthorizationType.CUSTOM))
            .thenReturn(
                Optional.of(
                    authorization(AuthorizationType.CUSTOM)
                        .acquire((connectionParameters, context) -> Map.of("token", "acquired"))));

        assertThat(connectionDefinitionService.getAuthorizationRefreshOn("custom", 1, AuthorizationType.CUSTOM))
            .isNotEmpty();
    }

    @Test
    void testGetAuthorizationRefreshOnKeepsTheDefaultForOAuth2AuthorizationCode() {
        when(
            componentDefinitionRegistry.fetchAuthorization(
                "oauth", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
                    .thenReturn(Optional.of(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)));

        assertThat(
            connectionDefinitionService.getAuthorizationRefreshOn(
                "oauth", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
                    .isEqualTo(DEFAULT_REFRESH_ON);
    }

    @Test
    void testAuthorizationCodeParametersAreUnchangedAndHaveNoAuthorizationContext() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));

        OAuth2AuthorizationParameters parameters = connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE,
            Map.of(
                Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize", Authorization.CLIENT_ID,
                "manual", Authorization.SCOPES, "read,write"));

        assertThat(parameters.authorizationContext()).isNull();
        assertThat(parameters.authorizationUrl()).isEqualTo("https://auth.example.com/authorize");
        assertThat(parameters.clientId()).isEqualTo("manual");
        assertThat(parameters.extraQueryParameters()).isEmpty();
        assertThat(parameters.scopes()).containsOnlyKeys("read", "write");

        verify(oAuth2AuthorizationContextService, never()).seal(any());
    }

    @Test
    void testPkceParametersSealTheVerifierInsteadOfExposingIt() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        OAuth2AuthorizationParameters parameters = connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(
                Authorization.AUTHORIZATION_URL, "https://auth.example.com/authorize", Authorization.CLIENT_ID,
                "manual"));

        assertThat(parameters.authorizationContext()).isEqualTo("sealed-context");
        assertThat(parameters.authorizationUrl()).isEqualTo("https://auth.example.com/authorize");
        assertThat(parameters.clientId()).isEqualTo("manual");
        assertThat(parameters.extraQueryParameters())
            .containsKey("code_challenge")
            .containsEntry("code_challenge_method", "S256")
            .doesNotContainKey("code_verifier")
            .doesNotContainKey("resource");

        ArgumentCaptor<OAuth2AuthorizationContext> captor = ArgumentCaptor.forClass(OAuth2AuthorizationContext.class);

        verify(oAuth2AuthorizationContextService).seal(captor.capture());

        OAuth2AuthorizationContext authorizationContext = captor.getValue();

        assertThat(authorizationContext.isDiscovery()).isFalse();
        assertThat(authorizationContext.componentName()).isEqualTo(COMPONENT_NAME);
        assertThat(authorizationContext.codeVerifier()).isNotBlank();
    }

    @Test
    void testDiscoveryParametersUseDiscoveredServerAndRegisteredClient() {
        registerAuthorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl(
                    (connectionParameters, context) -> connectionParameters.getRequiredString("url")));

        DiscoveredAuthorizationServer authorizationServer = new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/authorize", "https://auth.example.com/token",
            "https://auth.example.com/register", "https://mcp.example.com/mcp", List.of("read", "write"),
            List.of("S256"), List.of("none"));

        when(oAuth2DiscoveryService.discover("https://mcp.example.com/mcp")).thenReturn(authorizationServer);
        when(oAuth2DiscoveryService.register(authorizationServer))
            .thenReturn(new RegisteredClient("reg-1", null, "none"));

        OAuth2AuthorizationParameters parameters = connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of("url", "https://mcp.example.com/mcp"));

        assertThat(parameters.authorizationContext()).isEqualTo("sealed-context");
        assertThat(parameters.authorizationUrl()).isEqualTo("https://auth.example.com/authorize");
        assertThat(parameters.clientId()).isEqualTo("reg-1");
        assertThat(parameters.scopes()).containsOnlyKeys("read", "write");
        assertThat(parameters.extraQueryParameters())
            .containsKey("code_challenge")
            .containsEntry("code_challenge_method", "S256")
            .containsEntry("resource", "https://mcp.example.com/mcp")
            .doesNotContainKey("code_verifier");

        ArgumentCaptor<OAuth2AuthorizationContext> captor = ArgumentCaptor.forClass(OAuth2AuthorizationContext.class);

        verify(oAuth2AuthorizationContextService).seal(captor.capture());

        OAuth2AuthorizationContext authorizationContext = captor.getValue();

        assertThat(authorizationContext.isDiscovery()).isTrue();
        assertThat(authorizationContext.clientId()).isEqualTo("reg-1");
        assertThat(authorizationContext.tokenUrl()).isEqualTo("https://auth.example.com/token");
        assertThat(authorizationContext.codeVerifier()).isNotBlank();
    }

    @Test
    void testDiscoveryErrorReachesCallerUnchanged() {
        registerAuthorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl(
                    (connectionParameters, context) -> connectionParameters.getRequiredString("url")));

        ConfigurationException discoveryException = new ConfigurationException(
            "Registration is not supported", OAuth2ErrorType.REGISTRATION_NOT_SUPPORTED);

        when(oAuth2DiscoveryService.discover("https://mcp.example.com/mcp")).thenThrow(discoveryException);

        assertThatThrownBy(() -> connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of("url", "https://mcp.example.com/mcp")))
                .isSameAs(discoveryException);
    }

    @Test
    void testDiscoveryFailsClearlyWhenDiscoveryServiceIsAbsent() {
        registerAuthorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl(
                    (connectionParameters, context) -> connectionParameters.getRequiredString("url")));

        when(discoveryServiceProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of("url", "https://mcp.example.com/mcp")))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("OAuth2 discovery service is not available")
                .satisfies(throwable -> {
                    ConfigurationException configurationException = (ConfigurationException) throwable;

                    assertThat(configurationException.getEntityClass()).isEqualTo(OAuth2ErrorType.class);
                    assertThat(configurationException.getErrorKey())
                        .isEqualTo(OAuth2ErrorType.DISCOVERY_NOT_AVAILABLE.getErrorKey());
                });
    }

    @Test
    void testManualEndpointsSkipDiscoveryAndUseTheManualClientWithPkce() {
        registerAuthorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl(
                    (connectionParameters, context) -> connectionParameters.getRequiredString("url")));

        OAuth2AuthorizationParameters parameters = connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(
                "url", "https://api.elevenlabs.io/mcp", Authorization.AUTHORIZATION_URL,
                "https://elevenlabs.io/app/oauth/authorize", Authorization.TOKEN_URL,
                "https://api.us.elevenlabs.io/v1/oauth/token", Authorization.CLIENT_ID, "manual", Authorization.SCOPES,
                "read,write"));

        assertThat(parameters.authorizationContext()).isEqualTo("sealed-context");
        assertThat(parameters.authorizationUrl()).isEqualTo("https://elevenlabs.io/app/oauth/authorize");
        assertThat(parameters.clientId()).isEqualTo("manual");
        assertThat(parameters.scopes()).containsOnlyKeys("read", "write");
        assertThat(parameters.extraQueryParameters())
            .containsKey("code_challenge")
            .containsEntry("code_challenge_method", "S256")
            .doesNotContainKey("code_verifier")
            .doesNotContainKey("resource");

        verify(oAuth2DiscoveryService, never()).discover(any());
        verify(oAuth2DiscoveryService, never()).register(any());

        ArgumentCaptor<OAuth2AuthorizationContext> captor = ArgumentCaptor.forClass(OAuth2AuthorizationContext.class);

        verify(oAuth2AuthorizationContextService).seal(captor.capture());

        OAuth2AuthorizationContext authorizationContext = captor.getValue();

        assertThat(authorizationContext.isDiscovery()).isFalse();
        assertThat(authorizationContext.codeVerifier()).isNotBlank();
    }

    @Test
    void testDiscoveryStillRunsWhenOnlyOneManualEndpointIsSet() {
        registerAuthorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl(
                    (connectionParameters, context) -> connectionParameters.getRequiredString("url")));

        DiscoveredAuthorizationServer authorizationServer = new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/authorize", "https://auth.example.com/token",
            "https://auth.example.com/register", "https://mcp.example.com/mcp", List.of("read"), List.of("S256"),
            List.of("none"));

        when(oAuth2DiscoveryService.discover("https://mcp.example.com/mcp")).thenReturn(authorizationServer);
        when(oAuth2DiscoveryService.register(authorizationServer))
            .thenReturn(new RegisteredClient("reg-1", null, "none"));

        OAuth2AuthorizationParameters parameters = connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(
                "url", "https://mcp.example.com/mcp", Authorization.AUTHORIZATION_URL,
                "https://manual.example.com/authorize", Authorization.TOKEN_URL, " "));

        assertThat(parameters.authorizationUrl()).isEqualTo("https://auth.example.com/authorize");
        assertThat(parameters.clientId()).isEqualTo("reg-1");

        verify(oAuth2DiscoveryService).discover("https://mcp.example.com/mcp");
    }

    @Test
    void testDefaultCallbackSendsCodeVerifierToManualTokenUrlAndOmitsBlankClientSecret() {
        registerAuthorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl(
                    (connectionParameters, context) -> connectionParameters.getRequiredString("url")));
        stubTokenEndpoint("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600}");

        AuthorizationCallbackResponse response = connectionDefinitionService.executeAuthorizationCallback(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(
                "url", "https://api.elevenlabs.io/mcp", Authorization.AUTHORIZATION_URL,
                "https://elevenlabs.io/app/oauth/authorize", Authorization.TOKEN_URL, wireMock.baseUrl() + "/token",
                Authorization.CLIENT_ID, "manual", Authorization.CLIENT_SECRET, "", Authorization.CODE, "the-code",
                Authorization.CODE_VERIFIER, "the-verifier"),
            "https://bytechef.example.com/callback");

        assertThat(response.result())
            .extractingByKey("access_token")
            .isEqualTo("at");

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withHeader("Authorization", absent())
            .withRequestBody(containing("client_id=manual"))
            .withRequestBody(containing("code=the-code"))
            .withRequestBody(containing("code_verifier=the-verifier"))
            .withRequestBody(notContaining("client_secret"))
            .withRequestBody(notContaining("resource")));

        verify(oAuth2DiscoveryService, never()).discover(any());
    }

    @Test
    void testDefaultCallbackOmitsClientSecretAndSendsResourceForPublicClient() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));
        stubTokenEndpoint("{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600}");

        AuthorizationCallbackResponse response = connectionDefinitionService.executeAuthorizationCallback(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(
                Authorization.CLIENT_ID, "reg-1", Authorization.TOKEN_URL, wireMock.baseUrl() + "/token",
                Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none", Authorization.RESOURCE, "https://mcp.example.com/mcp",
                Authorization.CODE, "the-code", Authorization.CODE_VERIFIER, "the-verifier"),
            "https://bytechef.example.com/callback");

        assertThat(response.result())
            .extractingByKey("access_token")
            .isEqualTo("at");

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withHeader("Authorization", absent())
            .withRequestBody(containing("client_id=reg-1"))
            .withRequestBody(containing("code_verifier=the-verifier"))
            .withRequestBody(containing("resource=https%3A%2F%2Fmcp.example.com%2Fmcp"))
            .withRequestBody(notContaining("client_secret")));
    }

    @Test
    void testDefaultCallbackUsesBasicAuthenticationForClientSecretBasic() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));
        stubTokenEndpoint("{\"access_token\":\"at\"}");

        connectionDefinitionService.executeAuthorizationCallback(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(
                Authorization.CLIENT_ID, "reg-1", Authorization.CLIENT_SECRET, "s3cret",
                Authorization.TOKEN_URL, wireMock.baseUrl() + "/token",
                Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "client_secret_basic", Authorization.CODE, "c"),
            "https://bytechef.example.com/callback");

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withHeader("Authorization", equalTo("Basic " + encode("reg-1:s3cret")))
            .withRequestBody(notContaining("client_secret")));
    }

    @Test
    void testDefaultCallbackFormUrlEncodesBasicCredentialsWithReservedCharacters() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));
        stubTokenEndpoint("{\"access_token\":\"at\"}");

        connectionDefinitionService.executeAuthorizationCallback(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of(
                Authorization.CLIENT_ID, "reg-1", Authorization.CLIENT_SECRET, "s3c+ret/=:%",
                Authorization.TOKEN_URL, wireMock.baseUrl() + "/token",
                Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "client_secret_basic", Authorization.CODE, "c"),
            "https://bytechef.example.com/callback");

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withHeader("Authorization", equalTo("Basic " + encode("reg-1:s3c%2Bret%2F%3D%3A%25")))
            .withRequestBody(notContaining("client_secret")));
    }

    @Test
    void testDefaultCallbackStillSendsFormSecretWhenNoAuthMethodIsStored() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));
        stubTokenEndpoint("{\"access_token\":\"at\"}");

        connectionDefinitionService.executeAuthorizationCallback(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE,
            Map.of(
                Authorization.CLIENT_ID, "legacy", Authorization.CLIENT_SECRET, "legacy-secret",
                Authorization.TOKEN_URL, wireMock.baseUrl() + "/token", Authorization.CODE, "c"),
            "https://bytechef.example.com/callback");

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withHeader("Authorization", absent())
            .withRequestBody(
                equalTo(
                    "client_id=legacy&client_secret=legacy-secret&code=c&grant_type=authorization_code" +
                        "&redirect_uri=https%3A%2F%2Fbytechef.example.com%2Fcallback")));
    }

    @Test
    void testDefaultRefreshSendsResourceAndOmitsSecretForPublicClient() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));
        stubTokenEndpoint("{\"access_token\":\"new-at\",\"refresh_token\":\"new-rt\",\"expires_in\":600}");

        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put(Authorization.CLIENT_ID, "reg-1");
        connectionParameters.put(Authorization.TOKEN_URL, wireMock.baseUrl() + "/token");
        connectionParameters.put(Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "none");
        connectionParameters.put(Authorization.RESOURCE, "https://mcp.example.com/mcp");
        connectionParameters.put(Authorization.REFRESH_TOKEN, "rt");

        RefreshTokenResponse response = connectionDefinitionService.executeRefresh(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, connectionParameters,
            createContext());

        assertThat(response.accessToken()).isEqualTo("new-at");
        assertThat(response.refreshToken()).isEqualTo("new-rt");

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withHeader("Authorization", absent())
            .withRequestBody(containing("grant_type=refresh_token"))
            .withRequestBody(containing("resource=https%3A%2F%2Fmcp.example.com%2Fmcp"))
            .withRequestBody(notContaining("client_secret")));
    }

    @Test
    void testDefaultRefreshUsesFormUrlEncodedBasicAuthenticationForClientSecretBasic() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));
        stubTokenEndpoint("{\"access_token\":\"new-at\"}");

        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put(Authorization.CLIENT_ID, "reg-1");
        connectionParameters.put(Authorization.CLIENT_SECRET, "s3c+ret/=:%");
        connectionParameters.put(Authorization.TOKEN_URL, wireMock.baseUrl() + "/token");
        connectionParameters.put(Authorization.TOKEN_ENDPOINT_AUTH_METHOD, "client_secret_basic");
        connectionParameters.put(Authorization.REFRESH_TOKEN, "rt");

        connectionDefinitionService.executeRefresh(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, connectionParameters,
            createContext());

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withHeader("Authorization", equalTo("Basic " + encode("reg-1:s3c%2Bret%2F%3D%3A%25")))
            .withRequestBody(containing("grant_type=refresh_token"))
            .withRequestBody(notContaining("client_secret")));
    }

    @Test
    void testDefaultRefreshStillSendsFormSecretWhenNoAuthMethodIsStored() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));
        stubTokenEndpoint("{\"access_token\":\"new-at\"}");

        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put(Authorization.CLIENT_ID, "legacy");
        connectionParameters.put(Authorization.CLIENT_SECRET, "legacy-secret");
        connectionParameters.put(Authorization.TOKEN_URL, wireMock.baseUrl() + "/token");
        connectionParameters.put(Authorization.REFRESH_TOKEN, "rt");

        connectionDefinitionService.executeRefresh(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE, connectionParameters, createContext());

        wireMock.verify(postRequestedFor(urlEqualTo("/token"))
            .withHeader("Authorization", absent())
            .withRequestBody(
                equalTo("client_id=legacy&client_secret=legacy-secret&refresh_token=rt&grant_type=refresh_token")));
    }

    @Test
    void testDiscoveryUsesTheUserScopesWhenProvided() {
        registerDiscoveryAuthorization();
        stubDiscovery(List.of("read", "write", "admin"));

        OAuth2AuthorizationParameters parameters = connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of("url", "https://mcp.example.com/mcp", Authorization.SCOPES, "read"));

        assertThat(parameters.scopes()).containsOnlyKeys("read");
    }

    @Test
    void testDiscoveryFallsBackToTheDiscoveredScopesWhenTheUserGivesNone() {
        registerDiscoveryAuthorization();
        stubDiscovery(List.of("read", "write"));

        OAuth2AuthorizationParameters parameters = connectionDefinitionService.getOAuth2AuthorizationParameters(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
            Map.of("url", "https://mcp.example.com/mcp", Authorization.SCOPES, " "));

        assertThat(parameters.scopes()).containsOnlyKeys("read", "write");
    }

    @Test
    void testDefaultCallbackRejectsAPrivateTokenUrlForADiscoveryAuthorization() {
        registerDiscoveryAuthorization();
        stubTokenEndpoint("{\"access_token\":\"at\"}");

        Throwable throwable = catchThrowable(
            () -> connectionDefinitionService.executeAuthorizationCallback(
                COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
                Map.of(
                    "url", "https://mcp.example.com/mcp", Authorization.CLIENT_ID, "reg-1", Authorization.TOKEN_URL,
                    loopbackBaseUrl() + "/token", Authorization.CODE, "c", Authorization.CODE_VERIFIER, "v"),
                "https://bytechef.example.com/callback"));

        assertUrlNotAllowed(throwable);

        assertThat(throwable).hasMessageContaining("bytechef.oauth2.discovery.allowed-hosts");

        wireMock.verify(0, postRequestedFor(urlEqualTo("/token")));
    }

    @Test
    void testDefaultRefreshRejectsAPrivateRefreshUrlForADiscoveryAuthorization() {
        registerDiscoveryAuthorization();
        stubTokenEndpoint("{\"access_token\":\"new-at\"}");

        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put("url", "https://mcp.example.com/mcp");
        connectionParameters.put(Authorization.CLIENT_ID, "reg-1");
        connectionParameters.put(Authorization.TOKEN_URL, "https://auth.example.com/token");
        connectionParameters.put(Authorization.REFRESH_URL, loopbackBaseUrl() + "/token");
        connectionParameters.put(Authorization.REFRESH_TOKEN, "rt");

        Throwable throwable = catchThrowable(
            () -> connectionDefinitionService.executeRefresh(
                COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, connectionParameters,
                createContext()));

        assertUrlNotAllowed(throwable);

        assertThat(throwable).hasMessageContaining("bytechef.oauth2.discovery.allowed-hosts");

        wireMock.verify(0, postRequestedFor(urlEqualTo("/token")));
    }

    @Test
    void testDefaultCallbackAndRefreshKeepPrivateTokenUrlsForOtherAuthorizations() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));
        stubTokenEndpoint("{\"access_token\":\"at\"}");

        connectionDefinitionService.executeAuthorizationCallback(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE,
            Map.of(
                Authorization.CLIENT_ID, "self-hosted", Authorization.TOKEN_URL, loopbackBaseUrl() + "/token",
                Authorization.CODE, "c"),
            "https://bytechef.example.com/callback");

        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put(Authorization.CLIENT_ID, "self-hosted");
        connectionParameters.put(Authorization.TOKEN_URL, loopbackBaseUrl() + "/token");
        connectionParameters.put(Authorization.REFRESH_TOKEN, "rt");

        connectionDefinitionService.executeRefresh(
            COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE, connectionParameters, createContext());

        wireMock.verify(2, postRequestedFor(urlEqualTo("/token")));
    }

    @Test
    void testDefaultCallbackReportsOnlyStatusAndOAuthErrorFieldsOfARejectedExchange() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));
        stubTokenEndpointError(
            "{\"error\":\"invalid_grant\",\"error_description\":\"Code expired\",\"trace\":\"db-password\"}");

        Throwable throwable = catchThrowable(
            () -> connectionDefinitionService.executeAuthorizationCallback(
                COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE,
                Map.of(
                    Authorization.CLIENT_ID, "c", Authorization.TOKEN_URL, wireMock.baseUrl() + "/token",
                    Authorization.CODE, "c"),
                "https://bytechef.example.com/callback"));

        assertThat(rootCause(throwable))
            .hasMessageContaining("HTTP 400: invalid_grant (Code expired)")
            .hasMessageNotContaining("db-password");
        assertThat(throwable).hasMessageNotContaining("db-password");
    }

    @Test
    void testDefaultCallbackDoesNotEchoANonJsonErrorBody() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));
        stubTokenEndpointError("<html>stack trace with db-password</html>");

        Throwable throwable = catchThrowable(
            () -> connectionDefinitionService.executeAuthorizationCallback(
                COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE,
                Map.of(
                    Authorization.CLIENT_ID, "c", Authorization.TOKEN_URL, wireMock.baseUrl() + "/token",
                    Authorization.CODE, "c"),
                "https://bytechef.example.com/callback"));

        assertThat(rootCause(throwable))
            .hasMessageEndingWith("HTTP 400")
            .hasMessageNotContaining("db-password");
    }

    @Test
    void testDefaultRefreshReportsOnlyStatusAndOAuthErrorFieldsOfARejectedRefresh() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));
        stubTokenEndpointError("{\"error\":\"invalid_grant\",\"trace\":\"db-password\"}");

        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put(Authorization.CLIENT_ID, "c");
        connectionParameters.put(Authorization.TOKEN_URL, wireMock.baseUrl() + "/token");
        connectionParameters.put(Authorization.REFRESH_TOKEN, "rt");

        Throwable throwable = catchThrowable(
            () -> connectionDefinitionService.executeRefresh(
                COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE, connectionParameters,
                createContext()));

        assertThat(rootCause(throwable))
            .hasMessageContaining("HTTP 400: invalid_grant")
            .hasMessageNotContaining("db-password");
    }

    @Test
    void testGetProtectedResourceUrlEvaluatesTheAuthorizationFunction() {
        registerDiscoveryAuthorization();

        assertThat(
            connectionDefinitionService.getProtectedResourceUrl(
                COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
                Map.of("url", "https://mcp.example.com/mcp")))
                    .contains("https://mcp.example.com/mcp");
    }

    @Test
    void testGetProtectedResourceUrlIsEmptyForAuthorizationsWithoutDiscovery() {
        registerAuthorization(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        assertThat(
            connectionDefinitionService.getProtectedResourceUrl(
                COMPONENT_NAME, 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE,
                Map.of("url", "https://mcp.example.com/mcp")))
                    .isEmpty();
    }

    private void registerDiscoveryAuthorization() {
        registerAuthorization(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE)
                .protectedResourceUrl(
                    (connectionParameters, context) -> connectionParameters.getRequiredString("url")));
    }

    private void stubDiscovery(List<String> scopes) {
        DiscoveredAuthorizationServer authorizationServer = new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/authorize", "https://auth.example.com/token",
            "https://auth.example.com/register", "https://mcp.example.com/mcp", scopes, List.of("S256"),
            List.of("none"));

        when(oAuth2DiscoveryService.discover("https://mcp.example.com/mcp")).thenReturn(authorizationServer);
        when(oAuth2DiscoveryService.register(authorizationServer))
            .thenReturn(new RegisteredClient("reg-1", null, "none"));
    }

    private static String loopbackBaseUrl() {
        return "http://127.0.0.1:" + wireMock.getPort();
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;

        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause;
    }

    private static void assertUrlNotAllowed(Throwable throwable) {
        Throwable cause = throwable;

        while (cause != null && !(cause instanceof ConfigurationException configurationException &&
            configurationException.getEntityClass() == OAuth2ErrorType.class)) {

            cause = cause.getCause();
        }

        assertThat(cause)
            .as("expected an OAuth2 URL_NOT_ALLOWED error in the cause chain of %s", throwable)
            .isInstanceOf(ConfigurationException.class);
        assertThat(((ConfigurationException) cause).getErrorKey())
            .isEqualTo(OAuth2ErrorType.URL_NOT_ALLOWED.getErrorKey());
    }

    private static void stubTokenEndpointError(String responseBody) {
        wireMock.stubFor(
            post(urlEqualTo("/token")).willReturn(
                aResponse().withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody)));
    }

    @SuppressWarnings("unchecked")
    private static Context createContext() {
        Context context = mock(Context.class);
        Context.Encoder encoder = mock(Context.Encoder.class);

        when(encoder.base64UrlEncode(any(byte[].class)))
            .thenAnswer(invocation -> EncodingUtils.urlEncodeBase64ToString((byte[]) invocation.getArgument(0)));
        when(context.encoder(any())).thenAnswer(invocation -> {
            ContextFunction<Context.Encoder, Object> encoderFunction = invocation.getArgument(0);

            return encoderFunction.apply(encoder);
        });

        return context;
    }

    private void registerAuthorization(Authorization authorization) {
        when(componentDefinitionRegistry.getAuthorization(COMPONENT_NAME, 1, authorization.getType()))
            .thenReturn(authorization);
    }

    @Test
    void testExecuteAuthorizationApplySendsEmptyPasswordForUsernameOnlyBasicAuth() {
        when(componentDefinitionRegistry.getAuthorization("bamboohr", 1, AuthorizationType.BASIC_AUTH))
            .thenReturn(authorization(AuthorizationType.BASIC_AUTH));

        ApplyResponse applyResponse = connectionDefinitionService.executeAuthorizationApply(
            "bamboohr", 1, AuthorizationType.BASIC_AUTH, Map.of(Authorization.USERNAME, "apiKey"),
            mock(Context.class));

        assertThat(applyResponse.getHeaders())
            .containsEntry("Authorization", List.of("Basic " + encode("apiKey:")));
    }

    @Test
    void testExecuteAuthorizationApplySendsUsernameAndPasswordForBasicAuth() {
        when(componentDefinitionRegistry.getAuthorization("http", 1, AuthorizationType.BASIC_AUTH))
            .thenReturn(authorization(AuthorizationType.BASIC_AUTH));

        ApplyResponse applyResponse = connectionDefinitionService.executeAuthorizationApply(
            "http", 1, AuthorizationType.BASIC_AUTH,
            Map.of(Authorization.USERNAME, "user", Authorization.PASSWORD, "secret"), mock(Context.class));

        assertThat(applyResponse.getHeaders())
            .containsEntry("Authorization", List.of("Basic " + encode("user:secret")));
    }

    @Test
    void testExecuteAuthorizationCallbackOmitsClientSecretForPublicPkceClient() {
        stubTokenEndpoint();

        when(componentDefinitionRegistry.getAuthorization("mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE))
            .thenReturn(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        AuthorizationCallbackResponse authorizationCallbackResponse = Objects.requireNonNull(
            connectionDefinitionService.executeAuthorizationCallback(
                "mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, getCallbackParameters(null),
                "http://localhost/callback"));

        assertThat(authorizationCallbackResponse.result())
            .extractingByKey(Authorization.ACCESS_TOKEN)
            .isEqualTo("new-access");

        wireMock.verify(
            postRequestedFor(urlPathEqualTo("/token"))
                .withRequestBody(containing("client_id=public-client"))
                .withRequestBody(containing("code_verifier=test-verifier"))
                .withRequestBody(notContaining("client_secret")));
    }

    @Test
    void testExecuteAuthorizationCallbackSendsClientSecretForConfidentialClient() {
        stubTokenEndpoint();

        when(componentDefinitionRegistry.getAuthorization("mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE))
            .thenReturn(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        connectionDefinitionService.executeAuthorizationCallback(
            "mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, getCallbackParameters("top-secret"),
            "http://localhost/callback");

        wireMock.verify(
            postRequestedFor(urlPathEqualTo("/token"))
                .withRequestBody(containing("client_secret=top-secret")));
    }

    @Test
    void testExecuteRefreshOmitsClientSecretForPublicPkceClient() {
        stubTokenEndpoint();

        when(componentDefinitionRegistry.getAuthorization("mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE))
            .thenReturn(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE));

        RefreshTokenResponse refreshTokenResponse = connectionDefinitionService.executeRefresh(
            "mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE, getRefreshParameters(null),
            mock(Context.class));

        assertThat(refreshTokenResponse.accessToken()).isEqualTo("new-access");

        wireMock.verify(
            postRequestedFor(urlPathEqualTo("/token"))
                .withRequestBody(containing("grant_type=refresh_token"))
                .withRequestBody(notContaining("client_secret")));
    }

    @Test
    void testExecuteRefreshSendsClientSecretForConfidentialClient() {
        stubTokenEndpoint();

        when(componentDefinitionRegistry.getAuthorization("mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
            .thenReturn(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE));

        connectionDefinitionService.executeRefresh(
            "mcp", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE, getRefreshParameters("top-secret"),
            mock(Context.class));

        wireMock.verify(
            postRequestedFor(urlPathEqualTo("/token"))
                .withRequestBody(containing("client_secret=top-secret")));
    }

    private static void stubTokenEndpoint(String responseBody) {
        wireMock.stubFor(
            post(urlEqualTo("/token")).willReturn(
                aResponse().withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(responseBody)));
    }

    private static Map<String, Object> getCallbackParameters(String clientSecret) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(Authorization.CLIENT_ID, "public-client");
        parameters.put(Authorization.CODE, "auth-code");
        parameters.put("code_verifier", "test-verifier");
        parameters.put(Authorization.TOKEN_URL, wireMock.baseUrl() + "/token");

        if (clientSecret != null) {
            parameters.put(Authorization.CLIENT_SECRET, clientSecret);
        }

        return parameters;
    }

    private static Map<String, Object> getRefreshParameters(String clientSecret) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put(Authorization.CLIENT_ID, "public-client");
        parameters.put(Authorization.REFRESH_TOKEN, "old-refresh");
        parameters.put(Authorization.TOKEN_URL, wireMock.baseUrl() + "/token");

        if (clientSecret != null) {
            parameters.put(Authorization.CLIENT_SECRET, clientSecret);
        }

        return parameters;
    }

    private static void stubTokenEndpoint() {
        wireMock.stubFor(
            post(urlPathEqualTo("/token"))
                .willReturn(okJson("{\"access_token\":\"new-access\",\"refresh_token\":\"new-refresh\"}")));
    }

    private static String encode(String value) {
        Base64.Encoder encoder = Base64.getEncoder();

        return encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
