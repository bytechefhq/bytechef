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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.transaction.support.TransactionOperations;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
class TenantRoutingOAuth2AuthorizationServiceTest {

    private static final RegisteredClient REGISTERED_CLIENT = RegisteredClient.withId("client")
        .clientId("client-id")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .redirectUri("http://127.0.0.1/callback")
        .build();

    private final OAuth2AuthorizationService delegate = mock(OAuth2AuthorizationService.class);
    private final JdbcOperations jdbcOperations = mock(JdbcOperations.class);
    private final TenantRoutingOAuth2AuthorizationService service = new TenantRoutingOAuth2AuthorizationService(
        delegate, jdbcOperations, TransactionOperations.withoutTransaction());

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testCodesOfTwoTenantsRouteToTheirOwnSchemas() {
        String acmeCode = TenantKeyedTokenValues.generate("acme");
        String globexCode = TenantKeyedTokenValues.generate("globex");
        List<String> seenTenantIds = new ArrayList<>();

        when(delegate.findByToken(anyString(), eq(new OAuth2TokenType(OAuth2ParameterNames.CODE))))
            .thenAnswer(invocation -> {
                seenTenantIds.add(TenantContext.getCurrentTenantId());

                return null;
            });

        TenantScope.run("acme", () -> service.findByToken(acmeCode, new OAuth2TokenType(OAuth2ParameterNames.CODE)));
        TenantScope.run(
            "globex", () -> service.findByToken(globexCode, new OAuth2TokenType(OAuth2ParameterNames.CODE)));

        assertThat(seenTenantIds).containsExactly("acme", "globex");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testRefreshTokensOfTwoTenantsRouteToTheirOwnSchemas() {
        String acmeRefreshToken = TenantKeyedTokenValues.generate("acme");
        String globexRefreshToken = TenantKeyedTokenValues.generate("globex");
        List<String> seenTenantIds = new ArrayList<>();

        when(delegate.findByToken(anyString(), eq(OAuth2TokenType.REFRESH_TOKEN)))
            .thenAnswer(invocation -> {
                seenTenantIds.add(TenantContext.getCurrentTenantId());

                return null;
            });

        TenantScope.run("acme", () -> service.findByToken(acmeRefreshToken, OAuth2TokenType.REFRESH_TOKEN));
        TenantScope.run("globex", () -> service.findByToken(globexRefreshToken, OAuth2TokenType.REFRESH_TOKEN));

        assertThat(seenTenantIds).containsExactly("acme", "globex");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testFindByTokenWithForeignValueReturnsNull() {
        assertThat(service.findByToken("legacy-opaque-value", OAuth2TokenType.REFRESH_TOKEN)).isNull();
        assertThat(
            service.findByToken("eyJhbGciOiJSUzI1NiJ9.e30.sig", new OAuth2TokenType(OAuth2ParameterNames.CODE)))
                .isNull();

        verifyNoInteractions(delegate);
    }

    @Test
    void testKeyedLookupForAnotherTenantReturnsNull() {
        String acmeRefreshToken = TenantKeyedTokenValues.generate("acme");
        String acmeCode = TenantKeyedTokenValues.generate("acme");

        TenantContext.setCurrentTenantId("globex");

        assertThat(service.findByToken(acmeRefreshToken, OAuth2TokenType.REFRESH_TOKEN)).isNull();
        assertThat(service.findByToken(acmeCode, new OAuth2TokenType(OAuth2ParameterNames.CODE))).isNull();

        verifyNoInteractions(delegate);
    }

    @Test
    void testFindByTokenWithoutTypeRoutesKeyedValueToItsTenant() {
        String acmeRefreshToken = TenantKeyedTokenValues.generate("acme");
        List<String> seenTenantIds = new ArrayList<>();

        when(delegate.findByToken(eq(acmeRefreshToken), isNull())).thenAnswer(invocation -> {
            seenTenantIds.add(TenantContext.getCurrentTenantId());

            return null;
        });

        service.findByToken(acmeRefreshToken, null);

        assertThat(seenTenantIds).containsExactly("acme");
    }

    @Test
    void testFindByTokenWithoutTypeAndUnkeyedValueUsesCurrentTenant() {
        List<String> seenTenantIds = new ArrayList<>();

        TenantContext.setCurrentTenantId("acme");

        when(delegate.findByToken(eq("jwt.access.token"), isNull())).thenAnswer(invocation -> {
            seenTenantIds.add(TenantContext.getCurrentTenantId());

            return null;
        });

        service.findByToken("jwt.access.token", null);

        assertThat(seenTenantIds).containsExactly("acme");
    }

    @Test
    void testFindByAccessTokenRunsInGivenTenant() {
        List<String> seenTenantIds = new ArrayList<>();

        when(delegate.findByToken("jwt", OAuth2TokenType.ACCESS_TOKEN)).thenAnswer(invocation -> {
            seenTenantIds.add(TenantContext.getCurrentTenantId());

            return null;
        });

        service.findByAccessToken("jwt", "acme");

        assertThat(seenTenantIds).containsExactly("acme");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testStateLookupUsesCurrentTenant() {
        TenantContext.setCurrentTenantId("acme");

        service.findByToken("state-value", new OAuth2TokenType(OAuth2ParameterNames.STATE));

        verify(delegate).findByToken("state-value", new OAuth2TokenType(OAuth2ParameterNames.STATE));
    }

    @Test
    void testSaveOfConsentStateAuthorizationInsertsWithoutExistenceCheck() {
        OAuth2Authorization authorization = authorizationBuilder(AuthorizationGrantType.AUTHORIZATION_CODE)
            .attribute(OAuth2ParameterNames.STATE, "state-value")
            .build();

        service.save(authorization);

        verify(delegate).save(authorization);
        verifyNoInteractions(jdbcOperations);
    }

    @Test
    void testSaveOfFreshlyIssuedAuthorizationCodeInsertsWithoutExistenceCheck() {
        Instant now = Instant.now();

        OAuth2Authorization authorization = authorizationBuilder(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(new OAuth2AuthorizationCode("code", now, now.plus(Duration.ofMinutes(5))))
            .build();

        service.save(authorization);

        verify(delegate).save(authorization);
        verifyNoInteractions(jdbcOperations);
    }

    @Test
    void testSaveOfClientCredentialsAuthorizationInsertsWithoutExistenceCheck() {
        OAuth2Authorization authorization = authorizationBuilder(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .token(createAccessToken())
            .build();

        service.save(authorization);

        verify(delegate).save(authorization);
        verifyNoInteractions(jdbcOperations);
    }

    @Test
    void testSaveOfIssuedTokensForAnExistingAuthorizationUpdatesIt() {
        OAuth2Authorization authorization = createIssuedAuthorization();

        when(jdbcOperations.queryForList(anyString(), eq(String.class), eq(authorization.getId())))
            .thenReturn(List.of(authorization.getId()));

        service.save(authorization);

        verify(delegate).save(authorization);
    }

    @Test
    void testSaveOfIssuedTokensForARevokedAuthorizationIsRefused() {
        OAuth2Authorization authorization = createIssuedAuthorization();

        when(jdbcOperations.queryForList(anyString(), eq(String.class), eq(authorization.getId())))
            .thenReturn(List.of());

        assertThatExceptionOfType(OAuth2AuthenticationException.class)
            .isThrownBy(() -> service.save(authorization))
            .satisfies(exception -> assertThat(exception.getError()
                .getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_GRANT));

        verify(delegate, never()).save(authorization);
    }

    @Test
    void testSaveOfInvalidatedCodeForARevokedAuthorizationIsRefused() {
        Instant now = Instant.now();

        OAuth2Authorization authorization = authorizationBuilder(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(
                new OAuth2AuthorizationCode("code", now, now.plus(Duration.ofMinutes(5))),
                metadata -> metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true))
            .build();

        when(jdbcOperations.queryForList(anyString(), eq(String.class), eq(authorization.getId())))
            .thenReturn(List.of());

        assertThatExceptionOfType(OAuth2AuthenticationException.class)
            .isThrownBy(() -> service.save(authorization));

        verify(delegate, never()).save(authorization);
    }

    private static OAuth2Authorization createIssuedAuthorization() {
        Instant now = Instant.now();

        return authorizationBuilder(AuthorizationGrantType.AUTHORIZATION_CODE)
            .token(
                new OAuth2AuthorizationCode("code", now, now.plus(Duration.ofMinutes(5))),
                metadata -> metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true))
            .token(createAccessToken())
            .refreshToken(new OAuth2RefreshToken("refresh-token", now, now.plus(Duration.ofDays(1))))
            .build();
    }

    private static OAuth2AccessToken createAccessToken() {
        Instant now = Instant.now();

        return new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "access-token", now, now.plus(Duration.ofMinutes(5)));
    }

    private static OAuth2Authorization.Builder authorizationBuilder(AuthorizationGrantType authorizationGrantType) {
        return OAuth2Authorization.withRegisteredClient(REGISTERED_CLIENT)
            .id("authorization-id")
            .principalName("alice")
            .authorizationGrantType(authorizationGrantType);
    }
}
