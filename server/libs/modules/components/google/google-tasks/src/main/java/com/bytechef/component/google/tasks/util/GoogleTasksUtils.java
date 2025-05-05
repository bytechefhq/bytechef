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
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GoogleTasksUtils {

    private GoogleTasksUtils() {
    }

    public static List<Option<String>> getListsIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, Context context) {

        Map<String, Object> response = context
            .http(http -> http.get("https://tasks.googleapis.com/tasks/v1/users/@me/lists"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(response);
    }

    public static List<Option<String>> getTasksIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
        String searchText, ActionContext actionContext) {

        Map<String, Object> response = actionContext
            .http(http -> http.get(
                "https://tasks.googleapis.com/tasks/v1/lists/" + inputParameters.getRequiredString(LIST_ID) + "/tasks"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(response);
    }

    private static List<Option<String>> getOptions(Map<String, Object> response) {
        List<Option<String>> tasksId = new ArrayList<>();

        if (response.get("items") instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> map) {
                    tasksId.add(option((String) map.get("title"), (String) map.get("id")));
                }
            }
        }

        return tasksId;
    }

    public static List<Map<?, ?>> getTasks(Context context, String listId, String timestamp) {
        List<Map<?, ?>> tasks = new ArrayList<>();

        Http.Executor executor = createHttpExecutor(listId, context, timestamp);

        Map<String, Object> response = executor.execute()
            .getBody(new TypeReference<>() {});

        extractTasks(response, tasks);

        return tasks;
    }

    private static Http.Executor createHttpExecutor(
        String listId, Context context, String timestamp) {

        Http.Executor executor = context
            .http(http -> http.get("https://tasks.googleapis.com/tasks/v1/lists/" + listId + "/tasks"))
            .configuration(Http.responseType(Http.ResponseType.JSON));

        if (timestamp != null) {
            String encode = URLEncoder.encode(timestamp, StandardCharsets.UTF_8);
            executor.queryParameter("updatedMin", encode);
        }
        return executor;
    }

    private static void extractTasks(Map<String, Object> response, List<Map<?, ?>> tasks) {
        if (response.get("items") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    tasks.add(map);
                }
            }
        }
    }
}
