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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * @author Ivica Cardic
 */
class AuthorizationServerTokenRevocationCheckerTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String TENANT_ID = "acme";

    private final TenantRoutingOAuth2AuthorizationService authorizationService =
        mock(TenantRoutingOAuth2AuthorizationService.class);
    private final AuthorizationServerTokenRevocationChecker authorizationServerTokenRevocationChecker =
        new AuthorizationServerTokenRevocationChecker(authorizationService);

    @Test
    void testMissingAuthorizationIsRevoked() {
        assertThat(authorizationServerTokenRevocationChecker.isRevoked(ACCESS_TOKEN, TENANT_ID)).isTrue();
    }

    @Test
    void testAuthorizationWithoutAccessTokenIsRevoked() {
        when(authorizationService.findByAccessToken(ACCESS_TOKEN, TENANT_ID))
            .thenReturn(authorizationBuilder().build());

        assertThat(authorizationServerTokenRevocationChecker.isRevoked(ACCESS_TOKEN, TENANT_ID)).isTrue();
    }

    @Test
    void testInvalidatedAccessTokenIsRevoked() {
        when(authorizationService.findByAccessToken(ACCESS_TOKEN, TENANT_ID))
            .thenReturn(authorizationBuilder()
                .token(
                    createAccessToken(),
                    metadata -> metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true))
                .build());

        assertThat(authorizationServerTokenRevocationChecker.isRevoked(ACCESS_TOKEN, TENANT_ID)).isTrue();
    }

    @Test
    void testActiveAccessTokenIsNotRevoked() {
        when(authorizationService.findByAccessToken(ACCESS_TOKEN, TENANT_ID))
            .thenReturn(authorizationBuilder()
                .token(createAccessToken())
                .build());

        assertThat(authorizationServerTokenRevocationChecker.isRevoked(ACCESS_TOKEN, TENANT_ID)).isFalse();
    }

    private static OAuth2AccessToken createAccessToken() {
        Instant now = Instant.now();

        return new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, ACCESS_TOKEN, now, now.plus(Duration.ofMinutes(5)));
    }

    private static OAuth2Authorization.Builder authorizationBuilder() {
        RegisteredClient registeredClient = RegisteredClient.withId("client")
            .clientId("client-id")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://127.0.0.1/callback")
            .build();

        return OAuth2Authorization.withRegisteredClient(registeredClient)
            .id("authorization-id")
            .principalName("alice")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
    }
}
