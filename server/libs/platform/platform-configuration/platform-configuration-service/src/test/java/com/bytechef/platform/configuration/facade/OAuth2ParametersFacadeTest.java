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

package com.bytechef.platform.configuration.facade;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class OAuth2ParametersFacadeTest {

    private static final AuthorizationType AUTHORIZATION_TYPE = AuthorizationType.OAUTH2_AUTHORIZATION_CODE_PKCE;

    @Mock
    private ConnectionDefinitionService connectionDefinitionService;

    @Mock
    private ConnectionFacade connectionFacade;

    @Mock
    private OAuth2Service oAuth2Service;

    private OAuth2ParametersFacade oAuth2ParametersFacade;

    @BeforeEach
    void beforeEach() {
        oAuth2ParametersFacade = new OAuth2ParametersFacadeImpl(
            connectionDefinitionService, connectionFacade, oAuth2Service);
    }

    @Test
    void testGetOAuth2AuthorizationParametersMergesStoredConnectionParametersUnderBrowserValues() {
        stubParameterPassThrough();

        doReturn(Map.of("url", "https://mcp.example.com/mcp", "transportType", "SSE"))
            .when(connectionFacade)
            .getReauthorizationParameters(42L, "mcpClient", 1);

        oAuth2ParametersFacade.getOAuth2AuthorizationParameters(
            "mcpClient", 1, Map.of("transportType", "HTTP_STREAMABLE"), AUTHORIZATION_TYPE, 42L);

        Assertions.assertThat(captureDefinitionServiceParameters())
            .isEqualTo(Map.of("url", "https://mcp.example.com/mcp", "transportType", "HTTP_STREAMABLE"));
    }

    @Test
    void testGetOAuth2AuthorizationParametersWithoutConnectionIdUsesOnlyBrowserValues() {
        stubParameterPassThrough();

        oAuth2ParametersFacade.getOAuth2AuthorizationParameters(
            "mcpClient", 1, Map.of("url", "https://mcp.example.com/mcp"), AUTHORIZATION_TYPE);

        verifyNoInteractions(connectionFacade);

        Assertions.assertThat(captureDefinitionServiceParameters())
            .isEqualTo(Map.of("url", "https://mcp.example.com/mcp"));
    }

    @Test
    void testGetOAuth2AuthorizationParametersRejectsConnectionTheUserMayNotUpdate() {
        when(connectionFacade.getReauthorizationParameters(42L, "mcpClient", 1))
            .thenThrow(new AccessDeniedException("denied"));

        Assertions.assertThatThrownBy(
            () -> oAuth2ParametersFacade.getOAuth2AuthorizationParameters(
                "mcpClient", 1, Map.of(), AUTHORIZATION_TYPE, 42L))
            .isInstanceOf(AccessDeniedException.class);

        verify(connectionDefinitionService, never()).getOAuth2AuthorizationParameters(
            eq("mcpClient"), eq(1), eq(AUTHORIZATION_TYPE), anyMap());
    }

    private Map<String, Object> captureDefinitionServiceParameters() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> parametersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(connectionDefinitionService).getOAuth2AuthorizationParameters(
            eq("mcpClient"), eq(1), eq(AUTHORIZATION_TYPE), parametersCaptor.capture());

        return new HashMap<>(parametersCaptor.getValue());
    }

    private void stubParameterPassThrough() {
        when(oAuth2Service.checkPredefinedParameters(eq("mcpClient"), anyMap()))
            .thenAnswer(invocation -> invocation.getArgument(1));
        when(connectionDefinitionService.getOAuth2AuthorizationParameters(
            eq("mcpClient"), eq(1), eq(AUTHORIZATION_TYPE), anyMap()))
                .thenReturn(new OAuth2AuthorizationParameters(null, null, Map.of(), Map.of(), null));
    }
}
