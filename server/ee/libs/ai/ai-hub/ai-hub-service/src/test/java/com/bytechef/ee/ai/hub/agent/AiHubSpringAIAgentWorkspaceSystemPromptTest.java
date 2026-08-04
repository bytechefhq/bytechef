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
import com.bytechef.ee.platform.ai.workspaceprompt.WorkspaceSystemPrompts;
import com.bytechef.ee.platform.ai.workspaceprompt.advisor.WorkspaceSystemPromptAdvisor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Pins {@link AiHubSpringAIAgent#resolveChatClient} as the seam that also attaches the workspace's
 * {@link WorkspaceSystemPromptAdvisor}: the prompt-set path appends the section, the no-prompt path skips the
 * {@code mutate()} entirely.
 *
 * @version ee
 */
class AiHubSpringAIAgentWorkspaceSystemPromptTest {

    private static final Long WORKSPACE_ID = 7L;

    private final WorkspaceSystemPrompts workspaceSystemPrompts = mock(WorkspaceSystemPrompts.class);

    @Test
    void testResolveChatClientAppendsWorkspacePromptOnDefaultChatClient() throws AGUIException {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn("Always answer in German.");

        CapturingChatModel capturingChatModel = new CapturingChatModel();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(capturingChatModel)
            .systemMessage("Base prompt.")
            .state(new State())
            .workspaceSystemPrompts(workspaceSystemPrompts)
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        List<ChatResponse> responses = streamAssistantReply(chatClient)
            .collectList()
            .block();

        assertThat(responses).isNotEmpty();
        assertThat(capturingChatModel.receivedPrompts).hasSize(1);

        String systemText = systemText(capturingChatModel.receivedPrompts.getFirst());

        assertThat(systemText).contains(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
        assertThat(systemText).endsWith("Always answer in German.");
    }

    @Test
    void testResolveChatClientSkipsAdvisorWhenNoPrompt() throws AGUIException {
        when(workspaceSystemPrompts.fetchPrompt(WORKSPACE_ID)).thenReturn(null);

        CapturingChatModel capturingChatModel = new CapturingChatModel();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(capturingChatModel)
            .systemMessage("Base prompt.")
            .state(new State())
            .workspaceSystemPrompts(workspaceSystemPrompts)
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        List<ChatResponse> responses = streamAssistantReply(chatClient)
            .collectList()
            .block();

        assertThat(responses).isNotEmpty();

        String systemText = systemText(capturingChatModel.receivedPrompts.getFirst());

        assertThat(systemText).doesNotContain(WorkspaceSystemPromptAdvisor.WORKSPACE_INSTRUCTIONS_HEADER);
    }

    /**
     * Backward-compatibility guard: with no engine wired (module absent), {@code resolveChatClient} keeps returning a
     * working {@link ChatClient}.
     */
    @Test
    void testResolveChatClientSkipsAdvisorWhenEngineAbsent() throws AGUIException {
        CapturingChatModel capturingChatModel = new CapturingChatModel();

        AiHubSpringAIAgent agent = AiHubSpringAIAgent.builder()
            .agentId("ai_hub_ask")
            .chatModel(capturingChatModel)
            .systemMessage("Base prompt.")
            .state(new State())
            .build();

        ChatClient chatClient = agent.resolveChatClient(runInput());

        List<ChatResponse> responses = streamAssistantReply(chatClient)
            .collectList()
            .block();

        assertThat(responses).isNotEmpty();
        assertThat(capturingChatModel.receivedPrompts).hasSize(1);
    }

    private static String systemText(Prompt prompt) {
        return prompt.getInstructions()
            .stream()
            .filter(message -> message.getMessageType() == MessageType.SYSTEM)
            .map(Message::getText)
            .findFirst()
            .orElseThrow();
    }

    private static RunAgentInput runInput() {
        State state = new State();

        state.set(AiHubStateKeys.VERIFIED_WORKSPACE_ID, WORKSPACE_ID);

        UserMessage userMessage = new UserMessage();

        userMessage.setContent("Tell me about the project");

        return new RunAgentInput(
            "thread", "run", state, List.of((BaseMessage) userMessage), List.of(), List.of(), null);
    }

    private static Flux<ChatResponse> streamAssistantReply(ChatClient chatClient) {
        return chatClient.prompt()
            .system("You are a helpful assistant.")
            .user("Tell me about the project")
            .stream()
            .chatResponse();
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
