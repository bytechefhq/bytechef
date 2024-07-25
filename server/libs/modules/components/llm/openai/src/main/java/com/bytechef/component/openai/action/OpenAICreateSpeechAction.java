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
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.openai.constant.OpenAIConstants.CREATE_SPEECH;
import static com.bytechef.component.openai.constant.OpenAIConstants.FILE;
import static com.bytechef.component.openai.constant.OpenAIConstants.INPUT;
import static com.bytechef.component.openai.constant.OpenAIConstants.LANGUAGE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.SPEED;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static com.bytechef.component.openai.constant.OpenAIConstants.VOICE;

import ch.qos.logback.core.rolling.helper.FileStoreUtil;
import ch.qos.logback.core.util.FileUtil;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.core.io.UrlResource;
import org.springframework.util.FileSystemUtils;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * @author Monika Domiter
 */
public class OpenAICreateSpeechAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_SPEECH)
        .title("Create speech")
        .description("Generate an audio recording from the input text")
        .properties(
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .description("Text-to-Speech model which will generate the audio.")
                .options(
                    option(OpenAiAudioApi.TtsModel.TTS_1.value, OpenAiAudioApi.TtsModel.TTS_1.value, "Model optimized for speed."),
                    option(OpenAiAudioApi.TtsModel.TTS_1_HD.value, OpenAiAudioApi.TtsModel.TTS_1_HD.value, "Model optimized for quality.")),
            string(INPUT)
                .label("Input")
                .description("The text to generate audio for.")
                .maxLength(4096)
                .required(true),
            object(VOICE)
                .label("Voice")
                .description("The voice to use when generating the audio.")
                .options(
                    option(OpenAiAudioApi.SpeechRequest.Voice.ALLOY.value, OpenAiAudioApi.SpeechRequest.Voice.ALLOY),
                    option(OpenAiAudioApi.SpeechRequest.Voice.ECHO.value, OpenAiAudioApi.SpeechRequest.Voice.ECHO),
                    option(OpenAiAudioApi.SpeechRequest.Voice.FABLE.value, OpenAiAudioApi.SpeechRequest.Voice.FABLE),
                    option(OpenAiAudioApi.SpeechRequest.Voice.ONYX.value, OpenAiAudioApi.SpeechRequest.Voice.ONYX),
                    option(OpenAiAudioApi.SpeechRequest.Voice.NOVA.value, OpenAiAudioApi.SpeechRequest.Voice.NOVA),
                    option(OpenAiAudioApi.SpeechRequest.Voice.SHIMMER.value, OpenAiAudioApi.SpeechRequest.Voice.SHIMMER))
                .required(true),
            object(RESPONSE_FORMAT)
                .label("Response format")
                .description("The format to audio in.")
                .options(
                    option(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3.value, OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3),
                    option(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.OPUS.value, OpenAiAudioApi.SpeechRequest.AudioResponseFormat.OPUS),
                    option(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.AAC.value, OpenAiAudioApi.SpeechRequest.AudioResponseFormat.AAC),
                    option(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.FLAC.value, OpenAiAudioApi.SpeechRequest.AudioResponseFormat.FLAC))
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
        OpenAiAudioApi.SpeechRequest.AudioResponseFormat audioResponseFormat = inputParameters.get(RESPONSE_FORMAT, OpenAiAudioApi.SpeechRequest.AudioResponseFormat.class);

        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
            .withModel(inputParameters.getRequiredString(MODEL))
            .withInput(input)
            .withVoice(inputParameters.get(VOICE, OpenAiAudioApi.SpeechRequest.Voice.class))
            .withResponseFormat(audioResponseFormat)
            .withSpeed(inputParameters.getFloat(SPEED))
            .build();
        SpeechModel speechModel = new OpenAiAudioSpeechModel(new OpenAiAudioApi(connectionParameters.getString(TOKEN)),  speechOptions);

        SpeechResponse response = speechModel.call(new SpeechPrompt(input));
        byte[] output = response.getResult().getOutput();
        return context.file(file -> file.storeContent("file." + audioResponseFormat.value, new ByteArrayInputStream(output)));
    }
}
