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

package com.bytechef.component.youtube.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.youtube.constant.YoutubeConstants.ID;
import static com.bytechef.component.youtube.constant.YoutubeConstants.ITEMS;
import static com.bytechef.component.youtube.constant.YoutubeConstants.SNIPPET;
import static com.bytechef.component.youtube.constant.YoutubeConstants.TITLE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class YoutubeUtils {

    public static String getChannelId(String identifier, TriggerContext triggerContext) {
        String channelId = "";

        Map<String, Object> response = triggerContext
            .http(http -> http.get("/search"))
            .queryParameters(
                "part", SNIPPET,
                "type", "channel",
                "q", identifier)
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response.get(ITEMS) instanceof List<?> channels &&
            channels.getFirst() instanceof Map<?, ?> channelMap &&
            channelMap.get(ID) instanceof Map<?, ?> channelIdMap) {

            channelId = (String) channelIdMap.get("channelId");
        }

        return channelId;
    }

    public static List<Option<String>> getVideoCategoryIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> stringStringMap, String s,
        Context context) {

        List<Option<String>> options = new ArrayList<>();

        Map<String, Object> response = context
            .http(http -> http.get("https://www.googleapis.com/youtube/v3/videoCategories"))
            .queryParameters(
                "part", SNIPPET,
                "regionCode", "US")
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response.get(ITEMS) instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> itemMap && itemMap.get(SNIPPET) instanceof Map<?, ?> snippetMap) {
                    options.add(option((String) snippetMap.get(TITLE), (String) itemMap.get(ID)));
                }
            }
        }

        return options;
    }
}
