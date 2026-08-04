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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActionDefinitionServiceImplVisibilityTest {

    @Test
    void testExecutePerformRejectsDisabledComponent() {
        ComponentVisibilityProvider disableSlack = componentName -> !componentName.equals("slack");

        ActionDefinitionServiceImpl service = new ActionDefinitionServiceImpl(
            mock(ComponentDefinitionRegistry.class), mock(ContextFactory.class), List.of(disableSlack));

        assertThatThrownBy(
            () -> service.executePerform(
                "slack", 1, "sendMessage", 1L, 1L, 1L, 1L, "workflow1", Map.of(), Map.of(), Map.of(), 1L, false,
                PlatformType.AUTOMATION, null, null, null))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("disabled");
    }

    @Test
    void testExecutePerformForPolyglotWithDisabledComponent() {
        ComponentVisibilityProvider disableSlack = componentName -> !componentName.equals("slack");

        ActionDefinitionServiceImpl service = new ActionDefinitionServiceImpl(
            mock(ComponentDefinitionRegistry.class), mock(ContextFactory.class), List.of(disableSlack));

        assertThatThrownBy(
            () -> service.executePerformForPolyglot(
                "slack", 1, "sendMessage", Map.of(), null, null, mock(ActionContext.class)))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("disabled");
    }

    @Test
    void testExecutePerformThrowsWhenActionDisabled() {
        ComponentVisibilityProvider componentVisibilityProvider = new ComponentVisibilityProvider() {

            @Override
            public boolean isVisible(String componentName) {
                return true;
            }

            @Override
            public boolean isActionVisible(String componentName, String actionName) {
                return !"sendMessage".equals(actionName);
            }
        };

        ActionDefinitionServiceImpl service = new ActionDefinitionServiceImpl(
            mock(ComponentDefinitionRegistry.class), mock(ContextFactory.class),
            List.of(componentVisibilityProvider));

        assertThatThrownBy(
            () -> service.executePerform(
                "slack", 1, "sendMessage", 1L, 1L, 1L, 1L, "workflow1", Map.of(), Map.of(), Map.of(), 1L, false,
                PlatformType.AUTOMATION, null, null, null))
                    .isInstanceOf(ConfigurationException.class)
                    .extracting(throwable -> ((ConfigurationException) throwable).getErrorKey())
                    .isEqualTo(ActionDefinitionErrorType.ACTION_DISABLED.getErrorKey());
    }

    @Test
    void testGetActionDefinitionsFiltersDisabledActions() {
        ComponentVisibilityProvider componentVisibilityProvider = new ComponentVisibilityProvider() {

            @Override
            public boolean isVisible(String componentName) {
                return true;
            }

            @Override
            public boolean isActionVisible(String componentName, String actionName) {
                return !"sendMessage".equals(actionName);
            }
        };

        com.bytechef.component.definition.ActionDefinition sendMessageActionDefinition =
            mock(com.bytechef.component.definition.ActionDefinition.class);

        when(sendMessageActionDefinition.getName()).thenReturn("sendMessage");

        com.bytechef.component.definition.ActionDefinition createChannelActionDefinition =
            mock(com.bytechef.component.definition.ActionDefinition.class);

        when(createChannelActionDefinition.getName()).thenReturn("createChannel");

        ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);

        doReturn(List.of(sendMessageActionDefinition, createChannelActionDefinition))
            .when(componentDefinitionRegistry)
            .getActionDefinitions("slack", 1);

        ActionDefinitionServiceImpl service = new ActionDefinitionServiceImpl(
            componentDefinitionRegistry, mock(ContextFactory.class), List.of(componentVisibilityProvider));

        List<com.bytechef.platform.component.domain.ActionDefinition> actionDefinitions =
            service.getActionDefinitions("slack", 1);

        assertThat(actionDefinitions)
            .extracting(com.bytechef.platform.component.domain.ActionDefinition::getName)
            .containsExactly("createChannel");
    }
}
