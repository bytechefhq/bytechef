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

package com.bytechef.component.ai.llm.advisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * @author Ivica Cardic
 */
class CodeFenceStrippingAdvisorTest {

    private static final String JSON_SCHEMA = """
        {"type":"object","required":["result"],"additionalProperties":false,
         "properties":{"result":{"type":"array","items":{"type":"string"}}}}
        """;

    private final CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);
    private final ChatClientRequest chatClientRequest = mock(ChatClientRequest.class);
    private final CodeFenceStrippingAdvisor codeFenceStrippingAdvisor = new CodeFenceStrippingAdvisor();

    @Test
    void testAdviseCallStripsJsonCodeFence() {
        ChatGenerationMetadata chatGenerationMetadata = ChatGenerationMetadata.builder()
            .finishReason("end_turn")
            .build();

        Generation generation = new Generation(
            AssistantMessage.builder()
                .content("```json\n{\"result\":[\"a\"]}\n```")
                .properties(Map.of("id", "msg_1"))
                .build(),
            chatGenerationMetadata);

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(generation))
            .metadata(
                ChatResponseMetadata.builder()
                    .id("msg_1")
                    .build())
            .build();

        when(callAdvisorChain.nextCall(any())).thenReturn(new ChatClientResponse(chatResponse, Map.of()));

        ChatClientResponse chatClientResponse = codeFenceStrippingAdvisor.adviseCall(
            chatClientRequest, callAdvisorChain);

        ChatResponse advisedChatResponse = chatClientResponse.chatResponse();

        Generation result = advisedChatResponse.getResult();

        AssistantMessage output = result.getOutput();

        assertEquals("{\"result\":[\"a\"]}", output.getText());
        assertEquals("msg_1", output.getMetadata()
            .get("id"));
        assertSame(chatGenerationMetadata, result.getMetadata());
        assertEquals("msg_1", advisedChatResponse.getMetadata()
            .getId());
    }

    @Test
    void testAdviseCallLeavesUnfencedResponseUntouched() {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(new Generation(new AssistantMessage("{\"result\":[]}"))))
            .build();

        ChatClientResponse chatClientResponse = new ChatClientResponse(chatResponse, Map.of());

        when(callAdvisorChain.nextCall(any())).thenReturn(chatClientResponse);

        assertSame(chatClientResponse, codeFenceStrippingAdvisor.adviseCall(chatClientRequest, callAdvisorChain));
    }

    @Test
    void testOrderRunsInsideValidation() {
        StructuredOutputValidationAdvisor structuredOutputValidationAdvisor = StructuredOutputValidationAdvisor
            .builder()
            .outputJsonSchema(JSON_SCHEMA)
            .build();

        assertTrue(codeFenceStrippingAdvisor.getOrder() > structuredOutputValidationAdvisor.getOrder());
    }

    @Test
    void testFencedReplyPassesValidationWithoutRetry() {
        AtomicInteger callCount = new AtomicInteger();

        ChatModel chatModel = new FencedJsonChatModel(callCount);

        String content = ChatClient.create(chatModel)
            .prompt()
            .user("List the items")
            .advisors(
                StructuredOutputValidationAdvisor.builder()
                    .outputJsonSchema(JSON_SCHEMA)
                    .build(),
                codeFenceStrippingAdvisor)
            .call()
            .content();

        assertEquals("{\"result\":[\"a\",\"b\"]}", content);
        assertEquals(1, callCount.get());
    }

    @Test
    void testFencedReplyIsRetriedWithoutAdvisor() {
        AtomicInteger callCount = new AtomicInteger();

        ChatModel chatModel = new FencedJsonChatModel(callCount);

        ChatClient.create(chatModel)
            .prompt()
            .user("List the items")
            .advisors(
                StructuredOutputValidationAdvisor.builder()
                    .outputJsonSchema(JSON_SCHEMA)
                    .build())
            .call()
            .content();

        assertEquals(4, callCount.get());
    }

    private record FencedJsonChatModel(AtomicInteger callCount) implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            callCount.incrementAndGet();

            return new ChatResponse(
                List.of(new Generation(new AssistantMessage("```json\n{\"result\":[\"a\",\"b\"]}\n```"))));
        }

        @Override
        public ChatOptions getOptions() {
            return ChatOptions.builder()
                .build();
        }
    }
}
