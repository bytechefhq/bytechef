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
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.elevenlabs.constant.ElevenLabsConstants.TEXT;
import static com.bytechef.component.elevenlabs.constant.ElevenLabsConstants.VOICE_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.elevenlabs.util.ElevenLabsUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ElevenLabsCreateSpeechWithTimingAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSpeechWithTiming")
        .title("Create Speech With Timing")
        .description(
            "Generate speech from text with precise character-level timing information for audio-text synchronization.")
        .properties(
            string(VOICE_ID)
                .label("Voice")
                .description("Voice you want to use for converting the text into speech.")
                .options((OptionsFunction<String>) ElevenLabsUtils::getVoiceOptions)
                .required(true),
            string(TEXT)
                .label("Text")
                .description("Text you want to convert into speech.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("audio_base64")
                            .description("Base64 encoded audio data"),
                        object("alignment")
                            .properties(
                                array("characters")
                                    .items(string()),
                                array("character_start_times_seconds")
                                    .items(number()),
                                array("character_end_times_seconds")
                                    .items(number())),
                        object("normalized_alignment")
                            .properties(
                                array("characters")
                                    .items(string()),
                                array("character_start_times_seconds")
                                    .items(number()),
                                array("character_end_times_seconds")
                                    .items(number())))))
        .perform(ElevenLabsCreateSpeechWithTimingAction::perform);

    private ElevenLabsCreateSpeechWithTimingAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post(
                "/text-to-speech/%s/with-timestamps".formatted(inputParameters.getRequiredString(VOICE_ID))))
            .body(Body.of(Map.of(TEXT, inputParameters.getRequiredString(TEXT))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
