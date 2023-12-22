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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
public class OpenAIAskChatGPTActionTest extends AbstractOpenAIActionTest {

    ArgumentCaptor<ChatCompletionRequest> chatCompletionRequestArgumentCaptor =
        ArgumentCaptor.forClass(ChatCompletionRequest.class);
    List<ChatMessage> chatMessageList = List.of(new ChatMessage());
    Map<String, Integer> map = new HashMap<>();
    List<String> stringList = List.of("a");

    @BeforeEach
    public void before() {
        Mockito.when((List<ChatMessage>) parameterMap.getList(MESSAGES))
            .thenReturn(chatMessageList);
        Mockito.when(parameterMap.getRequiredString(MODEL))
            .thenReturn("MODEL");
        Mockito.when(parameterMap.getDouble(FREQUENCY_PENALTY))
            .thenReturn(0.0);
        Mockito.when((Map<String, Integer>) parameterMap.getMap(LOGIT_BIAS))
            .thenReturn(map);
        Mockito.when(parameterMap.getInteger(MAX_TOKENS))
            .thenReturn(1);
        Mockito.when(parameterMap.getInteger(N))
            .thenReturn(1);
        Mockito.when(parameterMap.getDouble(PRESENCE_PENALTY))
            .thenReturn(0.0);
        Mockito.when((List<String>) parameterMap.getList(STOP))
            .thenReturn(stringList);
        Mockito.when(parameterMap.getDouble(TEMPERATURE))
            .thenReturn(1.0);
        Mockito.when(parameterMap.getDouble(TOP_P))
            .thenReturn(1.0);
        Mockito.when(parameterMap.getString(USER))
            .thenReturn("USER");
    }

    @Test
    public void testPerformIsNotStream() {
        ChatCompletionResult chatCompletionResult = Mockito.mock(ChatCompletionResult.class);

        Mockito.when(parameterMap.getBoolean(STREAM))
            .thenReturn(false);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction =
            Mockito.mockConstruction(OpenAiService.class,
                (mock, context) -> when(mock.createChatCompletion(chatCompletionRequestArgumentCaptor.capture()))
                    .thenReturn(chatCompletionResult))) {

            Object perform = OpenAIAskChatGPTAction.perform(parameterMap, parameterMap, context);

            Assertions.assertEquals(1, openAiServiceMockedConstruction.constructed()
                .size());
            Assertions.assertEquals(chatCompletionResult, perform);

            OpenAiService mock = openAiServiceMockedConstruction.constructed()
                .get(0);

            verify(mock).createChatCompletion(chatCompletionRequestArgumentCaptor.capture());
            verify(mock, times(1)).createChatCompletion(chatCompletionRequestArgumentCaptor.capture());

            Assertions.assertEquals(false, chatCompletionRequestArgumentCaptor.getValue()
                .getStream());

        }
    }

    @Test
    public void testPerformIsStream() {
        Flowable<ChatCompletionChunk> chatCompletionChunkFlowable = Mockito.mock(Flowable.class);

        Mockito.when(parameterMap.getBoolean(STREAM))
            .thenReturn(true);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction =
            Mockito.mockConstruction(OpenAiService.class,
                (mock, context) -> when(mock.streamChatCompletion(chatCompletionRequestArgumentCaptor.capture()))
                    .thenReturn(chatCompletionChunkFlowable))) {

            Object perform = OpenAIAskChatGPTAction.perform(parameterMap, parameterMap, context);

            Assertions.assertEquals(1, openAiServiceMockedConstruction.constructed()
                .size());
            Assertions.assertEquals(chatCompletionChunkFlowable.toList(), perform);

            OpenAiService mock = openAiServiceMockedConstruction.constructed()
                .get(0);

            verify(mock).streamChatCompletion(chatCompletionRequestArgumentCaptor.capture());
            verify(mock, times(1)).streamChatCompletion(chatCompletionRequestArgumentCaptor.capture());

            Assertions.assertEquals(true, chatCompletionRequestArgumentCaptor.getValue()
                .getStream());
        }
    }

    @AfterEach
    public void afterEach() {
        Assertions.assertEquals(chatMessageList, chatCompletionRequestArgumentCaptor.getValue()
            .getMessages());
        Assertions.assertEquals("MODEL", chatCompletionRequestArgumentCaptor.getValue()
            .getModel());
        Assertions.assertEquals(0.0, chatCompletionRequestArgumentCaptor.getValue()
            .getFrequencyPenalty());
        Assertions.assertEquals(map, chatCompletionRequestArgumentCaptor.getValue()
            .getLogitBias());
        Assertions.assertEquals(1, chatCompletionRequestArgumentCaptor.getValue()
            .getMaxTokens());
        Assertions.assertEquals(1, chatCompletionRequestArgumentCaptor.getValue()
            .getN());
        Assertions.assertEquals(0.0, chatCompletionRequestArgumentCaptor.getValue()
            .getPresencePenalty());
        Assertions.assertEquals(stringList, chatCompletionRequestArgumentCaptor.getValue()
            .getStop());
        Assertions.assertEquals(1.0, chatCompletionRequestArgumentCaptor.getValue()
            .getTemperature());
        Assertions.assertEquals(1.0, chatCompletionRequestArgumentCaptor.getValue()
            .getTopP());
        Assertions.assertEquals("USER", chatCompletionRequestArgumentCaptor.getValue()
            .getUser());
    }
}
