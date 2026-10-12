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
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
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

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private McpServerService mcpServerService;

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
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        fixtures.saveClient("client-1");
        fixtures.saveClient("client-2");
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveExpiredAuthorization("client-1", "bob");
        fixtures.saveExpiredAuthorization("client-2", "carol");

        assertThat(registeredClientService.getRegisteredClients())
            .extracting(RegisteredClientInfo::id, RegisteredClientInfo::grantCount, RegisteredClientInfo::deletable)
            .containsExactlyInAnyOrder(tuple("client-1", 1, true), tuple("client-2", 0, true));
    }

    @Test
    void testRegisteredClientFieldsAreMapped() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        fixtures.saveClient("client-1");

        List<RegisteredClientInfo> registeredClients = registeredClientService.getRegisteredClients();

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
    void testMultiTenantListsOnlyClientsWithActiveGrantsAndForbidsDelete() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);

        fixtures.saveClient("client-1");
        fixtures.saveClient("client-2");
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveExpiredAuthorization("client-2", "bob");

        assertThat(registeredClientService.getRegisteredClients())
            .extracting(RegisteredClientInfo::id, RegisteredClientInfo::deletable)
            .containsExactly(tuple("client-1", false));
        assertThatThrownBy(() -> registeredClientService.deleteRegisteredClient("client-1"))
            .isInstanceOf(ConfigurationException.class)
            .satisfies(exception -> assertThat(((ConfigurationException) exception).getErrorKey())
                .isEqualTo(RegisteredClientErrorType.DELETE_SHARED_REGISTERED_CLIENT.getErrorKey()));
        assertThat(registeredClientRepository.findById("client-1")).isNotNull();
    }

    @Test
    void testDeleteRemovesClientAuthorizationsAndConsents() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        fixtures.saveClient("client-1");
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent("client-1", "alice");

        registeredClientService.deleteRegisteredClient("client-1");

        assertThat(registeredClientRepository.findById("client-1")).isNull();
        assertThat(fixtures.countAuthorizations()).isZero();
        assertThat(fixtures.countConsents()).isZero();
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
}
