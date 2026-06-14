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

package com.bytechef.component.ai.llm;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MESSAGES;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @author Marko Kriskovic
 */
class ChatModelActionTest extends AbstractActionTest {

    private static final String ANSWER = "ANSWER";

    @Test
    void testGetResponse() {
        when(mockedParameters.getRequiredString(FORMAT))
            .thenReturn(ChatModel.Format.ADVANCED.name());
        when(mockedParameters.getList(eq(MESSAGES), any(TypeReference.class)))
            .thenReturn(List.of(new ChatModel.Message("QUESTION", List.of(), ChatModel.Role.USER)));
        when(mockedParameters.getRequiredFromPath(RESPONSE + "." + RESPONSE_FORMAT, ChatModel.ResponseFormat.class))
            .thenReturn(ChatModel.ResponseFormat.TEXT);

        ChatModel mockedChat = spy(new MockChatModel());
        org.springframework.ai.chat.model.ChatModel mockedChatModelModel = mock(
            org.springframework.ai.chat.model.ChatModel.class);

        when(mockedChat.createChatModel(mockedParameters, mockedParameters, true)).thenReturn(mockedChatModelModel);

        when(mockedChatModelModel.getOptions()).thenReturn(ChatOptions.builder()
            .build());
        when(mockedChatModelModel.call(any(Prompt.class))).thenReturn(
            new ChatResponse(List.of(new Generation(new AssistantMessage(ANSWER)))));

        Object response = mockedChat.getResponse(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(ANSWER, response);
    }

    @Test
    void testGetResponseWithTextResponseFormatDoesNotReprompt() {
        when(mockedParameters.getRequiredString(FORMAT))
            .thenReturn(ChatModel.Format.ADVANCED.name());
        when(mockedParameters.getList(eq(MESSAGES), any(TypeReference.class)))
            .thenReturn(List.of(new ChatModel.Message("QUESTION", List.of(), ChatModel.Role.USER)));
        when(mockedParameters.getRequiredFromPath(RESPONSE + "." + RESPONSE_FORMAT, ChatModel.ResponseFormat.class))
            .thenReturn(ChatModel.ResponseFormat.TEXT);

        ChatModel mockedChat = spy(new MockChatModel());
        org.springframework.ai.chat.model.ChatModel mockedChatModelModel = mock(
            org.springframework.ai.chat.model.ChatModel.class);

        when(mockedChat.createChatModel(mockedParameters, mockedParameters, true)).thenReturn(mockedChatModelModel);

        when(mockedChatModelModel.getOptions()).thenReturn(ChatOptions.builder()
            .build());
        when(mockedChatModelModel.call(any(Prompt.class))).thenReturn(
            new ChatResponse(List.of(new Generation(new AssistantMessage(ANSWER)))));

        Object response = mockedChat.getResponse(mockedParameters, mockedParameters, mockedActionContext);

        // TEXT responses carry no schema, so no StructuredOutputValidationAdvisor is attached: the model is called
        // exactly once, with no re-prompt.
        verify(mockedChatModelModel, times(1)).call(any(Prompt.class));

        assertEquals(ANSWER, response);
    }

    @Test
    void testGetResponseWithJsonResponseFormatRepromptsOnSchemaViolation() {
        when(mockedParameters.getRequiredString(FORMAT))
            .thenReturn(ChatModel.Format.ADVANCED.name());
        when(mockedParameters.getList(eq(MESSAGES), any(TypeReference.class)))
            .thenReturn(List.of(new ChatModel.Message("QUESTION", List.of(), ChatModel.Role.USER)));
        when(mockedParameters.getRequiredFromPath(RESPONSE + "." + RESPONSE_FORMAT, ChatModel.ResponseFormat.class))
            .thenReturn(ChatModel.ResponseFormat.JSON);
        when(mockedParameters.getFromPath(eq(RESPONSE + "." + RESPONSE_SCHEMA), eq(String.class)))
            .thenReturn("{\"type\":\"object\",\"properties\":{\"answer\":{\"type\":\"string\"}}}");

        // Wire context.json so JsonSchemaStructuredOutputConverter can read the schema map and serialize it. The
        // serialized schema is what the StructuredOutputValidationAdvisor validates the model output against.
        Context.Json json = mock(Context.Json.class);

        Map<String, Object> schemaMap = new LinkedHashMap<>();

        schemaMap.put("type", "object");
        schemaMap.put("properties", Map.of("answer", Map.of("type", "string")));

        when(json.readMap(anyString(), eq(Object.class))).thenReturn(schemaMap);
        when(json.write(any())).thenReturn(
            "{\"type\":\"object\",\"properties\":{\"answer\":{\"type\":\"string\"}}," +
                "\"required\":[\"answer\"],\"additionalProperties\":false}");
        when(json.read(anyString(), any(TypeReference.class))).thenReturn(Map.of("answer", "hello"));
        when(mockedActionContext.json(any())).thenAnswer(invocation -> {
            Context.ContextFunction<Context.Json, Object> contextFunction = invocation.getArgument(0);

            return contextFunction.apply(json);
        });

        ChatModel mockedChat = spy(new MockChatModel());
        org.springframework.ai.chat.model.ChatModel mockedChatModelModel = mock(
            org.springframework.ai.chat.model.ChatModel.class);

        when(mockedChat.createChatModel(mockedParameters, mockedParameters, true)).thenReturn(mockedChatModelModel);

        when(mockedChatModelModel.getOptions()).thenReturn(ChatOptions.builder()
            .build());

        // First the model returns schema-invalid JSON (missing required "answer", extra property), then valid JSON.
        when(mockedChatModelModel.call(any(Prompt.class))).thenReturn(
            new ChatResponse(List.of(new Generation(new AssistantMessage("{\"name\":\"x\"}")))),
            new ChatResponse(List.of(new Generation(new AssistantMessage("{\"answer\":\"hello\"}")))));

        Object response = mockedChat.getResponse(mockedParameters, mockedParameters, mockedActionContext);

        // The validation advisor re-prompts after the first (invalid) output, so the model is called twice.
        verify(mockedChatModelModel, times(2)).call(any(Prompt.class));

        assertEquals(Map.of("answer", "hello"), response);
    }

    @Test
    void testStructuredOutputValidationAdvisorDoesNotSupportStreaming() {
        // Pins WHY ChatModel.stream() omits the validation advisor: the advisor cannot operate on a streaming
        // response. If a future Spring AI release adds streaming support, this fails and the stream() wiring can be
        // revisited.
        StructuredOutputValidationAdvisor advisor = StructuredOutputValidationAdvisor.builder()
            .outputJsonSchema("{\"type\":\"object\"}")
            .build();

        Flux<ChatClientResponse> flux = advisor.adviseStream(
            mock(ChatClientRequest.class), mock(StreamAdvisorChain.class));

        assertThrows(UnsupportedOperationException.class, flux::blockLast);
    }

    private static class MockChatModel implements ChatModel {

        @Override
        public org.springframework.ai.chat.model.ChatModel createChatModel(
            Parameters inputParameters, Parameters connectionParameters, boolean responseFormatRequired) {

            return prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(ANSWER))));
        }
    }
}
