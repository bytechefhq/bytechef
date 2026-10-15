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

package com.bytechef.component.ai.llm.router.requesty.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.util.ModelUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

/**
 * @author Thibault Jaigu
 */
class RequestyImageModelTest {

    private final RestClient.Builder mockedBuilder = mock(RestClient.Builder.class);
    private final RequestBodySpec mockedRequestBodySpec = mock(RequestBodySpec.class);
    private final RequestBodyUriSpec mockedRequestBodyUriSpec = mock(RequestBodyUriSpec.class);
    private final ResponseSpec mockedResponseSpec = mock(ResponseSpec.class);
    private final RestClient mockedRestClient = mock(RestClient.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCall() {
        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = Mockito.mockStatic(ModelUtils.class)) {
            mockRestClient(modelUtilsMockedStatic, Map.of("data", List.of(Map.of("b64_json", "aW1hZ2U="))));

            ImageResponse imageResponse = buildImageModel().call(new ImagePrompt("A red circle"));

            List<ImageGeneration> imageGenerations = imageResponse.getResults();

            assertEquals(1, imageGenerations.size());

            ImageGeneration imageGeneration = imageGenerations.getFirst();
            Image image = imageGeneration.getOutput();

            assertEquals("aW1hZ2U=", image.getB64Json());
            assertNull(image.getUrl());
            assertEquals(
                List.of("https://router.requesty.ai/v1", "Authorization", "Bearer token", "/images/generations"),
                stringArgumentCaptor.getAllValues());
            assertEquals(
                Map.of(
                    "model", "openai/gpt-image-2", "prompt", "A red circle", "quality", "low", "size", "1024x1024",
                    "user", "user"),
                objectArgumentCaptor.getValue());
        }
    }

    @Test
    void testCallWithEmptyResponse() {
        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = Mockito.mockStatic(ModelUtils.class)) {
            mockRestClient(modelUtilsMockedStatic, null);

            RequestyImageModel requestyImageModel = buildImageModel();
            ImagePrompt imagePrompt = new ImagePrompt("A red circle");

            assertThrows(IllegalStateException.class, () -> requestyImageModel.call(imagePrompt));
        }
    }

    private static RequestyImageModel buildImageModel() {
        return RequestyImageModel.builder()
            .apiKey("token")
            .model("openai/gpt-image-2")
            .quality("low")
            .size("1024x1024")
            .user("user")
            .build();
    }

    private void mockRestClient(MockedStatic<ModelUtils> modelUtilsMockedStatic, Map<String, Object> responseBody) {
        modelUtilsMockedStatic.when(ModelUtils::getRestClientBuilder)
            .thenReturn(mockedBuilder);

        when(mockedBuilder.baseUrl(stringArgumentCaptor.capture()))
            .thenReturn(mockedBuilder);
        when(mockedBuilder.defaultHeader(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedBuilder);
        when(mockedBuilder.build())
            .thenReturn(mockedRestClient);

        when(mockedRestClient.post())
            .thenReturn(mockedRequestBodyUriSpec);
        when(mockedRequestBodyUriSpec.uri(stringArgumentCaptor.capture()))
            .thenReturn(mockedRequestBodySpec);
        when(mockedRequestBodySpec.contentType(any()))
            .thenReturn(mockedRequestBodySpec);
        when(mockedRequestBodySpec.body(objectArgumentCaptor.capture()))
            .thenReturn(mockedRequestBodySpec);
        when(mockedRequestBodySpec.retrieve())
            .thenReturn(mockedResponseSpec);
        when(mockedResponseSpec.body(any(ParameterizedTypeReference.class)))
            .thenReturn(responseBody);
    }
}
