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

package com.bytechef.platform.component.definition;

import static com.bytechef.component.definition.AgentChannelDefinition.ATTACHMENTS;
import static com.bytechef.component.definition.AgentChannelDefinition.MESSAGE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.agentChannel;
import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static com.bytechef.component.definition.ComponentDsl.agentReply;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AbstractComponentDefinitionWrapperTest {

    /**
     * Every {@code @AutoService} component reaches the registry through {@code AbstractComponentHandlerLoader}, which
     * wraps its definition. A field the wrapper forgets to copy is not merely absent from the wrapper -- it falls back
     * to {@code AgentChannelComponentDefinition}'s empty default, so the component silently declares no channels while
     * its own definition declares one. That made every ServiceLoader-loaded channel (slack, telegram, twilio,
     * rocketchat, infobip, whatsapp) invisible to the agent channel registry, and poisoned the build-time component
     * index, which reads the same wrapped definitions.
     */
    @Test
    void testWrapperPreservesAgentChannels() {
        ComponentDefinition componentDefinition = componentWithAgentChannel();

        ComponentDefinitionWrapper componentDefinitionWrapper =
            new ComponentDefinitionWrapper(componentDefinition, componentDefinition.getActions());

        assertThat(componentDefinitionWrapper.getAgentChannels())
            .extracting(agentChannelDefinition -> agentChannelDefinition.getName())
            .containsExactly("slack");
    }

    @Test
    void testWrapperReportsNoAgentChannelsForAComponentDeclaringNone() {
        ComponentDefinition componentDefinition = component("brave").version(1);

        ComponentDefinitionWrapper componentDefinitionWrapper =
            new ComponentDefinitionWrapper(componentDefinition, List.of());

        assertThat(componentDefinitionWrapper.getAgentChannels()).isEmpty();
    }

    private static ComponentDefinition componentWithAgentChannel() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(outputSchema(agentChannelRequest()))
            .agentRequest(agentRequest());

        ModifiableActionDefinition actionDefinition = action("sendChannelMessage")
            .properties(
                string(MESSAGE),
                array(ATTACHMENTS).items(fileEntry()))
            .agentReply(agentReply().attachments(ATTACHMENTS));

        return component("slack")
            .actions(actionDefinition)
            .triggers(triggerDefinition)
            .agentChannels(
                agentChannel("slack", triggerDefinition, actionDefinition)
                    .title("Slack"))
            .version(1);
    }
}
