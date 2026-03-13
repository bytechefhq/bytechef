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
import static com.bytechef.component.definition.ComponentDsl.array;
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
public class NiftyAddLabelsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("addLabels")
        .title("Add Labels")
        .description("Add labels to the task.")
        .metadata(
            Map.of(
                "method", "PUT",
                "path", "/tasks/{taskId}/labels", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("taskId").label("Task ID")
            .description("ID of the task to add label to.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) NiftyUtils::getTaskIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            array("labels").items(string().metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .description("List of labels to add to the task."))
                .placeholder("Add to Labels")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Labels")
                .description("List of labels to add to the task.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) NiftyUtils::getLabelsOptions))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("message")
                            .description(
                                "A confirmation message indicating whether the labels where added successfully.")
                            .required(false),
                        array("labels").items(string().description("List of labels added to the task."))
                            .description("List of labels added to the task.")
                            .required(false))
                    .metadata(
                        Map.of(
                            "responseType", ResponseType.JSON))));

    private NiftyAddLabelsAction() {
    }
}
