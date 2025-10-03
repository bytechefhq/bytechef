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

package com.bytechef.component.microsoft.todo.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.IMPORTANCE;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.IS_REMINDER_ON;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.OUTPUT_TASK_PROPERTY;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.TASK_LIST_ID;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.TITLE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.todo.util.MicrosoftToDoUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftToDoCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task.")
        .properties(
            string(TASK_LIST_ID)
                .label("Task List ID")
                .description("ID of the task list where the task will be created.")
                .options((ActionOptionsFunction<String>) MicrosoftToDoUtils::getTaskListIdOptions)
                .required(true),
            string(TITLE)
                .label("Title")
                .description("Title of the task.")
                .required(true),
            string(IMPORTANCE)
                .label("Importance")
                .description("Importance of the task")
                .options(
                    option("Low", "low"),
                    option("Normal", "normal"),
                    option("High", "high"))
                .required(false),
            bool(IS_REMINDER_ON)
                .label("Reminder")
                .description("Set to true if an alert is set to remind the user of the task.")
                .required(false))
        .output(outputSchema(OUTPUT_TASK_PROPERTY))
        .perform(MicrosoftToDoCreateTaskAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftToDoCreateTaskAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post(
                "/me/todo/lists/%s/tasks".formatted(inputParameters.getRequiredString(TASK_LIST_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    TITLE, inputParameters.getRequiredString(TITLE),
                    IMPORTANCE, inputParameters.getString(IMPORTANCE),
                    IS_REMINDER_ON, inputParameters.getBoolean(IS_REMINDER_ON)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
