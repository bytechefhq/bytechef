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

package com.bytechef.platform.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.oauth2.domain.DiscoveredAuthorizationServer;
import com.bytechef.platform.oauth2.domain.RegisteredClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAuth2DiscoveryServiceTest {

    @Test
    void testRegisterUsesConfiguredRedirectUri() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getOauth2()
            .setRedirectUri("https://bytechef.example.com/callback");

        OAuth2DiscoveryClient discoveryClient = mock(OAuth2DiscoveryClient.class);
        OAuth2DynamicClientRegistrar registrar = mock(OAuth2DynamicClientRegistrar.class);
        DiscoveredAuthorizationServer authorizationServer = new DiscoveredAuthorizationServer(
            "https://auth.example.com", "https://auth.example.com/a", "https://auth.example.com/t",
            "https://auth.example.com/r", "https://mcp.example.com/mcp", List.of(), List.of(), List.of());
        RegisteredClient registeredClient = new RegisteredClient("c", null, "none");

        when(registrar.register(authorizationServer, "https://bytechef.example.com/callback"))
            .thenReturn(registeredClient);

        OAuth2DiscoveryServiceImpl service = new OAuth2DiscoveryServiceImpl(
            applicationProperties, discoveryClient, registrar);

        assertThat(service.register(authorizationServer)).isSameAs(registeredClient);
    }
}
