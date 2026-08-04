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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.exception.TriggerDefinitionErrorType;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class TriggerDefinitionServiceImplVisibilityTest {

    @Test
    void testExecuteTriggerRejectsDisabledComponent() {
        ComponentVisibilityProvider disableSlack = componentName -> !componentName.equals("slack");

        TriggerDefinitionServiceImpl service = new TriggerDefinitionServiceImpl(
            mock(ComponentDefinitionRegistry.class), mock(ContextFactory.class),
            mock(ApplicationEventPublisher.class), List.of(disableSlack));

        assertThatThrownBy(
            () -> service.executeTrigger(
                "slack", 1, "newMessage", 1L, "uuid", Map.of(), null, null, null, 1L, PlatformType.AUTOMATION,
                false))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("disabled");
    }

    @Test
    void testExecuteTriggerThrowsWhenTriggerDisabled() {
        ComponentVisibilityProvider componentVisibilityProvider = new ComponentVisibilityProvider() {

            @Override
            public boolean isVisible(String componentName) {
                return true;
            }

            @Override
            public boolean isTriggerVisible(String componentName, String triggerName) {
                return !"newMessage".equals(triggerName);
            }
        };

        TriggerDefinitionServiceImpl service = new TriggerDefinitionServiceImpl(
            mock(ComponentDefinitionRegistry.class), mock(ContextFactory.class),
            mock(ApplicationEventPublisher.class), List.of(componentVisibilityProvider));

        ConfigurationException configurationException = assertThrows(
            ConfigurationException.class,
            () -> service.executeTrigger(
                "slack", 1, "newMessage", 1L, "uuid", Map.of(), null, null, null, 1L, PlatformType.AUTOMATION,
                false));

        assertThat(configurationException.getErrorKey())
            .isEqualTo(TriggerDefinitionErrorType.TRIGGER_DISABLED.getErrorKey());
    }

    @Test
    void testGetTriggerDefinitionsFiltersDisabledTriggers() {
        ComponentVisibilityProvider componentVisibilityProvider = new ComponentVisibilityProvider() {

            @Override
            public boolean isVisible(String componentName) {
                return true;
            }

            @Override
            public boolean isTriggerVisible(String componentName, String triggerName) {
                return !"newMessage".equals(triggerName);
            }
        };

        com.bytechef.component.definition.TriggerDefinition newMessageTriggerDefinition =
            mock(com.bytechef.component.definition.TriggerDefinition.class);

        when(newMessageTriggerDefinition.getName()).thenReturn("newMessage");
        when(newMessageTriggerDefinition.getType())
            .thenReturn(com.bytechef.component.definition.TriggerDefinition.TriggerType.STATIC_WEBHOOK);

        com.bytechef.component.definition.TriggerDefinition messageDeletedTriggerDefinition =
            mock(com.bytechef.component.definition.TriggerDefinition.class);

        when(messageDeletedTriggerDefinition.getName()).thenReturn("messageDeleted");
        when(messageDeletedTriggerDefinition.getType())
            .thenReturn(com.bytechef.component.definition.TriggerDefinition.TriggerType.STATIC_WEBHOOK);

        ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);

        doReturn(List.of(newMessageTriggerDefinition, messageDeletedTriggerDefinition))
            .when(componentDefinitionRegistry)
            .getTriggerDefinitions("slack", 1);

        TriggerDefinitionServiceImpl service = new TriggerDefinitionServiceImpl(
            componentDefinitionRegistry, mock(ContextFactory.class), mock(ApplicationEventPublisher.class),
            List.of(componentVisibilityProvider));

        List<com.bytechef.platform.component.domain.TriggerDefinition> triggerDefinitions =
            service.getTriggerDefinitions("slack", 1);

        assertThat(triggerDefinitions)
            .extracting(com.bytechef.platform.component.domain.TriggerDefinition::getName)
            .containsExactly("messageDeleted");
    }
}
