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

package com.bytechef.component.google.tasks.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.MAX_RESULTS;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.PAGE_TOKEN;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
public class GoogleTasksUtils {

    private GoogleTasksUtils() {
    }

    public static List<Option<String>> getListsIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        return fetchOptions(context, "https://tasks.googleapis.com/tasks/v1/users/@me/lists");
    }

    public static List<Option<String>> getTasksIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext actionContext) {

        String url = "https://tasks.googleapis.com/tasks/v1/lists/%s/tasks"
            .formatted(inputParameters.getRequiredString(LIST_ID));

        return fetchOptions(actionContext, url);
    }

    private static List<Option<String>> fetchOptions(Context context, String url) {
        List<Option<String>> options = new ArrayList<>();
        String nextPageToken = null;

        do {
            Map<String, Object> response = context
                .http(http -> http.get(url))
                .queryParameters(PAGE_TOKEN, nextPageToken, MAX_RESULTS, 100)
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (response.get("items") instanceof List<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> map) {
                        options.add(option((String) map.get("title"), (String) map.get(ID)));
                    }
                }
            }

            nextPageToken = (String) response.getOrDefault(NEXT_PAGE_TOKEN, null);
        } while (nextPageToken != null);

        return options;
    }
}
