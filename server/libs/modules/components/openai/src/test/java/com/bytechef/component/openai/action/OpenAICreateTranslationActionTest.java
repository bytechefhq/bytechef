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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.hermes.component.definition.ActionContext;
import com.theokanning.openai.audio.CreateTranslationRequest;
import com.theokanning.openai.audio.TranslationResult;
import com.theokanning.openai.service.OpenAiService;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
 class OpenAICreateTranslationActionTest extends AbstractOpenAIActionTest {

    @Test
     void testPerform() {
        TranslationResult translationResult = Mockito.mock(TranslationResult.class);
        File file = Mockito.mock(File.class);
        ActionContext.FileEntry fileEntry = Mockito.mock(ActionContext.FileEntry.class);

        ArgumentCaptor<CreateTranslationRequest> createTranslationRequestArgumentCaptor =
            ArgumentCaptor.forClass(CreateTranslationRequest.class);
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);

        Mockito.when(parameterMap.getRequiredString(MODEL))
            .thenReturn("MODEL");
        Mockito.when(parameterMap.getString(PROMPT))
            .thenReturn("PROMPT");
        Mockito.when(parameterMap.getString(RESPONSE_FORMAT))
            .thenReturn("RESPONSE_FORMAT");
        Mockito.when(parameterMap.getDouble(TEMPERATURE))
            .thenReturn(0.0);
        Mockito.when(parameterMap.getRequiredFileEntry(FILE))
            .thenReturn(fileEntry);
        Mockito.when(context.file(any()))
            .thenReturn(file);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction =
            Mockito.mockConstruction(OpenAiService.class, (mock, context) -> when(
                mock.createTranslation(createTranslationRequestArgumentCaptor.capture(), fileArgumentCaptor.capture()))
                    .thenReturn(translationResult))) {

            TranslationResult translationResult1 =
                OpenAICreateTranslationAction.perform(parameterMap, parameterMap, context);

            Assertions.assertEquals(1, openAiServiceMockedConstruction.constructed()
                .size());
            Assertions.assertEquals(translationResult, translationResult1);

            OpenAiService mock = openAiServiceMockedConstruction.constructed()
                .getFirst();

            verify(mock).createTranslation(createTranslationRequestArgumentCaptor.capture(),
                fileArgumentCaptor.capture());
            verify(mock, times(1)).createTranslation(createTranslationRequestArgumentCaptor.capture(),
                fileArgumentCaptor.capture());

            Assertions.assertEquals("MODEL", createTranslationRequestArgumentCaptor.getValue()
                .getModel());
            Assertions.assertEquals("PROMPT", createTranslationRequestArgumentCaptor.getValue()
                .getPrompt());
            Assertions.assertEquals("RESPONSE_FORMAT", createTranslationRequestArgumentCaptor.getValue()
                .getResponseFormat());
            Assertions.assertEquals(0.0, createTranslationRequestArgumentCaptor.getValue()
                .getTemperature());
        }

    }
}
