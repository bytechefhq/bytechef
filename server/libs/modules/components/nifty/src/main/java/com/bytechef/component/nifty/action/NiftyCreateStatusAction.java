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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
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
public class NiftyCreateStatusAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createStatus")
        .title("Create Status")
        .description("Creates new status")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/taskgroups", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("name").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Name")
            .description("Name of the status.")
            .required(true),
            string("project_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Project ID")
                .description("Project ID that the status belongs to.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) NiftyUtils::getProjectIdOptions))
        .output(outputSchema(object()
            .properties(string("message").required(false),
                object("task_group").properties(string("id").description("ID of the status.")
                    .required(false),
                    string("name").description("Name of the status.")
                        .required(false),
                    string("color").description("Color of the status.")
                        .required(false),
                    string("created_by").description("ID of the user that created the status.")
                        .required(false),
                    string("project").description("ID of the project the status belongs to.")
                        .required(false),
                    integer("order").description("Order of the status in the project.")
                        .required(false))
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private NiftyCreateStatusAction() {
    }
}
