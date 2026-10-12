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

package com.bytechef.platform.oauth2.authorizationserver;

import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.ACME_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.GLOBEX_TENANT_ID;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.TENANT_IDS;
import static com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration.getDatabaseSchema;
import static com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures.MANAGEMENT_URL;
import static com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures.SCOPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.oauth2.authorizationserver.config.MultiTenantSchemaIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestConfiguration;
import com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures;
import com.bytechef.platform.oauth2.authorizationserver.domain.OAuth2AuthorizationGrant;
import com.bytechef.platform.oauth2.authorizationserver.domain.RegisteredClientInfo;
import com.bytechef.platform.oauth2.authorizationserver.service.OAuth2AuthorizationGrantService;
import com.bytechef.platform.oauth2.authorizationserver.service.RegisteredClientService;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantKeyedTokenValues;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantRoutingOAuth2AuthorizationService;
import com.bytechef.platform.oauth2.authorizationserver.tenant.TenantScope;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Proves the multi-tenant placement of the authorization server tables against a schema-per-tenant PostgreSQL database:
 * registered clients stay in the shared {@code public} schema, while authorizations and consents are written to and
 * read from the schema of the current tenant (or of the tenant carried by a keyed token value).
 *
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
class MultiTenantOAuth2AuthorizationIntTest {

    private static final String CLIENT_ID = "shared-client";
    private static final String OTHER_CLIENT_ID = "globex-only-client";
    private static final String PUBLIC_SCHEMA = TenantContext.DEFAULT_TENANT_ID;

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private McpServerService mcpServerService;

    @Autowired
    private OAuth2AuthorizationGrantService oAuth2AuthorizationGrantService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private RegisteredClientService registeredClientService;

    @Autowired
    private TenantRoutingOAuth2AuthorizationService tenantRoutingOAuth2AuthorizationService;

    @MockitoBean
    private TenantService tenantService;

    private RegisteredClientIntTestFixtures fixtures;
    private JdbcTemplate jdbcTemplate;
    private JdbcOAuth2AuthorizationService seedAuthorizationService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        seedAuthorizationService = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        fixtures = new RegisteredClientIntTestFixtures(
            dataSource, registeredClientRepository, authorizationConsentService);

        deleteAllRows();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);

        TenantScope.run(ACME_TENANT_ID, () -> fixtures.saveClient(CLIENT_ID));
        TenantScope.run(GLOBEX_TENANT_ID, () -> fixtures.saveClient(OTHER_CLIENT_ID));
    }

    @AfterEach
    void afterEach() {
        deleteAllRows();
    }

    @Test
    void testRegisteredClientsAreStoredInAndReadFromThePublicSchema() {
        assertThat(countRows(PUBLIC_SCHEMA, "oauth2_registered_client")).isEqualTo(2);

        for (String tenantId : TENANT_IDS) {
            assertThat(tableExists(getDatabaseSchema(tenantId), "oauth2_registered_client")).isFalse();
            assertThat(tableExists(getDatabaseSchema(tenantId), "oauth2_authorization")).isTrue();
            assertThat(tableExists(getDatabaseSchema(tenantId), "oauth2_authorization_consent")).isTrue();

            RegisteredClient registeredClient = TenantScope.call(
                tenantId, () -> registeredClientRepository.findByClientId(CLIENT_ID + "-client-id"));

            assertThat(registeredClient).isNotNull();
            assertThat(registeredClient.getId()).isEqualTo(CLIENT_ID);
        }
    }

    @Test
    void testConsentClientIsReadFromThePublicSchemaInEveryTenant() {
        for (String tenantId : TENANT_IDS) {
            assertThat(TenantScope.call(
                tenantId, () -> registeredClientService.fetchConsentClient(CLIENT_ID + "-client-id")))
                    .hasValueSatisfying(consentClient -> assertThat(consentClient.clientName())
                        .isEqualTo(CLIENT_ID + " name"));
        }
    }

    @Test
    void testAuthorizationsAndConsentsAreStoredInTheCurrentTenantSchema() {
        String authorizationId = TenantScope.call(ACME_TENANT_ID, () -> {
            fixtures.saveConsent(CLIENT_ID, "alice");

            return saveAuthorization(CLIENT_ID, "alice", ACME_TENANT_ID).getId();
        });

        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isEqualTo(1);
        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization_consent")).isEqualTo(1);
        assertThat(countRows(getDatabaseSchema(GLOBEX_TENANT_ID), "oauth2_authorization")).isZero();
        assertThat(countRows(getDatabaseSchema(GLOBEX_TENANT_ID), "oauth2_authorization_consent")).isZero();
        assertThat(countRows(PUBLIC_SCHEMA, "oauth2_authorization")).isZero();
        assertThat(countRows(PUBLIC_SCHEMA, "oauth2_authorization_consent")).isZero();

        assertThat(TenantScope.call(ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findById(
            authorizationId))).isNotNull();
        assertThat(TenantScope.call(ACME_TENANT_ID, () -> authorizationConsentService.findById(CLIENT_ID, "alice")))
            .isNotNull();
        assertThat(TenantScope.call(GLOBEX_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findById(
            authorizationId))).isNull();
        assertThat(TenantScope.call(GLOBEX_TENANT_ID, () -> authorizationConsentService.findById(CLIENT_ID, "alice")))
            .isNull();
        assertThat(tenantRoutingOAuth2AuthorizationService.findById(authorizationId)).isNull();
    }

    @Test
    void testRevokingAGrantInOneTenantLeavesTheOtherTenantGrantIntact() {
        for (String tenantId : TENANT_IDS) {
            TenantScope.run(tenantId, () -> {
                saveAuthorization(CLIENT_ID, "alice", tenantId);
                fixtures.saveConsent(CLIENT_ID, "alice");
            });
        }

        TenantScope.run(ACME_TENANT_ID, () -> oAuth2AuthorizationGrantService.revokeGrant(CLIENT_ID, "alice"));

        assertThat(TenantScope.call(ACME_TENANT_ID, () -> oAuth2AuthorizationGrantService.getGrants(CLIENT_ID)))
            .isEmpty();
        assertThat(TenantScope.call(ACME_TENANT_ID, () -> authorizationConsentService.findById(CLIENT_ID, "alice")))
            .isNull();

        assertThat(TenantScope.call(GLOBEX_TENANT_ID, () -> oAuth2AuthorizationGrantService.getGrants(CLIENT_ID)))
            .extracting(OAuth2AuthorizationGrant::principalName)
            .containsExactly("alice");
        assertThat(TenantScope.call(GLOBEX_TENANT_ID, () -> authorizationConsentService.findById(CLIENT_ID, "alice")))
            .isNotNull();
        assertThat(countRows(getDatabaseSchema(GLOBEX_TENANT_ID), "oauth2_authorization")).isEqualTo(1);
    }

    @Test
    void testRevokingAllGrantsInOneTenantLeavesTheOtherTenantGrantsIntact() {
        for (String tenantId : TENANT_IDS) {
            TenantScope.run(tenantId, () -> {
                saveAuthorization(CLIENT_ID, "alice", tenantId);
                saveAuthorization(CLIENT_ID, "bob", tenantId);
                fixtures.saveConsent(CLIENT_ID, "alice");
                fixtures.saveConsent(CLIENT_ID, "bob");
            });
        }

        TenantScope.run(ACME_TENANT_ID, () -> oAuth2AuthorizationGrantService.revokeGrants(CLIENT_ID));

        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isZero();
        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization_consent")).isZero();

        assertThat(countRows(getDatabaseSchema(GLOBEX_TENANT_ID), "oauth2_authorization")).isEqualTo(2);
        assertThat(countRows(getDatabaseSchema(GLOBEX_TENANT_ID), "oauth2_authorization_consent")).isEqualTo(2);
        assertThat(TenantScope.call(GLOBEX_TENANT_ID, () -> oAuth2AuthorizationGrantService.getGrants(CLIENT_ID)))
            .extracting(OAuth2AuthorizationGrant::principalName)
            .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void testTokensIssuedForAnAuthorizationRevokedMidExchangeAreNotPersisted() {
        OAuth2Authorization authorization = TenantScope.call(
            ACME_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "alice", ACME_TENANT_ID));

        Instant now = Instant.now();

        OAuth2Authorization refreshedAuthorization = OAuth2Authorization.from(authorization)
            .token(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "refreshed-access-" + UUID.randomUUID(), now,
                now.plus(Duration.ofHours(1))))
            .refreshToken(new OAuth2RefreshToken(
                TenantKeyedTokenValues.generate(ACME_TENANT_ID), now, now.plus(Duration.ofHours(1))))
            .build();

        TenantScope.run(ACME_TENANT_ID, () -> oAuth2AuthorizationGrantService.revokeGrants(CLIENT_ID));

        assertThatExceptionOfType(OAuth2AuthenticationException.class)
            .isThrownBy(() -> TenantScope.run(
                ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.save(refreshedAuthorization)))
            .satisfies(exception -> assertThat(exception.getError()
                .getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_GRANT));

        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isZero();
    }

    @Test
    void testNewAuthorizationsAndTokenIssuanceForAnExistingAuthorizationArePersisted() {
        RegisteredClient registeredClient = registeredClientRepository.findById(CLIENT_ID);

        assertThat(registeredClient).isNotNull();

        Instant now = Instant.now();

        OAuth2Authorization codeAuthorization = OAuth2Authorization.withRegisteredClient(registeredClient)
            .id(UUID.randomUUID()
                .toString())
            .principalName("alice")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of(SCOPE))
            .token(new OAuth2AuthorizationCode(
                TenantKeyedTokenValues.generate(ACME_TENANT_ID), now, now.plus(Duration.ofMinutes(5))))
            .build();

        TenantScope.run(ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.save(codeAuthorization));

        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode = Objects.requireNonNull(
            codeAuthorization.getToken(OAuth2AuthorizationCode.class));

        OAuth2Authorization exchangedAuthorization = OAuth2Authorization.from(codeAuthorization)
            .invalidate(authorizationCode.getToken())
            .token(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-" + UUID.randomUUID(), now, now.plus(Duration.ofHours(1))))
            .build();

        TenantScope.run(
            ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.save(exchangedAuthorization));

        OAuth2Authorization storedAuthorization = TenantScope.call(
            ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findById(codeAuthorization.getId()));

        assertThat(storedAuthorization).isNotNull();
        assertThat(storedAuthorization.getAccessToken()).isNotNull();
        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isEqualTo(1);
    }

    @Test
    void testRegisteredClientsCountOnlyTheGrantsOfTheCurrentTenant() {
        TenantScope.run(ACME_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "alice", ACME_TENANT_ID));
        TenantScope.run(GLOBEX_TENANT_ID, () -> {
            saveAuthorization(CLIENT_ID, "alice", GLOBEX_TENANT_ID);
            saveAuthorization(CLIENT_ID, "bob", GLOBEX_TENANT_ID);
            saveAuthorization(OTHER_CLIENT_ID, "carol", GLOBEX_TENANT_ID);
        });

        List<RegisteredClientInfo> acmeRegisteredClients = TenantScope.call(
            ACME_TENANT_ID, registeredClientService::getRegisteredClients);

        assertThat(acmeRegisteredClients).singleElement()
            .satisfies(registeredClientInfo -> {
                assertThat(registeredClientInfo.id()).isEqualTo(CLIENT_ID);
                assertThat(registeredClientInfo.grantCount()).isEqualTo(1);
            });

        List<RegisteredClientInfo> globexRegisteredClients = TenantScope.call(
            GLOBEX_TENANT_ID, registeredClientService::getRegisteredClients);

        assertThat(globexRegisteredClients)
            .extracting(RegisteredClientInfo::id, RegisteredClientInfo::grantCount)
            .containsExactlyInAnyOrder(
                tuple(CLIENT_ID, 2), tuple(OTHER_CLIENT_ID, 1));
    }

    @Test
    void testKeyedTokensAreNotFoundWhileAnotherTenantIsCurrent() {
        OAuth2Authorization authorization = TenantScope.call(
            ACME_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "alice", ACME_TENANT_ID));

        String authorizationCode = getTokenValue(authorization, OAuth2AuthorizationCode.class);
        String refreshToken = getTokenValue(authorization, OAuth2RefreshToken.class);
        String accessToken = getTokenValue(authorization, OAuth2AccessToken.class);

        OAuth2TokenType authorizationCodeTokenType = new OAuth2TokenType(OAuth2ParameterNames.CODE);

        assertThat(TenantScope.call(ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(
            authorizationCode, authorizationCodeTokenType))).isNotNull();
        assertThat(TenantScope.call(ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(
            refreshToken, OAuth2TokenType.REFRESH_TOKEN))).isNotNull();

        assertThat(TenantScope.call(GLOBEX_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(
            authorizationCode, authorizationCodeTokenType))).isNull();
        assertThat(TenantScope.call(GLOBEX_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(
            refreshToken, OAuth2TokenType.REFRESH_TOKEN))).isNull();

        assertThat(tenantRoutingOAuth2AuthorizationService.findByAccessToken(accessToken, ACME_TENANT_ID))
            .isNotNull();
        assertThat(tenantRoutingOAuth2AuthorizationService.findByAccessToken(accessToken, GLOBEX_TENANT_ID))
            .isNull();
    }

    @Test
    void testTokenKeyedForAnotherTenantIsLookedUpInThatTenantSchema() {
        OAuth2Authorization authorization = TenantScope.call(
            ACME_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "alice", GLOBEX_TENANT_ID));

        String refreshToken = getTokenValue(authorization, OAuth2RefreshToken.class);

        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isEqualTo(1);
        assertThat(TenantScope.call(ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(
            refreshToken, null))).isNull();
    }

    private OAuth2Authorization saveAuthorization(
        String registeredClientId, String principalName, String tokenTenantId) {

        RegisteredClient registeredClient = registeredClientRepository.findById(registeredClientId);

        if (registeredClient == null) {
            throw new IllegalStateException("Unknown registered client " + registeredClientId);
        }

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofHours(1));

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("http://localhost/oauth2/authorize")
            .clientId(registeredClient.getClientId())
            .redirectUri("http://127.0.0.1/callback")
            .additionalParameters(Map.of(OAuth2ParameterNames.RESOURCE, MANAGEMENT_URL))
            .build();

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
            .id(UUID.randomUUID()
                .toString())
            .principalName(principalName)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of(SCOPE))
            .attribute(OAuth2AuthorizationRequest.class.getName(), authorizationRequest)
            .token(new OAuth2AuthorizationCode(TenantKeyedTokenValues.generate(tokenTenantId), now, expiresAt))
            .token(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-" + UUID.randomUUID(), now, expiresAt))
            .refreshToken(new OAuth2RefreshToken(TenantKeyedTokenValues.generate(tokenTenantId), now, expiresAt))
            .build();

        seedAuthorizationService.save(authorization);

        return authorization;
    }

    private static String getTokenValue(
        OAuth2Authorization authorization, Class<? extends OAuth2Token> type) {

        OAuth2Authorization.Token<? extends OAuth2Token> token =
            authorization.getToken(type);

        if (token == null) {
            throw new IllegalStateException("Authorization has no " + type.getSimpleName());
        }

        return token.getToken()
            .getTokenValue();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private int countRows(String schema, String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + "." + table, Integer.class);

        return count == null ? 0 : count;
    }

    private boolean tableExists(String schema, String table) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?",
            Integer.class, schema, table);

        return count != null && count > 0;
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
