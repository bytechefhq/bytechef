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

import static com.bytechef.component.ai.llm.constant.LLMConstants.INPUT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SPEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VOICE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechMessage;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions.AudioResponseFormat;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions.Voice;

/**
 * @author Nikolina Spehar
 */
class OpenAiCreateSpeechActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "test-api-key"));
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(MODEL, "tts-1", INPUT, "Hello world", VOICE, Voice.ALLOY.name(),
            RESPONSE_FORMAT, AudioResponseFormat.MP3.name(), SPEED, 1.0));
    private final TextToSpeechResponse mockedResponse = mock(TextToSpeechResponse.class);
    private final Speech mockedSpeech = mock(Speech.class);
    private final OpenAiAudioSpeechModel mockedSpeechModel = mock(OpenAiAudioSpeechModel.class);
    private final ArgumentCaptor<OpenAiAudioSpeechOptions> optionsCaptor = forClass(OpenAiAudioSpeechOptions.class);
    private final ArgumentCaptor<TextToSpeechPrompt> promptArgumentCaptor = forClass(TextToSpeechPrompt.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<OpenAIClient> openAIClientArgumentCaptor = forClass(OpenAIClient.class);

    @Test
    void testPerform() {
        when(mockedSpeech.getOutput())
            .thenReturn(new byte[] {
                1, 2, 3
            });
        when(mockedResponse.getResult())
            .thenReturn(mockedSpeech);
        when(mockedSpeechModel.call(promptArgumentCaptor.capture()))
            .thenReturn(mockedResponse);
        when(mockedContext.file(any()))
            .thenReturn(mockedFileEntry);

        try (MockedStatic<OpenAIOkHttpClient> openAIOkHttpClientMockedStatic = mockStatic(OpenAIOkHttpClient.class);
            MockedStatic<OpenAiAudioSpeechModel> openAiAudioSpeechModelMockedStatic =
                mockStatic(OpenAiAudioSpeechModel.class)) {

            OpenAIOkHttpClient.Builder mockedClientBuilder = mock(OpenAIOkHttpClient.Builder.class);

            openAIOkHttpClientMockedStatic.when(OpenAIOkHttpClient::builder)
                .thenReturn(mockedClientBuilder);
            when(mockedClientBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedClientBuilder);
            OpenAIClient mockedOpenAIClient = mock(OpenAIClient.class);
            when(mockedClientBuilder.build())
                .thenReturn(mockedOpenAIClient);

            OpenAiAudioSpeechModel.Builder mockedSpeechModelBuilder = mock(OpenAiAudioSpeechModel.Builder.class);

            openAiAudioSpeechModelMockedStatic.when(OpenAiAudioSpeechModel::builder)
                .thenReturn(mockedSpeechModelBuilder);
            when(mockedSpeechModelBuilder.openAiClient(openAIClientArgumentCaptor.capture()))
                .thenReturn(mockedSpeechModelBuilder);
            when(mockedSpeechModelBuilder.defaultOptions(optionsCaptor.capture()))
                .thenReturn(mockedSpeechModelBuilder);
            when(mockedSpeechModelBuilder.build())
                .thenReturn(mockedSpeechModel);

            FileEntry result = OpenAiCreateSpeechAction.perform(
                mockedInputParameters, mockedConnectionParameters, mockedContext);

            assertEquals(mockedFileEntry, result);
            assertEquals(mockedOpenAIClient, openAIClientArgumentCaptor.getValue());
            assertEquals("test-api-key", stringArgumentCaptor.getValue());

            OpenAiAudioSpeechOptions capturedOptions = optionsCaptor.getValue();

            assertEquals("tts-1", capturedOptions.getModel());
            assertEquals("Hello world", capturedOptions.getInput());
            assertEquals(Voice.ALLOY.getValue(), capturedOptions.getVoice());
            assertEquals(AudioResponseFormat.MP3.getValue(), capturedOptions.getResponseFormat());
            assertEquals(1.0f, capturedOptions.getSpeed());

            TextToSpeechPrompt textToSpeechPrompt = promptArgumentCaptor.getValue();
            TextToSpeechMessage textToSpeechMessage = textToSpeechPrompt.getInstructions();

            assertEquals("Hello world", textToSpeechMessage.getText());
        }
    }
}
