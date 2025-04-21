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

package com.bytechef.component.microsoft.todo.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.ID;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.TASK_LIST_ID;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.TITLE;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftToDoUtils {

    private MicrosoftToDoUtils() {
    }

    public static List<Option<String>> getTaskIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext actionContext) {

        Map<String, ?> body = actionContext
            .http(http -> http.get(
                "/me/todo/lists/%s/tasks".formatted(inputParameters.getRequiredString(TASK_LIST_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, TITLE);
    }

    public static List<Option<String>> getTaskListIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext actionContext) {

        Map<String, ?> body = actionContext
            .http(http -> http.get("/me/todo/lists"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, DISPLAY_NAME);
    }

    private static List<Option<String>> getOptions(Map<String, ?> body, String label) {
        List<Option<String>> options = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(label), (String) map.get(ID)));
                }
            }
        }

        return options;
    }
}
