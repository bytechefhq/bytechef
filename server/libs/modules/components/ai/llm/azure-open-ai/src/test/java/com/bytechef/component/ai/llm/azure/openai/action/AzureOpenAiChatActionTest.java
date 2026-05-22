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

package com.bytechef.component.ai.llm.azure.openai.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ENDPOINT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * @author Nikolina Spehar
 */
class AzureOpenAiChatActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(TOKEN, "TOKEN", ENDPOINT, "https://my-resource.openai.azure.com"));
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.ofEntries(
            Map.entry(RESPONSE, Map.of(RESPONSE_FORMAT, ChatModel.ResponseFormat.TEXT.name())),
            Map.entry(MODEL, "gpt-4o"),
            Map.entry(FREQUENCY_PENALTY, 0.5),
            Map.entry(LOGIT_BIAS, Map.of()),
            Map.entry(MAX_TOKENS, 100),
            Map.entry(PRESENCE_PENALTY, 0.5),
            Map.entry(STOP, List.of("stop")),
            Map.entry(TEMPERATURE, 0.7),
            Map.entry(TOP_P, 0.9),
            Map.entry(USER, "user")));

    @Test
    void testCreateChatModelWithResponseFormat() {
        org.springframework.ai.chat.model.ChatModel chatModel = AzureOpenAiChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, true);

        assertNotNull(chatModel);
        assertInstanceOf(OpenAiChatModel.class, chatModel);

        OpenAiChatModel openAiChatModel = (OpenAiChatModel) chatModel;
        OpenAiChatOptions openAiChatOptions = openAiChatModel.getOptions();

        assertEquals("https://my-resource.openai.azure.com", openAiChatOptions.getBaseUrl());
        assertEquals("TOKEN", openAiChatOptions.getApiKey());
        assertTrue(openAiChatOptions.isMicrosoftFoundry());
        assertEquals("gpt-4o", openAiChatOptions.getModel());
        assertEquals("gpt-4o", openAiChatOptions.getDeploymentName());
        assertEquals(0.5, openAiChatOptions.getFrequencyPenalty());
        assertEquals(Map.of(), openAiChatOptions.getLogitBias());
        assertEquals(100, openAiChatOptions.getMaxTokens());
        assertEquals(0.5, openAiChatOptions.getPresencePenalty());

        ResponseFormat responseFormat = openAiChatOptions.getResponseFormat();

        assertNotNull(responseFormat);
        assertEquals(ResponseFormat.Type.TEXT, responseFormat.getType());
        assertEquals(List.of("stop"), openAiChatOptions.getStop());
        assertEquals(0.7, openAiChatOptions.getTemperature());
        assertEquals(0.9, openAiChatOptions.getTopP());
        assertEquals("user", openAiChatOptions.getUser());
    }

    @Test
    void testCreateChatModelWithoutResponseFormat() {
        org.springframework.ai.chat.model.ChatModel chatModel = AzureOpenAiChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, false);

        assertNotNull(chatModel);
        assertInstanceOf(OpenAiChatModel.class, chatModel);

        OpenAiChatOptions openAiChatOptions = ((OpenAiChatModel) chatModel).getOptions();

        assertNotNull(openAiChatOptions);
        assertEquals("https://my-resource.openai.azure.com", openAiChatOptions.getBaseUrl());
        assertEquals("TOKEN", openAiChatOptions.getApiKey());
        assertTrue(openAiChatOptions.isMicrosoftFoundry());
        assertEquals("gpt-4o", openAiChatOptions.getModel());
        assertEquals("gpt-4o", openAiChatOptions.getDeploymentName());
        assertEquals(0.5, openAiChatOptions.getFrequencyPenalty());
        assertEquals(Map.of(), openAiChatOptions.getLogitBias());
        assertEquals(100, openAiChatOptions.getMaxTokens());
        assertEquals(0.5, openAiChatOptions.getPresencePenalty());
        assertNull(openAiChatOptions.getResponseFormat());
        assertEquals(List.of("stop"), openAiChatOptions.getStop());
        assertEquals(0.7, openAiChatOptions.getTemperature());
        assertEquals(0.9, openAiChatOptions.getTopP());
        assertEquals("user", openAiChatOptions.getUser());
    }
}
