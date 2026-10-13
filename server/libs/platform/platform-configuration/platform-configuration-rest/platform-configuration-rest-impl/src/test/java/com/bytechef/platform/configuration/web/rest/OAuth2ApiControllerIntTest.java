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

package com.bytechef.platform.configuration.web.rest;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.web.rest.config.PlatformConfigurationRestTestConfiguration;
import com.bytechef.platform.configuration.web.rest.config.WorkflowConfigurationRestTestConfigurationSharedMocks;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = PlatformConfigurationRestTestConfiguration.class)
@WebMvcTest(OAuth2ApiController.class)
@WorkflowConfigurationRestTestConfigurationSharedMocks
class OAuth2ApiControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuth2ParametersFacade oAuth2ParametersFacade;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testGetOAuth2AuthorizationParametersReturnsAuthorizationContext() {
        when(
            oAuth2ParametersFacade.getOAuth2AuthorizationParameters(
                eq("mcpClient"), eq(1), anyMap(), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE), isNull()))
                    .thenReturn(
                        new OAuth2AuthorizationParameters(
                            "https://auth.example.com/authorize", "reg-1",
                            Map.of("resource", "https://mcp.example.com/mcp"), Map.of("read", true),
                            "sealed-context"));

        webTestClient.post()
            .uri("/internal/oauth2/authorization-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                    {"componentName":"mcpClient","connectionVersion":1,
                     "authorizationType":"OAUTH2_AUTHORIZATION_CODE_PKCE","parameters":{"url":"https://mcp.example.com/mcp"}}
                    """)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.authorizationUrl")
            .isEqualTo("https://auth.example.com/authorize")
            .jsonPath("$.clientId")
            .isEqualTo("reg-1")
            .jsonPath("$.extraQueryParameters.resource")
            .isEqualTo("https://mcp.example.com/mcp")
            .jsonPath("$.authorizationContext")
            .isEqualTo("sealed-context");
    }

    @Test
    void testGetOAuth2AuthorizationParametersPassesConnectionIdToFacade() {
        when(
            oAuth2ParametersFacade.getOAuth2AuthorizationParameters(
                eq("mcpClient"), eq(1), eq(Map.of()), eq(AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE), eq(42L)))
                    .thenReturn(
                        new OAuth2AuthorizationParameters(
                            "https://auth.example.com/authorize", "reg-1", Map.of(), Map.of(), "sealed-context"));

        webTestClient.post()
            .uri("/internal/oauth2/authorization-parameters")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                    {"componentName":"mcpClient","connectionId":42,"connectionVersion":1,
                     "authorizationType":"OAUTH2_AUTHORIZATION_CODE_PKCE","parameters":{}}
                    """)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.authorizationContext")
            .isEqualTo("sealed-context");
    }
}
