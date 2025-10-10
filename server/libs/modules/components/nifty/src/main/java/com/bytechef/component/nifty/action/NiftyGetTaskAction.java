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

package com.bytechef.component.nifty.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.nifty.util.NiftyUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class NiftyGetTaskAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getTask")
        .title("Get Task")
        .description("Gets task details.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/tasks/{taskId}"

            ))
        .properties(string("taskId").label("Task ID")
            .description("ID of the task to get details for.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) NiftyUtils::getTaskIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(string("id").description("ID of the task.")
            .required(false),
            string("name").description("Name of the task.")
                .required(false),
            string("project").description("ID of the project the task belongs to.")
                .required(false),
            string("description").description("Description of the task.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private NiftyGetTaskAction() {
    }
}
