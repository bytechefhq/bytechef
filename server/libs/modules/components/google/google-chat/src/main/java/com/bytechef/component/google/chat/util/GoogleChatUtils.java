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

package com.bytechef.component.google.chat.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.chat.constant.GoogleChatConstants.DISPLAY_NAME;

import com.bytechef.component.definition.Context;
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
public class GoogleChatUtils {

    public static List<Option<String>> getSpaceOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        List<Option<String>> options = new ArrayList<>();
        String nextPageToken = null;

        do {
            Map<String, ?> body = fetchSpaces(context, nextPageToken);
            nextPageToken = (String) body.get("nextPageToken");

            if (body.get("spaces") instanceof List<?> spaces) {
                for (Object space : spaces) {
                    if (space instanceof Map<?, ?> item) {
                        options.add(option((String) item.get(DISPLAY_NAME), (String) item.get("name")));
                    }
                }
            }
        } while (nextPageToken != null);

        return options;
    }

    private static Map<String, ?> fetchSpaces(Context context, String pageToken) {
        return context
            .http(http -> http.get("/spaces"))
            .queryParameters("pageSize", 1000, "pageToken", pageToken)
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
