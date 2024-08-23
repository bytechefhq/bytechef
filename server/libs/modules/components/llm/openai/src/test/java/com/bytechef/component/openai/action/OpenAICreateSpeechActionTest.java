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

import static com.bytechef.component.llm.constants.LLMConstants.INPUT;
import static com.bytechef.component.llm.constants.LLMConstants.MODEL;
import static com.bytechef.component.llm.constants.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.llm.constants.LLMConstants.SPEED;
import static com.bytechef.component.llm.constants.LLMConstants.VOICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.llm.action.AbstractLLMActionTest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.Speech;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;

class OpenAICreateSpeechActionTest extends AbstractLLMActionTest {
    private final FileEntry answer = new FileEntry() {
        @Override
        public String getExtension() {
            return ".extension";
        }

        @Override
        public String getMimeType() {
            return "mimeType";
        }

        @Override
        public String getName() {
            return "Name";
        }

        @Override
        public String getUrl() {
            return "http://testUrl";
        }
    };

    @Test
    public void testPerform() {
        when(mockedParameters.getRequiredString(INPUT)).thenReturn("Input");
        when(mockedParameters.get(RESPONSE_FORMAT, OpenAiAudioApi.SpeechRequest.AudioResponseFormat.class))
            .thenReturn(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3);
        when(mockedParameters.getRequiredString(MODEL)).thenReturn("Model");
        when(mockedParameters.get(VOICE, OpenAiAudioApi.SpeechRequest.Voice.class))
            .thenReturn(OpenAiAudioApi.SpeechRequest.Voice.ECHO);
        when(mockedParameters.getFloat(SPEED)).thenReturn(1f);


        try (MockedConstruction<OpenAiAudioSpeechModel> ignored = Mockito.mockConstruction(OpenAiAudioSpeechModel.class, (mock, context) -> {
            SpeechResponse mockedSpeechResponse = mock(SpeechResponse.class);
            when(mock.call(any(SpeechPrompt.class))).thenReturn(mockedSpeechResponse);
            when(mockedSpeechResponse.getResult()).thenReturn(new Speech(new byte[0]));
        })) {

            when(mockedContext.file(any())).thenReturn(answer);

            FileEntry response = OpenAICreateSpeechAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(answer, response);
        }
    }
}
