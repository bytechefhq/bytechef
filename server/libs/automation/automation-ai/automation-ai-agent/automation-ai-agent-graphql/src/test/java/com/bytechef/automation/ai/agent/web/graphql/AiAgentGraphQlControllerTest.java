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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * Slice tests for {@link AiAgentGraphQlController} against a mocked {@link AiAgentFacade} — every mutation/query is
 * pinned to a single facade call with the string-ID-to-{@code long} argument mapping GraphQL requires, and one test
 * confirms a typed {@link ConfigurationException} raised by the facade surfaces as a GraphQL error rather than an
 * uncaught exception or a silently-successful response.
 *
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
        AiAgent agent = createMockAgent(1L, "agent-one", "Agent One", "desc", 10L, 100L);
        AiAgentChannel channel = createMockChannel(5L, "chat", 0);
        AiAgentElement element = createMockElement(7L, AiAgentElement.KIND_MODEL, 0);
        AiAgentDTO agentDTO = new AiAgentDTO(agent, List.of(channel), List.of(element), true, 0, null, List.of());

        when(agentFacade.getAgents(10L)).thenReturn(List.of(agentDTO));

        this.graphQlTester
            .document("""
                query {
                    aiAgents(workspaceId: "10") {
                        id
                        name
                        title
                        workspaceId
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
    void testAgentDeploymentsReturnsMappedRowsFromFacade() {
        AiAgentDeploymentTriggerDTO chatTrigger = new AiAgentDeploymentTriggerDTO(
            "trigger_1", "chat/v1/chat", Map.of(), "https://example.com/webhooks/abc");
        AiAgentDeploymentTriggerDTO telegramTrigger = new AiAgentDeploymentTriggerDTO(
            "trigger_2", "telegram/v1/newMessage", Map.of(), null);
        AiAgentDeploymentWorkflowDTO agentDeploymentWorkflowDTO = new AiAgentDeploymentWorkflowDTO(
            "workflow-1", true, List.of(chatTrigger, telegramTrigger));
        AiAgentDeploymentDTO agentDeploymentDTO = new AiAgentDeploymentDTO(
            20L, "agent-one", 1L, "Agent One", 100L, 1, true, 3, List.of(agentDeploymentWorkflowDTO), List.of(), null);

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

    /**
     * Pins the "Agent Chats" read model at the GraphQL boundary: both string IDs reach
     * {@link AiAgentFacade#getWorkspaceChatAgents(Long, long)} as numbers, and every {@code ChatAgentDTO} component is
     * exposed under the exact {@code ChatAgent} field name the client queries — in particular
     * {@code workflowExecutionId}, which the client feeds back verbatim as the chat webhook's path segment, so a
     * renamed or reshaped field here would leave the client with rows it cannot open.
     */
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
        AiAgent createdAgent = createMockAgent(2L, "new-agent", "New Agent", "d", 10L, 100L);
        AiAgentDTO agentDTO = new AiAgentDTO(createdAgent, List.of(), List.of(), true, 0, null, List.of());

        when(agentFacade.createAgent("New Agent", "d", 10L)).thenReturn(agentDTO);

        this.graphQlTester
            .document("""
                mutation {
                    createAiAgent(input: { title: "New Agent", description: "d", workspaceId: "10" }) {
                        id
                        title
                        description
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
            .isEqualTo("d");

        verify(agentFacade).createAgent("New Agent", "d", 10L);
    }

    /**
     * Would fail if the mutation stopped delegating to {@link AiAgentFacade#deleteAgentChannel(long)}, or if a typed
     * error raised by the facade were swallowed instead of surfacing to the caller as a GraphQL error. This slice test
     * does not include the app-wide {@code GlobalDataFetcherExceptionResolver} — it lives in {@code graphql-impl}
     * (package-private, so it cannot even be named from another module's test) and is only ever picked up via
     * {@code ServerApplication}'s {@code scanBasePackages = "com.bytechef"} full component scan in the running server.
     * So, same as {@code McpServerGraphQlControllerIntTest}'s pinned-INTERNAL_ERROR precedent, the error here surfaces
     * through Spring GraphQL's default (sanitized, detail-free) {@code INTERNAL_ERROR} handling rather than the
     * {@code BAD_REQUEST}-with-message classification {@link ConfigurationException} gets in the running server. What
     * this test pins is narrower and resolver-independent: the exception reaches the client as a single GraphQL error
     * (not an uncaught exception, not a silently-successful response) rather than a fabricated {@code true} — and since
     * {@code deleteAiAgentChannel} is non-null ({@code Boolean!}), the error nulls out the entire {@code data} object
     * per standard GraphQL null-propagation, rather than leaving a partial response behind.
     */
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
    void testPublishAgentReturnsNewDraftVersion() {
        when(agentFacade.publishAgent(3L, "First release")).thenReturn(2);

        this.graphQlTester
            .document("""
                mutation {
                    publishAiAgent(id: "3", description: "First release")
                }
                """)
            .execute()
            .errors()
            .verify()
            .path("publishAiAgent")
            .entity(Integer.class)
            .isEqualTo(2);

        verify(agentFacade).publishAgent(3L, "First release");
    }

    /**
     * Pins {@link AiAgentFacade#updateAgentChannel(long, Map, Long)}'s partial-update contract at the GraphQL boundary:
     * a {@code connectionId} left out of the input must reach the facade as {@code null} (leave-unchanged), not as some
     * sentinel/zero value the controller invents.
     */
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

    /**
     * Pins {@link AiAgentFacade#updateAgentSettings(long, Map)}'s GraphQL wiring: the raw {@code settings} object
     * literal reaches the facade untouched (no controller-side reshaping), and the mutation returns {@code true} on
     * success, mirroring {@code updateAiAgentChannel}/{@code updateAiAgentElement}'s void-facade-call-then-true shape.
     */
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

    /**
     * Pins the {@code AiAgent.settings} field to {@link AiAgentDTO#settings()} (a flattened read of
     * {@code AiAgent.getSettings()}), the same read path {@code title}/{@code description} etc. already use.
     */
    @Test
    void testAgentSettingsFieldReflectsAgentSettings() {
        AiAgent agent = createMockAgent(1L, "agent-one", "Agent One", "desc", 10L, 100L);

        agent.setSettings(Map.of("builtInTools", Map.of("webSearch", true)));

        AiAgentDTO agentDTO = new AiAgentDTO(agent, List.of(), List.of(), true, 0, null, List.of());

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

    private AiAgent createMockAgent(
        long id, String name, String title, String description, Long workspaceId, long projectId) {

        AiAgent agent = new AiAgent();

        agent.setId(id);
        agent.setName(name);
        agent.setTitle(title);
        agent.setDescription(description);
        agent.setWorkspaceId(workspaceId);
        agent.setProjectId(projectId);
        agent.setUuid(UUID.randomUUID());

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
