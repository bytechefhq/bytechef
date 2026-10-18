/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.ai.hub.approval.AiHubApprovalGateToolCallback;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.ai.hub.util.Mode;
import com.bytechef.ee.ai.hub.util.Source;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiHubSpringAIAgentTest {

    @Test
    void testBuildInvocationContextExtractsWorkspaceIdFromState() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);

        AiHubToolInvocationContext context = agent.buildInvocationContext(newInput(state));

        assertThat(context.workspaceId()).isEqualTo(7L);
        assertThat(context.sourceOrdinal()).isEqualTo((short) Source.AI_HUB.ordinal());
        assertThat(context.lastUserPrompt()).isEqualTo("Draft a spec");
    }

    /**
     * Privilege-escalation defense: a malicious or buggy client could populate {@code state.workspaceId} with a
     * different value than the controller-injected {@code VERIFIED_WORKSPACE_ID}. The agent MUST read identity from the
     * verified key only.
     */
    @Test
    void testBuildInvocationContextIgnoresClientSuppliedWorkspaceIdWhenVerifiedKeyIsPresent() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        state.set("workspaceId", 999L);

        AiHubToolInvocationContext context = agent.buildInvocationContext(newInput(state));

        assertThat(context.workspaceId())
            .as("Agent must use server-injected VERIFIED_WORKSPACE_ID, not client-supplied workspaceId")
            .isEqualTo(7L);
    }

    @Test
    void testToolContextCarriesTheGateKeysAndTheAgentMode() throws AGUIException {
        AiHubSpringAIAgent agent = newAgentBuilder()
            .mode(Mode.BUILD)
            .build();

        State state = verifiedState();

        state.set("mode", "ASK");

        Map<String, Object> toolContext = agent.toolContext(newInput(state));

        AgentToolInvocationContext invocationContext =
            AgentToolInvocationContext.fromToolContext(new ToolContext(toolContext));

        assertThat(invocationContext).isNotNull();
        assertThat(invocationContext.conversationId()).isEqualTo("thread-1");
        assertThat(invocationContext.workspaceId()).isEqualTo(7L);
        assertThat(invocationContext.userId()).isEqualTo(10L);
        assertThat(invocationContext.tenantId()).isEqualTo("acme");
        assertThat(toolContext).containsEntry(AiHubApprovalGateToolCallback.TOOL_CONTEXT_MODE_KEY, "BUILD");
        assertThat(toolContext).containsEntry(AiHubToolInvocationContext.TOOL_CONTEXT_THREAD_ID_KEY, "thread-1");
    }

    @Test
    void testAdvisorParamsCarryOnlyTheConversationId() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        assertThat(agent.advisorParams(newInput(verifiedState())))
            .containsExactly(Map.entry(ChatMemory.CONVERSATION_ID, "thread"));
    }

    @Test
    void testEndsTurnOnAwaitingAnswerAndAwaitingApproval() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        assertThat(agent.endsTurn(toolResponse("{\"kind\":\"ask-user-question\",\"awaitingAnswer\":true}")))
            .isTrue();
        assertThat(agent.endsTurn(toolResponse("{\"kind\":\"tool-approval-request\",\"awaitingApproval\":true}")))
            .isTrue();
        assertThat(agent.endsTurn(toolResponse("{\"awaitingAnswer\":false,\"awaitingApproval\":false}"))).isFalse();
        assertThat(agent.endsTurn(toolResponse("{\"result\":\"ok\"}"))).isFalse();
        assertThat(agent.endsTurn(toolResponse("awaitingApproval: true, not json"))).isFalse();
        assertThat(agent.endsTurn(toolResponse(null))).isFalse();
    }

    @Test
    void testCreateSystemMessageIncludesActiveTabContext() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        state.set("activeTab", Map.of("kind", "workflow", "id", "42", "name", "Leads Sync"));

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).contains("Active Tab");
        assertThat(systemMessage.getContent()).contains("kind=workflow");
        assertThat(systemMessage.getContent()).contains("id=42");
        assertThat(systemMessage.getContent()).contains("name=Leads Sync");
    }

    @Test
    void testCreateSystemMessageIncludesReferencedResourcesContext() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        state.set(
            "referencedResources",
            List.of(
                Map.of("kind", "workflow", "id", "42", "name", "Leads Sync"),
                Map.of("kind", "project", "id", "7", "name", "Sales")));

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).contains("Referenced Resources");
        assertThat(systemMessage.getContent()).contains("kind=workflow");
        assertThat(systemMessage.getContent()).contains("name=Leads Sync");
        assertThat(systemMessage.getContent()).contains("kind=project");
        assertThat(systemMessage.getContent()).contains("name=Sales");
    }

    @Test
    void testCreateSystemMessageOmitsActiveTabAndReferencedResourcesWhenMissing() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        state.set("activeFileId", "42");
        state.set("currentTabs", List.of(Map.of("fileId", "42", "name", "spec.md", "viewMode", "preview")));

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).doesNotContain("Active Tab");
        assertThat(systemMessage.getContent()).doesNotContain("Referenced Resources");
        assertThat(systemMessage.getContent()).doesNotContain("Active File");
        assertThat(systemMessage.getContent()).doesNotContain("Open Tabs");
    }

    private static State verifiedState() {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        state.set(AiHubStateKeys.AUTHENTICATED_USER_ID, 10L);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, "thread-1");
        state.set(AiHubStateKeys.VERIFIED_TENANT_ID, "acme");

        return state;
    }

    private static RunAgentInput newInput(State state) {
        UserMessage userMessage = new UserMessage();

        userMessage.setContent("Draft a spec");

        return new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);
    }

    private static ToolResponseMessage.ToolResponse toolResponse(String responseData) {
        return new ToolResponseMessage.ToolResponse("call-1", "someTool", responseData);
    }

    private static AiHubSpringAIAgent newAgent() throws AGUIException {
        return newAgentBuilder().build();
    }

    private static AiHubSpringAIAgent.Builder newAgentBuilder() {
        return AiHubSpringAIAgent.builder()
            .agentId("ai_hub")
            .chatModel((ChatModel) (prompt -> null))
            .systemMessage("test")
            .state(new State());
    }
}
