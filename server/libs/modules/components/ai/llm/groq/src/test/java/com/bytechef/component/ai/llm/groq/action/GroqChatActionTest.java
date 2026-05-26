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

package com.bytechef.component.ai.llm.groq.action;

import static com.bytechef.component.ai.llm.ChatModel.ResponseFormat.JSON;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat.Type;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * @author Nikolina Spehar
 */
class GroqChatActionTest {

    private final ArgumentCaptor<Duration> durationArgumentCaptor = forClass(Duration.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "token"));
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        new HashMap<>() {
            {
                put(MODEL, "llama-3.3-70b-versatile");
                put(FREQUENCY_PENALTY, 0.2);
                put(LOGIT_BIAS, Map.of("100", 1));
                put(MAX_TOKENS, 1000);
                put(N, 1);
                put(PRESENCE_PENALTY, 0.3);
                put(RESPONSE, Map.of(RESPONSE_FORMAT, JSON));
                put(STOP, List.of("STOP"));
                put(TEMPERATURE, 0.7);
                put(TOP_P, 0.9);
                put(USER, "test-user");
            }
        });
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreateChatModel() {
        try (MockedStatic<OpenAIOkHttpClient> openAIOkHttpClientMockedStatic = mockStatic(OpenAIOkHttpClient.class)) {
            OpenAIOkHttpClient.Builder mockedOpenAIOkHttpClientBuilder = mock(OpenAIOkHttpClient.Builder.class);

            openAIOkHttpClientMockedStatic.when(OpenAIOkHttpClient::builder)
                .thenReturn(mockedOpenAIOkHttpClientBuilder);

            when(mockedOpenAIOkHttpClientBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.baseUrl(stringArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.timeout(durationArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.build())
                .thenReturn(mock(OpenAIClient.class));

            org.springframework.ai.chat.model.ChatModel chatModel = GroqChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, true);

            assertNotNull(chatModel);
            assertInstanceOf(OpenAiChatModel.class, chatModel);

            OpenAiChatModel openAiChatModel = (OpenAiChatModel) chatModel;

            OpenAiChatOptions openAiChatOptions = (OpenAiChatOptions) openAiChatModel.getDefaultOptions();

            assertEquals("token", openAiChatOptions.getApiKey());
            assertEquals("llama-3.3-70b-versatile", openAiChatOptions.getModel());
            assertEquals(0.2, openAiChatOptions.getFrequencyPenalty());
            assertEquals(Map.of("100", 1), openAiChatOptions.getLogitBias());
            assertEquals(1000, openAiChatOptions.getMaxTokens());
            assertEquals(1, openAiChatOptions.getN());
            assertEquals(0.3, openAiChatOptions.getPresencePenalty());

            ResponseFormat responseFormat = openAiChatOptions.getResponseFormat();

            assertNotNull(responseFormat);
            assertEquals(Type.JSON_OBJECT, responseFormat.getType());
            assertEquals(List.of("STOP"), openAiChatOptions.getStop());
            assertEquals(0.7, openAiChatOptions.getTemperature());
            assertEquals(0.9, openAiChatOptions.getTopP());
            assertEquals("test-user", openAiChatOptions.getUser());

            assertEquals(List.of("token", "https://api.groq.com/openai/v1"), stringArgumentCaptor.getAllValues());
            assertEquals(Duration.ofMinutes(5), durationArgumentCaptor.getValue());
        }
    }
}
