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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.TASK_ID;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.TASK_LIST_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.todo.util.MicrosoftToDoUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftToDoDeleteTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteTask")
        .title("Delete Task")
        .description("Deletes a task by ID.")
        .help("", "https://docs.bytechef.io/reference/components/microsoft-to-do_v1#delete-task")
        .properties(
            string(TASK_LIST_ID)
                .label("Task List ID")
                .description("ID of the task list where the task is located.")
                .options((OptionsFunction<String>) MicrosoftToDoUtils::getTaskListIdOptions)
                .required(true),
            string(TASK_ID)
                .label("Task ID")
                .description("ID of the task to delete.")
                .options((OptionsFunction<String>) MicrosoftToDoUtils::getTaskIdOptions)
                .optionsLookupDependsOn(TASK_LIST_ID)
                .required(true))
        .perform(MicrosoftToDoDeleteTaskAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftToDoDeleteTaskAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context
            .http(http -> http.delete(
                "/me/todo/lists/%s/tasks/%s".formatted(
                    inputParameters.getRequiredString(TASK_LIST_ID), inputParameters.getRequiredString(TASK_ID))))
            .execute();

        return null;
    }
}
