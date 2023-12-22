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

import static com.bytechef.component.openai.constant.OpenAIConstants.INPUT;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.SPEED;
import static com.bytechef.component.openai.constant.OpenAIConstants.VOICE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.hermes.component.definition.ActionContext;
import com.theokanning.openai.audio.CreateSpeechRequest;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
 class OpenAICreateSpeechActionTest extends AbstractOpenAIActionTest {

    @Test
     void testPerform() {
        ResponseBody responseBody = Mockito.mock(ResponseBody.class);
        ActionContext.FileEntry fileEntry = Mockito.mock(ActionContext.FileEntry.class);

        ArgumentCaptor<CreateSpeechRequest> createSpeechRequestArgumentCaptor = ArgumentCaptor.forClass(
            CreateSpeechRequest.class);

        Mockito.when(parameterMap.getRequiredString(MODEL))
            .thenReturn("MODEL");
        Mockito.when(parameterMap.getRequiredString(INPUT))
            .thenReturn("INPUT");
        Mockito.when(parameterMap.getRequiredString(VOICE))
            .thenReturn("VOICE");
        Mockito.when(parameterMap.getString(RESPONSE_FORMAT))
            .thenReturn("RESPONSE_FORMAT");
        Mockito.when(parameterMap.getDouble(SPEED))
            .thenReturn(1.0);
        Mockito.when(context.file(any()))
            .thenReturn(fileEntry);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction =
            Mockito.mockConstruction(OpenAiService.class,
                (mock, context) -> when(mock.createSpeech(createSpeechRequestArgumentCaptor.capture()))
                    .thenReturn(responseBody))) {

            ActionContext.FileEntry perform = OpenAICreateSpeechAction.perform(parameterMap, parameterMap, context);

            Assertions.assertEquals(1, openAiServiceMockedConstruction.constructed()
                .size());
            Assertions.assertEquals(fileEntry, perform);

            OpenAiService mock = openAiServiceMockedConstruction.constructed()
                .getFirst();
            verify(mock).createSpeech(createSpeechRequestArgumentCaptor.capture());
            verify(mock, times(1)).createSpeech(createSpeechRequestArgumentCaptor.capture());

            Assertions.assertEquals("MODEL", createSpeechRequestArgumentCaptor.getValue()
                .getModel());
            Assertions.assertEquals("INPUT", createSpeechRequestArgumentCaptor.getValue()
                .getInput());
            Assertions.assertEquals("VOICE", createSpeechRequestArgumentCaptor.getValue()
                .getVoice());
            Assertions.assertEquals("RESPONSE_FORMAT", createSpeechRequestArgumentCaptor.getValue()
                .getResponseFormat());
            Assertions.assertEquals(1.0, createSpeechRequestArgumentCaptor.getValue()
                .getSpeed());

        }
    }
}
