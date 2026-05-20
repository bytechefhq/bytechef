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

import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.definition.Language;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.audio.AudioModel;
import com.openai.models.audio.AudioResponseFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.Model;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;

/**
 * @author Nikolina Spehar
 */
class OpenAiCreateTranscriptionActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.ofEntries(
            Map.entry(RESPONSE_FORMAT, "json"),
            Map.entry(MODEL, AudioModel.GPT_4O_MINI_TRANSCRIBE.asString()),
            Map.entry(LANGUAGE, Language.HR.name()),
            Map.entry(PROMPT, "prompt"),
            Map.entry(TEMPERATURE, 0.0)));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreateAudioTranscriptionModelWithResponseFormat() {
        try (MockedStatic<OpenAIOkHttpClient> openAIOkHttpClientMockedStatic = mockStatic(OpenAIOkHttpClient.class)) {

            OpenAIOkHttpClient.Builder mockedOpenAIOkHttpClientBuilder = mock(OpenAIOkHttpClient.Builder.class);

            openAIOkHttpClientMockedStatic.when(OpenAIOkHttpClient::builder)
                .thenReturn(mockedOpenAIOkHttpClientBuilder);

            when(mockedOpenAIOkHttpClientBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedOpenAIOkHttpClientBuilder);
            when(mockedOpenAIOkHttpClientBuilder.build())
                .thenReturn(mock(OpenAIClient.class));

            Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> audioTranscriptionModel =
                OpenAiCreateTranscriptionAction.AUDIO_TRANSCRIPTION.createAudioTranscriptionModel(
                    mockedInputParameters, mockedConnectionParameters);

            assertNotNull(audioTranscriptionModel);
            assertInstanceOf(OpenAiAudioTranscriptionModel.class, audioTranscriptionModel);

            assertEquals("TOKEN", stringArgumentCaptor.getValue());

            OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel =
                (OpenAiAudioTranscriptionModel) audioTranscriptionModel;
            OpenAiAudioTranscriptionOptions openAiAudioTranscriptionModelOptions = openAiAudioTranscriptionModel
                .getOptions();

            assertEquals(AudioModel.GPT_4O_MINI_TRANSCRIBE.asString(), openAiAudioTranscriptionModelOptions.getModel());
            assertEquals("prompt", openAiAudioTranscriptionModelOptions.getPrompt());
            assertEquals(Language.HR.getCode(), openAiAudioTranscriptionModelOptions.getLanguage());
            assertEquals(AudioResponseFormat.of("json"), openAiAudioTranscriptionModelOptions.getResponseFormat());
            assertEquals(0.0F, openAiAudioTranscriptionModelOptions.getTemperature());
        }
    }
}
