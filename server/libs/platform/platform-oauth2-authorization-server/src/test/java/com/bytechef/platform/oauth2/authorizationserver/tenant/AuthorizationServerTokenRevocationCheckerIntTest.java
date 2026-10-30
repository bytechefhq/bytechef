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

package com.bytechef.platform.oauth2.authorizationserver.tenant;

import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.ACME_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.GLOBEX_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.getDatabaseSchema;
import static com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures.SCOPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
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
        "bytechef.oauth2.authorization-server.enabled=true", "bytechef.edition=ee", "spring.liquibase.contexts=mono"
    })
@Import({
    MultiTenantSchemaIntTestConfiguration.class, PostgreSQLContainerConfiguration.class
})
class AuthorizationServerTokenRevocationCheckerIntTest {

    private static final String CLIENT_ID = "revocation-client";
    private static final String PUBLIC_SCHEMA = TenantContext.DEFAULT_TENANT_ID;

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired
    private AuthorizationServerTokenRevocationChecker authorizationServerTokenRevocationChecker;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OAuth2AuthorizationGrantService oAuth2AuthorizationGrantService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private TenantRoutingOAuth2AuthorizationService tenantRoutingOAuth2AuthorizationService;

    @MockitoBean
    private TenantService tenantService;

    private JdbcTemplate jdbcTemplate;
    private JdbcOAuth2AuthorizationService seedAuthorizationService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        seedAuthorizationService = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);

        RegisteredClientIntTestFixtures fixtures = new RegisteredClientIntTestFixtures(
            dataSource, registeredClientRepository, authorizationConsentService);

        deleteAllRows();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);

        fixtures.saveClient(CLIENT_ID);
    }

    @AfterEach
    void afterEach() {
        deleteAllRows();
    }

    @Test
    void testMissingAuthorizationIsRevoked() {
        String accessToken = "access-" + UUID.randomUUID();

        assertThat(authorizationServerTokenRevocationChecker.isRevoked(accessToken, ACME_TENANT_ID)).isTrue();
    }

    @Test
    void testAuthorizationRemovedByGrantRevocationIsRevoked() {
        OAuth2Authorization authorization = saveAuthorization(ACME_TENANT_ID);

        String accessToken = getAccessTokenValue(authorization);

        assertThat(authorizationServerTokenRevocationChecker.isRevoked(accessToken, ACME_TENANT_ID)).isFalse();

        TenantScope.run(ACME_TENANT_ID, () -> oAuth2AuthorizationGrantService.revokeGrant(CLIENT_ID, "alice"));

        assertThat(authorizationServerTokenRevocationChecker.isRevoked(accessToken, ACME_TENANT_ID)).isTrue();
    }

    @Test
    void testInvalidatedAccessTokenIsRevoked() {
        OAuth2Authorization authorization = saveAuthorization(ACME_TENANT_ID);

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();

        assertThat(accessToken).isNotNull();

        OAuth2Authorization invalidatedAuthorization = OAuth2Authorization.from(authorization)
            .invalidate(accessToken.getToken())
            .build();

        TenantScope.run(
            ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.save(invalidatedAuthorization));

        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isEqualTo(1);
        assertThat(
            authorizationServerTokenRevocationChecker.isRevoked(getAccessTokenValue(authorization), ACME_TENANT_ID))
                .isTrue();
    }

    @Test
    void testActiveAccessTokenIsNotRevoked() {
        OAuth2Authorization authorization = saveAuthorization(ACME_TENANT_ID);

        assertThat(
            authorizationServerTokenRevocationChecker.isRevoked(getAccessTokenValue(authorization), ACME_TENANT_ID))
                .isFalse();
    }

    @Test
    void testActiveAccessTokenOfAnotherTenantIsRevoked() {
        OAuth2Authorization authorization = saveAuthorization(ACME_TENANT_ID);

        assertThat(
            authorizationServerTokenRevocationChecker.isRevoked(getAccessTokenValue(authorization), GLOBEX_TENANT_ID))
                .isTrue();
    }

    private OAuth2Authorization saveAuthorization(String tenantId) {
        RegisteredClient registeredClient = registeredClientRepository.findById(CLIENT_ID);

        if (registeredClient == null) {
            throw new IllegalStateException("Unknown registered client " + CLIENT_ID);
        }

        Instant now = Instant.now();

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
            .id(UUID.randomUUID()
                .toString())
            .principalName("alice")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of(SCOPE))
            .token(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-" + UUID.randomUUID(), now, now.plus(Duration.ofHours(1))))
            .build();

        TenantScope.run(tenantId, () -> seedAuthorizationService.save(authorization));

        return authorization;
    }

    private static String getAccessTokenValue(OAuth2Authorization authorization) {
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();

        if (accessToken == null) {
            throw new IllegalStateException("Authorization has no access token");
        }

        OAuth2AccessToken token = accessToken.getToken();

        return token.getTokenValue();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private int countRows(String schema, String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + "." + table, Integer.class);

        return count == null ? 0 : count;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void deleteAllRows() {
        for (String schema : List.of(
            PUBLIC_SCHEMA, getDatabaseSchema(ACME_TENANT_ID), getDatabaseSchema(GLOBEX_TENANT_ID))) {

            jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization_consent");
            jdbcTemplate.update("DELETE FROM " + schema + ".oauth2_authorization");
        }

        jdbcTemplate.update("DELETE FROM " + PUBLIC_SCHEMA + ".oauth2_registered_client");
    }
}
