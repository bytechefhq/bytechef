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

package com.bytechef.component.youtube.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

public class YoutubeTesting {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("test")
        .title("Test")
        .description("Uploads video to Youtube.")
        .properties(
            string("input")
                .required(true))
        .output(outputSchema(string()))
        .perform(YoutubeTesting::perform);

    private YoutubeTesting() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> response = context.http(http -> http.get("https://www.googleapis.com/youtube/v3/search"))
            .queryParameters(
                "part", "snippet",
                "type", "channel",
                "q", inputParameters.getRequiredString("input"))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        String channelId = "";

        if (response.get("items") instanceof List<?> channels &&
            channels.getFirst() instanceof Map<?, ?> channelMap &&
            channelMap.get("id") instanceof Map<?, ?> channelIdMap) {

            channelId = (String) channelIdMap.get("channelId");
        }

        return channelId;
    }
}
