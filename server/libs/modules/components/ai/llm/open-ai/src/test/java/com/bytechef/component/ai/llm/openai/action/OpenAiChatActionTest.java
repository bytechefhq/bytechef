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

package com.bytechef.component.ai.llm.openai.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STORE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VERBOSITY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * @author Monika Kušter
 */
class OpenAiChatActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.ofEntries(
            Map.entry(RESPONSE, Map.of(RESPONSE_FORMAT, ChatModel.ResponseFormat.TEXT.name())),
            Map.entry(MODEL, "gpt-4o"),
            Map.entry(FREQUENCY_PENALTY, 0.5),
            Map.entry(LOGIT_BIAS, Map.of()),
            Map.entry(MAX_TOKENS, 100),
            Map.entry(N, 1),
            Map.entry(PRESENCE_PENALTY, 0.5),
            Map.entry(STOP, List.of("stop")),
            Map.entry(TEMPERATURE, 0.7),
            Map.entry(TOP_P, 0.9),
            Map.entry(USER, "user"),
            Map.entry(REASONING, "low"),
            Map.entry(VERBOSITY, "low"),
            Map.entry(STORE, true)));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Duration> durationArgumentCaptor = forClass(Duration.class);

    @Test
    void testCreateChatModelWithResponseFormat() {
        try (MockedStatic<OpenAIOkHttpClient> openAIOkHttpClientMockedStatic = mockStatic(OpenAIOkHttpClient.class);
            MockedStatic<OpenAIOkHttpClientAsync> openAIOkHttpClientAsyncMockedStatic =
                mockStatic(OpenAIOkHttpClientAsync.class)) {

            OpenAIOkHttpClient.Builder mockedOpenAIOkHttpClientBuilder = mock(OpenAIOkHttpClient.Builder.class);

            openAIOkHttpClientMockedStatic.when(OpenAIOkHttpClient::builder)
                .thenReturn(mockedOpenAIOkHttpClientBuilder);

            when(mockedOpenAIOkHttpClientBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.timeout(durationArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.build())
                .thenReturn(mock(OpenAIClient.class));

            OpenAIOkHttpClientAsync.Builder mockedOpenAIOkHttpClientAsyncBuilder =
                mock(OpenAIOkHttpClientAsync.Builder.class);

            openAIOkHttpClientAsyncMockedStatic.when(OpenAIOkHttpClientAsync::builder)
                .thenReturn(mockedOpenAIOkHttpClientAsyncBuilder);

            when(mockedOpenAIOkHttpClientAsyncBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientAsyncBuilder);
            when(mockedOpenAIOkHttpClientAsyncBuilder.timeout(durationArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientAsyncBuilder);
            when(mockedOpenAIOkHttpClientAsyncBuilder.build())
                .thenReturn(mock(OpenAIClientAsync.class));

            org.springframework.ai.chat.model.ChatModel chatModel = OpenAiChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, true);

            assertNotNull(chatModel);
            assertInstanceOf(OpenAiChatModel.class, chatModel);

            assertEquals(List.of("TOKEN", "TOKEN"), stringArgumentCaptor.getAllValues());
            assertEquals(List.of(Duration.ofMinutes(5), Duration.ofMinutes(5)), durationArgumentCaptor.getAllValues());

            OpenAiChatModel openAiChatModel = (OpenAiChatModel) chatModel;
            OpenAiChatOptions openAiChatOptions = openAiChatModel.getOptions();

            assertEquals("gpt-4o", openAiChatOptions.getModel());
            assertEquals(0.5, openAiChatOptions.getFrequencyPenalty());
            assertEquals(Map.of(), openAiChatOptions.getLogitBias());
            assertEquals(100, openAiChatOptions.getMaxTokens());
            assertEquals(1, openAiChatOptions.getN());
            assertEquals(0.5, openAiChatOptions.getPresencePenalty());

            ResponseFormat responseFormat = openAiChatOptions.getResponseFormat();

            assertNotNull(responseFormat);
            assertEquals(ResponseFormat.Type.TEXT, responseFormat.getType());
            assertEquals(List.of("stop"), openAiChatOptions.getStop());
            assertEquals(0.7, openAiChatOptions.getTemperature());
            assertEquals(0.9, openAiChatOptions.getTopP());
            assertEquals("user", openAiChatOptions.getUser());
            assertEquals("low", openAiChatOptions.getReasoningEffort());
            assertEquals("low", openAiChatOptions.getVerbosity());
            assertEquals(Boolean.TRUE, openAiChatOptions.getStore());
        }
    }
}
