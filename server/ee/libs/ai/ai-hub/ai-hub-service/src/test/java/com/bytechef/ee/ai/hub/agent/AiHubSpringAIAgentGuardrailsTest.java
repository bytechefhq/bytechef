/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailViolationException;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * Pins {@link AiHubSpringAIAgent#resolveChatClient} as the single seam that attaches the workspace's
 * {@code AiGuardrailsAdvisor} to every AI Hub LLM turn: the default (builder-time) {@link ChatClient} AND a per-request
 * override client (the personal-agent model-override path), both a BLOCK-mode violation and a REDACT_AND_CONTINUE
 * downgrade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubSpringAIAgentGuardrailsTest {

    private static final Long WORKSPACE_ID = 7L;
    private static final String BLOCKED_TERM = "classified";

    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final AiGuardrailMetrics aiGuardrailMetrics = new AiGuardrailMetrics(meterRegistry, "ai_hub");

    @Test
    void testResolveChatClientBlocksViolatingContentOnDefaultChatClient() throws AGUIException {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(unreachableChatModel())
            .systemMessage("test")
            .state(new State())
            .aiGuardrails(blockingGuardrails(), aiGuardrailMetrics)
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        StepVerifier.create(streamAssistantReply(chatClient))
            .expectErrorSatisfies(throwable -> {
                assertThat(throwable).isInstanceOf(AiGuardrailViolationException.class);
                assertThat(throwable.getMessage()).contains("blocked_term");
                assertThat(throwable.getMessage()).doesNotContain(BLOCKED_TERM);
            })
            .verify();
    }

    /**
     * Coverage rationale pin: a personal agent's model-override ChatClient is resolved by
     * {@link AiHubSpringAIAgent.OverrideChatClientResolver}, but it still flows through
     * {@link AiHubSpringAIAgent#resolveChatClient} before the request spec is built, so the guardrail must be attached
     * there too — not only on the builder-time default client.
     */
    @Test
    void testResolveChatClientBlocksViolatingContentOnOverrideChatClient() throws AGUIException {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        ChatClient overrideClient = ChatClient.builder(unreachableChatModel())
            .build();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(unreachableChatModel())
            .systemMessage("test")
            .state(new State())
            .aiGuardrails(blockingGuardrails(), aiGuardrailMetrics)
            .overrideChatClientResolver(state -> overrideClient)
            .build();

        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);
        state.set(AiHubStateKeys.PERSONAL_AGENT_TITLE_KEY, "My Agent");

        ChatClient chatClient = agent.resolveChatClient(runInput(state));

        StepVerifier.create(streamAssistantReply(chatClient))
            .expectErrorSatisfies(throwable -> {
                assertThat(throwable).isInstanceOf(AiGuardrailViolationException.class);
                assertThat(throwable.getMessage()).contains("blocked_term");
                assertThat(throwable.getMessage()).doesNotContain(BLOCKED_TERM);
            })
            .verify();
    }

    @Test
    void testResolveChatClientRedactAndContinueMasksAndProceeds() throws AGUIException {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                WORKSPACE_ID, null, null, null, null, null, null, BlockingMode.REDACT_AND_CONTINUE)));

        CapturingChatModel capturingChatModel = new CapturingChatModel();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(capturingChatModel)
            .systemMessage("test")
            .state(new State())
            .aiGuardrails(blockingGuardrails(), aiGuardrailMetrics)
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        List<ChatResponse> responses = streamAssistantReply(chatClient)
            .collectList()
            .block();

        assertThat(responses).isNotEmpty();
        assertThat(capturingChatModel.receivedPrompts).hasSize(1);

        String forwardedText = capturingChatModel.receivedPrompts.getFirst()
            .getInstructions()
            .stream()
            .map(message -> message.getText())
            .reduce("", String::concat);

        assertThat(forwardedText).doesNotContain(BLOCKED_TERM);
        assertThat(forwardedText).contains("[REDACTED_BLOCKED_TERM]");
        assertThat(meterRegistry.counter(AiGuardrailMetrics.COUNTER_NAME, "event", "blocking_downgraded", "surface",
            "ai_hub")
            .count()).isEqualTo(1.0);
    }

    /**
     * Backward-compatibility guard: when no {@link AiGuardrails} bean is wired (EE guardrails module absent from the
     * classpath, or the deployment's own wiring skipped {@code aiGuardrails(...)}), {@code resolveChatClient} must keep
     * returning a working {@link ChatClient} rather than throwing.
     */
    @Test
    void testResolveChatClientSkipsAdvisorWhenAiGuardrailsAbsent() throws AGUIException {
        CapturingChatModel capturingChatModel = new CapturingChatModel();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(capturingChatModel)
            .systemMessage("test")
            .state(new State())
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        List<ChatResponse> responses = streamAssistantReply(chatClient)
            .collectList()
            .block();

        assertThat(responses).isNotEmpty();
        assertThat(capturingChatModel.receivedPrompts).hasSize(1);
    }

    /**
     * When every guardrail is disabled for the workspace, {@code isActive} is {@code false} and the advisor is never
     * attached — a turn that would have matched a blocked term (had one been configured) sails through unredacted. This
     * pins the no-op fast path so an inactive policy pays no per-turn advisor overhead.
     */
    @Test
    void testResolveChatClientSkipsAdvisorWhenGuardrailsInactiveForWorkspace() throws AGUIException {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        CapturingChatModel capturingChatModel = new CapturingChatModel();
        AiGuardrails inactiveGuardrails = new AiGuardrails(
            settingsService, null, null, aiGuardrailMetrics, false, false, "", false, false, false, false);

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(capturingChatModel)
            .systemMessage("test")
            .state(new State())
            .aiGuardrails(inactiveGuardrails, aiGuardrailMetrics)
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        List<ChatResponse> responses = streamAssistantReply(chatClient)
            .collectList()
            .block();

        assertThat(responses).isNotEmpty();
        assertThat(capturingChatModel.receivedPrompts).hasSize(1);

        String forwardedText = capturingChatModel.receivedPrompts.getFirst()
            .getInstructions()
            .stream()
            .map(message -> message.getText())
            .reduce("", String::concat);

        assertThat(forwardedText).containsIgnoringCase(BLOCKED_TERM);
    }

    private AiGuardrails blockingGuardrails() {
        return new AiGuardrails(
            settingsService, null, null, aiGuardrailMetrics, false, false, BLOCKED_TERM, false, false, false, false);
    }

    private static RunAgentInput runInput() {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);

        return runInput(state);
    }

    private static RunAgentInput runInput(State state) {
        UserMessage userMessage = new UserMessage();

        userMessage.setContent("Tell me about the " + BLOCKED_TERM.toUpperCase() + " project");

        return new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);
    }

    private static Flux<ChatResponse> streamAssistantReply(ChatClient chatClient) {
        return chatClient.prompt()
            .system("You are a helpful assistant.")
            .user("Tell me about the " + BLOCKED_TERM.toUpperCase() + " project")
            .stream()
            .chatResponse();
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
