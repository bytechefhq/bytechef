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
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.ai.hub.util.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubSpringAIAgentTest {

    @Test
    void testBuildInvocationContextExtractsWorkspaceIdFromState() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);

        UserMessage userMessage = new UserMessage();

        userMessage.setContent("Draft a spec");

        RunAgentInput input = new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);

        AiHubToolInvocationContext context = agent.buildInvocationContext(input);

        assertThat(context.workspaceId()).isEqualTo(7L);
        assertThat(context.sourceOrdinal()).isEqualTo((short) Source.AI_HUB.ordinal());
        assertThat(context.lastUserPrompt()).isEqualTo("Draft a spec");
    }

    /**
     * Privilege-escalation defense: a malicious or buggy client could populate {@code state.workspaceId} with a
     * different value than the controller-injected {@code VERIFIED_WORKSPACE_ID}. The agent MUST read identity from the
     * verified key only — a regression that swapped {@code state.get(VERIFIED_WORKSPACE_ID)} back to
     * {@code state.get("workspaceId")} would let an attacker cross tenants via prompt injection of the AG-UI state.
     */
    @Test
    void testBuildInvocationContextIgnoresClientSuppliedWorkspaceIdWhenVerifiedKeyIsPresent() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        // Forged client state — must be ignored.
        state.set("workspaceId", 999L);

        UserMessage userMessage = new UserMessage();

        userMessage.setContent("Draft a spec");

        RunAgentInput input = new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);

        AiHubToolInvocationContext context = agent.buildInvocationContext(input);

        assertThat(context.workspaceId())
            .as("Agent must use server-injected VERIFIED_WORKSPACE_ID, not client-supplied workspaceId")
            .isEqualTo(7L);
    }

    @Test
    void testCreateSystemMessageIncludesActiveFileAndOpenTabsContext() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        state.set("activeFileId", "42");
        state.set(
            "currentTabs",
            List.of(
                Map.of("fileId", "42", "name", "spec.md", "viewMode", "preview"),
                Map.of("fileId", "43", "name", "notes.md", "viewMode", "editor")));

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).contains("Active File");
        assertThat(systemMessage.getContent()).contains("42");
        assertThat(systemMessage.getContent()).contains("Open Tabs");
        assertThat(systemMessage.getContent()).contains("spec.md");
        assertThat(systemMessage.getContent()).contains("notes.md");
    }

    @Test
    void testCreateSystemMessageOmitsTabBlocksWhenEmpty() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).doesNotContain("Active File");
        assertThat(systemMessage.getContent()).doesNotContain("Open Tabs");
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
                Map.of("kind", "file", "id", "42", "name", "spec.md"),
                Map.of("kind", "dataTable", "id", "7", "name", "leads")));

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).contains("Referenced Resources");
        assertThat(systemMessage.getContent()).contains("kind=file");
        assertThat(systemMessage.getContent()).contains("name=spec.md");
        assertThat(systemMessage.getContent()).contains("kind=dataTable");
        assertThat(systemMessage.getContent()).contains("name=leads");
    }

    @Test
    void testCreateSystemMessageOmitsActiveTabAndReferencedResourcesWhenMissing() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).doesNotContain("Active Tab");
        assertThat(systemMessage.getContent()).doesNotContain("Referenced Resources");
    }

    @Test
    void testCreateSystemMessageIncludesMemoryIndexContextWhenResolverReturnsBlock() throws AGUIException {
        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub")
            .chatModel((ChatModel) (prompt -> null))
            .systemMessage("test")
            .state(new State())
            .memoryIndexResolver(
                (workspaceId, userId, environment) -> "- name=user_profile, title=User Profile, memoryType=USER\n")
            .threadUserIdResolver(threadId -> 10L)
            .build();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, "thread-1");

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).contains("Memory Index");
        assertThat(systemMessage.getContent()).contains("user_profile");
    }

    @Test
    void testCreateSystemMessageOmitsMemoryIndexWhenResolverAbsent() throws AGUIException {
        AiHubSpringAIAgent agent = newAgent();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, "thread-1");

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).doesNotContain("Memory Index");
    }

    @Test
    void testCreateSystemMessageOmitsMemoryIndexWhenResolverReturnsNull() throws AGUIException {
        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub")
            .chatModel((ChatModel) (prompt -> null))
            .systemMessage("test")
            .state(new State())
            .memoryIndexResolver((workspaceId, userId, environment) -> null)
            .threadUserIdResolver(threadId -> 10L)
            .build();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, 7L);
        state.set(AiHubStateKeys.VERIFIED_THREAD_ID, "thread-1");

        SystemMessage systemMessage = agent.createSystemMessage(state, new ArrayList<>());

        assertThat(systemMessage.getContent()).doesNotContain("Memory Index");
    }

    private AiHubSpringAIAgent newAgent() throws AGUIException {
        return AiHubSpringAIAgent.builder()
            .agentId("ai_hub")
            .chatModel((ChatModel) (prompt -> null))
            .systemMessage("test")
            .state(new State())
            .build();
    }
}
