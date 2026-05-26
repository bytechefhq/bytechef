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

package com.bytechef.component.ai.llm.mistral.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.mistral.constant.MistralConstants.SAFE_PROMPT;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static java.lang.Boolean.FALSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.mistralai.api.MistralAiApi.ChatCompletionRequest.ResponseFormat;

/**
 * @author Nikolina Spehar
 */
class MistralChatActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreateChatModelWithResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.ofEntries(
                Map.entry(RESPONSE, Map.of(RESPONSE_FORMAT, ChatModel.ResponseFormat.TEXT.name())),
                Map.entry(MODEL, "mistral-large-latest"),
                Map.entry(TEMPERATURE, 0.7),
                Map.entry(MAX_TOKENS, 1000),
                Map.entry(TOP_P, 0.9),
                Map.entry(STOP, List.of("stop")),
                Map.entry(SAFE_PROMPT, false),
                Map.entry(SEED, 42)));

        try (MockedStatic<MistralAiApi> mistralAiApiMockedStatic = mockStatic(MistralAiApi.class)) {
            MistralAiApi.Builder mockedApiBuilder = mock(MistralAiApi.Builder.class);
            MistralAiApi mockedApi = mock(MistralAiApi.class);

            mistralAiApiMockedStatic.when(MistralAiApi::builder)
                .thenReturn(mockedApiBuilder);

            when(mockedApiBuilder.baseUrl(stringArgumentCaptor.capture()))
                .thenReturn(mockedApiBuilder);
            when(mockedApiBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedApiBuilder);
            when(mockedApiBuilder.restClientBuilder(any()))
                .thenReturn(mockedApiBuilder);
            when(mockedApiBuilder.responseErrorHandler(any()))
                .thenReturn(mockedApiBuilder);
            when(mockedApiBuilder.build())
                .thenReturn(mockedApi);

            org.springframework.ai.chat.model.ChatModel chatModel = MistralChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, true);

            assertNotNull(chatModel);
            assertInstanceOf(MistralAiChatModel.class, chatModel);

            assertEquals("https://api.mistral.ai", stringArgumentCaptor.getAllValues()
                .get(0));
            assertEquals("TOKEN", stringArgumentCaptor.getAllValues()
                .get(1));

            MistralAiChatOptions options = (MistralAiChatOptions) chatModel.getDefaultOptions();

            assertEquals("mistral-large-latest", options.getModel());
            assertEquals(0.7, options.getTemperature());
            assertEquals(1000, options.getMaxTokens());
            assertEquals(0.9, options.getTopP());
            assertEquals(List.of("stop"), options.getStop());
            assertEquals(FALSE, options.getSafePrompt());
            assertEquals(42, options.getRandomSeed());

            ResponseFormat responseFormat = options.getResponseFormat();

            assertNotNull(responseFormat);
            assertEquals("text", responseFormat.getType()
                .getValue());
        }
    }

    @Test
    void testCreateChatModelWithoutResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.ofEntries(
                Map.entry(MODEL, "mistral-large-latest"),
                Map.entry(TEMPERATURE, 0.7),
                Map.entry(MAX_TOKENS, 1000),
                Map.entry(TOP_P, 0.9),
                Map.entry(STOP, List.of("stop")),
                Map.entry(SAFE_PROMPT, false),
                Map.entry(SEED, 42)));

        try (MockedStatic<MistralAiApi> mistralAiApiMockedStatic = mockStatic(MistralAiApi.class)) {
            MistralAiApi.Builder mockedApiBuilder = mock(MistralAiApi.Builder.class);
            MistralAiApi mockedApi = mock(MistralAiApi.class);

            mistralAiApiMockedStatic.when(MistralAiApi::builder)
                .thenReturn(mockedApiBuilder);

            when(mockedApiBuilder.baseUrl(stringArgumentCaptor.capture()))
                .thenReturn(mockedApiBuilder);
            when(mockedApiBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedApiBuilder);
            when(mockedApiBuilder.restClientBuilder(any()))
                .thenReturn(mockedApiBuilder);
            when(mockedApiBuilder.responseErrorHandler(any()))
                .thenReturn(mockedApiBuilder);
            when(mockedApiBuilder.build())
                .thenReturn(mockedApi);

            org.springframework.ai.chat.model.ChatModel chatModel = MistralChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, false);

            assertNotNull(chatModel);
            assertInstanceOf(MistralAiChatModel.class, chatModel);

            MistralAiChatOptions options = (MistralAiChatOptions) chatModel.getDefaultOptions();

            assertEquals("mistral-large-latest", options.getModel());
            assertEquals(0.7, options.getTemperature());
            assertEquals(1000, options.getMaxTokens());
            assertEquals(0.9, options.getTopP());
            assertEquals(List.of("stop"), options.getStop());
            assertEquals(FALSE, options.getSafePrompt());
            assertEquals(42, options.getRandomSeed());

            assertNull(options.getResponseFormat());
        }
    }
}
