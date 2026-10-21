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

package com.bytechef.platform.oauth2.authorizationserver.registration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * @author Ivica Cardic
 */
class AppClientsTest {

    @Test
    void testClientWithTheAppScopeIsAnAppClient() {
        assertThat(AppClients.isAppClient(createClient(List.of("openid", "profile", "app:use")))).isTrue();
    }

    @Test
    void testClientWithoutTheAppScopeIsNotAnAppClient() {
        assertThat(AppClients.isAppClient(createClient(List.of("openid", "mcp:management")))).isFalse();
    }

    @Test
    void testScopeSetIsAnAppScopeSetOnlyWithTheAppScope() {
        assertThat(AppClients.isAppScopeSet(List.of("app:use"))).isTrue();
        assertThat(AppClients.isAppScopeSet(List.of("openid", "profile"))).isFalse();
    }

    private static RegisteredClient createClient(List<String> scopes) {
        return RegisteredClient.withId("client")
            .clientId("client-id")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://app.example.com/auth/callback")
            .scopes(registeredScopes -> registeredScopes.addAll(scopes))
            .build();
    }
}
