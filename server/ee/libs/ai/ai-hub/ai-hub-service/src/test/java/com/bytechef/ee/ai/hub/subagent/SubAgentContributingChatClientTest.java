/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SubAgentContributingChatClientTest {

    @Test
    void testWrapReturnsTheChatClientUnchangedWithoutContributors() {
        ChatClient chatClient = ChatClient.builder(new CannedChatModel())
            .build();

        assertThat(SubAgentContributingChatClient.wrap(chatClient, List.of())).isSameAs(chatClient);
    }

    @Test
    void testCallRunsEveryContributorWithTheForwardedToolContext() {
        List<Map<String, Object>> receivedToolContexts = new ArrayList<>();
        List<String> invokedAdvisors = new ArrayList<>();

        ChatClient chatClient = SubAgentContributingChatClient.wrap(
            ChatClient.builder(new CannedChatModel())
                .build(),
            List.of(
                recordingContributor("first", receivedToolContexts, invokedAdvisors),
                recordingContributor("second", receivedToolContexts, invokedAdvisors)));

        Map<String, Object> toolContext = new HashMap<>(Map.of("conversationId", "thread-1"));

        String content = chatClient.prompt("hello")
            .toolContext(toolContext)
            .call()
            .content();

        toolContext.put("conversationId", "changed-after-capture");

        assertThat(content).isEqualTo("OK");
        assertThat(receivedToolContexts).containsExactly(
            Map.of("conversationId", "thread-1"), Map.of("conversationId", "thread-1"));
        assertThat(invokedAdvisors).containsExactlyInAnyOrder("first", "second");
    }

    @Test
    void testCallWithoutAToolContextPassesNullToTheContributors() {
        List<Map<String, Object>> receivedToolContexts = new ArrayList<>();
        List<String> invokedAdvisors = new ArrayList<>();

        ChatClient chatClient = SubAgentContributingChatClient.wrap(
            ChatClient.builder(new CannedChatModel())
                .build(),
            List.of(recordingContributor("only", receivedToolContexts, invokedAdvisors)));

        chatClient.prompt()
            .user("hello")
            .call()
            .content();

        assertThat(receivedToolContexts).containsExactly((Map<String, Object>) null);
        assertThat(invokedAdvisors).containsExactly("only");
    }

    @Test
    void testStreamRunsTheContributorsToo() {
        List<Map<String, Object>> receivedToolContexts = new ArrayList<>();

        ChatClient chatClient = SubAgentContributingChatClient.wrap(
            ChatClient.builder(new CannedChatModel())
                .build(),
            List.of((chatClientRequestSpec, toolContext) -> {
                receivedToolContexts.add(toolContext);

                return chatClientRequestSpec;
            }));

        List<String> chunks = chatClient.prompt("hello")
            .toolContext(Map.of("conversationId", "thread-1"))
            .stream()
            .content()
            .collectList()
            .block();

        assertThat(chunks).containsExactly("OK");
        assertThat(receivedToolContexts).containsExactly(Map.of("conversationId", "thread-1"));
    }

    @Test
    void testEachPromptCapturesItsOwnToolContext() {
        List<Map<String, Object>> receivedToolContexts = new ArrayList<>();

        ChatClient chatClient = SubAgentContributingChatClient.wrap(
            ChatClient.builder(new CannedChatModel())
                .build(),
            List.of((chatClientRequestSpec, toolContext) -> {
                receivedToolContexts.add(toolContext);

                return chatClientRequestSpec;
            }));

        chatClient.prompt("first")
            .toolContext(Map.of("conversationId", "thread-1"))
            .call()
            .content();
        chatClient.prompt("second")
            .call()
            .content();

        assertThat(receivedToolContexts).containsExactly(Map.of("conversationId", "thread-1"), null);
    }

    private static SubAgentAdvisorContributor recordingContributor(
        String name, List<Map<String, Object>> receivedToolContexts, List<String> invokedAdvisors) {

        return (chatClientRequestSpec, toolContext) -> {
            receivedToolContexts.add(toolContext);

            return chatClientRequestSpec.advisors(new RecordingAdvisor(name, invokedAdvisors));
        };
    }

    private record RecordingAdvisor(String name, List<String> invokedAdvisors) implements CallAdvisor {

        @Override
        public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
            invokedAdvisors.add(name);

            return callAdvisorChain.nextCall(chatClientRequest);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getOrder() {
            return 0;
        }
    }

    private static final class CannedChatModel implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            return cannedResponse();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(cannedResponse());
        }

        private static ChatResponse cannedResponse() {
            return ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("OK"))))
                .build();
        }
    }
}
