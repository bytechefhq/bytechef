/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.openai.action;

import static com.bytechef.component.openai.constant.OpenAIConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.LOGIT_BIAS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MAX_TOKENS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MESSAGES;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
import static com.bytechef.component.openai.constant.OpenAIConstants.PRESENCE_PENALTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.STOP;
import static com.bytechef.component.openai.constant.OpenAIConstants.STREAM;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOP_P;
import static com.bytechef.component.openai.constant.OpenAIConstants.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.TypeReference;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.Flowable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

/**
 * @author Monika Domiter
 */
public class OpenAIAskChatGPTActionTest extends AbstractOpenAIActionTest {

    private final ArgumentCaptor<ChatCompletionRequest> chatCompletionRequestArgumentCaptor =
        ArgumentCaptor.forClass(ChatCompletionRequest.class);
    private final List<ChatMessage> chatMessages = List.of(new ChatMessage());
    private final Map<String, Integer> map = new HashMap<>();
    private final List<String> strings = List.of("a");

    @BeforeEach
    public void before() {
        when(mockedParameters.getList(eq(MESSAGES), any(TypeReference.class)))
            .thenReturn(chatMessages);
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn("MODEL");
        when(mockedParameters.getDouble(FREQUENCY_PENALTY))
            .thenReturn(0.0);
        when(mockedParameters.getMap(eq(LOGIT_BIAS), any(TypeReference.class)))
            .thenReturn(map);
        when(mockedParameters.getInteger(MAX_TOKENS))
            .thenReturn(1);
        when(mockedParameters.getInteger(N))
            .thenReturn(1);
        when(mockedParameters.getDouble(PRESENCE_PENALTY))
            .thenReturn(0.0);
        when(mockedParameters.getList(eq(STOP), any(TypeReference.class)))
            .thenReturn(strings);
        when(mockedParameters.getDouble(TEMPERATURE))
            .thenReturn(1.0);
        when(mockedParameters.getDouble(TOP_P))
            .thenReturn(1.0);
        when(mockedParameters.getString(USER))
            .thenReturn("USER");
    }

    @Test
    public void testPerformIsNotStream() {
        ChatCompletionResult mockedChatCompletionResult = mock(ChatCompletionResult.class);

        when(mockedParameters.getRequiredBoolean(STREAM))
            .thenReturn(false);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction =
            mockConstruction(
                OpenAiService.class,
                (mock, context) -> when(
                    mock.createChatCompletion(chatCompletionRequestArgumentCaptor.capture()))
                        .thenReturn(mockedChatCompletionResult))) {

            Object result = OpenAIAskChatGPTAction.perform(mockedParameters, mockedParameters, mockedContext);

            List<OpenAiService> openAiServices = openAiServiceMockedConstruction.constructed();

            assertEquals(1, openAiServices.size());
            assertEquals(mockedChatCompletionResult, result);

            OpenAiService openAiService = openAiServices.getFirst();

            verify(openAiService).createChatCompletion(chatCompletionRequestArgumentCaptor.capture());
            verify(openAiService, times(1)).createChatCompletion(chatCompletionRequestArgumentCaptor.capture());

            ChatCompletionRequest chatCompletionRequest = chatCompletionRequestArgumentCaptor.getValue();

            assertEquals(false, chatCompletionRequest.getStream());
        }
    }

    @Test
    public void testPerformIsStream() {
        Flowable<ChatCompletionChunk> chatCompletionChunkFlowable = mock(Flowable.class);

        when(mockedParameters.getRequiredBoolean(STREAM))
            .thenReturn(true);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction = mockConstruction(
            OpenAiService.class,
            (openAiService, context) -> when(openAiService.streamChatCompletion(any()))
                .thenReturn(chatCompletionChunkFlowable))) {

            Object result = OpenAIAskChatGPTAction.perform(mockedParameters, mockedParameters, mockedContext);

            List<OpenAiService> openAiServices = openAiServiceMockedConstruction.constructed();

            assertEquals(1, openAiServices.size());
            assertEquals(chatCompletionChunkFlowable.toList(), result);

            OpenAiService openAiService = openAiServices.getFirst();

            verify(openAiService, times(1)).streamChatCompletion(chatCompletionRequestArgumentCaptor.capture());

            ChatCompletionRequest chatCompletionRequest = chatCompletionRequestArgumentCaptor.getValue();

            assertEquals(true, chatCompletionRequest.getStream());
        }
    }

    @AfterEach
    public void afterEach() {
        ChatCompletionRequest chatCompletionRequest = chatCompletionRequestArgumentCaptor.getValue();

        assertEquals(chatMessages, chatCompletionRequest.getMessages());
        assertEquals("MODEL", chatCompletionRequest.getModel());
        assertEquals(0.0, chatCompletionRequest.getFrequencyPenalty());
        assertEquals(map, chatCompletionRequest.getLogitBias());
        assertEquals(1, chatCompletionRequest.getMaxTokens());
        assertEquals(1, chatCompletionRequest.getN());
        assertEquals(0.0, chatCompletionRequest.getPresencePenalty());
        assertEquals(strings, chatCompletionRequest.getStop());
        assertEquals(1.0, chatCompletionRequest.getTemperature());
        assertEquals(1.0, chatCompletionRequest.getTopP());
        assertEquals("USER", chatCompletionRequest.getUser());
    }
}
