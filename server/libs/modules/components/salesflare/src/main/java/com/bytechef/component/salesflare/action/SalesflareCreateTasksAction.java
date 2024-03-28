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

package com.bytechef.component.salesflare.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class SalesflareCreateTasksAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createTasks")
        .title("Create tasks")
        .description("Creates new tasks")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/tasks", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(array("__items").items(object().properties(string("description").label("Description")
            .description("Description of new task")
            .required(true),
            date("reminder_date").label("Reminder Date")
                .required(false)))
            .placeholder("Add to Items")
            .label("Tasks")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object().properties(array("body").items(object().properties(integer("id").required(false)))
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));
}
