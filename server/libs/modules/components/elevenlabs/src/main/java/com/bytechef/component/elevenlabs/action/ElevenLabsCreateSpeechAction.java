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
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
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
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.elevenlabs.util.ElevenLabsUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ElevenLabsCreateSpeechAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSpeech")
        .title("Create Speech")
        .description("Converts text into speech using a voice of your choice and returns audio.")
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
                fileEntry()
                    .description("Speech audio that was created.")))
        .perform(ElevenLabsCreateSpeechAction::perform);

    private ElevenLabsCreateSpeechAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/text-to-speech/" + inputParameters.getRequiredString(VOICE_ID)))
            .body(Body.of(Map.of(TEXT, inputParameters.getRequiredString(TEXT))))
            .configuration(responseType(ResponseType.binary("audio/mpeg")))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
