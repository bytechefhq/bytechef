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
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.TASK_ID;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.TASK_OUTPUT_PROPERTY;
import static com.bytechef.component.google.tasks.constant.GoogleTasksConstants.TITLE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.tasks.util.GoogleTasksUtils;
import com.bytechef.google.commons.GoogleUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GoogleTasksUpdateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateTask")
        .title("Update Task")
        .description("Updates a specific task on the specified task list.")
        .properties(
            string(LIST_ID)
                .label("List ID")
                .description("ID of the list where specific task is stored.")
                .options((OptionsFunction<String>) GoogleTasksUtils::getListsIdOptions)
                .required(true),
            string(TASK_ID)
                .label("Task ID")
                .description("ID of the task to update.")
                .options((OptionsFunction<String>) GoogleTasksUtils::getTasksIdOptions)
                .optionsLookupDependsOn(LIST_ID)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("Title of the task to be updated. If empty, title will not be changed.")
                .required(false),
            string(STATUS)
                .label("Status")
                .description("Status of the task. If empty, status will not be changed.")
                .options(
                    option("Needs Action", "needsAction", "Issues needs action."),
                    option("Completed", "completed", "Issues is completed."))
                .required(false),
            string(NOTES)
                .label("Notes")
                .description("Notes describing the task. If empty, notes will not be changed.")
                .required(false))
        .output(outputSchema(TASK_OUTPUT_PROPERTY))
        .perform(GoogleTasksUpdateTaskAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleTasksUpdateTaskAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.patch(
                "/lists/" + inputParameters.getRequiredString(LIST_ID) +
                    "/tasks/" + inputParameters.getRequiredString(TASK_ID)))
            .configuration(responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    TITLE, inputParameters.getString(TITLE),
                    STATUS, inputParameters.getString(STATUS),
                    NOTES, inputParameters.getString(NOTES)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
