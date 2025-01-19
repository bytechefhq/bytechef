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

package com.bytechef.component.ai.llm.azure.openai.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_TRANSCRIPTION;
import static com.bytechef.component.ai.llm.constant.LLMConstants.ENDPOINT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FILE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PROMPT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.springframework.ai.azure.openai.AzureOpenAiAudioTranscriptionOptions.WhisperModel;

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.KeyCredential;
import com.bytechef.component.ai.llm.AudioTranscriptionModel;
import com.bytechef.component.ai.llm.constant.Language;
import com.bytechef.component.ai.llm.util.LLMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.azure.openai.AzureOpenAiAudioTranscriptionModel;
import org.springframework.ai.azure.openai.AzureOpenAiAudioTranscriptionOptions;
import org.springframework.ai.azure.openai.AzureOpenAiAudioTranscriptionOptions.TranscriptResponseFormat;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public class AzureOpenAiCreateTranscriptionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TRANSCRIPTION)
        .title("Create Transcriptions")
        .description("Transcribes audio into the input language.")
        .properties(
            fileEntry(FILE)
                .label("File")
                .description("The audio file object to transcribe, in one of these formats: flac, mp3, mp4, mpeg, " +
                    "mpga, m4a, ogg, wav, or webm.")
                .required(true),
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(WhisperModel.values())
                            .collect(Collectors.toMap(WhisperModel::getValue, WhisperModel::getValue)))),
            LANGUAGE_PROPERTY,
            string(PROMPT)
                .label("Prompt")
                .description(
                    "An optional text to guide the model's style or continue a previous audio segment. The prompt " +
                        "should match the audio language.")
                .required(false),
            string(RESPONSE_FORMAT)
                .label("Response Format")
                .description("The format of the transcript output")
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(TranscriptResponseFormat.values())
                            .collect(Collectors.toMap(clazz -> String.valueOf(clazz.getValue()), Enum::name))))
                .required(true),
            number(TEMPERATURE)
                .label("Temperature")
                .description(
                    "The sampling temperature, between 0 and 1. Higher values like will make the output more random, " +
                        "while lower values will make it more focused and deterministic. ")
                .defaultValue(0)
                .minValue(0)
                .maxValue(1)
                .required(false))
        .output()
        .perform(AzureOpenAiCreateTranscriptionAction::perform);

    private AzureOpenAiCreateTranscriptionAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws MalformedURLException {

        return AUDIO_TRANSCRIPTION.getResponse(inputParameters, connectionParameters);
    }

    private static final AudioTranscriptionModel AUDIO_TRANSCRIPTION = (inputParameters, connectionParameters) -> {
        Language language = inputParameters.get(LANGUAGE, Language.class);

        return new AzureOpenAiAudioTranscriptionModel(
            new OpenAIClientBuilder()
                .credential(new KeyCredential(connectionParameters.getString(TOKEN)))
                .endpoint(connectionParameters.getString(ENDPOINT))
                .buildClient(),
            AzureOpenAiAudioTranscriptionOptions.builder()
                .model(inputParameters.getRequiredString(MODEL))
                .prompt(inputParameters.getString(PROMPT))
                .language(language.getCode())
                .responseFormat(TranscriptResponseFormat.valueOf(inputParameters.getString(RESPONSE_FORMAT)))
                .temperature(inputParameters.getFloat(TEMPERATURE))
                .build());
    };
}
