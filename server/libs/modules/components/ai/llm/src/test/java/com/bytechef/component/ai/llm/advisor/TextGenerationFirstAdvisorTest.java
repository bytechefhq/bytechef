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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/**
 * @author Ivica Cardic
 */
class TextGenerationFirstAdvisorTest {

    private final CallAdvisorChain callAdvisorChain = mock(CallAdvisorChain.class);
    private final ChatClientRequest chatClientRequest = mock(ChatClientRequest.class);
    private final TextGenerationFirstAdvisor textGenerationFirstAdvisor = new TextGenerationFirstAdvisor();

    @Test
    void testAdviseCallMovesTextGenerationAheadOfThinkingGenerations() {
        Generation thinkingGeneration = new Generation(
            AssistantMessage.builder()
                .content("")
                .properties(Map.of("signature", "Eu0LCpABCBIYAipA"))
                .build());
        Generation redactedThinkingGeneration = new Generation(
            AssistantMessage.builder()
                .properties(Map.of("data", "EmwKAhgBEgy3va3pzix"))
                .build());
        Generation textGeneration = new Generation(new AssistantMessage("{\"result\":[]}"));

        ChatResponseMetadata chatResponseMetadata = ChatResponseMetadata.builder()
            .id("msg_011CfQDE97saWzUai2x6pFCA")
            .build();

        ChatResponse chatResponse = ChatResponse.builder()
            .generations(List.of(thinkingGeneration, redactedThinkingGeneration, textGeneration))
            .metadata(chatResponseMetadata)
            .build();

        when(callAdvisorChain.nextCall(any())).thenReturn(new ChatClientResponse(chatResponse, Map.of()));

        ChatClientResponse chatClientResponse = textGenerationFirstAdvisor.adviseCall(
            chatClientRequest, callAdvisorChain);

        ChatResponse advisedChatResponse = chatClientResponse.chatResponse();

        Generation result = advisedChatResponse.getResult();

        assertEquals("{\"result\":[]}", result.getOutput()
            .getText());
        assertEquals(
            List.of(textGeneration, thinkingGeneration, redactedThinkingGeneration),
            advisedChatResponse.getResults());
        assertEquals("msg_011CfQDE97saWzUai2x6pFCA", advisedChatResponse.getMetadata()
            .getId());
    }

    @Test
    void testAdviseCallLeavesResponseWithoutThinkingGenerationsUntouched() {
        ChatResponse chatResponse = ChatResponse.builder()
            .generations(
                List.of(
                    new Generation(new AssistantMessage("first choice")),
                    new Generation(new AssistantMessage("second choice"))))
            .build();

        ChatClientResponse chatClientResponse = new ChatClientResponse(chatResponse, Map.of());

        when(callAdvisorChain.nextCall(any())).thenReturn(chatClientResponse);

        assertSame(chatClientResponse, textGenerationFirstAdvisor.adviseCall(chatClientRequest, callAdvisorChain));
    }
}
