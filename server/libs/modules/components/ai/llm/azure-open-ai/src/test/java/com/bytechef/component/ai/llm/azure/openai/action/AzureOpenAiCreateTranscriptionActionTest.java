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

package com.bytechef.component.ai.llm.azure.openai.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.ENDPOINT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bytechef.component.ai.llm.definition.Language;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.openai.models.audio.AudioResponseFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.Model;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;

/**
 * @author Nikolina Spehar
 */
class AzureOpenAiCreateTranscriptionActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(
            TOKEN, "TOKEN",
            ENDPOINT, "https://test.openai.azure.com"));

    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.ofEntries(
            Map.entry(RESPONSE_FORMAT, "json"),
            Map.entry(MODEL, "whisper"),
            Map.entry(LANGUAGE, Language.HR.name()),
            Map.entry(PROMPT, "prompt"),
            Map.entry(TEMPERATURE, 0.0)));

    @Test
    void testCreateAudioTranscriptionModelWithResponseFormat() {

        Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> audioTranscriptionModel =
            getAudioTranscriptionModel();

        assertNotNull(audioTranscriptionModel);
        assertInstanceOf(OpenAiAudioTranscriptionModel.class, audioTranscriptionModel);

        OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel =
            (OpenAiAudioTranscriptionModel) audioTranscriptionModel;

        OpenAiAudioTranscriptionOptions options =
            openAiAudioTranscriptionModel.getOptions();

        assertEquals("https://test.openai.azure.com", options.getBaseUrl());
        assertEquals("TOKEN", options.getApiKey());

        assertEquals("whisper", options.getDeploymentName());
        assertEquals("whisper", options.getModel());

        assertEquals("prompt", options.getPrompt());
        assertEquals(Language.HR.getCode(), options.getLanguage());

        assertEquals(
            AudioResponseFormat.of("json"),
            options.getResponseFormat());

        assertEquals(0.0F, options.getTemperature());
    }

    private Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> getAudioTranscriptionModel() {

        Language language = mockedInputParameters.get(LANGUAGE, Language.class);

        return OpenAiAudioTranscriptionModel.builder()
            .options(
                OpenAiAudioTranscriptionOptions.builder()
                    .baseUrl(mockedConnectionParameters.getString(ENDPOINT))
                    .apiKey(mockedConnectionParameters.getString(TOKEN))
                    .microsoftFoundry(true)
                    .deploymentName(mockedInputParameters.getRequiredString(MODEL))
                    .model(mockedInputParameters.getRequiredString(MODEL))
                    .prompt(mockedInputParameters.getString(PROMPT))
                    .language(language.getCode())
                    .responseFormat(
                        AudioResponseFormat.of(
                            mockedInputParameters.getString(RESPONSE_FORMAT)))
                    .temperature(mockedInputParameters.getFloat(TEMPERATURE))
                    .build())
            .build();
    }
}
