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

package com.bytechef.platform.component;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.agentChannel;
import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static com.bytechef.component.definition.ComponentDsl.agentReply;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.AgentChannelDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ComponentDefinitionRegistryAgentChannelTest {

    private static final ModifiableTriggerDefinition TRIGGER = trigger("newAiAgentMessage")
        .type(TriggerType.STATIC_WEBHOOK)
        .output(outputSchema(agentChannelRequest()))
        .agentRequest(agentRequest());
    private static final ModifiableActionDefinition ACTION = action("sendAiAgentReply")
        .properties(string(AgentChannelDefinition.MESSAGE), string(AgentChannelDefinition.CONVERSATION_ID))
        .agentReply(agentReply());

    @Test
    void testGetAgentChannelDefinitionsCollectsAcrossComponents() {
        ComponentDefinitionRegistry registry = registry(
            handler(component("acme").triggers(TRIGGER)
                .actions(ACTION)
                .agentChannels(agentChannel("acme", TRIGGER, ACTION).title("Acme"))),
            handler(component("beta").triggers(TRIGGER)
                .agentChannels(agentChannel("beta", TRIGGER))));

        List<com.bytechef.platform.component.domain.AgentChannelDefinition> definitions =
            registry.getAgentChannelDefinitions();

        assertThat(definitions).extracting("name")
            .containsExactly("acme", "beta");
        assertThat(registry.fetchAgentChannelDefinition("acme"))
            .map(com.bytechef.platform.component.domain.AgentChannelDefinition::getReplyActionName)
            .contains("sendAiAgentReply");
        // getReplyActionName() is null for "beta" (no reply action); Optional.map collapses a null-returning
        // mapper to empty, and AssertJ's contains() rejects a null expected value, so emptiness is the correct
        // assertion here.
        assertThat(registry.fetchAgentChannelDefinition("beta"))
            .map(com.bytechef.platform.component.domain.AgentChannelDefinition::getReplyActionName)
            .isEmpty();
        assertThat(registry.fetchAgentChannelDefinition("nope"))
            .isEmpty();
    }

    @Test
    void testDuplicateAgentChannelNamesFailFast() {
        ComponentDefinitionRegistry registry = registry(
            handler(component("acme").triggers(TRIGGER)
                .agentChannels(agentChannel("dup", TRIGGER))),
            handler(component("beta").triggers(TRIGGER)
                .agentChannels(agentChannel("dup", TRIGGER))));

        assertThatThrownBy(registry::getAgentChannelDefinitions)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("dup");
    }

    @Test
    void testAgentChannelReferencingUnregisteredTriggerFailsFast() {
        ComponentDefinitionRegistry registry = registry(
            handler(component("acme").agentChannels(agentChannel("acme", TRIGGER))));

        assertThatThrownBy(registry::getAgentChannelDefinitions)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("newAiAgentMessage");
    }

    private static ComponentHandler handler(ComponentDefinition componentDefinition) {
        return () -> componentDefinition;
    }

    private static ComponentDefinitionRegistry registry(ComponentHandler... componentHandlers) {
        return new ComponentDefinitionRegistry(
            new com.bytechef.config.ApplicationProperties(), List.of(componentHandlers), List::of, List.of(),
            Optional::empty);
    }
}
