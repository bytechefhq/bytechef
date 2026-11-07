/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubAgentSessionMemoryContinuityTest {

    private static final String THREAD_ID = "thread-1";
    private static final String OTHER_THREAD_ID = "thread-2";
    private static final String BUILD_WORKFLOW = CopilotAgentType.BUILD_WORKFLOW.key();
    private static final String IMPORT_WORKFLOW = CopilotAgentType.IMPORT_WORKFLOW.key();

    private AiHubSessionMemory aiHubSessionMemory;

    @BeforeEach
    void setUp() {
        aiHubSessionMemory = new AiHubSessionMemory(
            InMemorySessionRepository.builder()
                .build(),
            null);
    }

    @Test
    void testSecondDelegationSeesTheFirstExchange() {
        String sessionId = SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, BUILD_WORKFLOW);

        appendExchange(sessionId, "add a Slack step", "Added a Slack step.");

        List<SessionEvent> events = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(events).hasSize(2);
        assertThat(
            events.get(1)
                .getMessage()
                .getText()).contains("Added a Slack step");
    }

    @Test
    void testDifferentSpecialistsOnOneThreadDoNotSeeEachOther() {
        appendExchange(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, BUILD_WORKFLOW), "add a Slack step",
            "Added a Slack step.");

        List<SessionEvent> otherSpecialistEvents = aiHubSessionMemory.sessionService()
            .getEvents(
                SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, IMPORT_WORKFLOW),
                EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(otherSpecialistEvents).isEmpty();
    }

    @Test
    void testOneSpecialistOnDifferentThreadsDoesNotCrossConversations() {
        appendExchange(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, BUILD_WORKFLOW), "add a Slack step",
            "Added a Slack step.");

        List<SessionEvent> otherConversationEvents = aiHubSessionMemory.sessionService()
            .getEvents(
                SubAgentSessionMemoryContributor.sessionKey(OTHER_THREAD_ID, BUILD_WORKFLOW),
                EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(otherConversationEvents).isEmpty();
    }

    @Test
    void testReplayIsCappedAtMaxEvents() {
        String sessionId = SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, BUILD_WORKFLOW);

        for (int exchangeIndex = 0; exchangeIndex < SubAgentSessionMemoryContributor.MAX_EVENTS; exchangeIndex++) {
            appendExchange(sessionId, "request " + exchangeIndex, "reply " + exchangeIndex);
        }

        List<SessionEvent> events = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.lastN(SubAgentSessionMemoryContributor.MAX_EVENTS));

        assertThat(events).hasSize(SubAgentSessionMemoryContributor.MAX_EVENTS);

        List<SessionEvent> allEvents = aiHubSessionMemory.sessionService()
            .getEvents(sessionId, EventFilter.all());

        assertThat(allEvents).hasSize(SubAgentSessionMemoryContributor.MAX_EVENTS * 2);
    }

    @Test
    void testSecondCallThroughTheChatClientReplaysTheFirstCall() {
        PromptCapturingChatModel chatModel = new PromptCapturingChatModel();

        ChatClient chatClient = memoryChatClient(chatModel, BUILD_WORKFLOW);

        callWith(chatClient, "add a Slack step", THREAD_ID);
        callWith(chatClient, "make the message shorter", THREAD_ID);

        assertThat(conversationTexts(chatModel.prompts.getLast())).containsExactly(
            "add a Slack step", "reply to: add a Slack step", "make the message shorter");
    }

    @Test
    void testCallOnAnotherThreadStartsWithoutTheFirstThreadsCalls() {
        PromptCapturingChatModel chatModel = new PromptCapturingChatModel();

        ChatClient chatClient = memoryChatClient(chatModel, BUILD_WORKFLOW);

        callWith(chatClient, "add a Slack step", THREAD_ID);
        callWith(chatClient, "make the message shorter", OTHER_THREAD_ID);

        assertThat(conversationTexts(chatModel.prompts.getLast())).containsExactly("make the message shorter");
    }

    @Test
    void testCallOfAnotherAgentTypeOnTheSameThreadStartsWithoutTheFirstAgentsCalls() {
        PromptCapturingChatModel chatModel = new PromptCapturingChatModel();

        callWith(memoryChatClient(chatModel, BUILD_WORKFLOW), "add a Slack step", THREAD_ID);
        callWith(memoryChatClient(chatModel, IMPORT_WORKFLOW), "import this n8n workflow", THREAD_ID);

        assertThat(conversationTexts(chatModel.prompts.getLast())).containsExactly("import this n8n workflow");
    }

    @Test
    void testCallThroughTheChatClientReplaysAtMostMaxEvents() {
        PromptCapturingChatModel chatModel = new PromptCapturingChatModel();

        ChatClient chatClient = memoryChatClient(chatModel, BUILD_WORKFLOW);

        int exchangeCount = SubAgentSessionMemoryContributor.MAX_EVENTS;

        for (int exchangeIndex = 0; exchangeIndex < exchangeCount; exchangeIndex++) {
            callWith(chatClient, "request " + exchangeIndex, THREAD_ID);
        }

        callWith(chatClient, "final request", THREAD_ID);

        List<String> conversationTexts = conversationTexts(chatModel.prompts.getLast());

        assertThat(conversationTexts).hasSize(SubAgentSessionMemoryContributor.MAX_EVENTS + 1);
        assertThat(conversationTexts).doesNotContain("request 0", "reply to: request 0");
        assertThat(conversationTexts.getLast()).isEqualTo("final request");

        List<SessionEvent> allEvents = aiHubSessionMemory.sessionService()
            .getEvents(SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, BUILD_WORKFLOW), EventFilter.all());

        assertThat(allEvents).hasSize((exchangeCount + 1) * 2);
    }

    @Test
    void testCallWithoutAConversationIdKeepsNoMemory() {
        PromptCapturingChatModel chatModel = new PromptCapturingChatModel();

        ChatClient chatClient = memoryChatClient(chatModel, BUILD_WORKFLOW);

        chatClient.prompt("add a Slack step")
            .call()
            .content();
        chatClient.prompt("make the message shorter")
            .call()
            .content();

        assertThat(conversationTexts(chatModel.prompts.getLast())).containsExactly("make the message shorter");
    }

    private ChatClient memoryChatClient(ChatModel chatModel, String agentTypeKey) {
        return SubAgentContributingChatClient.wrap(
            ChatClient.builder(chatModel)
                .build(),
            List.of(new SubAgentSessionMemoryContributor(aiHubSessionMemory, agentTypeKey)));
    }

    private static void callWith(ChatClient chatClient, String request, String threadId) {
        Map<String, Object> toolContext = AgentToolInvocationContext.builder()
            .conversationId(threadId)
            .build()
            .toToolContext();

        chatClient.prompt(request)
            .toolContext(toolContext)
            .call()
            .content();
    }

    private static List<String> conversationTexts(Prompt prompt) {
        return prompt.getInstructions()
            .stream()
            .filter(message -> message.getMessageType() != MessageType.SYSTEM)
            .map(Message::getText)
            .toList();
    }

    private void appendExchange(String sessionId, String userText, String assistantText) {
        if (aiHubSessionMemory.sessionRepository()
            .findById(sessionId) == null) {

            aiHubSessionMemory.sessionRepository()
                .save(
                    Session.builder()
                        .id(sessionId)
                        .userId(AiHubSessionMemory.SESSION_USER_ID)
                        .createdAt(Instant.now())
                        .build());
        }

        aiHubSessionMemory.sessionRepository()
            .appendEvent(
                SessionEvent.builder()
                    .sessionId(sessionId)
                    .message(new UserMessage(userText))
                    .build());

        aiHubSessionMemory.sessionRepository()
            .appendEvent(
                SessionEvent.builder()
                    .sessionId(sessionId)
                    .message(new AssistantMessage(assistantText))
                    .build());
    }

    private static final class PromptCapturingChatModel implements ChatModel {

        private final List<Prompt> prompts = new ArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            return reply(prompt);
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            prompts.add(prompt);

            return Flux.just(reply(prompt));
        }

        private static ChatResponse reply(Prompt prompt) {
            UserMessage userMessage = prompt.getUserMessage();

            return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("reply to: " + userMessage.getText()))))
                .build();
        }
    }
}
