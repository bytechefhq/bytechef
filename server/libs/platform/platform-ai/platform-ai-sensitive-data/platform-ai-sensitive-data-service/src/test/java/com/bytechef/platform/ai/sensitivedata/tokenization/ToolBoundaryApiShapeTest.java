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

package com.bytechef.platform.ai.sensitivedata.tokenization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.model.tool.ToolExecutionResult;

class ToolBoundaryApiShapeTest {

    @Test
    void testToolCallArgumentsAreReachableAndRebuildableOnAChatResponse() {
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
            "call-1", "function", "sendEmail", "{\"to\":\"[PII_EMAIL_ADDRESS_1_k3n9]\"}");
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(toolCall))
            .build();
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(assistantMessage)));

        Generation generation = chatResponse.getResult();

        if (generation == null) {
            throw new IllegalStateException("Expected a generation in the ChatResponse");
        }

        AssistantMessage output = generation.getOutput();

        assertThat(output.getToolCalls()).singleElement()
            .extracting(AssistantMessage.ToolCall::arguments)
            .isEqualTo("{\"to\":\"[PII_EMAIL_ADDRESS_1_k3n9]\"}");

        AssistantMessage.ToolCall rewritten = new AssistantMessage.ToolCall(
            toolCall.id(), toolCall.type(), toolCall.name(), "{\"to\":\"bob@acme.io\"}");
        AssistantMessage rebuiltAssistantMessage = AssistantMessage.builder()
            .content(output.getText())
            .properties(output.getMetadata())
            .toolCalls(List.of(rewritten))
            .build();
        ChatResponse rebuilt = new ChatResponse(
            List.of(new Generation(rebuiltAssistantMessage)), chatResponse.getMetadata());

        Generation rebuiltGeneration = rebuilt.getResult();

        if (rebuiltGeneration == null) {
            throw new IllegalStateException("Expected a generation in the rebuilt ChatResponse");
        }

        assertThat(rebuiltGeneration.getOutput()
            .getToolCalls()).singleElement()
                .extracting(AssistantMessage.ToolCall::arguments)
                .isEqualTo("{\"to\":\"bob@acme.io\"}");
    }

    @Test
    void testToolResultsAreReachableAndRebuildableOnAToolExecutionResult() {
        ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse(
            "call-1", "lookupCustomer", "{\"email\":\"bob@acme.io\"}");
        ToolExecutionResult result = ToolExecutionResult.builder()
            .conversationHistory(
                List.of(
                    ToolResponseMessage.builder()
                        .responses(List.of(response))
                        .build()))
            .build();

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) result.conversationHistory()
            .getLast();

        assertThat(toolResponseMessage.getResponses()).singleElement()
            .extracting(ToolResponseMessage.ToolResponse::responseData)
            .isEqualTo("{\"email\":\"bob@acme.io\"}");

        ToolResponseMessage.ToolResponse rewrittenResponse = new ToolResponseMessage.ToolResponse(
            response.id(), response.name(), "{\"email\":\"[PII_EMAIL_ADDRESS_1_k3n9]\"}");
        ToolExecutionResult rebuilt = ToolExecutionResult.builder()
            .conversationHistory(
                List.of(
                    ToolResponseMessage.builder()
                        .responses(List.of(rewrittenResponse))
                        .build()))
            .returnDirect(result.returnDirect())
            .build();

        ToolResponseMessage rebuiltToolResponseMessage = (ToolResponseMessage) rebuilt.conversationHistory()
            .getLast();

        assertThat(rebuiltToolResponseMessage.getResponses()).singleElement()
            .extracting(ToolResponseMessage.ToolResponse::responseData)
            .isEqualTo("{\"email\":\"[PII_EMAIL_ADDRESS_1_k3n9]\"}");
        assertThat(rebuilt.returnDirect()).isEqualTo(result.returnDirect());
    }
}
