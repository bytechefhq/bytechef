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

package com.bytechef.component.llm;

import static com.bytechef.component.llm.constant.LLMConstants.MESSAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

/**
 * @author Marko Kriskovic
 */
public class ImageActionTest extends AbstractActionTest {

    private static final org.springframework.ai.image.Image ANSWER =
        new org.springframework.ai.image.Image("url", "b64JSON");

    @Test
    public void testGetResponse() {
        when(mockedParameters.getList(eq(MESSAGES), any(TypeReference.class)))
            .thenReturn(List.of(new ImageMessage("PROMPT", 1f)));

        Image mockedImage = spy(new MockImage());
        ImageModel mockedImageModel = mock(ImageModel.class);

        when(mockedImage.createImageModel(mockedParameters, mockedParameters)).thenReturn(mockedImageModel);

        ImageResponse imageResponse = new ImageResponse(List.of(new ImageGeneration(ANSWER)));

        ImageResponse mockedImageResponse = spy(imageResponse);

        when(mockedImageModel.call(any(ImagePrompt.class))).thenReturn(mockedImageResponse);

        org.springframework.ai.image.Image response = (org.springframework.ai.image.Image) mockedImage.getResponse(
            mockedParameters, mockedParameters);

        assertEquals(ANSWER, response);
    }

    private static class MockImage implements Image {

        @Override
        public ImageModel createImageModel(Parameters inputParameters, Parameters connectionParameters) {
            return null;
        }
    }
}
