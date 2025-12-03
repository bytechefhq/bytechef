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

package com.bytechef.component.google.tasks.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.MAX_RESULTS;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.PAGE_TOKEN;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.SHOW_COMPLETED;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.TASK_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.tasks.util.GoogleTasksUtils;
import com.bytechef.google.commons.GoogleUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GoogleTasksListTasksAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listTasks")
        .title("List Tasks")
        .description("Returns all tasks in the specified task list.")
        .properties(
            string(LIST_ID)
                .label("List ID")
                .description("ID of the list where tasks are stored.")
                .options((OptionsFunction<String>) GoogleTasksUtils::getListsIdOptions)
                .required(true),
            bool(SHOW_COMPLETED)
                .label("Show completed")
                .description(
                    "Show also completed tasks. By default both completed task and task that needs action will be " +
                        "shown.")
                .defaultValue(true)
                .required(true))
        .output(outputSchema(array().items(TASK_OUTPUT_PROPERTY)))
        .perform(GoogleTasksListTasksAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleTasksListTasksAction() {
    }

    public static List<?> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Object> tasks = new ArrayList<>();
        String nextPageToken = null;

        do {
            Map<String, Object> response = context
                .http(http -> http.get("/lists/%s/tasks".formatted(
                    inputParameters.getRequiredString(LIST_ID))))
                .queryParameters(
                    PAGE_TOKEN, nextPageToken,
                    MAX_RESULTS, 100,
                    SHOW_COMPLETED, inputParameters.getRequiredBoolean(SHOW_COMPLETED))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (response.get("items") instanceof List<?> items) {
                tasks.addAll(items);
            }

            nextPageToken = (String) response.getOrDefault(NEXT_PAGE_TOKEN, null);
        } while (nextPageToken != null);

        return tasks;
    }
}
