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

package com.bytechef.component.elevenlabs.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.elevenlabs.constant.ElevenLabsConstants.FILE;
import static com.bytechef.component.elevenlabs.constant.ElevenLabsConstants.MODEL_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ElevenLabsCreateTranscriptAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTranscript")
        .title("Create Transcript")
        .description("Transcribe an audio or video file.")
        .properties(
            string(MODEL_ID)
                .label("Model")
                .description("The ID of the model to use for transcription, currently only ‘scribe_v1’ is available.")
                .defaultValue("scribe_v1")
                .required(true),
            fileEntry(FILE)
                .label("File")
                .description(
                    "The file to transcribe. All major audio and video formats are supported. The file size must be " +
                        "less than 1GB.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("language_code")
                            .description("The detected language code (e.g. ‘eng’ for English)."),
                        number("language_probability")
                            .description("The confidence score of the language detection (0 to 1)."),
                        string("text")
                            .description("The raw text of the transcription."),
                        array("words")
                            .description("List of words with their timing information.")
                            .items(
                                object()
                                    .properties(
                                        string("text")
                                            .description("The word or sound that was transcribed."),
                                        number("start")
                                            .description("The start time of the word or sound in seconds"),
                                        number("end")
                                            .description("The end time of the word or sound in seconds"),
                                        string("type")
                                            .description(
                                                "The type of the word or sound. ‘audio_event’ is used for non-word " +
                                                    "sounds like laughter or footsteps. Allowed values: word, " +
                                                    "spacing and audio_event."))))))
        .perform(ElevenLabsCreateTranscriptAction::perform);

    private ElevenLabsCreateTranscriptAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/speech-to-text"))
            .header("Content-Type", "multipart/form-data")
            .body(
                Body.of(
                    Map.of(
                        MODEL_ID, inputParameters.getRequiredString(MODEL_ID),
                        FILE, inputParameters.getRequiredFileEntry(FILE)),
                    BodyContentType.FORM_DATA))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
