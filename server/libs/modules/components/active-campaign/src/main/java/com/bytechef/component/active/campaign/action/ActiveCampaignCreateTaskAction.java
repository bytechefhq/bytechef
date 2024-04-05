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

package com.bytechef.component.active.campaign.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
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
public class ActiveCampaignCreateTaskAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Creates a task")
        .description("Creates a new task")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/dealTasks", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(object("__item").properties(object("dealTask").properties(string("title").label("Title")
            .description("The title to be assigned to the task")
            .required(false),
            integer("relid").label("Assigned   To")
                .description("The id of the relational object for this task")
                .required(true),
            date("duedate").label("Due   Date")
                .description("Due date of the task")
                .required(true),
            integer("dealTasktype").label("Task   Type")
                .description("The type of the task based on the available Task Types in the account")
                .required(true))
            .label("Deal Task")
            .required(false))
            .label("Task")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .outputSchema(object()
            .properties(object("body")
                .properties(object("dealTask")
                    .properties(string("id").required(false), string("title").required(false),
                        integer("relid").required(false), date("duedate").required(false),
                        integer("dealTasktype").required(false))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON)));

    private ActiveCampaignCreateTaskAction() {
    }
}
