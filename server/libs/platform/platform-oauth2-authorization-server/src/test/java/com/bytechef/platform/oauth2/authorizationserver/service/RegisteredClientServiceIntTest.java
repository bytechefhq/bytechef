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

package com.bytechef.platform.oauth2.authorizationserver.service;

import static com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures.MANAGEMENT_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures;
import com.bytechef.platform.oauth2.authorizationserver.domain.ClientRegistrationRequest;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import com.bytechef.platform.oauth2.authorizationserver.registration.AppClients;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = RegisteredClientIntTestConfiguration.class,
    properties = {
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee"
    })
@Import(PostgreSQLContainerConfiguration.class)
class RegisteredClientServiceIntTest {

    private static final String APP_REDIRECT_URI = "https://app.example.com/auth/callback";
    private static final Set<String> APP_SCOPES = Set.of("openid", "profile", "app:use");

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private RegisteredClientService registeredClientService;

    @MockitoBean
    private TenantService tenantService;

    private RegisteredClientIntTestFixtures fixtures;

    @BeforeEach
    void beforeEach() {
        fixtures = new RegisteredClientIntTestFixtures(
            dataSource, registeredClientRepository, authorizationConsentService);

        fixtures.deleteAll();
    }

    @AfterEach
    void afterEach() {
        fixtures.deleteAll();
    }

    @Test
    void testSingleTenantListsAllClientsWithActiveGrantCounts() {
        fixtures.saveClient("client-1");
        fixtures.saveClient("client-2");
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveExpiredAuthorization("client-1", "bob");
        fixtures.saveExpiredAuthorization("client-2", "carol");

        assertThat(registeredClientService.getRegisteredClients(true))
            .extracting(RegisteredClientInfo::id, RegisteredClientInfo::grantCount, RegisteredClientInfo::deletable)
            .containsExactlyInAnyOrder(tuple("client-1", 1, true), tuple("client-2", 0, true));
    }

    @Test
    void testRegisteredClientFieldsAreMapped() {
        fixtures.saveClient("client-1");

        List<RegisteredClientInfo> registeredClients = registeredClientService.getRegisteredClients(true);

        assertThat(registeredClients).singleElement()
            .satisfies(registeredClient -> {
                assertThat(registeredClient.clientId()).isEqualTo("client-1-client-id");
                assertThat(registeredClient.clientName()).isEqualTo("client-1 name");
                assertThat(registeredClient.clientIdIssuedAt()).isNotNull();
                assertThat(registeredClient.scopes()).containsExactly(RegisteredClientIntTestFixtures.SCOPE);
                assertThat(registeredClient.authorizationGrantTypes())
                    .containsExactlyInAnyOrder("authorization_code", "refresh_token");
                assertThat(registeredClient.redirectUris()).containsExactly("http://127.0.0.1/callback");
            });
    }

    @Test
    void testRegisteredClientsListEveryClientWithTheGivenDeletableFlag() {
        fixtures.saveClient("client-1");
        fixtures.saveClient("client-2");
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveExpiredAuthorization("client-2", "bob");

        assertThat(registeredClientService.getRegisteredClients(false))
            .extracting(RegisteredClientInfo::id, RegisteredClientInfo::grantCount, RegisteredClientInfo::deletable)
            .containsExactlyInAnyOrder(tuple("client-1", 1, false), tuple("client-2", 0, false));
    }

    @Test
    void testDeleteRemovesClientAuthorizationsAndConsents() {
        fixtures.saveClient("client-1");
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent("client-1", "alice");

        assertThat(registeredClientService.deleteRegisteredClient("client-1")).isTrue();

        assertThat(registeredClientRepository.findById("client-1")).isNull();
        assertThat(fixtures.countAuthorizations()).isZero();
        assertThat(fixtures.countConsents()).isZero();
    }

    @Test
    void testDeleteRefusesAClientTheServerRegisteredForAnAppInstance() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        String clientId = createAppClientId();

        String id = registeredClientService.registerClient(
            new ClientRegistrationRequest(clientId, "App", List.of(APP_REDIRECT_URI), APP_SCOPES, false));

        fixtures.saveAuthorization(id, "alice", MANAGEMENT_URL, Instant.now());

        assertConfigurationException(
            () -> registeredClientService.deleteRegisteredClient(id),
            RegisteredClientErrorType.REGISTERED_CLIENT_MANAGED_BY_SERVER);

        assertThat(registeredClientRepository.findByClientId(clientId)).isNotNull();
        assertThat(fixtures.countAuthorizations()).isEqualTo(1);
    }

    @Test
    void testDeleteOfAMissingClientReportsThatNothingWasDeleted() {
        assertThat(registeredClientService.deleteRegisteredClient("missing-client")).isFalse();
    }

    @Test
    void testConsentClientExposesNameAndRedirectHosts() {
        RegisteredClient registeredClient = RegisteredClient.withId("client-1")
            .clientId("client-1-client-id")
            .clientName("Claude")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://claude.ai/api/mcp/auth_callback")
            .redirectUri("https://claude.ai/other_callback")
            .redirectUri("http://localhost:6274/oauth/callback")
            .redirectUri("cursor://anysphere.cursor-retrieval/oauth/callback")
            .scope(RegisteredClientIntTestFixtures.SCOPE)
            .build();

        registeredClientRepository.save(registeredClient);

        assertThat(registeredClientService.fetchConsentClient("client-1-client-id")).hasValueSatisfying(
            consentClient -> {
                assertThat(consentClient.clientId()).isEqualTo("client-1-client-id");
                assertThat(consentClient.clientName()).isEqualTo("Claude");
                assertThat(consentClient.redirectHosts())
                    .containsExactlyInAnyOrder("claude.ai", "localhost", "anysphere.cursor-retrieval");
            });
        assertThat(registeredClientService.fetchConsentClient("unknown-client-id")).isEmpty();
    }

    @Test
    void testRegisterClientIsPublicWithProofKey() {
        String clientId = createAppClientId();

        String id = registeredClientService.registerClient(
            new ClientRegistrationRequest(
                clientId, "Expense Tracker", List.of(APP_REDIRECT_URI, "http://localhost:5173/auth/callback"),
                APP_SCOPES, false));

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        assertThat(registeredClient).isNotNull();
        assertThat(registeredClient.getId()).isEqualTo(id);
        assertThat(registeredClient.getClientName()).isEqualTo("Expense Tracker");
        assertThat(registeredClient.getClientIdIssuedAt()).isNotNull();
        assertThat(registeredClient.getClientSecret()).isNull();
        assertThat(registeredClient.getClientAuthenticationMethods())
            .containsExactly(ClientAuthenticationMethod.NONE);
        assertThat(registeredClient.getAuthorizationGrantTypes())
            .containsExactlyInAnyOrder(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN);
        assertThat(registeredClient.getRedirectUris())
            .containsExactlyInAnyOrder(APP_REDIRECT_URI, "http://localhost:5173/auth/callback");
        assertThat(registeredClient.getScopes()).containsExactlyInAnyOrderElementsOf(APP_SCOPES);

        ClientSettings clientSettings = registeredClient.getClientSettings();

        assertThat(clientSettings.isRequireProofKey()).isTrue();
        assertThat(clientSettings.isRequireAuthorizationConsent()).isFalse();

        TokenSettings tokenSettings = registeredClient.getTokenSettings();

        assertThat(tokenSettings.isReuseRefreshTokens()).isFalse();
    }

    @Test
    void testRegisterClientRefusesAClientIdThatIsTaken() {
        String clientId = createAppClientId();

        registeredClientService.registerClient(
            new ClientRegistrationRequest(clientId, "First", List.of(APP_REDIRECT_URI), APP_SCOPES, false));

        assertConfigurationException(
            () -> registeredClientService.registerClient(
                new ClientRegistrationRequest(
                    clientId, "Second", List.of("https://other.example.com/auth/callback"), APP_SCOPES, false)),
            RegisteredClientErrorType.CLIENT_ID_ALREADY_REGISTERED);

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        assertThat(registeredClient).isNotNull();
        assertThat(registeredClient.getClientName()).isEqualTo("First");
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "http://app.example.com/auth/callback", "https://app.example.com/auth/callback#fragment",
            "https://*.example.com/auth/callback", "https://app.example.com/*", "/auth/callback",
            "ftp://app.example.com/auth/callback", "custom-scheme://auth/callback",
            "http://localhost.example.com/auth/callback", "https:///auth/callback", "not a uri"
        })
    void testRegisterClientRefusesAnInvalidRedirectUri(String redirectUri) {
        String clientId = createAppClientId();

        assertConfigurationException(
            () -> registeredClientService.registerClient(
                new ClientRegistrationRequest(clientId, "App", List.of(redirectUri), APP_SCOPES, false)),
            RegisteredClientErrorType.INVALID_REDIRECT_URI);

        assertThat(registeredClientRepository.findByClientId(clientId)).isNull();
    }

    @Test
    void testRegisterClientRefusesAnEmptyRedirectUriList() {
        assertConfigurationException(
            () -> registeredClientService.registerClient(
                new ClientRegistrationRequest(createAppClientId(), "App", List.of(), APP_SCOPES, false)),
            RegisteredClientErrorType.INVALID_REDIRECT_URI);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "http://localhost:3000/auth/callback", "http://127.0.0.1:3000/auth/callback",
            "http://[::1]:3000/auth/callback", "https://app.example.com:8443/auth/callback?tenant=acme"
        })
    void testRegisterClientAcceptsHttpsAndLoopbackHttpRedirectUris(String redirectUri) {
        String clientId = createAppClientId();

        registeredClientService.registerClient(
            new ClientRegistrationRequest(clientId, "App", List.of(redirectUri), APP_SCOPES, false));

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        assertThat(registeredClient).isNotNull();
        assertThat(registeredClient.getRedirectUris()).containsExactly(redirectUri);
    }

    @Test
    void testRegisterClientRefusesTheAppScopeMixedWithAnMcpScope() {
        String clientId = createAppClientId();

        assertConfigurationException(
            () -> registeredClientService.registerClient(
                new ClientRegistrationRequest(
                    clientId, "App", List.of(APP_REDIRECT_URI), Set.of("openid", "app:use", "mcp:management"),
                    false)),
            RegisteredClientErrorType.INVALID_SCOPE);

        assertThat(registeredClientRepository.findByClientId(clientId)).isNull();
    }

    @Test
    void testUpdateClientReplacesRedirectUris() {
        String clientId = createAppClientId();

        String id = registeredClientService.registerClient(
            new ClientRegistrationRequest(
                clientId, "Expense Tracker", List.of(APP_REDIRECT_URI, "http://localhost:5173/auth/callback"),
                APP_SCOPES, false));

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        assertThat(registeredClient).isNotNull();

        Instant clientIdIssuedAt = registeredClient.getClientIdIssuedAt();

        registeredClientService.updateClient(
            new ClientRegistrationRequest(
                clientId, "Expense Tracker 2", List.of("https://expenses.example.com/auth/callback"), APP_SCOPES,
                true));

        RegisteredClient updatedRegisteredClient = registeredClientRepository.findByClientId(clientId);

        assertThat(updatedRegisteredClient).isNotNull();
        assertThat(updatedRegisteredClient.getId()).isEqualTo(id);
        assertThat(updatedRegisteredClient.getClientIdIssuedAt()).isEqualTo(clientIdIssuedAt);
        assertThat(updatedRegisteredClient.getClientName()).isEqualTo("Expense Tracker 2");
        assertThat(updatedRegisteredClient.getRedirectUris())
            .containsExactly("https://expenses.example.com/auth/callback");
        assertThat(updatedRegisteredClient.getClientAuthenticationMethods())
            .containsExactly(ClientAuthenticationMethod.NONE);

        ClientSettings clientSettings = updatedRegisteredClient.getClientSettings();

        assertThat(clientSettings.isRequireAuthorizationConsent()).isTrue();
        assertThat(clientSettings.isRequireProofKey()).isTrue();
    }

    @Test
    void testUpdateClientRefusesAnInvalidRedirectUri() {
        String clientId = createAppClientId();

        registeredClientService.registerClient(
            new ClientRegistrationRequest(clientId, "App", List.of(APP_REDIRECT_URI), APP_SCOPES, false));

        assertConfigurationException(
            () -> registeredClientService.updateClient(
                new ClientRegistrationRequest(
                    clientId, "App", List.of("http://app.example.com/auth/callback"), APP_SCOPES, false)),
            RegisteredClientErrorType.INVALID_REDIRECT_URI);

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        assertThat(registeredClient).isNotNull();
        assertThat(registeredClient.getRedirectUris()).containsExactly(APP_REDIRECT_URI);
    }

    @Test
    void testUpdateClientRefusesTheAppScopeMixedWithAnMcpScope() {
        String clientId = createAppClientId();

        registeredClientService.registerClient(
            new ClientRegistrationRequest(clientId, "App", List.of(APP_REDIRECT_URI), APP_SCOPES, false));

        assertConfigurationException(
            () -> registeredClientService.updateClient(
                new ClientRegistrationRequest(
                    clientId, "App", List.of(APP_REDIRECT_URI), Set.of("app:use", "mcp:automation"), false)),
            RegisteredClientErrorType.INVALID_SCOPE);

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        assertThat(registeredClient).isNotNull();
        assertThat(registeredClient.getScopes()).containsExactlyInAnyOrderElementsOf(APP_SCOPES);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "mcp:automation", "openid app:use", "openid profile app:use email"
    })
    void testUpdateClientCannotChangeTheScopes(String requestedScopes) {
        String clientId = createAppClientId();

        registeredClientService.registerClient(
            new ClientRegistrationRequest(clientId, "App", List.of(APP_REDIRECT_URI), APP_SCOPES, false));

        assertConfigurationException(
            () -> registeredClientService.updateClient(
                new ClientRegistrationRequest(
                    clientId, "Renamed", List.of("https://other.example.com/auth/callback"),
                    Set.of(requestedScopes.split(" ")), true)),
            RegisteredClientErrorType.SCOPES_NOT_UPDATABLE);

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        assertThat(registeredClient).isNotNull();
        assertThat(registeredClient.getScopes()).containsExactlyInAnyOrderElementsOf(APP_SCOPES);
        assertThat(registeredClient.getClientName()).isEqualTo("App");
        assertThat(registeredClient.getRedirectUris()).containsExactly(APP_REDIRECT_URI);
        assertThat(AppClients.isAppClient(registeredClient)).isTrue();
    }

    @Test
    void testUpdateClientRefusesAClientThatWasNotRegisteredThroughTheService() {
        fixtures.saveClient("client-1");

        assertConfigurationException(
            () -> registeredClientService.updateClient(
                new ClientRegistrationRequest(
                    "client-1-client-id", "Renamed", List.of(APP_REDIRECT_URI), APP_SCOPES, false)),
            RegisteredClientErrorType.REGISTERED_CLIENT_NOT_UPDATABLE);

        RegisteredClient registeredClient = registeredClientRepository.findById("client-1");

        assertThat(registeredClient).isNotNull();
        assertThat(registeredClient.getClientName()).isEqualTo("client-1 name");
        assertThat(registeredClient.getScopes()).containsExactly(RegisteredClientIntTestFixtures.SCOPE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {
        false, true
    })
    void testDeleteClientRemovesAClientRegisteredThroughTheServiceWithItsAuthorizations(boolean multiTenantEnabled) {
        when(tenantService.isMultiTenantEnabled()).thenReturn(multiTenantEnabled);

        String clientId = createAppClientId();

        String id = registeredClientService.registerClient(
            new ClientRegistrationRequest(clientId, "App", List.of(APP_REDIRECT_URI), APP_SCOPES, false));

        fixtures.saveAuthorization(id, "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent(id, "alice");

        assertThat(registeredClientService.deleteClient(clientId)).isTrue();

        assertThat(registeredClientRepository.findByClientId(clientId)).isNull();
        assertThat(fixtures.countAuthorizations()).isZero();
        assertThat(fixtures.countConsents()).isZero();
    }

    @Test
    void testDeleteClientRefusesAClientThatWasNotRegisteredThroughTheService() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        fixtures.saveClient("client-1");
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());

        assertConfigurationException(
            () -> registeredClientService.deleteClient("client-1-client-id"),
            RegisteredClientErrorType.REGISTERED_CLIENT_NOT_DELETABLE);

        assertThat(registeredClientRepository.findById("client-1")).isNotNull();
        assertThat(fixtures.countAuthorizations()).isEqualTo(1);
    }

    @Test
    void testDeleteClientOfAnUnknownClientReportsThatNothingWasDeleted() {
        assertThat(registeredClientService.deleteClient(createAppClientId())).isFalse();
    }

    @Test
    void testFetchRegisteredClientIdResolvesTheClientId() {
        String clientId = createAppClientId();

        String id = registeredClientService.registerClient(
            new ClientRegistrationRequest(clientId, "App", List.of(APP_REDIRECT_URI), APP_SCOPES, false));

        assertThat(registeredClientService.fetchRegisteredClientId(clientId)).contains(id);
        assertThat(registeredClientService.fetchRegisteredClientId(createAppClientId())).isEmpty();
    }

    @Test
    void testUpdateClientRefusesAnUnknownClient() {
        assertConfigurationException(
            () -> registeredClientService.updateClient(
                new ClientRegistrationRequest(createAppClientId(), "App", List.of(APP_REDIRECT_URI), APP_SCOPES,
                    false)),
            RegisteredClientErrorType.REGISTERED_CLIENT_NOT_FOUND);
    }

    private static void assertConfigurationException(
        Runnable runnable, RegisteredClientErrorType registeredClientErrorType) {

        assertThatThrownBy(runnable::run)
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(registeredClientErrorType.getErrorKey()));
    }

    private static String createAppClientId() {
        return "app-" + UUID.randomUUID();
    }
}
