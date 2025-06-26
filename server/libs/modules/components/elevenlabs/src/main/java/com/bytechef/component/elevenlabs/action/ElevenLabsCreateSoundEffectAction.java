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

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ElevenLabsCreateSoundEffectAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSoundEffect")
        .title("Create Sound Effect")
        .description(
            "Turn text into sound effects for your videos, voice-overs or video games using the most advanced sound " +
                "effects model in the world.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text that will get converted into a sound effect.")
                .required(true))
        .output(
            outputSchema(
                fileEntry()
                    .description("Sound effect that was created.")))
        .perform(ElevenLabsCreateSoundEffectAction::perform);

    private ElevenLabsCreateSoundEffectAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/sound-generation"))
            .body(Body.of(Map.of(TEXT, inputParameters.getRequiredString(TEXT))))
            .configuration(responseType(ResponseType.binary("audio/mpeg")))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
