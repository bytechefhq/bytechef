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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.theokanning.openai.image.CreateImageRequest;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
public class OpenAICreateImageActionTest extends AbstractOpenAIActionTest {

    @Test
    public void testPerform() {
        ImageResult imageResult = Mockito.mock(ImageResult.class);

        ArgumentCaptor<CreateImageRequest> createImageRequestArgumentCaptor = ArgumentCaptor.forClass(
            CreateImageRequest.class);

        Mockito.when(parameterMap.getRequiredString(PROMPT))
            .thenReturn("PROMPT");
        Mockito.when(parameterMap.getString(MODEL))
            .thenReturn("MODEL");
        Mockito.when(parameterMap.getInteger(N))
            .thenReturn(1);
        Mockito.when(parameterMap.getString(QUALITY))
            .thenReturn("QUALITY");
        Mockito.when(parameterMap.getString(RESPONSE_FORMAT))
            .thenReturn("RESPONSE_FORMAT");
        Mockito.when(parameterMap.getString(SIZE))
            .thenReturn("SIZE");
        Mockito.when(parameterMap.getString(STYLE))
            .thenReturn("STYLE");
        Mockito.when(parameterMap.getString(USER))
            .thenReturn("USER");

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction =
            Mockito.mockConstruction(OpenAiService.class,
                (mock, context) -> when(mock.createImage(createImageRequestArgumentCaptor.capture()))
                    .thenReturn(imageResult))) {

            ImageResult perform = OpenAICreateImageAction.perform(parameterMap, parameterMap, context);

            Assertions.assertEquals(1, openAiServiceMockedConstruction.constructed()
                .size());
            Assertions.assertEquals(imageResult, perform);

            OpenAiService mock = openAiServiceMockedConstruction.constructed()
                .get(0);
            verify(mock).createImage(createImageRequestArgumentCaptor.capture());
            verify(mock, times(1)).createImage(createImageRequestArgumentCaptor.capture());

            Assertions.assertEquals("PROMPT", createImageRequestArgumentCaptor.getValue()
                .getPrompt());
            Assertions.assertEquals("MODEL", createImageRequestArgumentCaptor.getValue()
                .getModel());
            Assertions.assertEquals(1, createImageRequestArgumentCaptor.getValue()
                .getN());
            Assertions.assertEquals("QUALITY", createImageRequestArgumentCaptor.getValue()
                .getQuality());
            Assertions.assertEquals("RESPONSE_FORMAT", createImageRequestArgumentCaptor.getValue()
                .getResponseFormat());
            Assertions.assertEquals("SIZE", createImageRequestArgumentCaptor.getValue()
                .getSize());
            Assertions.assertEquals("STYLE", createImageRequestArgumentCaptor.getValue()
                .getStyle());
            Assertions.assertEquals("USER", createImageRequestArgumentCaptor.getValue()
                .getUser());
        }

    }
}
