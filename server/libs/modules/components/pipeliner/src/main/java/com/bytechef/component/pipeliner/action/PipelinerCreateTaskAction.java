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

package com.bytechef.component.pipeliner.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.pipeliner.util.PipelinerUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class PipelinerCreateTaskAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createTask")
        .title("Create Task")
        .description("Creates new Task")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/entities/Tasks", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("subject").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Subject")
            .description("Name of the entity and its default text representation.")
            .required(true),
            string("activity_type_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Activity Type ID")
                .description("Id of the activity type of task.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) PipelinerUtils::getActivityTypeIdOptions),
            string("unit_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Unit ID")
                .description("Sales Unit ID")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) PipelinerUtils::getUnitIdOptions),
            string("owner_id").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Owner ID")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) PipelinerUtils::getOwnerIdOptions))
        .output(outputSchema(object()
            .properties(bool("success").required(false), object("data").properties(
                string("id").description("ID of the task.")
                    .required(false),
                string("subject").description("Name of the entity and its default text representation.")
                    .required(false),
                string("activity_type_id").description("Id of the activity type of task.")
                    .required(false),
                string("unit_id").description("Sales Unit ID.")
                    .required(false),
                string("owner_id").description("ID of the user in Pipeliner Application that is the owner of the task.")
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private PipelinerCreateTaskAction() {
    }
}
