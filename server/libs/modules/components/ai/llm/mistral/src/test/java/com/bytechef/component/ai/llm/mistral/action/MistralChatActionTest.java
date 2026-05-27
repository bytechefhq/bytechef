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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.mistralai.api.MistralAiApi.ChatCompletionRequest.ResponseFormat;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

/**
 * @author Nikolina Spehar
 */
class MistralChatActionTest {

    private final RestClient.Builder mockRestClientBuilder = mock(RestClient.Builder.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<RestClient.Builder> restClientBuilderArgumentCaptor =
        forClass(RestClient.Builder.class);
    private final ArgumentCaptor<ResponseErrorHandler> responseErrorHandlerArgumentCaptor =
        forClass(ResponseErrorHandler.class);
    private MockedStatic<MistralAiApi> mistralAiApiMockedStatic;
    private MockedStatic<ModelUtils> modelUtilsMockedStatic;

    @BeforeEach
    void beforeEach() {
        mistralAiApiMockedStatic = mockStatic(MistralAiApi.class);
        modelUtilsMockedStatic = mockStatic(ModelUtils.class);

        modelUtilsMockedStatic.when(ModelUtils::getRestClientBuilder)
            .thenReturn(mockRestClientBuilder);

        MistralAiApi.Builder mockedApiBuilder = mock(MistralAiApi.Builder.class);
        MistralAiApi mockedApi = mock(MistralAiApi.class);

        mistralAiApiMockedStatic.when(MistralAiApi::builder)
            .thenReturn(mockedApiBuilder);

        when(mockedApiBuilder.baseUrl(stringArgumentCaptor.capture()))
            .thenReturn(mockedApiBuilder);
        when(mockedApiBuilder.apiKey(stringArgumentCaptor.capture()))
            .thenReturn(mockedApiBuilder);
        when(mockedApiBuilder.restClientBuilder(restClientBuilderArgumentCaptor.capture()))
            .thenReturn(mockedApiBuilder);
        when(mockedApiBuilder.responseErrorHandler(responseErrorHandlerArgumentCaptor.capture()))
            .thenReturn(mockedApiBuilder);
        when(mockedApiBuilder.build())
            .thenReturn(mockedApi);
    }

    @AfterEach
    void afterEach() {
        assertEquals(List.of("https://api.mistral.ai", "TOKEN"), stringArgumentCaptor.getAllValues());
        assertEquals(mockRestClientBuilder, restClientBuilderArgumentCaptor.getValue());
        assertEquals(RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER, responseErrorHandlerArgumentCaptor.getValue());

        mistralAiApiMockedStatic.close();
        modelUtilsMockedStatic.close();
    }

    @Test
    void testCreateChatModelWithResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                RESPONSE, Map.of(RESPONSE_FORMAT, ChatModel.ResponseFormat.TEXT.name()),
                MODEL, "mistral-large-latest", TEMPERATURE, 0.7, MAX_TOKENS, 1000, TOP_P, 0.9,
                STOP, List.of("stop"), SAFE_PROMPT, false, SEED, 42));

        org.springframework.ai.chat.model.ChatModel chatModel = MistralChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, true);

        assertNotNull(chatModel);
        assertInstanceOf(MistralAiChatModel.class, chatModel);

        MistralAiChatOptions mistralAiChatOptions = (MistralAiChatOptions) chatModel.getDefaultOptions();

        assertEquals("mistral-large-latest", mistralAiChatOptions.getModel());
        assertEquals(0.7, mistralAiChatOptions.getTemperature());
        assertEquals(1000, mistralAiChatOptions.getMaxTokens());
        assertEquals(0.9, mistralAiChatOptions.getTopP());
        assertEquals(List.of("stop"), mistralAiChatOptions.getStop());
        assertEquals(FALSE, mistralAiChatOptions.getSafePrompt());
        assertEquals(42, mistralAiChatOptions.getRandomSeed());

        ResponseFormat responseFormat = mistralAiChatOptions.getResponseFormat();

        assertNotNull(responseFormat);
        assertEquals(ResponseFormat.text(), responseFormat);
    }

    @Test
    void testCreateChatModelWithoutResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "mistral-large-latest", TEMPERATURE, 0.7, MAX_TOKENS, 1000, TOP_P, 0.9,
                STOP, List.of("stop"), SAFE_PROMPT, false, SEED, 42));

        org.springframework.ai.chat.model.ChatModel chatModel = MistralChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, false);

        assertNotNull(chatModel);
        assertInstanceOf(MistralAiChatModel.class, chatModel);

        MistralAiChatOptions mistralAiChatOptions = (MistralAiChatOptions) chatModel.getDefaultOptions();

        assertEquals("mistral-large-latest", mistralAiChatOptions.getModel());
        assertEquals(0.7, mistralAiChatOptions.getTemperature());
        assertEquals(1000, mistralAiChatOptions.getMaxTokens());
        assertEquals(0.9, mistralAiChatOptions.getTopP());
        assertEquals(List.of("stop"), mistralAiChatOptions.getStop());
        assertEquals(FALSE, mistralAiChatOptions.getSafePrompt());
        assertEquals(42, mistralAiChatOptions.getRandomSeed());
        assertNull(mistralAiChatOptions.getResponseFormat());
    }
}
