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
import static com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures.MANAGEMENT_URL;
import static com.bytechef.platform.oauth2.authorizationserver.config.RegisteredClientIntTestFixtures.SCOPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
class TenantRoutingOAuth2AuthorizationServiceIntTest {

    private static final OAuth2TokenType AUTHORIZATION_CODE_TOKEN_TYPE = new OAuth2TokenType(OAuth2ParameterNames.CODE);
    private static final String CLIENT_ID = "routing-client";
    private static final String PUBLIC_SCHEMA = TenantContext.DEFAULT_TENANT_ID;
    private static final OAuth2TokenType STATE_TOKEN_TYPE = new OAuth2TokenType(OAuth2ParameterNames.STATE);

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

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

        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testCodesOfTwoTenantsRouteToTheirOwnSchemas() {
        OAuth2Authorization acmeAuthorization = TenantScope.call(
            ACME_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "alice", ACME_TENANT_ID));
        OAuth2Authorization globexAuthorization = TenantScope.call(
            GLOBEX_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "bob", GLOBEX_TENANT_ID));

        String acmeCode = getTokenValue(acmeAuthorization, OAuth2AuthorizationCode.class);
        String globexCode = getTokenValue(globexAuthorization, OAuth2AuthorizationCode.class);

        OAuth2Authorization foundAcmeAuthorization = TenantScope.call(
            ACME_TENANT_ID,
            () -> tenantRoutingOAuth2AuthorizationService.findByToken(acmeCode, AUTHORIZATION_CODE_TOKEN_TYPE));
        OAuth2Authorization foundGlobexAuthorization = TenantScope.call(
            GLOBEX_TENANT_ID,
            () -> tenantRoutingOAuth2AuthorizationService.findByToken(globexCode, AUTHORIZATION_CODE_TOKEN_TYPE));

        assertThat(foundAcmeAuthorization).isNotNull();
        assertThat(foundAcmeAuthorization.getId()).isEqualTo(acmeAuthorization.getId());
        assertThat(foundGlobexAuthorization).isNotNull();
        assertThat(foundGlobexAuthorization.getId()).isEqualTo(globexAuthorization.getId());
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRefreshTokensOfTwoTenantsRouteToTheirOwnSchemas() {
        OAuth2Authorization acmeAuthorization = TenantScope.call(
            ACME_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "alice", ACME_TENANT_ID));
        OAuth2Authorization globexAuthorization = TenantScope.call(
            GLOBEX_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "bob", GLOBEX_TENANT_ID));

        String acmeRefreshToken = getTokenValue(acmeAuthorization, OAuth2RefreshToken.class);
        String globexRefreshToken = getTokenValue(globexAuthorization, OAuth2RefreshToken.class);

        OAuth2Authorization foundAcmeAuthorization = TenantScope.call(
            ACME_TENANT_ID,
            () -> tenantRoutingOAuth2AuthorizationService.findByToken(acmeRefreshToken, OAuth2TokenType.REFRESH_TOKEN));
        OAuth2Authorization foundGlobexAuthorization = TenantScope.call(
            GLOBEX_TENANT_ID,
            () -> tenantRoutingOAuth2AuthorizationService.findByToken(
                globexRefreshToken, OAuth2TokenType.REFRESH_TOKEN));

        assertThat(foundAcmeAuthorization).isNotNull();
        assertThat(foundAcmeAuthorization.getId()).isEqualTo(acmeAuthorization.getId());
        assertThat(foundGlobexAuthorization).isNotNull();
        assertThat(foundGlobexAuthorization.getId()).isEqualTo(globexAuthorization.getId());
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testFindByTokenWithForeignValueReturnsNull() {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofHours(1));
        String foreignCode = "eyJhbGciOiJSUzI1NiJ9.e30.sig-" + UUID.randomUUID();
        String foreignRefreshToken = "legacy-opaque-value-" + UUID.randomUUID();

        OAuth2Authorization authorization = authorizationBuilder(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(new OAuth2AuthorizationCode(foreignCode, now, expiresAt))
            .refreshToken(new OAuth2RefreshToken(foreignRefreshToken, now, expiresAt))
            .build();

        TenantScope.run(ACME_TENANT_ID, () -> seedAuthorizationService.save(authorization));

        TenantScope.run(ACME_TENANT_ID, () -> {
            assertThat(seedAuthorizationService.findByToken(foreignRefreshToken, OAuth2TokenType.REFRESH_TOKEN))
                .isNotNull();
            assertThat(seedAuthorizationService.findByToken(foreignCode, AUTHORIZATION_CODE_TOKEN_TYPE))
                .isNotNull();

            assertThat(tenantRoutingOAuth2AuthorizationService.findByToken(
                foreignRefreshToken, OAuth2TokenType.REFRESH_TOKEN)).isNull();
            assertThat(tenantRoutingOAuth2AuthorizationService.findByToken(
                foreignCode, AUTHORIZATION_CODE_TOKEN_TYPE)).isNull();
        });
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
    void testFindByTokenWithoutTypeRoutesKeyedValueToItsTenant() {
        OAuth2Authorization authorization = TenantScope.call(
            ACME_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "alice", ACME_TENANT_ID));

        String refreshToken = getTokenValue(authorization, OAuth2RefreshToken.class);

        OAuth2Authorization foundFromDefaultTenant = tenantRoutingOAuth2AuthorizationService.findByToken(
            refreshToken, null);
        OAuth2Authorization foundFromGlobexTenant = TenantScope.call(
            GLOBEX_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(refreshToken, null));

        assertThat(foundFromDefaultTenant).isNotNull();
        assertThat(foundFromDefaultTenant.getId()).isEqualTo(authorization.getId());
        assertThat(foundFromGlobexTenant).isNotNull();
        assertThat(foundFromGlobexTenant.getId()).isEqualTo(authorization.getId());
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
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

    @Test
    void testFindByTokenWithoutTypeAndUnkeyedValueUsesCurrentTenant() {
        OAuth2Authorization authorization = TenantScope.call(
            ACME_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "alice", ACME_TENANT_ID));

        String accessToken = getTokenValue(authorization, OAuth2AccessToken.class);

        OAuth2Authorization foundAuthorization = TenantScope.call(
            ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(accessToken, null));

        assertThat(foundAuthorization).isNotNull();
        assertThat(foundAuthorization.getId()).isEqualTo(authorization.getId());
        assertThat(TenantScope.call(
            GLOBEX_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(accessToken, null)))
                .isNull();
    }

    @Test
    void testFindByAccessTokenRunsInGivenTenant() {
        OAuth2Authorization authorization = TenantScope.call(
            ACME_TENANT_ID, () -> saveAuthorization(CLIENT_ID, "alice", ACME_TENANT_ID));

        String accessToken = getTokenValue(authorization, OAuth2AccessToken.class);

        OAuth2Authorization foundAuthorization = tenantRoutingOAuth2AuthorizationService.findByAccessToken(
            accessToken, ACME_TENANT_ID);

        assertThat(foundAuthorization).isNotNull();
        assertThat(foundAuthorization.getId()).isEqualTo(authorization.getId());
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testStateLookupUsesCurrentTenant() {
        String state = "state-" + UUID.randomUUID();

        OAuth2Authorization authorization = authorizationBuilder(AuthorizationGrantType.AUTHORIZATION_CODE)
            .attribute(OAuth2ParameterNames.STATE, state)
            .build();

        TenantScope.run(ACME_TENANT_ID, () -> seedAuthorizationService.save(authorization));

        OAuth2Authorization foundAuthorization = TenantScope.call(
            ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(state, STATE_TOKEN_TYPE));

        assertThat(foundAuthorization).isNotNull();
        assertThat(foundAuthorization.getId()).isEqualTo(authorization.getId());
        assertThat(TenantScope.call(
            GLOBEX_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findByToken(state, STATE_TOKEN_TYPE)))
                .isNull();
    }

    @Test
    void testSaveOfConsentStateAuthorizationInsertsWithoutExistenceCheck() {
        OAuth2Authorization authorization = authorizationBuilder(AuthorizationGrantType.AUTHORIZATION_CODE)
            .attribute(OAuth2ParameterNames.STATE, "state-" + UUID.randomUUID())
            .build();

        TenantScope.run(ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.save(authorization));

        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isEqualTo(1);
        assertThat(countRows(getDatabaseSchema(GLOBEX_TENANT_ID), "oauth2_authorization")).isZero();
        assertThat(countRows(PUBLIC_SCHEMA, "oauth2_authorization")).isZero();
    }

    @Test
    void testSaveOfClientCredentialsAuthorizationInsertsWithoutExistenceCheck() {
        Instant now = Instant.now();

        OAuth2Authorization authorization = authorizationBuilder(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .token(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-" + UUID.randomUUID(), now, now.plus(Duration.ofHours(1))))
            .build();

        TenantScope.run(ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.save(authorization));

        assertThat(TenantScope.call(
            ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.findById(authorization.getId())))
                .isNotNull();
        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isEqualTo(1);
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
    void testSaveOfInvalidatedCodeForARevokedAuthorizationIsRefused() {
        Instant now = Instant.now();

        OAuth2Authorization authorization = authorizationBuilder(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(
                new OAuth2AuthorizationCode(
                    TenantKeyedTokenValues.generate(ACME_TENANT_ID), now, now.plus(Duration.ofMinutes(5))),
                metadata -> metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true))
            .build();

        assertThatExceptionOfType(OAuth2AuthenticationException.class)
            .isThrownBy(() -> TenantScope.run(
                ACME_TENANT_ID, () -> tenantRoutingOAuth2AuthorizationService.save(authorization)))
            .satisfies(exception -> assertThat(exception.getError()
                .getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_GRANT));

        assertThat(countRows(getDatabaseSchema(ACME_TENANT_ID), "oauth2_authorization")).isZero();
    }

    private OAuth2Authorization.Builder authorizationBuilder(AuthorizationGrantType authorizationGrantType) {
        RegisteredClient registeredClient = registeredClientRepository.findById(CLIENT_ID);

        if (registeredClient == null) {
            throw new IllegalStateException("Unknown registered client " + CLIENT_ID);
        }

        return OAuth2Authorization.withRegisteredClient(registeredClient)
            .id(UUID.randomUUID()
                .toString())
            .principalName("alice")
            .authorizationGrantType(authorizationGrantType)
            .authorizedScopes(Set.of(SCOPE));
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

    private static String getTokenValue(OAuth2Authorization authorization, Class<? extends OAuth2Token> type) {
        OAuth2Authorization.Token<? extends OAuth2Token> token = authorization.getToken(type);

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
