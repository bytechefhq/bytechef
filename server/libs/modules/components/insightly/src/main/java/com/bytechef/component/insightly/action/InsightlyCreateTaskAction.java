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

package com.bytechef.component.insightly.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
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
public class InsightlyCreateTaskAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create task")
        .description("Creates new Task")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/Tasks", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(string("TITLE").maxLength(500)
            .label("Title")
            .required(true),
            string("STATUS").label("Status")
                .description("Task status")
                .options(option("Not Started", "Not Started"), option("In Progress", "In Progress"),
                    option("Completed", "Completed"), option("Deferred", "Deferred"), option("Waiting", "Waiting"))
                .required(false))
            .label("Task")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(integer("TASK_ID").required(false), string("TITLE").required(false),
                string("STATUS").required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private InsightlyCreateTaskAction() {
    }
}
