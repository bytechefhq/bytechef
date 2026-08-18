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

package com.bytechef.automation.ai.agent.config;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.agentChannel;
import static com.bytechef.component.definition.ComponentDsl.agentChannelRequest;
import static com.bytechef.component.definition.ComponentDsl.agentReply;
import static com.bytechef.component.definition.ComponentDsl.agentRequest;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.platform.component.domain.AgentChannelDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mockito.Mockito;

/**
 * The components this test slice's agents are reached through, as real SDK definitions projected onto the platform DTOs
 * the registry would serve. Only the shapes {@code AgentChannelResolver} reads are modelled — a trigger's declared
 * properties, the paired reply action, and the request/reply descriptors — not each component's full operation set.
 * <p>
 * {@code schedule} is present because it is the one non-channel the resolver synthesizes, and it reads the REAL
 * {@code cron} trigger's properties to build its row-parameter allow-list.
 *
 * @author Ivica Cardic
 */
public final class TestComponentDefinitions {

    private TestComponentDefinitions() {
    }

    public static ComponentDefinitionService componentDefinitionService() {
        Map<String, ComponentDefinition> componentDefinitions = new LinkedHashMap<>();

        for (ModifiableComponentDefinition modifiableComponentDefinition : List.of(
            chatComponentDefinition(), workflowComponentDefinition(), slackComponentDefinition(),
            telegramComponentDefinition(), twilioComponentDefinition(), scheduleComponentDefinition())) {

            componentDefinitions.put(
                modifiableComponentDefinition.getName(), new ComponentDefinition(modifiableComponentDefinition));
        }

        Map<String, AgentChannelDefinition> agentChannelDefinitions = new LinkedHashMap<>();

        for (ComponentDefinition componentDefinition : componentDefinitions.values()) {
            for (AgentChannelDefinition agentChannelDefinition : componentDefinition.getAgentChannels()) {
                agentChannelDefinitions.put(agentChannelDefinition.getName(), agentChannelDefinition);
            }
        }

        ComponentDefinitionService componentDefinitionService = Mockito.mock(ComponentDefinitionService.class);

        Mockito.when(componentDefinitionService.fetchAgentChannelDefinition(Mockito.anyString()))
            .thenAnswer(invocation -> Optional.ofNullable(agentChannelDefinitions.get(invocation.getArgument(0))));
        Mockito.when(componentDefinitionService.getAgentChannelDefinitions())
            .thenReturn(List.copyOf(agentChannelDefinitions.values()));
        Mockito.when(componentDefinitionService.getComponentDefinition(Mockito.anyString(), Mockito.any()))
            .thenAnswer(invocation -> {
                String name = invocation.getArgument(0);

                ComponentDefinition componentDefinition = componentDefinitions.get(name);

                if (componentDefinition == null) {
                    throw new IllegalArgumentException("No test component definition " + name);
                }

                return componentDefinition;
            });

        return componentDefinitionService;
    }

    private static ModifiableComponentDefinition chatComponentDefinition() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newChatRequest")
            .type(TriggerType.STATIC_WEBHOOK)
            .properties(integer("mode").defaultValue(1))
            .output(outputSchema(agentChannelRequest()))
            .agentRequest(agentRequest().attachments("attachments"));
        ModifiableActionDefinition actionDefinition = action("responseToRequest")
            .properties(string("message"), array("attachments"))
            .agentReply(agentReply().attachments("attachments"));

        return component("chat")
            .title("Chat")
            .triggers(triggerDefinition)
            .actions(actionDefinition)
            .agentChannels(
                agentChannel("chat", triggerDefinition, actionDefinition).title("Chat")
                    .approvalChannel("chat"));
    }

    private static ModifiableComponentDefinition workflowComponentDefinition() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newWorkflowCall")
            .type(TriggerType.STATIC_WEBHOOK)
            .properties(string("inputSchema"))
            .output(outputSchema(agentChannelRequest()))
            .agentRequest(agentRequest().attachments("attachments"));
        ModifiableActionDefinition actionDefinition = action("responseToWorkflowCall")
            .properties(string("outputSchema"), string("response"))
            .agentReply(
                agentReply().message("response.message")
                    .fixedParameter("outputSchema", "{\"type\":\"object\"}"));

        return component("workflow")
            .title("Workflow")
            .triggers(triggerDefinition)
            .actions(actionDefinition)
            .agentChannels(
                agentChannel("workflowCall", triggerDefinition, actionDefinition).title("Workflow Call")
                    .triggerParameters(Map.of("inputSchema", "{\"type\":\"object\"}")));
    }

    private static ModifiableComponentDefinition slackComponentDefinition() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(outputSchema(agentChannelRequest()))
            .agentRequest(agentRequest().attachments("attachments"));
        ModifiableActionDefinition actionDefinition = action("sendChannelMessage")
            .properties(string("channel"), string("text"))
            .agentReply(
                agentReply().conversationId("channel")
                    .message("text"));

        return component("slack")
            .title("Slack")
            .triggers(triggerDefinition)
            .actions(actionDefinition)
            .agentChannels(
                agentChannel("slack", triggerDefinition, actionDefinition).title("Slack")
                    .approvalChannel("slack"));
    }

    /**
     * The one connection-requiring channel this slice reaches: {@code validateForPublish} refuses to publish an agent
     * whose connection-requiring channel has no connection wired, and reads that flag off the component.
     */
    private static ModifiableComponentDefinition telegramComponentDefinition() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .output(outputSchema(agentChannelRequest()))
            .agentRequest(agentRequest().attachments("attachments"));
        ModifiableActionDefinition actionDefinition = action("sendMessage")
            .properties(string("chat_id"), string("text"))
            .agentReply(
                agentReply().conversationId("chat_id")
                    .message("text"));

        return component("telegram")
            .title("Telegram")
            .connection(connection().properties(string("token").required(true)))
            .triggers(triggerDefinition)
            .actions(actionDefinition)
            .agentChannels(
                agentChannel("telegram", triggerDefinition, actionDefinition).title("Telegram")
                    .approvalChannel("telegram"));
    }

    /**
     * The one channel whose reply action has a REQUIRED property fed from the channel row, mirroring the real twilio
     * WhatsApp pair: the reply's {@code From} is required, while the trigger's {@code number} that supplies it is
     * deliberately optional (making it required would invalidate every saved workflow). That asymmetry is what publish
     * validation has to catch, so it is modelled exactly.
     */
    private static ModifiableComponentDefinition twilioComponentDefinition() {
        ModifiableTriggerDefinition triggerDefinition = trigger("newWhatsappMessage")
            .type(TriggerType.STATIC_WEBHOOK)
            .properties(string("number"))
            .output(outputSchema(agentChannelRequest()))
            .agentRequest(agentRequest());
        ModifiableActionDefinition actionDefinition = action("sendWhatsAppMessage")
            .properties(
                string("From").required(true), string("To").required(true), string("Body").required(true))
            .agentReply(
                agentReply().conversationId("To")
                    .message("Body")
                    .channelParameter("number", "From"));

        return component("twilio")
            .title("Twilio")
            .triggers(triggerDefinition)
            .actions(actionDefinition)
            .agentChannels(
                agentChannel("twilio", triggerDefinition, actionDefinition).title("Twilio"));
    }

    /**
     * Declares no agent channel, exactly as the real {@code schedule} component does — the resolver synthesizes the
     * schedule entry and reads {@code cron}'s properties off this definition.
     */
    private static ModifiableComponentDefinition scheduleComponentDefinition() {
        return component("schedule")
            .title("Schedule")
            .triggers(
                trigger("cron").type(TriggerType.LISTENER)
                    .properties(string("expression"), string("timezone")));
    }
}
