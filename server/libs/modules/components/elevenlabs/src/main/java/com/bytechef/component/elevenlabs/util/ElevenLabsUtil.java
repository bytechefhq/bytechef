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

package com.bytechef.component.elevenlabs.util;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ElevenLabsUtil {

    private ElevenLabsUtil() {
    }

    public static ModifiableObjectProperty alignmentObject(String alignment) {
        return object(alignment)
            .properties(
                array("characters"),
                array("character_start_times_seconds"),
                array("character_end_times_seconds"));
    }

    public static List<Option<String>> getVoiceOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get("/voices"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> voiceOptions = new ArrayList<>();

        if (body.get("voices") instanceof List<?> bodyList) {
            for (Object object : bodyList) {
                if (object instanceof Map<?, ?> voices
                    && voices.get("name") instanceof String name
                    && voices.get("voice_id") instanceof String voice_id) {

                    voiceOptions.add(option(name, voice_id));
                }
            }
        }

        return voiceOptions;
    }

}
