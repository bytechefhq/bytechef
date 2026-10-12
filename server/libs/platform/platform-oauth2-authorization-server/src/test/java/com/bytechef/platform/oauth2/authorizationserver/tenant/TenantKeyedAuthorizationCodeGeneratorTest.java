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
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;

/**
 * @author Ivica Cardic
 */
class TenantKeyedAuthorizationCodeGeneratorTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testGeneratesCodeForCurrentTenant() {
        TenantContext.setCurrentTenantId("acme");

        RegisteredClient registeredClient = createRegisteredClient();
        OAuth2TokenContext context = DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .tokenType(new OAuth2TokenType(OAuth2ParameterNames.CODE))
            .build();

        OAuth2AuthorizationCode code = new TenantKeyedAuthorizationCodeGenerator().generate(context);

        assertThat(code).isNotNull();
        assertThat(TenantKeyedTokenValues.parseTenantId(code.getTokenValue())).contains("acme");
        assertThat(Duration.between(code.getIssuedAt(), code.getExpiresAt()))
            .isEqualTo(registeredClient.getTokenSettings()
                .getAuthorizationCodeTimeToLive());
    }

    @Test
    void testIgnoresOtherTokenTypes() {
        OAuth2TokenContext context = DefaultOAuth2TokenContext.builder()
            .registeredClient(createRegisteredClient())
            .tokenType(OAuth2TokenType.ACCESS_TOKEN)
            .build();

        assertThat(new TenantKeyedAuthorizationCodeGenerator().generate(context)).isNull();
    }

    private static RegisteredClient createRegisteredClient() {
        return RegisteredClient.withId("id")
            .clientId("client")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/cb")
            .build();
    }
}
