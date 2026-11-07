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
import com.bytechef.ee.ai.hub.config.AiHubAgentFixtures;
import com.bytechef.ee.ai.hub.util.AiHubStateKeys;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import com.bytechef.ee.platform.ai.guardrails.AiGuardrails;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsSettingsScope;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings;
import com.bytechef.ee.platform.ai.guardrails.domain.AiGuardrailsWorkspaceSettings.BlockingMode;
import com.bytechef.ee.platform.ai.guardrails.exception.AiGuardrailViolationException;
import com.bytechef.ee.platform.ai.guardrails.service.AiGuardrailsWorkspaceSettingsService;
import com.bytechef.platform.ai.guardrails.GuardrailSurface;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubSpringAIAgentGuardrailsTest {

    private static final long WORKSPACE_ID = 7L;
    private static final String BLOCKED_TERM = "classified";
    private static final String EMAIL = "bob@acme.io";

    private final AiGuardrailsWorkspaceSettingsService settingsService =
        mock(AiGuardrailsWorkspaceSettingsService.class);

    @Test
    void testAskAgentBlocksAWorkspaceBlockedTermInTheUserInput() throws AGUIException {
        workspaceSettings(null, null, BLOCKED_TERM, BlockingMode.BLOCK);

        AiHubSpringAIAgent agent = askAgent(new UnreachableChatModel(), guardrails());

        StepVerifier.create(streamReply(agent, "Summarize the " + BLOCKED_TERM + " memo"))
            .expectErrorSatisfies(throwable -> {
                assertThat(throwable).isInstanceOf(AiGuardrailViolationException.class);
                assertThat(throwable.getMessage()).contains("blocked_term");
                assertThat(throwable.getMessage()).doesNotContain(BLOCKED_TERM);
            })
            .verify();
    }

    @Test
    void testBuildAgentBlocksAWorkspaceBlockedTermInTheUserInput() throws AGUIException {
        workspaceSettings(null, null, BLOCKED_TERM, BlockingMode.BLOCK);

        AiHubSpringAIAgent agent = buildAgent(new UnreachableChatModel(), guardrails());

        StepVerifier.create(streamReply(agent, "Summarize the " + BLOCKED_TERM + " memo"))
            .expectError(AiGuardrailViolationException.class)
            .verify();
    }

    @Test
    void testAskAgentRedactsWorkspacePiiInTheUserInputBeforeTheModelSeesIt() throws AGUIException {
        workspaceSettings(true, null, null, BlockingMode.BLOCK);

        CapturingChatModel chatModel = new CapturingChatModel("Noted.");

        AiHubSpringAIAgent agent = askAgent(chatModel, guardrails());

        collectText(streamReply(agent, "Send the report to " + EMAIL));

        assertThat(chatModel.lastUserText()).isNotNull()
            .doesNotContain(EMAIL);
    }

    @Test
    void testAskAgentRedactsWorkspacePiiInTheModelOutputBeforeItReachesTheUser() throws AGUIException {
        workspaceSettings(true, true, null, BlockingMode.BLOCK);

        CapturingChatModel chatModel = new CapturingChatModel("Contact " + EMAIL + " for details");

        AiHubSpringAIAgent agent = askAgent(chatModel, guardrails());

        String reply = collectText(streamReply(agent, "Who do I contact?"));

        assertThat(reply).doesNotContain(EMAIL)
            .contains("[REDACTED_EMAIL_ADDRESS]");
    }

    @Test
    void testBuildAgentRedactsWorkspacePiiInTheModelOutputBeforeItReachesTheUser() throws AGUIException {
        workspaceSettings(true, true, null, BlockingMode.BLOCK);

        CapturingChatModel chatModel = new CapturingChatModel("Contact " + EMAIL + " for details");

        AiHubSpringAIAgent agent = buildAgent(chatModel, guardrails());

        String reply = collectText(streamReply(agent, "Who do I contact?"));

        assertThat(reply).doesNotContain(EMAIL)
            .contains("[REDACTED_EMAIL_ADDRESS]");
    }

    @Test
    void testAgentWithoutAiGuardrailsBehavesAsBefore() throws AGUIException {
        CapturingChatModel chatModel = new CapturingChatModel("Contact " + EMAIL + " for details");

        AiHubSpringAIAgent agent = askAgent(chatModel, null);

        String reply = collectText(streamReply(agent, "Summarize the " + BLOCKED_TERM + " memo for " + EMAIL));

        assertThat(chatModel.lastUserText()).isEqualTo("Summarize the " + BLOCKED_TERM + " memo for " + EMAIL);
        assertThat(reply).isEqualTo("Contact " + EMAIL + " for details");
    }

    @Test
    void testAgentWithInactiveGuardrailsBehavesAsBefore() throws AGUIException {
        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.empty());

        CapturingChatModel chatModel = new CapturingChatModel("Contact " + EMAIL + " for details");

        AiHubSpringAIAgent agent = buildAgent(chatModel, guardrails());

        String reply = collectText(streamReply(agent, "Summarize the " + BLOCKED_TERM + " memo for " + EMAIL));

        assertThat(chatModel.lastUserText()).isEqualTo("Summarize the " + BLOCKED_TERM + " memo for " + EMAIL);
        assertThat(reply).isEqualTo("Contact " + EMAIL + " for details");
    }

    @Test
    void testOverrideChatClientIsGuardedToo() throws AGUIException {
        workspaceSettings(null, null, BLOCKED_TERM, BlockingMode.BLOCK);

        ChatClient overrideChatClient = ChatClient.builder(new UnreachableChatModel())
            .build();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(new UnreachableChatModel())
            .systemMessage("test")
            .state(new State())
            .aiGuardrails(guardrails(), new AiGuardrailMetrics(new SimpleMeterRegistry(), GuardrailSurface.AI_HUB))
            .overrideChatClientResolver(state -> overrideChatClient)
            .build();

        StepVerifier.create(streamReply(agent, "Summarize the " + BLOCKED_TERM + " memo"))
            .expectError(AiGuardrailViolationException.class)
            .verify();
    }

    private void workspaceSettings(
        Boolean redactPii, Boolean scanResponses, String blockedTerms, BlockingMode blockingMode) {

        when(settingsService.fetchSettings(WORKSPACE_ID)).thenReturn(Optional.of(
            new AiGuardrailsWorkspaceSettings(
                AiGuardrailsSettingsScope.WORKSPACE, WORKSPACE_ID, redactPii, null, blockedTerms, null, null,
                scanResponses, blockingMode, null, null, null)));
    }

    private AiGuardrails guardrails() {
        return new AiGuardrails(settingsService, null, null, null, false, false, "", false, false, false, true);
    }

    private static AiHubSpringAIAgent askAgent(ChatModel chatModel, AiGuardrails aiGuardrails)
        throws AGUIException {

        return fixtures(aiGuardrails).askAgent(chatModel, state -> null);
    }

    private static AiHubSpringAIAgent buildAgent(ChatModel chatModel, AiGuardrails aiGuardrails)
        throws AGUIException {

        return fixtures(aiGuardrails).buildAgent(chatModel, state -> null);
    }

    private static AiHubAgentFixtures fixtures(AiGuardrails aiGuardrails) {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        if (aiGuardrails != null) {
            fixtures.withAiGuardrails(aiGuardrails);
        }

        return fixtures;
    }

    private static Flux<ChatResponse> streamReply(AiHubSpringAIAgent agent, String userText) {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);

        UserMessage userMessage = new UserMessage();

        userMessage.setContent(userText);

        RunAgentInput runAgentInput = new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);

        ChatClient chatClient = agent.resolveChatClient(runAgentInput);

        return chatClient.prompt()
            .user(userText)
            .advisors(ToolCallingAdvisor.builder()
                .build())
            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, "thread"))
            .stream()
            .chatResponse();
    }

    private static String collectText(Flux<ChatResponse> chatResponses) {
        List<ChatResponse> responses = Objects.requireNonNull(chatResponses.collectList()
            .block());

        return responses.stream()
            .map(ChatResponse::getResult)
            .filter(Objects::nonNull)
            .map(generation -> generation.getOutput()
                .getText())
            .filter(Objects::nonNull)
            .collect(Collectors.joining());
    }

    private static final class UnreachableChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            throw new AssertionError("Model must not be called when the guardrail blocks the turn");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            throw new AssertionError("Model must not be called when the guardrail blocks the turn");
        }
    }

    private static final class CapturingChatModel implements ChatModel {

        private final String reply;
        private final List<Prompt> prompts = new ArrayList<>();

        private CapturingChatModel(String reply) {
            this.reply = reply;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            return response();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            prompts.add(prompt);

            return Flux.just(response());
        }

        private String lastUserText() {
            Prompt prompt = prompts.getLast();

            List<String> userTexts = prompt.getInstructions()
                .stream()
                .filter(message -> message.getMessageType() == MessageType.USER)
                .map(Message::getText)
                .toList();

            return userTexts.getLast();
        }

        private ChatResponse response() {
            return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage(reply))))
                .build();
        }
    }
}
