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

import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.N;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.QUALITY;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.SIZE;
import static com.bytechef.component.openai.constant.OpenAIConstants.STYLE;
import static com.bytechef.component.openai.constant.OpenAIConstants.USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

/**
 * @author Monika Domiter
 */
public class OpenAICreateImageActionTest extends AbstractOpenAIActionTest {

    @Test
    public void testPerform() {
        ImageResult mockedImageResult = mock(ImageResult.class);
        ArgumentCaptor<CreateImageRequest> createImageRequestArgumentCaptor = ArgumentCaptor.forClass(
            CreateImageRequest.class);

        when(mockedParameters.getRequiredString(PROMPT))
            .thenReturn("PROMPT");
        when(mockedParameters.getString(MODEL))
            .thenReturn("MODEL");
        when(mockedParameters.getInteger(N))
            .thenReturn(1);
        when(mockedParameters.getString(QUALITY))
            .thenReturn("QUALITY");
        when(mockedParameters.getString(RESPONSE_FORMAT))
            .thenReturn("RESPONSE_FORMAT");
        when(mockedParameters.getString(SIZE))
            .thenReturn("SIZE");
        when(mockedParameters.getString(STYLE))
            .thenReturn("STYLE");
        when(mockedParameters.getString(USER))
            .thenReturn("USER");

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction = mockConstruction(
            OpenAiService.class,
            (openAiService, context) -> when(openAiService.createImage(any()))
                .thenReturn(mockedImageResult))) {

            ImageResult imageResult = OpenAICreateImageAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            List<OpenAiService> openAiServices = openAiServiceMockedConstruction.constructed();

            assertEquals(1, openAiServices.size());
            assertEquals(mockedImageResult, imageResult);

            OpenAiService openAiService = openAiServices.getFirst();

            verify(openAiService, times(1)).createImage(createImageRequestArgumentCaptor.capture());

            CreateImageRequest createImageRequest = createImageRequestArgumentCaptor.getValue();

            assertEquals("PROMPT", createImageRequest.getPrompt());
            assertEquals("MODEL", createImageRequest.getModel());
            assertEquals(1, createImageRequest.getN());
            assertEquals("QUALITY", createImageRequest.getQuality());
            assertEquals("RESPONSE_FORMAT", createImageRequest.getResponseFormat());
            assertEquals("SIZE", createImageRequest.getSize());
            assertEquals("STYLE", createImageRequest.getStyle());
            assertEquals("USER", createImageRequest.getUser());
        }
    }
}
