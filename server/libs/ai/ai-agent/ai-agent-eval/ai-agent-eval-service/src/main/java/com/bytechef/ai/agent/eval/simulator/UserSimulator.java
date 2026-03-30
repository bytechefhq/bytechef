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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * Simulates a user persona in multi-turn conversations with an AI agent.
 *
 * <p>
 * Uses a {@link ChatClient} to generate contextually appropriate user messages based on a given persona prompt and the
 * ongoing conversation history.
 * </p>
 *
 * @author ByteChef
 */
public class UserSimulator {

    private static final String CONVERSATION_COMPLETE_MARKER = "[CONVERSATION_COMPLETE]";

    private final ChatClient chatClient;
    private final String personaPrompt;

    public UserSimulator(ChatClient chatClient, String personaPrompt) {
        this.chatClient = chatClient;
        this.personaPrompt = personaPrompt;
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    public SimulationResult generateNextMessage(List<Map<String, String>> conversationHistory) {
        String systemPrompt = """
            You are simulating a user in a conversation with an AI agent.
            Your persona: %s

            Guidelines:
            - Stay in character throughout the conversation
            - Respond naturally based on what the agent says
            - If the agent resolves your issue satisfactorily, respond with "%s"
            - Do not break character or acknowledge you are a simulation
            """.formatted(personaPrompt, CONVERSATION_COMPLETE_MARKER);

        List<Message> chatMessages = new ArrayList<>();

        for (Map<String, String> entry : conversationHistory) {
            String role = entry.get("role");
            String content = entry.get("content");

            if ("user".equals(role)) {
                chatMessages.add(new UserMessage(content));
            } else if ("assistant".equals(role)) {
                chatMessages.add(new AssistantMessage(content));
            }
        }

        ChatResponse chatResponse = chatClient.prompt()
            .system(systemPrompt)
            .messages(chatMessages)
            .call()
            .chatResponse();

        String message = "";
        int promptTokens = 0;
        int completionTokens = 0;

        if (chatResponse != null) {
            if (chatResponse.getResult() != null) {
                message = chatResponse.getResult()
                    .getOutput()
                    .getText();
            }

            ChatResponseMetadata metadata = chatResponse.getMetadata();

            if (metadata != null) {
                Usage usage = metadata.getUsage();

                if (usage != null) {
                    promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                    completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                }
            }
        }

        return new SimulationResult(message != null ? message : "", promptTokens, completionTokens);
    }

    public boolean isConversationComplete(String message) {
        return message.toUpperCase()
            .contains(CONVERSATION_COMPLETE_MARKER) ||
            message.toLowerCase()
                .contains("conversation complete");
    }

    public record SimulationResult(String message, int promptTokens, int completionTokens) {
    }

}
