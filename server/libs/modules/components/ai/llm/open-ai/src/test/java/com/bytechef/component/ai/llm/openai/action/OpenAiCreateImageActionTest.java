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

import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SIZE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STYLE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.ai.llm.openai.constant.OpenAiConstants.QUALITY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.ImageModel.Quality;
import com.bytechef.component.ai.llm.ImageModel.ResponseFormat;
import com.bytechef.component.ai.llm.ImageModel.Style;
import com.bytechef.component.ai.llm.openai.definition.Size;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;

/**
 * @author Nikolina Spehar
 */
class OpenAiCreateImageActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreateImageModelWithGptModel() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "gpt-image-1", SIZE, Size._1024x1024, N, 1, USER, "user"));

        try (MockedStatic<OpenAIOkHttpClient> openAIOkHttpClientMockedStatic = mockStatic(OpenAIOkHttpClient.class)) {
            OpenAIOkHttpClient.Builder mockedOpenAIOkHttpClientBuilder = mock(OpenAIOkHttpClient.Builder.class);

            openAIOkHttpClientMockedStatic.when(OpenAIOkHttpClient::builder)
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.build())
                .thenReturn(mock(OpenAIClient.class));

            org.springframework.ai.image.ImageModel imageModel = OpenAiCreateImageAction.IMAGE_MODEL.createImageModel(
                mockedInputParameters, mockedConnectionParameters);

            assertNotNull(imageModel);
            assertInstanceOf(OpenAiImageModel.class, imageModel);
            assertEquals("TOKEN", stringArgumentCaptor.getValue());

            OpenAiImageOptions openAiImageOptions = ((OpenAiImageModel) imageModel).getOptions();

            assertEquals("gpt-image-1", openAiImageOptions.getModel());
            assertEquals(Size._1024x1024.getDimensions()[0], openAiImageOptions.getWidth());
            assertEquals(Size._1024x1024.getDimensions()[1], openAiImageOptions.getHeight());
            assertEquals(1, openAiImageOptions.getN());
            assertEquals("user", openAiImageOptions.getUser());

            assertNull(openAiImageOptions.getResponseFormat());
            assertNull(openAiImageOptions.getStyle());
            assertNull(openAiImageOptions.getQuality());
        }
    }

    @Test
    void testCreateImageModelWithDallE3() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "dall-e-3", SIZE, Size._1024x1024, N, 1, USER, "user",
                RESPONSE_FORMAT, ResponseFormat.URL, STYLE, Style.VIVID, QUALITY, Quality.HD));

        try (MockedStatic<OpenAIOkHttpClient> openAIOkHttpClientMockedStatic = mockStatic(OpenAIOkHttpClient.class)) {
            OpenAIOkHttpClient.Builder mockedOpenAIOkHttpClientBuilder = mock(OpenAIOkHttpClient.Builder.class);

            openAIOkHttpClientMockedStatic.when(OpenAIOkHttpClient::builder)
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.build())
                .thenReturn(mock(OpenAIClient.class));

            org.springframework.ai.image.ImageModel imageModel = OpenAiCreateImageAction.IMAGE_MODEL.createImageModel(
                mockedInputParameters, mockedConnectionParameters);

            assertNotNull(imageModel);
            assertInstanceOf(OpenAiImageModel.class, imageModel);

            OpenAiImageOptions openAiImageOptions = ((OpenAiImageModel) imageModel).getOptions();

            assertEquals("dall-e-3", openAiImageOptions.getModel());
            assertEquals(Size._1024x1024.getDimensions()[0], openAiImageOptions.getWidth());
            assertEquals(Size._1024x1024.getDimensions()[1], openAiImageOptions.getHeight());
            assertEquals(1, openAiImageOptions.getN());
            assertEquals("user", openAiImageOptions.getUser());
            assertEquals(ResponseFormat.URL.getValue(), openAiImageOptions.getResponseFormat());
            assertEquals(Style.VIVID.getValue(), openAiImageOptions.getStyle());
            assertEquals(Quality.HD.getValue(), openAiImageOptions.getQuality());
        }
    }

    @Test
    void testCreateImageModelWithDallE2() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "dall-e-2", SIZE, Size.DALL_E_2_256x256, N, 1, USER, "user",
                RESPONSE_FORMAT, ResponseFormat.B64_JSON));

        try (MockedStatic<OpenAIOkHttpClient> openAIOkHttpClientMockedStatic = mockStatic(OpenAIOkHttpClient.class)) {
            OpenAIOkHttpClient.Builder mockedOpenAIOkHttpClientBuilder = mock(OpenAIOkHttpClient.Builder.class);

            openAIOkHttpClientMockedStatic.when(OpenAIOkHttpClient::builder)
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.build())
                .thenReturn(mock(OpenAIClient.class));

            org.springframework.ai.image.ImageModel imageModel = OpenAiCreateImageAction.IMAGE_MODEL.createImageModel(
                mockedInputParameters, mockedConnectionParameters);

            assertNotNull(imageModel);
            assertInstanceOf(OpenAiImageModel.class, imageModel);

            OpenAiImageOptions openAiImageOptions = ((OpenAiImageModel) imageModel).getOptions();

            assertEquals("dall-e-2", openAiImageOptions.getModel());
            assertEquals(Size.DALL_E_2_256x256.getDimensions()[0], openAiImageOptions.getWidth());
            assertEquals(Size.DALL_E_2_256x256.getDimensions()[1], openAiImageOptions.getHeight());
            assertEquals(1, openAiImageOptions.getN());
            assertEquals("user", openAiImageOptions.getUser());
            assertEquals(ResponseFormat.B64_JSON.getValue(), openAiImageOptions.getResponseFormat());
            assertNull(openAiImageOptions.getStyle());
            assertNull(openAiImageOptions.getQuality());
        }
    }
}
