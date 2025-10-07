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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.LIST_ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.NOTES;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.STATUS;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.TASK_OUTPUT_PROPERTY;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.TITLE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.tasks.util.GoogleTasksUtils;
import com.bytechef.google.commons.GoogleUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GoogleTasksCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task on the specified task list.")
        .properties(
            string(TITLE)
                .label("Title")
                .description("Title of the new task to be created.")
                .required(true),
            string(LIST_ID)
                .label("List ID")
                .description("ID of the list where the new task will be stored.")
                .options((ActionOptionsFunction<String>) GoogleTasksUtils::getListsIdOptions)
                .required(true),
            string(STATUS)
                .label("Status")
                .description("Status of the task.")
                .options(
                    option("Needs Action", "needsAction", "Issues needs action."),
                    option("Completed", "completed", "Issues is completed."))
                .defaultValue("needsAction")
                .required(true),
            string(NOTES)
                .label("Notes")
                .description("Notes describing the task.")
                .required(false))
        .output(outputSchema(TASK_OUTPUT_PROPERTY))
        .perform(GoogleTasksCreateTaskAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleTasksCreateTaskAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post(
                "https://tasks.googleapis.com/tasks/v1/lists/" + inputParameters.getRequiredString(LIST_ID) + "/tasks"))
            .configuration(responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    TITLE, inputParameters.getRequiredString(TITLE),
                    STATUS, inputParameters.getRequiredString(STATUS),
                    NOTES, inputParameters.getString(NOTES)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
