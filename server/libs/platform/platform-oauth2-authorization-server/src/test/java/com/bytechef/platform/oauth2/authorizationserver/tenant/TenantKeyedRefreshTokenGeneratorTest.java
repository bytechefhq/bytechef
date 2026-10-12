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

import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;

/**
 * @author Ivica Cardic
 */
class TenantKeyedRefreshTokenGeneratorTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testGeneratesRefreshTokenForCurrentTenant() {
        TenantContext.setCurrentTenantId("acme");

        RegisteredClient registeredClient = RegisteredClient.withId("id")
            .clientId("client")
            .clientSecret("{noop}secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost/cb")
            .build();
        OAuth2TokenContext context = DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .tokenType(OAuth2TokenType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .build();

        OAuth2RefreshToken refreshToken = new TenantKeyedRefreshTokenGenerator().generate(context);

        assertThat(refreshToken).isNotNull();
        assertThat(TenantKeyedTokenValues.parseTenantId(refreshToken.getTokenValue())).contains("acme");
        assertThat(refreshToken.getExpiresAt()).isAfter(refreshToken.getIssuedAt());
    }

    @Test
    void testPublicClientGetsNoRefreshToken() {
        TenantContext.setCurrentTenantId("acme");

        RegisteredClient registeredClient = RegisteredClient.withId("id")
            .clientId("client")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost/cb")
            .build();
        OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(
            registeredClient, ClientAuthenticationMethod.NONE, null);
        OAuth2TokenContext context = DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .tokenType(OAuth2TokenType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrant(
                new OAuth2AuthorizationCodeAuthenticationToken("code", clientPrincipal, "http://localhost/cb", null))
            .build();

        assertThat(new TenantKeyedRefreshTokenGenerator().generate(context)).isNull();
    }
}
