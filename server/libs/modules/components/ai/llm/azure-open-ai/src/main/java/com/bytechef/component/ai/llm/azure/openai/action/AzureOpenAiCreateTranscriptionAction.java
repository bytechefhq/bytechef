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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.AudioTranscriptionModel;
import com.bytechef.component.ai.llm.definition.Language;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.openai.models.audio.AudioResponseFormat;
import java.net.MalformedURLException;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;

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
                .label("File Entry")
                .description("The audio file object to transcribe, in one of these formats: flac, mp3, mp4, mpeg, " +
                    "mpga, m4a, ogg, wav, or webm.")
                .required(true),
            string(MODEL)
                .label("Model")
                .description("Whisper deployment name.")
                .required(true)
                .exampleValue("whisper"),
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
                    option("JSON", "json"),
                    option("Text", "text"),
                    option("SRT", "srt"),
                    option("Verbose JSON", "verbose_json"),
                    option("VTT", "vtt"))
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
        .help(
            "", "https://docs.bytechef.io/reference/components/azure-open-ai_v1#create-transcriptions")
        .perform(AzureOpenAiCreateTranscriptionAction::perform);

    private AzureOpenAiCreateTranscriptionAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws MalformedURLException {

        return AUDIO_TRANSCRIPTION.getResponse(inputParameters, connectionParameters, context);
    }

    public static final AudioTranscriptionModel AUDIO_TRANSCRIPTION =
        (inputParameters, connectionParameters) -> {
            Language language = inputParameters.get(LANGUAGE, Language.class);

            return OpenAiAudioTranscriptionModel.builder()
                .options(
                    OpenAiAudioTranscriptionOptions.builder()
                        .baseUrl(connectionParameters.getString(ENDPOINT))
                        .apiKey(connectionParameters.getString(TOKEN))
                        .microsoftFoundry(true)
                        .deploymentName(inputParameters.getRequiredString(MODEL))
                        .model(inputParameters.getRequiredString(MODEL))
                        .prompt(inputParameters.getString(PROMPT))
                        .language(language.getCode())
                        .responseFormat(AudioResponseFormat.of(inputParameters.getString(RESPONSE_FORMAT)))
                        .temperature(inputParameters.getFloat(TEMPERATURE))
                        .build())
                .build();
        };
}
