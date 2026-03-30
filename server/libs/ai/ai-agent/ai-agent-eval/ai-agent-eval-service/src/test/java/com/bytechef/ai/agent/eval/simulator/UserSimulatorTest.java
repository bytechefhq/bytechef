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

package com.bytechef.ai.agent.eval.simulator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.eval.simulator.UserSimulator.SimulationResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * @author ByteChef
 */
class UserSimulatorTest {

    @Test
    void testIsConversationCompleteWithExactMarker() {
        UserSimulator simulator = new UserSimulator(mock(ChatClient.class), "test persona");

        assertTrue(simulator.isConversationComplete("[CONVERSATION_COMPLETE]"));
    }

    @Test
    void testIsConversationCompleteWithLowercaseMarker() {
        UserSimulator simulator = new UserSimulator(mock(ChatClient.class), "test persona");

        assertTrue(simulator.isConversationComplete("[conversation_complete]"));
    }

    @Test
    void testIsConversationCompleteWithPartialMatch() {
        UserSimulator simulator = new UserSimulator(mock(ChatClient.class), "test persona");

        assertTrue(simulator.isConversationComplete("The conversation complete now."));
    }

    @Test
    void testIsConversationCompleteWithNormalMessage() {
        UserSimulator simulator = new UserSimulator(mock(ChatClient.class), "test persona");

        assertFalse(simulator.isConversationComplete("Thanks for helping"));
    }

    @Test
    void testIsConversationCompleteWithMarkerInMiddleOfText() {
        UserSimulator simulator = new UserSimulator(mock(ChatClient.class), "test persona");

        assertTrue(
            simulator.isConversationComplete("That resolves my issue. [CONVERSATION_COMPLETE] Thank you!"));
    }

    @Test
    void testGenerateNextMessageReturnsResponse() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(
            buildChatResponse("I need help with my order", 10, 5));

        UserSimulator simulator = new UserSimulator(chatClient, "frustrated customer");

        List<Map<String, String>> conversationHistory = List.of(
            Map.of("role", "assistant", "content", "Hello, how can I help you?"));

        SimulationResult result = simulator.generateNextMessage(conversationHistory);

        assertEquals("I need help with my order", result.message());
        assertEquals(10, result.promptTokens());
        assertEquals(5, result.completionTokens());
    }

    @Test
    void testGenerateNextMessageSystemPromptContainsPersona() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(buildChatResponse("response", 0, 0));

        String persona = "impatient business executive";

        UserSimulator simulator = new UserSimulator(chatClient, persona);

        simulator.generateNextMessage(List.of());

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);

        verify(requestSpec).system(systemPromptCaptor.capture());

        assertTrue(systemPromptCaptor.getValue()
            .contains(persona));
    }

    @Test
    void testGenerateNextMessagePassesConversationHistory() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.messages(anyList())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.chatResponse()).thenReturn(buildChatResponse("response", 0, 0));

        UserSimulator simulator = new UserSimulator(chatClient, "test persona");

        List<Map<String, String>> conversationHistory = List.of(
            Map.of("role", "user", "content", "Hi there"),
            Map.of("role", "assistant", "content", "Hello!"),
            Map.of("role", "user", "content", "I need help"));

        simulator.generateNextMessage(conversationHistory);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> messagesCaptor = ArgumentCaptor.forClass(List.class);

        verify(requestSpec).messages(messagesCaptor.capture());

        List<Message> capturedMessages = messagesCaptor.getValue();

        assertEquals(3, capturedMessages.size());
    }

    private ChatResponse buildChatResponse(String content, int promptTokens, int completionTokens) {
        Generation generation = new Generation(new AssistantMessage(content));

        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
            .usage(new DefaultUsage(promptTokens, completionTokens))
            .build();

        return ChatResponse.builder()
            .generations(List.of(generation))
            .metadata(metadata)
            .build();
    }

}
