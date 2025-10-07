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

package com.bytechef.component.todoist.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.todoist.util.TodoistUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class TodoistMarkTaskCompletedAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("markTaskCompleted")
        .title("Mark Task as Completed")
        .description("Mark a tas as being completed.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/tasks/{taskId}/close"

            ))
        .properties(string("taskId").label("Task ID")
            .description("ID of the task to be closed.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) TodoistUtils::getTaskIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)));

    private TodoistMarkTaskCompletedAction() {
    }
}
