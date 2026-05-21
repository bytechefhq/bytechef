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
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SIZE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STYLE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.component.ai.llm.ImageModel.ResponseFormat;
import com.bytechef.component.ai.llm.ImageModel.Style;
import com.bytechef.component.ai.llm.azure.openai.definition.Size;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;

/**
 * @author Nikolina Spehar
 */
class AzureOpenAiCreateImageActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(
            TOKEN, "TOKEN",
            ENDPOINT, "https://my-resource.openai.azure.com"));
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(
            MODEL, "dall-e-3",
            SIZE, Size._1024x1024,
            N, 1,
            RESPONSE_FORMAT, ResponseFormat.URL,
            STYLE, Style.VIVID,
            USER, "user"));

    @Test
    void testCreateImageModel() {
        org.springframework.ai.image.ImageModel imageModel = AzureOpenAiCreateImageAction.IMAGE_MODEL.createImageModel(
            mockedInputParameters, mockedConnectionParameters);

        assertNotNull(imageModel);
        assertInstanceOf(OpenAiImageModel.class, imageModel);

        OpenAiImageOptions openAiImageOptions = ((OpenAiImageModel) imageModel).getOptions();

        assertEquals("https://my-resource.openai.azure.com", openAiImageOptions.getBaseUrl());
        assertEquals("TOKEN", openAiImageOptions.getApiKey());
        assertEquals("dall-e-3", openAiImageOptions.getDeploymentName());
        assertEquals("dall-e-3", openAiImageOptions.getModel());
        assertEquals(Size._1024x1024.getDimensions()[0], openAiImageOptions.getWidth());
        assertEquals(Size._1024x1024.getDimensions()[1], openAiImageOptions.getHeight());
        assertEquals(1, openAiImageOptions.getN());
        assertEquals(ResponseFormat.URL.getValue(), openAiImageOptions.getResponseFormat());
        assertEquals(Style.VIVID.getValue(), openAiImageOptions.getStyle());
        assertEquals("user", openAiImageOptions.getUser());
    }
}
