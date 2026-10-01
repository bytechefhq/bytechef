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

package com.bytechef.automation.ai.agent.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.agent.channel.ResolvedAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgent;
import com.bytechef.automation.ai.agent.domain.AiAgentChannel;
import com.bytechef.automation.ai.agent.domain.AiAgentElement;
import com.bytechef.automation.ai.agent.dto.AiAgentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO.AiAgentDeploymentTriggerDTO;
import com.bytechef.automation.ai.agent.dto.AiAgentDeploymentDTO.AiAgentDeploymentWorkflowDTO;
import com.bytechef.automation.ai.agent.dto.ChatAgentDTO;
import com.bytechef.automation.ai.agent.exception.AiAgentErrorType;
import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.agent.web.graphql.config.AiAgentGraphQlConfigurationSharedMocks;
import com.bytechef.automation.ai.agent.web.graphql.config.AiAgentGraphQlTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AiAgentGraphQlTestConfiguration.class,
    AiAgentGraphQlController.class
})
@GraphQlTest(
    controllers = AiAgentGraphQlController.class,
    properties = "spring.graphql.schema.locations=classpath*:/graphql/")
@AiAgentGraphQlConfigurationSharedMocks
class AiAgentGraphQlControllerTest {

    @Autowired
    private AiAgentFacade agentFacade;

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void testAgentsReturnsMappedRowsFromFacade() {
        AiAgent agent = createMockAgent(1L, "agent-one", "Agent One", "desc", 10L);
        AiAgentChannel channel = createMockChannel(5L, "chat", 0);
        AiAgentElement element = createMockElement(7L, AiAgentElement.KIND_MODEL, 0);
        AiAgentDTO agentDTO = new AiAgentDTO(
            agent, 100L, List.of(channel), List.of(element), true, 0, null);

        when(agentFacade.getAgents(10L)).thenReturn(List.of(agentDTO));

        this.graphQlTester
            .document("""
                query {
                    aiAgents(workspaceId: "10") {
                        id
                        name
                        title
                        workspaceId
                        projectId
                        unpublishedChanges
                        lastPublishedVersion
                        channels {
                            id
                            channelType
                        }
                        elements {
                            id
                            kind
                        }
                    }
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("aiAgents[0].id")
            .entity(String.class)
            .isEqualTo("1")
            .path("aiAgents[0].name")
            .entity(String.class)
            .isEqualTo("agent-one")
            .path("aiAgents[0].title")
            .entity(String.class)
            .isEqualTo("Agent One")
            .path("aiAgents[0].workspaceId")
            .entity(String.class)
            .isEqualTo("10")
            .path("aiAgents[0].projectId")
            .entity(String.class)
            .isEqualTo("100")
            .path("aiAgents[0].unpublishedChanges")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("aiAgents[0].lastPublishedVersion")
            .entity(Integer.class)
            .isEqualTo(0)
            .path("aiAgents[0].channels[0].id")
            .entity(String.class)
            .isEqualTo("5")
            .path("aiAgents[0].channels[0].channelType")
            .entity(String.class)
            .isEqualTo("chat")
            .path("aiAgents[0].elements[0].id")
            .entity(String.class)
            .isEqualTo("7")
            .path("aiAgents[0].elements[0].kind")
            .entity(String.class)
            .isEqualTo(AiAgentElement.KIND_MODEL);
    }

    @Test
    void testAiAgentChannelDefinitionsReturnsMappedRowsFromFacade() {
        ResolvedAgentChannel slackChannel = new ResolvedAgentChannel(
            "slack", "Slack", "Reach the agent from a Slack channel", "path:assets/slack.svg", "slack/v1/newMessage",
            "slack/v1/sendChannelMessage", true, Map.of(), Map.of(), Set.of(), Set.of(),
            new ResolvedAgentChannel.Binding(
                "conversationId", "message", null, "text", "channel", null, Map.of(), Map.of()),
            new ResolvedAgentChannel.ApprovalDelivery("slack", "slack"));
        ResolvedAgentChannel scheduleChannel = new ResolvedAgentChannel(
            "schedule", "Schedule", null, "path:assets/schedule.svg", "schedule/v1/cron", null, false, Map.of(),
            Map.of(), Set.of("expression", "timezone"), Set.of(),
            new ResolvedAgentChannel.Binding(null, null, null, null, null, null, Map.of(), Map.of()), null);

        when(agentFacade.getAgentChannelDefinitions()).thenReturn(List.of(slackChannel, scheduleChannel));

        this.graphQlTester
            .document("""
                query {
                    aiAgentChannelDefinitions {
                        channelType
                        componentName
                        componentVersion
                        triggerName
                        replyActionName
                        title
                        description
                        icon
                        connectionRequired
                        propertiesConfigurable
                        approvalCapable
                        pinned
                        schedule
                    }
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("aiAgentChannelDefinitions[0].channelType")
            .entity(String.class)
            .isEqualTo("slack")
            .path("aiAgentChannelDefinitions[0].componentName")
            .entity(String.class)
            .isEqualTo("slack")
            .path("aiAgentChannelDefinitions[0].componentVersion")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("aiAgentChannelDefinitions[0].triggerName")
            .entity(String.class)
            .isEqualTo("newMessage")
            .path("aiAgentChannelDefinitions[0].replyActionName")
            .entity(String.class)
            .isEqualTo("sendChannelMessage")
            .path("aiAgentChannelDefinitions[0].title")
            .entity(String.class)
            .isEqualTo("Slack")
            .path("aiAgentChannelDefinitions[0].description")
            .entity(String.class)
            .isEqualTo("Reach the agent from a Slack channel")
            .path("aiAgentChannelDefinitions[0].icon")
            .entity(String.class)
            .isEqualTo("path:assets/slack.svg")
            .path("aiAgentChannelDefinitions[0].connectionRequired")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("aiAgentChannelDefinitions[0].approvalCapable")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("aiAgentChannelDefinitions[0].pinned")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("aiAgentChannelDefinitions[0].schedule")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("aiAgentChannelDefinitions[0].propertiesConfigurable")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("aiAgentChannelDefinitions[1].channelType")
            .entity(String.class)
            .isEqualTo("schedule")
            .path("aiAgentChannelDefinitions[1].componentName")
            .entity(String.class)
            .isEqualTo("schedule")
            .path("aiAgentChannelDefinitions[1].triggerName")
            .entity(String.class)
            .isEqualTo("cron")
            .path("aiAgentChannelDefinitions[1].replyActionName")
            .valueIsNull()
            .path("aiAgentChannelDefinitions[1].title")
            .entity(String.class)
            .isEqualTo("Schedule")
            .path("aiAgentChannelDefinitions[1].connectionRequired")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("aiAgentChannelDefinitions[1].approvalCapable")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("aiAgentChannelDefinitions[1].pinned")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("aiAgentChannelDefinitions[1].schedule")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("aiAgentChannelDefinitions[1].propertiesConfigurable")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(agentFacade).getAgentChannelDefinitions();
    }

    @Test
    void testAiAgentChannelDefinitionsMarksChatAndWorkflowCallPinned() {
        ResolvedAgentChannel chatChannel = new ResolvedAgentChannel(
            "chat", "Chat", null, "path:assets/chat.svg", "chat/v1/newChatRequest", "chat/v1/responseToRequest", false,
            Map.of(), Map.of(), Set.of(), Set.of(),
            new ResolvedAgentChannel.Binding(
                "conversationId", "message", null, "message", null, null, Map.of(), Map.of()),
            null);
        ResolvedAgentChannel workflowCallChannel = new ResolvedAgentChannel(
            "workflowCall", "Workflow Call", null, "path:assets/workflow.svg", "workflow/v1/newWorkflowCall",
            "workflow/v1/responseToWorkflowCall", false, Map.of(), Map.of(), Set.of(), Set.of(),
            new ResolvedAgentChannel.Binding(
                "conversationId", "message", null, "response.message", null, null, Map.of(), Map.of()),
            null);

        when(agentFacade.getAgentChannelDefinitions()).thenReturn(List.of(chatChannel, workflowCallChannel));

        this.graphQlTester
            .document("""
                query {
                    aiAgentChannelDefinitions {
                        channelType
                        pinned
                        schedule
                    }
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("aiAgentChannelDefinitions[0].pinned")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("aiAgentChannelDefinitions[0].schedule")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("aiAgentChannelDefinitions[1].pinned")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("aiAgentChannelDefinitions[1].schedule")
            .entity(Boolean.class)
            .isEqualTo(false);
    }

    @Test
    void testAgentDeploymentsReturnsMappedRowsFromFacade() {
        AiAgentDeploymentTriggerDTO chatTrigger = new AiAgentDeploymentTriggerDTO(
            "trigger_1", "chat/v1/chat", Map.of(), "https://example.com/webhooks/abc");
        AiAgentDeploymentTriggerDTO telegramTrigger = new AiAgentDeploymentTriggerDTO(
            "trigger_2", "telegram/v1/newMessage", Map.of(), null);
        AiAgentDeploymentWorkflowDTO agentDeploymentWorkflowDTO = new AiAgentDeploymentWorkflowDTO(
            "workflow-1", true, List.of(chatTrigger, telegramTrigger));
        AiAgentDeploymentDTO agentDeploymentDTO = new AiAgentDeploymentDTO(
            20L, "agent-one", 1L, "Agent One", 100L, 1, true, 3, List.of(agentDeploymentWorkflowDTO), null);

        when(agentFacade.getAgentDeployments(10L)).thenReturn(List.of(agentDeploymentDTO));

        this.graphQlTester
            .document("""
                query {
                    aiAgentDeployments(workspaceId: "10") {
                        id
                        name
                        agentId
                        agentTitle
                        projectId
                        environmentId
                        enabled
                        projectVersion
                        workflows {
                            workflowId
                            triggers {
                                name
                                type
                                staticWebhookUrl
                            }
                        }
                    }
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("aiAgentDeployments[0].id")
            .entity(String.class)
            .isEqualTo("20")
            .path("aiAgentDeployments[0].name")
            .entity(String.class)
            .isEqualTo("agent-one")
            .path("aiAgentDeployments[0].agentId")
            .entity(String.class)
            .isEqualTo("1")
            .path("aiAgentDeployments[0].agentTitle")
            .entity(String.class)
            .isEqualTo("Agent One")
            .path("aiAgentDeployments[0].projectId")
            .entity(String.class)
            .isEqualTo("100")
            .path("aiAgentDeployments[0].environmentId")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("aiAgentDeployments[0].enabled")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("aiAgentDeployments[0].projectVersion")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("aiAgentDeployments[0].workflows[0].workflowId")
            .entity(String.class)
            .isEqualTo("workflow-1")
            .path("aiAgentDeployments[0].workflows[0].triggers[0].staticWebhookUrl")
            .entity(String.class)
            .isEqualTo("https://example.com/webhooks/abc")
            .path("aiAgentDeployments[0].workflows[0].triggers[1].type")
            .entity(String.class)
            .isEqualTo("telegram/v1/newMessage")
            .path("aiAgentDeployments[0].workflows[0].triggers[1].staticWebhookUrl")
            .valueIsNull();

        verify(agentFacade).getAgentDeployments(10L);
    }

    @Test
    void testWorkspaceChatAgentsReturnsMappedRowsFromFacade() {
        ChatAgentDTO chatAgentDTO = new ChatAgentDTO(
            7L, "support-agent", "Support Agent", 11L, "automation:11:uuid:trigger_1", "Chat");

        when(agentFacade.getWorkspaceChatAgents(1L, 2L)).thenReturn(List.of(chatAgentDTO));

        this.graphQlTester
            .document("""
                query {
                    workspaceChatAgents(workspaceId: "1", environmentId: "2") {
                        aiAgentId
                        agentName
                        agentTitle
                        projectDeploymentId
                        workflowExecutionId
                        workflowLabel
                    }
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("workspaceChatAgents[0].aiAgentId")
            .entity(String.class)
            .isEqualTo("7")
            .path("workspaceChatAgents[0].agentName")
            .entity(String.class)
            .isEqualTo("support-agent")
            .path("workspaceChatAgents[0].agentTitle")
            .entity(String.class)
            .isEqualTo("Support Agent")
            .path("workspaceChatAgents[0].projectDeploymentId")
            .entity(String.class)
            .isEqualTo("11")
            .path("workspaceChatAgents[0].workflowExecutionId")
            .entity(String.class)
            .isEqualTo("automation:11:uuid:trigger_1")
            .path("workspaceChatAgents[0].workflowLabel")
            .entity(String.class)
            .isEqualTo("Chat");

        verify(agentFacade).getWorkspaceChatAgents(1L, 2L);
    }

    @Test
    void testCreateAgentMapsInputToFacadeCall() {
        AiAgent createdAgent = createMockAgent(2L, "new-agent", "New Agent", "d", 10L);
        AiAgentDTO agentDTO = new AiAgentDTO(
            createdAgent, 100L, List.of(), List.of(), true, 0, null);

        when(agentFacade.createAgent("New Agent", "d", 10L, 100L)).thenReturn(agentDTO);

        this.graphQlTester
            .document("""
                mutation {
                    createAiAgent(
                        input: { title: "New Agent", description: "d", workspaceId: "10", projectId: "100" }
                    ) {
                        id
                        title
                        description
                        projectId
                        projectWorkflowUuid
                    }
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("createAiAgent.id")
            .entity(String.class)
            .isEqualTo("2")
            .path("createAiAgent.title")
            .entity(String.class)
            .isEqualTo("New Agent")
            .path("createAiAgent.description")
            .entity(String.class)
            .isEqualTo("d")
            .path("createAiAgent.projectId")
            .entity(String.class)
            .isEqualTo("100")
            .path("createAiAgent.projectWorkflowUuid")
            .entity(String.class)
            .isEqualTo(String.valueOf(createdAgent.getProjectWorkflowUuid()));

        verify(agentFacade).createAgent("New Agent", "d", 10L, 100L);
    }

    @Test
    void testDeleteAgentChannelPropagatesTypedErrorAsGraphQlError() {
        doThrow(new ConfigurationException(
            "Channel 5 of type 'chat' cannot be deleted", AiAgentErrorType.CHANNEL_NOT_DELETABLE))
                .when(agentFacade)
                .deleteAgentChannel(5L);

        this.graphQlTester
            .document("""
                mutation {
                    deleteAiAgentChannel(id: "5")
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.get(0)
                    .getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
            });

        verify(agentFacade).deleteAgentChannel(5L);
    }

    @Test
    void testUpdateAgentChannelPassesNullConnectionIdThrough() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAiAgentChannel(input: { id: "5", parameters: { key: "value" } })
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("updateAiAgentChannel")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(agentFacade).updateAgentChannel(eq(5L), eq(Map.of("key", "value")), isNull());
    }

    @Test
    void testUpdateAiAgentSettingsPassesSettingsThrough() {
        this.graphQlTester
            .document("""
                mutation {
                    updateAiAgentSettings(id: "5", settings: { builtInTools: { webSearch: true } })
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("updateAiAgentSettings")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(agentFacade).updateAgentSettings(5L, Map.of("builtInTools", Map.of("webSearch", true)));
    }

    @Test
    void testAgentSettingsFieldReflectsAgentSettings() {
        AiAgent agent = createMockAgent(1L, "agent-one", "Agent One", "desc", 10L);

        agent.setSettings(Map.of("builtInTools", Map.of("webSearch", true)));

        AiAgentDTO agentDTO = new AiAgentDTO(
            agent, 100L, List.of(), List.of(), true, 0, null);

        when(agentFacade.getAgent(1L)).thenReturn(agentDTO);

        this.graphQlTester
            .document("""
                query {
                    aiAgent(id: "1") {
                        settings
                    }
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("aiAgent.settings")
            .entity(Map.class)
            .isEqualTo(Map.of("builtInTools", Map.of("webSearch", true)));
    }

    private AiAgent createMockAgent(long id, String name, String title, String description, Long workspaceId) {

        AiAgent agent = new AiAgent();

        agent.setId(id);
        agent.setName(name);
        agent.setTitle(title);
        agent.setDescription(description);
        agent.setWorkspaceId(workspaceId);
        agent.setUuid(UUID.randomUUID());
        agent.setProjectWorkflowUuid(UUID.randomUUID());

        return agent;
    }

    private AiAgentChannel createMockChannel(long id, String channelType, int position) {
        AiAgentChannel channel = new AiAgentChannel(0, channelType);

        channel.setId(id);
        channel.setPosition(position);

        return channel;
    }

    private AiAgentElement createMockElement(long id, String kind, int position) {
        AiAgentElement element = new AiAgentElement(0, kind);

        element.setId(id);
        element.setPosition(position);

        return element;
    }
}
