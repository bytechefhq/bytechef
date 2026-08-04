/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.guardrails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.automation.ai.tool.ManagerAgentType;
import com.bytechef.automation.ai.tool.ManagerSubAgentToolCallback;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailViolationException;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.ee.platform.ai.workspaceprompt.advisor.WorkspaceSystemPromptAdvisor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Pins {@link SubAgentGuardrailedChatClient} as the seam that attaches the calling workspace's
 * {@code AiGuardrailsAdvisor} to an AI Hub subagent delegate's own one-shot {@link ChatClient} call — the follow-up to
 * {@code AiHubSpringAIAgentGuardrailsTest}, which pins the same coverage for the top-level agent.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubAgentGuardrailedChatClientTest {

    private static final Long WORKSPACE_ID = 42L;
    private static final String BLOCKED_TERM = "classified";

    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AiGuardrailMetrics aiGuardrailMetrics = new AiGuardrailMetrics(meterRegistry, "ai_hub");

    @Test
    void testWrapAttachesAdvisorAndBlocksViolatingContentWhenActive() {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        ChatClient inner = ChatClient.builder(unreachableChatModel())
            .build();
        ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, blockingGuardrails(), aiGuardrailMetrics, null);

        Map<String, Object> forwardedContext =
            Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);

        assertThatThrownBy(
            () -> guarded.prompt("Tell me about the " + BLOCKED_TERM + " project")
                .toolContext(forwardedContext)
                .call()
                .content())
                    .isInstanceOf(AiGuardrailViolationException.class)
                    .satisfies(exception -> {
                        assertThat(exception.getMessage()).contains("blocked_term");
                        assertThat(exception.getMessage()).doesNotContain(BLOCKED_TERM);
                    });
    }

    @Test
    void testWrapReturnsChatClientUnchangedWhenAiGuardrailsAbsent() {
        ChatClient inner = ChatClient.builder(new CapturingChatModel())
            .build();

        ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, null, null, null);

        assertThat(guarded).isSameAs(inner);
    }

    @Test
    void testWrapAttachesWorkspaceSystemPromptAdvisorWhenPromptSet() {
        WorkspaceSystemPrompts workspaceSystemPrompts = mock(WorkspaceSystemPrompts.class);

        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn("Always answer in German.");

        CapturingChatModel capturingChatModel = new CapturingChatModel();
        ChatClient inner = ChatClient.builder(capturingChatModel)
            .build();

        ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, null, null, workspaceSystemPrompts);

        Map<String, Object> forwardedContext =
            Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);

        String content = guarded.prompt()
            .system("Base prompt.")
            .user("hi")
            .toolContext(forwardedContext)
            .call()
            .content();

        assertThat(content).isEqualTo("OK");

        String forwardedSystemText = capturingChatModel.receivedPrompts.getFirst()
            .getInstructions()
            .stream()
            .filter(message -> message.getMessageType() == MessageType.SYSTEM)
            .map(Message::getText)
            .findFirst()
            .orElseThrow();

        assertThat(forwardedSystemText).contains(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
        assertThat(forwardedSystemText).endsWith("Always answer in German.");
    }

    @Test
    void testWrapSkipsPromptAdvisorWhenWorkspaceHasNoPrompt() {
        WorkspaceSystemPrompts workspaceSystemPrompts = mock(WorkspaceSystemPrompts.class);

        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn(null);

        CapturingChatModel capturingChatModel = new CapturingChatModel();
        ChatClient inner = ChatClient.builder(capturingChatModel)
            .build();

        ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, null, null, workspaceSystemPrompts);

        Map<String, Object> forwardedContext =
            Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);

        String content = guarded.prompt()
            .system("Base prompt.")
            .user("hi")
            .toolContext(forwardedContext)
            .call()
            .content();

        assertThat(content).isEqualTo("OK");

        String forwardedSystemText = capturingChatModel.receivedPrompts.getFirst()
            .getInstructions()
            .stream()
            .filter(message -> message.getMessageType() == MessageType.SYSTEM)
            .map(Message::getText)
            .findFirst()
            .orElseThrow();

        assertThat(forwardedSystemText).doesNotContain(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
    }

    @Test
    void testWrapReturnsUnwrappedWhenBothEnginesAbsent() {
        ChatClient inner = ChatClient.builder(new CapturingChatModel())
            .build();

        assertThat(SubAgentGuardrailedChatClient.wrap(inner, null, null, null)).isSameAs(inner);
    }

    @Test
    void testWrapSkipsAdvisorWhenGuardrailsInactiveForWorkspace() {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiGuardrails inactiveGuardrails = new AiGuardrails(
            settingsService, null, null, aiGuardrailMetrics, false, false, "", false, false, false, false);
        CapturingChatModel capturingChatModel = new CapturingChatModel();
        ChatClient inner = ChatClient.builder(capturingChatModel)
            .build();

        ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, inactiveGuardrails, aiGuardrailMetrics, null);

        Map<String, Object> forwardedContext =
            Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);

        String content = guarded.prompt("Tell me about the " + BLOCKED_TERM + " project")
            .toolContext(forwardedContext)
            .call()
            .content();

        assertThat(content).isEqualTo("OK");
        assertThat(capturingChatModel.receivedPrompts).hasSize(1);
    }

    /**
     * Proves workspace resolution reads the id the caller forwarded via {@code .toolContext(...)} — not a fixed or
     * ignored value. {@code WORKSPACE_ID} has no settings row either, so the only way {@code otherWorkspaceId} gets
     * looked up is if the wrapper actually parsed it out of the forwarded map.
     */
    @Test
    void testCallResolvesWorkspaceIdFromForwardedToolContext() {
        Long otherWorkspaceId = 999L;

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());
        when(settingsService.fetchSettings(otherWorkspaceId)).thenReturn(Optional.empty());

        ChatClient inner = ChatClient.builder(new CapturingChatModel())
            .build();
        ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, blockingGuardrails(), aiGuardrailMetrics, null);

        Map<String, Object> forwardedContext =
            Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, otherWorkspaceId);

        guarded.prompt("hello, nothing blocked here")
            .toolContext(forwardedContext)
            .call()
            .content();

        verify(settingsService, atLeastOnce()).fetchSettings(otherWorkspaceId);
    }

    /**
     * A delegate call that never forwards a {@code ToolContext} (or whose forwarded map carries no workspace key) falls
     * back to the tenant-default {@code null} workspace id — the same fallback
     * {@code AiHubSpringAIAgent#attachGuardrailsAdvisor} uses for the top-level agent.
     */
    @Test
    void testCallWithoutToolContextResolvesNullTenantDefaultWorkspaceId() {
        when(settingsService.fetchSettings((Long) null)).thenReturn(Optional.empty());

        ChatClient inner = ChatClient.builder(new CapturingChatModel())
            .build();
        ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, blockingGuardrails(), aiGuardrailMetrics, null);

        guarded.prompt("hello, nothing blocked here")
            .call()
            .content();

        verify(settingsService, atLeastOnce()).fetchSettings((Long) null);
    }

    /**
     * End-to-end pin, mirroring the AI Guardrails F3 spec: a BLOCK-mode violation inside a subagent delegate call must
     * surface to the parent LLM as an ordinary tool-error string, not an unhandled exception that would crash the whole
     * agent turn. Uses the actual hand-rolled delegate shape (parse -> call chatClient -> catch RuntimeException)
     * rather than re-implementing it, so the pin tracks real behavior.
     */
    @Test
    void testBlockedDelegateCallIsCatchableAsToolErrorNotUnhandledCrash() {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        ChatClient inner = ChatClient.builder(unreachableChatModel())
            .build();
        ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, blockingGuardrails(), aiGuardrailMetrics, null);

        Map<String, Object> forwardedContext =
            Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);

        String toolResult;

        try {
            toolResult = guarded.prompt("Tell me about the " + BLOCKED_TERM + " project")
                .toolContext(forwardedContext)
                .call()
                .content();
        } catch (RuntimeException exception) {
            // Mirrors every hand-rolled delegate ToolCallback's own catch (RuntimeException) arm (e.g.
            // ManagerSubAgentToolCallback.call, SkillsAgentToolCallback.call), which converts any escaping
            // RuntimeException — including AiGuardrailViolationException — into a JSON tool-error string via
            // ToolErrors.runtimeFailure instead of letting it propagate further.
            assertThat(exception).isInstanceOf(AiGuardrailViolationException.class);

            toolResult = "{\"error\":\"delegate failed (" + exception.getClass()
                .getSimpleName() + ")\"}";
        }

        assertThat(toolResult).isEqualTo("{\"error\":\"delegate failed (AiGuardrailViolationException)\"}");
        assertThat(toolResult).doesNotContain(BLOCKED_TERM);
    }

    /**
     * Same pin as {@link #testBlockedDelegateCallIsCatchableAsToolErrorNotUnhandledCrash}, but exercised through a REAL
     * delegate class (the manager family's {@link ManagerSubAgentToolCallback}, shared by mcp_manager /
     * personal_agent_manager / deployment_manager / api_collection_manager) instead of a hand-simulated catch block —
     * proves the whole seam (wrap the ChatClient in AiHubConfiguration, forward the ToolContext, let the delegate's own
     * catch (RuntimeException) arm convert the violation) works end to end for one representative family.
     */
    @Test
    void testManagerDelegateSurfacesBlockAsToolErrorViaRealCallback() {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        ChatClient inner = ChatClient.builder(unreachableChatModel())
            .build();
        ChatClient guarded = SubAgentGuardrailedChatClient.wrap(inner, blockingGuardrails(), aiGuardrailMetrics, null);

        ManagerSubAgentToolCallback managerToolCallback =
            new ManagerSubAgentToolCallback(ManagerAgentType.MCP_MANAGER, guarded, "test manager");

        Map<String, Object> forwardedContext =
            Map.of(AgentToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID);
        ToolContext toolContext = new ToolContext(forwardedContext);

        String toolResult = managerToolCallback.call(
            "{\"request\":\"Tell me about the " + BLOCKED_TERM + " project\"}", toolContext);

        assertThat(toolResult).contains("AiGuardrailViolationException");
        assertThat(toolResult).doesNotContain(BLOCKED_TERM);
    }

    private AiGuardrails blockingGuardrails() {
        return new AiGuardrails(
            settingsService, null, null, aiGuardrailMetrics, false, false, BLOCKED_TERM, false, false, false, false);
    }

    private static ChatModel unreachableChatModel() {
        return new ChatModel() {

            @Override
            public ChatResponse call(Prompt prompt) {
                throw new AssertionError("Model must not be called when the guardrail blocks the turn");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                throw new AssertionError("Model must not be called when the guardrail blocks the turn");
            }
        };
    }

    private static final class CapturingChatModel implements ChatModel {

        private final List<Prompt> receivedPrompts = new ArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            receivedPrompts.add(prompt);

            return cannedResponse();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            receivedPrompts.add(prompt);

            return Flux.just(cannedResponse());
        }

        private static ChatResponse cannedResponse() {
            return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("OK"))))
                .build();
        }
    }
}
