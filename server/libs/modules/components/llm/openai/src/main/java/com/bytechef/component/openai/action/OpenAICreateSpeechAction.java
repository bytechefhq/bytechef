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

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static constants.LLMConstants.CREATE_SPEECH;
import static constants.LLMConstants.INPUT;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.RESPONSE_FORMAT;
import static constants.LLMConstants.SPEED;
import static constants.LLMConstants.VOICE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import util.LLMUtils;

/**
 * @author Monika Domiter
 */
public class OpenAICreateSpeechAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_SPEECH)
        .title("Text-To-Speech")
        .description("Generate an audio recording from the input text")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .description("Text-to-Speech model which will generate the audio.")
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(OpenAiAudioApi.TtsModel.values())
                        .collect(Collectors.toMap(
                            OpenAiAudioApi.TtsModel::getValue, OpenAiAudioApi.TtsModel::getValue)))),
            string(INPUT)
                .label("Input")
                .description("The text to generate audio for.")
                .maxLength(4096)
                .required(true),
            object(VOICE)
                .label("Voice")
                .description("The voice to use when generating the audio.")
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(OpenAiAudioApi.SpeechRequest.Voice.values())
                        .collect(Collectors.toMap(
                            OpenAiAudioApi.SpeechRequest.Voice::getValue, clas -> clas))))
                .required(true),
            object(RESPONSE_FORMAT)
                .label("Response format")
                .description("The format to audio in.")
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.values())
                        .collect(Collectors.toMap(
                            OpenAiAudioApi.SpeechRequest.AudioResponseFormat::getValue, clas -> clas))))
                .required(false),
            number(SPEED)
                .label("Speed")
                .description("The speed of the generated audio.")
                .defaultValue(1.0)
                .minValue(0.25)
                .maxValue(4.0)
                .required(false))
        .outputSchema(fileEntry())
        .perform(OpenAICreateSpeechAction::perform);

    private OpenAICreateSpeechAction() {
    }

    public static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String input = inputParameters.getRequiredString(INPUT);
        OpenAiAudioApi.SpeechRequest.AudioResponseFormat audioResponseFormat =
            inputParameters.get(RESPONSE_FORMAT, OpenAiAudioApi.SpeechRequest.AudioResponseFormat.class);

        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
            .withModel(inputParameters.getRequiredString(MODEL))
            .withInput(input)
            .withVoice(inputParameters.get(VOICE, OpenAiAudioApi.SpeechRequest.Voice.class))
            .withResponseFormat(audioResponseFormat)
            .withSpeed(inputParameters.getFloat(SPEED))
            .build();
        SpeechModel speechModel =
            new OpenAiAudioSpeechModel(new OpenAiAudioApi(connectionParameters.getString(TOKEN)), speechOptions);

        SpeechResponse response = speechModel.call(new SpeechPrompt(input));
        byte[] output = response.getResult()
            .getOutput();
        return context
            .file(file -> file.storeContent("file." + audioResponseFormat.value, new ByteArrayInputStream(output)));
    }
}
