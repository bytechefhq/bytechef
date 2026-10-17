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

package com.bytechef.platform.component.service;

import static com.bytechef.platform.component.domain.Authorization.DEFAULT_REFRESH_ON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.aspect.TokenRefreshHandler;
import com.bytechef.platform.component.context.ContextFactory;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionServiceTest {

    private final ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
    private final ConnectionDefinitionService connectionDefinitionService = new ConnectionDefinitionServiceImpl(
        componentDefinitionRegistry, mock(ContextFactory.class), mock(TokenRefreshHandler.class));

    @Test
    public void testGetAuthorizationRefreshOnIsEmptyForCustomAuthorizationWithoutAcquire() {
        when(componentDefinitionRegistry.fetchAuthorization("telegram", 1, AuthorizationType.CUSTOM))
            .thenReturn(Optional.of(ComponentDsl.authorization(AuthorizationType.CUSTOM)));

        assertThat(connectionDefinitionService.getAuthorizationRefreshOn("telegram", 1, AuthorizationType.CUSTOM))
            .isEmpty();
    }

    @Test
    public void testGetAuthorizationRefreshOnKeepsTheDefaultForCustomAuthorizationWithAcquire() {
        when(componentDefinitionRegistry.fetchAuthorization("custom", 1, AuthorizationType.CUSTOM))
            .thenReturn(
                Optional.of(
                    ComponentDsl.authorization(AuthorizationType.CUSTOM)
                        .acquire((connectionParameters, context) -> Map.of("token", "acquired"))));

        assertThat(connectionDefinitionService.getAuthorizationRefreshOn("custom", 1, AuthorizationType.CUSTOM))
            .isNotEmpty();
    }

    @Test
    public void testGetAuthorizationRefreshOnKeepsTheDefaultForOAuth2AuthorizationCode() {
        when(
            componentDefinitionRegistry.fetchAuthorization(
                "oauth", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
                    .thenReturn(Optional.of(ComponentDsl.authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)));

        assertThat(
            connectionDefinitionService.getAuthorizationRefreshOn(
                "oauth", 1, AuthorizationType.OAUTH2_AUTHORIZATION_CODE))
                    .isEqualTo(DEFAULT_REFRESH_ON);
    }

    @Disabled
    @Test
    public void testConnectionExists() {
// TODO
    }

    @Disabled
    @Test
    public void testPerformAuthorizationApply() {
// TODO
    }

    @Disabled
    @Test
    public void testPerformAuthorizationCallback() {
// TODO
    }

    @Disabled
    @Test
    public void testFetchBaseUri() {
// TODO
    }

    @Disabled
    @Test
    public void testGetAuthorizationType() {
// TODO
    }

    @Disabled
    @Test
    public void testGetConnectionDefinition() {
// TODO
    }

    @Disabled
    @Test
    public void testGetConnectionDefinitions() {
// TODO
    }

    @Disabled
    @Test
    public void testGetOAuth2Parameters() {
        // TODO
    }
}
