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

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_SPEECH;
import static com.bytechef.component.ai.llm.constant.LLMConstants.INPUT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SPEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VOICE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest;
import org.springframework.ai.openai.api.OpenAiAudioApi.SpeechRequest.AudioResponseFormat;
import org.springframework.ai.openai.api.OpenAiAudioApi.TtsModel;

/**
 * @author Monika Domiter
 */
public class OpenAiCreateSpeechAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_SPEECH)
        .title("Text-To-Speech")
        .description("Generate an audio recording from the input text")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .description("Text-to-Speech model which will generate the audio.")
                .options(
                    ModelUtils.getEnumOptions(
                        Arrays.stream(TtsModel.values())
                            .collect(Collectors.toMap(TtsModel::getValue, TtsModel::getValue)))),
            string(INPUT)
                .label("Input")
                .description("The text to generate audio for.")
                .maxLength(4096)
                .required(true),
            string(VOICE)
                .label("Voice")
                .description("The voice to use when generating the audio.")
                .options(
                    ModelUtils.getEnumOptions(
                        Arrays.stream(SpeechRequest.Voice.values())
                            .collect(Collectors.toMap(OpenAiAudioApi.SpeechRequest.Voice::getValue, Enum::name))))
                .required(true),
            string(RESPONSE_FORMAT)
                .label("Response format")
                .description("The format to audio in.")
                .options(
                    ModelUtils.getEnumOptions(
                        Arrays.stream(AudioResponseFormat.values())
                            .collect(
                                Collectors.toMap(
                                    OpenAiAudioApi.SpeechRequest.AudioResponseFormat::getValue, Enum::name))))
                .required(false),
            number(SPEED)
                .label("Speed")
                .description("The speed of the generated audio.")
                .defaultValue(1.0)
                .minValue(0.25)
                .maxValue(4.0)
                .required(false))
        .output(outputSchema(fileEntry().description("The generated audio file.")))
        .perform(OpenAiCreateSpeechAction::perform);

    private OpenAiCreateSpeechAction() {
    }

    public static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String input = inputParameters.getRequiredString(INPUT);
        AudioResponseFormat audioResponseFormat = AudioResponseFormat.valueOf(
            inputParameters.getString(RESPONSE_FORMAT));

        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
            .model(inputParameters.getRequiredString(MODEL))
            .input(input)
            .voice(SpeechRequest.Voice.valueOf(inputParameters.getString(VOICE)))
            .responseFormat(audioResponseFormat)
            .speed(inputParameters.getDouble(SPEED))
            .build();

        TextToSpeechModel speechModel = new OpenAiAudioSpeechModel(
            OpenAiAudioApi.builder()
                .apiKey(connectionParameters.getRequiredString(TOKEN))
                .build(),
            speechOptions);

        TextToSpeechResponse response = speechModel.call(new TextToSpeechPrompt(input));

        Speech result = response.getResult();

        byte[] output = result.getOutput();

        return context.file(
            file -> file.storeContent("file." + audioResponseFormat.value, new ByteArrayInputStream(output)));
    }
}
