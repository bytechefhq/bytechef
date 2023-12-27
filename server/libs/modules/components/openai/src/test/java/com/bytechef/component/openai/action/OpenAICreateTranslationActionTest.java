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

import static com.bytechef.component.openai.constant.OpenAIConstants.FILE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.hermes.component.definition.ActionContext.FileEntry;
import com.theokanning.openai.audio.CreateTranslationRequest;
import com.theokanning.openai.audio.TranslationResult;
import com.theokanning.openai.service.OpenAiService;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
public class OpenAICreateTranslationActionTest extends AbstractOpenAIActionTest {

    @Test
    public void testPerform() {
        TranslationResult mockedTranslationResult = mock(TranslationResult.class);
        File mockedFile = mock(File.class);
        FileEntry mockedFileEntry = mock(FileEntry.class);

        ArgumentCaptor<CreateTranslationRequest> createTranslationRequestArgumentCaptor = ArgumentCaptor.forClass(
            CreateTranslationRequest.class);
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);

        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn("MODEL");
        when(mockedParameters.getString(PROMPT))
            .thenReturn("PROMPT");
        when(mockedParameters.getString(RESPONSE_FORMAT))
            .thenReturn("RESPONSE_FORMAT");
        when(mockedParameters.getDouble(TEMPERATURE))
            .thenReturn(0.0);
        when(mockedParameters.getRequiredFileEntry(FILE))
            .thenReturn(mockedFileEntry);
        when(mockedContext.file(any()))
            .thenReturn(mockedFile);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction = Mockito.mockConstruction(
            OpenAiService.class,
            (mock, context) -> when(
                mock.createTranslation(createTranslationRequestArgumentCaptor.capture(), fileArgumentCaptor.capture()))
                    .thenReturn(mockedTranslationResult))) {

            TranslationResult translationResult =
                OpenAICreateTranslationAction.perform(mockedParameters, mockedParameters, mockedContext);

            List<OpenAiService> openAiServices = openAiServiceMockedConstruction.constructed();

            assertEquals(1, openAiServices.size());
            assertEquals(mockedTranslationResult, translationResult);

            OpenAiService openAiService = openAiServices.getFirst();

            verify(openAiService, times(1)).createTranslation(
                createTranslationRequestArgumentCaptor.capture(), fileArgumentCaptor.capture());

            CreateTranslationRequest createTranslationRequest = createTranslationRequestArgumentCaptor.getValue();

            assertEquals("MODEL", createTranslationRequest.getModel());
            assertEquals("PROMPT", createTranslationRequest.getPrompt());
            assertEquals("RESPONSE_FORMAT", createTranslationRequest.getResponseFormat());
            assertEquals(0.0, createTranslationRequest.getTemperature());
        }
    }
}
