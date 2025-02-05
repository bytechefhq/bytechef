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

package com.bytechef.component.google.tasks.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.NOTES;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.STATUS;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.TITLE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GoogleTasksUtils {

    private GoogleTasksUtils() {
    }

    public static List<Option<String>> getListsIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> listsId = new ArrayList<>();
        Context.Http.Executor executor = context
            .http(http -> http.get("https://tasks.googleapis.com/tasks/v1/users/@me/lists"))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON));

        Map<String, Object> response = executor.execute()
            .getBody(new TypeReference<>() {});

        if (response.get("items") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    listsId.add(
                        option((String) map.get("title"), (String) map.get("id")));
                }
            }
        }
        return listsId;
    }

    public static List<Option<String>> getTasksIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, Context context) {

        List<Option<String>> tasksId = new ArrayList<>();

        Map<String, Object> response = context
            .http(http -> http.get("https://tasks.googleapis.com/tasks/v1/lists/" +
                inputParameters.getRequiredString(LIST_ID) + "/tasks"))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (response.get("items") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    tasksId.add(
                        option((String) map.get("title"), (String) map.get("id")));
                }
            }
        }
        return tasksId;
    }

    public static Map<String, Object> createTaskRequestBody(Parameters inputParameters) {

        Map<String, Object> body = new HashMap<>();

        if (inputParameters.get(TITLE) != null) {
            body.put(TITLE, inputParameters.getRequiredString(TITLE));
        }

        if (inputParameters.get(STATUS) != null) {
            body.put(STATUS, inputParameters.getRequiredString(STATUS));
        }

        if (inputParameters.get(NOTES) != null && inputParameters.getString(NOTES) != null) {
            body.put(NOTES, inputParameters.getString(NOTES));
        }

        return body;
    }
}
