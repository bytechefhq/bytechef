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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.filter.ComponentDefinitionFilter;
import com.bytechef.platform.component.visibility.ComponentVisibilityProvider;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComponentDefinitionServiceImplVisibilityTest {

    private final ComponentDefinitionRegistry componentDefinitionRegistry = mock(ComponentDefinitionRegistry.class);
    private final ContextFactory contextFactory = mock(ContextFactory.class);

    private final ComponentDefinitionFilter allowAllAutomationFilter = new ComponentDefinitionFilter() {
        @Override
        public boolean filter(ComponentDefinition componentDefinition) {
            return true;
        }

        @Override
        public boolean supports(PlatformType type) {
            return type == PlatformType.AUTOMATION;
        }
    };

    @Test
    void testDisabledComponentHiddenFromListingButReturnedByNoArgGetter() {
        when(componentDefinitionRegistry.getStaticComponentDefinitions())
            .thenReturn(
                List.of(
                    (com.bytechef.component.definition.ComponentDefinition) component("slack")
                        .title("Slack")
                        .actions(action("sendMessage").title("Send Message")),
                    (com.bytechef.component.definition.ComponentDefinition) component("mailchimp")
                        .title("Mailchimp")
                        .actions(action("addMember").title("Add Member"))));
        when(componentDefinitionRegistry.getDynamicComponentDefinitions())
            .thenReturn(List.of());

        ComponentVisibilityProvider disableSlack = componentName -> !componentName.equals("slack");

        ComponentDefinitionServiceImpl service = new ComponentDefinitionServiceImpl(
            List.of(allowAllAutomationFilter), componentDefinitionRegistry, contextFactory, List.of(disableSlack));

        List<ComponentDefinition> listed =
            service.getComponentDefinitions(true, null, null, null, null, PlatformType.AUTOMATION);

        assertThat(listed)
            .extracting(ComponentDefinition::getName)
            .containsExactly("mailchimp");

        assertThat(service.getComponentDefinitions())
            .extracting(ComponentDefinition::getName)
            .contains("slack", "mailchimp");
    }

    @Test
    void testDisabledComponentHiddenFromSearchOverload() {
        when(componentDefinitionRegistry.getStaticComponentDefinitions())
            .thenReturn(
                List.of(
                    (com.bytechef.component.definition.ComponentDefinition) component("slack")
                        .title("Slack")
                        .actions(action("sendMessage").title("Send Message")),
                    (com.bytechef.component.definition.ComponentDefinition) component("mailchimp")
                        .title("Mailchimp")
                        .actions(action("addMember").title("Add Member"))));
        when(componentDefinitionRegistry.getDynamicComponentDefinitions())
            .thenReturn(List.of());

        ComponentVisibilityProvider disableSlack = componentName -> !componentName.equals("slack");

        ComponentDefinitionServiceImpl service = new ComponentDefinitionServiceImpl(
            List.of(allowAllAutomationFilter), componentDefinitionRegistry, contextFactory, List.of(disableSlack));

        assertThat(service.getComponentDefinitions("slack", PlatformType.AUTOMATION))
            .as("disabled provider 'slack' must not appear in search results")
            .isEmpty();

        assertThat(service.getComponentDefinitions("mailchimp", PlatformType.AUTOMATION))
            .as("enabled provider 'mailchimp' must appear in search results")
            .extracting(ComponentDefinition::getName)
            .containsExactly("mailchimp");
    }

    @Test
    void testGetComponentDefinitionFiltersDisabledActionsAndTriggers() {
        when(componentDefinitionRegistry.getComponentDefinition("slack", null))
            .thenReturn(
                (com.bytechef.component.definition.ComponentDefinition) component("slack")
                    .title("Slack")
                    .actions(
                        action("sendMessage").title("Send Message"),
                        action("deleteMessage").title("Delete Message"))
                    .triggers(
                        trigger("newMessage").title("New Message")
                            .type(TriggerType.STATIC_WEBHOOK),
                        trigger("newChannel").title("New Channel")
                            .type(TriggerType.STATIC_WEBHOOK)));

        ComponentVisibilityProvider disableSendMessageAndNewChannel = new ComponentVisibilityProvider() {
            @Override
            public boolean isVisible(String componentName) {
                return true;
            }

            @Override
            public boolean isActionVisible(String componentName, String actionName) {
                return !actionName.equals("sendMessage");
            }

            @Override
            public boolean isTriggerVisible(String componentName, String triggerName) {
                return !triggerName.equals("newChannel");
            }
        };

        ComponentDefinitionServiceImpl service = new ComponentDefinitionServiceImpl(
            List.of(allowAllAutomationFilter), componentDefinitionRegistry, contextFactory,
            List.of(disableSendMessageAndNewChannel));

        ComponentDefinition componentDefinition = service.getComponentDefinition("slack", null);

        assertThat(componentDefinition.getActions())
            .extracting(ActionDefinition::getName)
            .containsExactly("deleteMessage");

        assertThat(componentDefinition.getTriggers())
            .extracting(TriggerDefinition::getName)
            .containsExactly("newMessage");
    }

    @Test
    void testGetComponentDefinitionReturnsUnfilteredInstanceWhenNothingIsDisabled() {
        when(componentDefinitionRegistry.getComponentDefinition("mailchimp", null))
            .thenReturn(
                (com.bytechef.component.definition.ComponentDefinition) component("mailchimp")
                    .title("Mailchimp")
                    .actions(action("addMember").title("Add Member"))
                    .triggers(
                        trigger("newMember").title("New Member")
                            .type(TriggerType.STATIC_WEBHOOK)));

        ComponentVisibilityProvider allowAll = componentName -> true;

        ComponentDefinitionServiceImpl service = new ComponentDefinitionServiceImpl(
            List.of(allowAllAutomationFilter), componentDefinitionRegistry, contextFactory, List.of(allowAll));

        ComponentDefinition componentDefinition = service.getComponentDefinition("mailchimp", null);

        assertThat(componentDefinition.getActions())
            .extracting(ActionDefinition::getName)
            .containsExactly("addMember");

        assertThat(componentDefinition.getTriggers())
            .extracting(TriggerDefinition::getName)
            .containsExactly("newMember");
    }
}
