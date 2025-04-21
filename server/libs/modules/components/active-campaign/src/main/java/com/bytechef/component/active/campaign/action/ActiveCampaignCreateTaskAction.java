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

package com.bytechef.component.active.campaign.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ActiveCampaignCreateTaskAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates a new task.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/dealTasks", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("dealTask").properties(string("title").label("Title")
            .description("The title to be assigned to the task.")
            .required(false),
            integer("relid").label("Assigned To")
                .description("The id of the relational object for this task.")
                .required(true),
            date("duedate").label("Due Date")
                .description("Due date of the task.")
                .required(true),
            integer("dealTasktype").label("Task Type ID")
                .description("ID of the task type.")
                .required(true))
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("Deal Task")
            .required(false))
        .output(outputSchema(object()
            .properties(object("dealTask").properties(string("id").description("ID of the task.")
                .required(false),
                string("title").description("Title of the task.")
                    .required(false),
                integer("relid").description("ID of the relational object for this task.")
                    .required(false),
                date("duedate").description("Due date of the task.")
                    .required(false),
                integer("dealTasktype").description("ID of the task type.")
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ActiveCampaignCreateTaskAction() {
    }
}
