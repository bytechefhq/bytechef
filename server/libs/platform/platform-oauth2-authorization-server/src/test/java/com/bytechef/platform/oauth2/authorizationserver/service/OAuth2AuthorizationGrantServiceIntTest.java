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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2AuthorizationGrant;
import com.bytechef.platform.oauth2.authorizationserver.exception.RegisteredClientErrorType;
import com.bytechef.platform.security.web.mcp.McpEndpoint;
import com.bytechef.platform.security.web.mcp.TestMcpEndpoint;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
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
@Import({
    OAuth2AuthorizationGrantServiceIntTest.ResourceNamingMcpEndpointConfiguration.class,
    PostgreSQLContainerConfiguration.class
})
class OAuth2AuthorizationGrantServiceIntTest {

    private static final String AUTOMATION_URL = "http://localhost/api/automation/sales-automation-secret/mcp";
    private static final String UNKNOWN_URL = "http://localhost/api/automation/unknown-secret/mcp";

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OAuth2AuthorizationGrantService oAuth2AuthorizationGrantService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @MockitoBean
    private TenantService tenantService;

    private RegisteredClientIntTestFixtures fixtures;

    @BeforeEach
    void beforeEach() {
        fixtures = new RegisteredClientIntTestFixtures(
            dataSource, registeredClientRepository, authorizationConsentService);

        fixtures.deleteAll();
        fixtures.saveClient("client-1");
    }

    @AfterEach
    void afterEach() {
        fixtures.deleteAll();
    }

    @Test
    void testGrantsAreGroupedByPrincipal() {
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.parse("2026-10-01T00:00:00Z"));
        fixtures.saveAuthorization("client-1", "alice", AUTOMATION_URL, Instant.parse("2026-10-02T00:00:00Z"));
        fixtures.saveAuthorization("client-1", "bob", MANAGEMENT_URL, Instant.parse("2026-10-03T00:00:00Z"));

        List<OAuth2AuthorizationGrant> grants = oAuth2AuthorizationGrantService.getGrants("client-1");

        assertThat(grants).extracting(OAuth2AuthorizationGrant::principalName)
            .containsExactly("bob", "alice");

        OAuth2AuthorizationGrant aliceGrant = grants.get(1);

        assertThat(aliceGrant.mcpServers()).containsExactlyInAnyOrder("Management", "Sales Automation");
        assertThat(aliceGrant.scopes()).containsExactly(RegisteredClientIntTestFixtures.SCOPE);
        assertThat(aliceGrant.authorizedAt()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(aliceGrant.lastIssuedAt()).isEqualTo(Instant.parse("2026-10-02T00:00:00Z"));
        assertThat(aliceGrant.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void testUnresolvableResourceIsShownAsRawUrl() {
        fixtures.saveAuthorization("client-1", "alice", UNKNOWN_URL, Instant.parse("2026-10-01T00:00:00Z"));

        List<OAuth2AuthorizationGrant> grants = oAuth2AuthorizationGrantService.getGrants("client-1");

        assertThat(grants).singleElement()
            .extracting(OAuth2AuthorizationGrant::mcpServers)
            .isEqualTo(List.of(UNKNOWN_URL));
    }

    @Test
    void testExpiredGrantsAreNotListed() {
        fixtures.saveExpiredAuthorization("client-1", "carol");

        assertThat(oAuth2AuthorizationGrantService.getGrants("client-1")).isEmpty();
    }

    @Test
    void testExpiredAuthorizationsAreNotLoaded() {
        fixtures.saveExpiredAuthorization("client-1", "carol");
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("UPDATE oauth2_authorization SET attributes = 'unreadable' WHERE principal_name = 'carol'");

        assertThat(oAuth2AuthorizationGrantService.getGrants("client-1"))
            .extracting(OAuth2AuthorizationGrant::principalName)
            .containsExactly("alice");
    }

    @Test
    void testRevokeGrantRemovesAuthorizationsAndConsent() {
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent("client-1", "alice");
        fixtures.saveAuthorization("client-1", "bob", MANAGEMENT_URL, Instant.now());

        oAuth2AuthorizationGrantService.revokeGrant("client-1", "alice");

        assertThat(oAuth2AuthorizationGrantService.getGrants("client-1"))
            .extracting(OAuth2AuthorizationGrant::principalName)
            .containsExactly("bob");
        assertThat(authorizationConsentService.findById("client-1", "alice")).isNull();
    }

    @Test
    void testRevokeGrantWithNothingToRevokeFails() {
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());

        assertThatExceptionOfType(ConfigurationException.class)
            .isThrownBy(() -> oAuth2AuthorizationGrantService.revokeGrant("client-1", "nobody"))
            .satisfies(exception -> assertThat(exception.getErrorKey())
                .isEqualTo(RegisteredClientErrorType.GRANT_NOT_FOUND.getErrorKey()));

        assertThat(fixtures.countAuthorizations()).isEqualTo(1);
    }

    @Test
    void testRevokeGrantsWithNothingToRevokeFails() {
        assertThatExceptionOfType(ConfigurationException.class)
            .isThrownBy(() -> oAuth2AuthorizationGrantService.revokeGrants("client-1"))
            .satisfies(exception -> assertThat(exception.getErrorKey())
                .isEqualTo(RegisteredClientErrorType.GRANT_NOT_FOUND.getErrorKey()));
    }

    @Test
    void testRevokeGrantOfAPrincipalWithOnlyAConsentSucceeds() {
        fixtures.saveConsent("client-1", "alice");

        oAuth2AuthorizationGrantService.revokeGrant("client-1", "alice");

        assertThat(authorizationConsentService.findById("client-1", "alice")).isNull();
    }

    @Test
    void testRevokeGrantLeavesTheConsentOfAnotherPrincipal() {
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent("client-1", "alice");
        fixtures.saveAuthorization("client-1", "bob", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent("client-1", "bob");

        oAuth2AuthorizationGrantService.revokeGrant("client-1", "alice");

        assertThat(authorizationConsentService.findById("client-1", "bob")).isNotNull();
        assertThat(fixtures.countConsents()).isEqualTo(1);
    }

    @Test
    void testRevokeGrantsLeavesTheRowsOfAnotherClient() {
        fixtures.saveClient("client-2");
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent("client-1", "alice");
        fixtures.saveAuthorization("client-2", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent("client-2", "alice");

        oAuth2AuthorizationGrantService.revokeGrants("client-1");

        assertThat(oAuth2AuthorizationGrantService.getGrants("client-2"))
            .extracting(OAuth2AuthorizationGrant::principalName)
            .containsExactly("alice");
        assertThat(authorizationConsentService.findById("client-2", "alice")).isNotNull();
        assertThat(fixtures.countAuthorizations()).isEqualTo(1);
        assertThat(fixtures.countConsents()).isEqualTo(1);
    }

    @Test
    void testRevokeGrantsRemovesAllAuthorizationsAndConsentsOfTheClient() {
        fixtures.saveAuthorization("client-1", "alice", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent("client-1", "alice");
        fixtures.saveAuthorization("client-1", "bob", MANAGEMENT_URL, Instant.now());
        fixtures.saveConsent("client-1", "bob");

        oAuth2AuthorizationGrantService.revokeGrants("client-1");

        assertThat(fixtures.countAuthorizations()).isZero();
        assertThat(fixtures.countConsents()).isZero();
    }

    @TestConfiguration
    static class ResourceNamingMcpEndpointConfiguration {

        @Bean
        McpEndpoint resourceNamingMcpEndpoint() {
            McpEndpoint delegate = TestMcpEndpoint.automation();

            return new McpEndpoint() {

                @Override
                public boolean matches(String path) {
                    return delegate.matches(path);
                }

                @Override
                public String getScope() {
                    return delegate.getScope();
                }

                @Override
                public Optional<String> resolveTenantId(String path) {
                    return delegate.resolveTenantId(path);
                }

                @Override
                public Optional<String> resolveResourceName(String resource) {
                    return switch (resource) {
                        case MANAGEMENT_URL -> Optional.of("Management");
                        case AUTOMATION_URL -> Optional.of("Sales Automation");
                        case UNKNOWN_URL -> throw new IllegalArgumentException("Unknown MCP server");
                        default -> Optional.empty();
                    };
                }
            };
        }
    }
}
