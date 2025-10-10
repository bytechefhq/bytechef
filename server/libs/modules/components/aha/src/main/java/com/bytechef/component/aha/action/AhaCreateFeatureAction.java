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

package com.bytechef.component.aha.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.aha.util.AhaUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class AhaCreateFeatureAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createFeature")
        .title("Create Feature")
        .description("Creates a new feature.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/releases/{releaseId}/features", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("releaseId").label("Release ID")
            .description("Numeric ID or key of the release.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) AhaUtils::getReleaseIdOptions)
            .optionsLookupDependsOn("productId")
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Name")
                .description("Name of the feature.")
                .required(true))
        .output(outputSchema(object()
            .properties(object("feature").properties(string("id").description("ID of the feature.")
                .required(false),
                string("name").description("Name of the feature.")
                    .required(false),
                string("reference_num").description("Reference number for the feature.")
                    .required(false),
                string("release_reference_num").description("Reference number for the associated release.")
                    .required(false),
                integer("position").description("Position of the feature in the list.")
                    .required(false),
                integer("score").description("Score assigned to the feature.")
                    .required(false),
                dateTime("created_at").description("Timestamp when the feature was created.")
                    .required(false),
                dateTime("updated_at").description("Timestamp when the feature was last updated.")
                    .required(false),
                string("product_id").description("ID of the associated product.")
                    .required(false),
                integer("progress").description("Progress completed on the feature.")
                    .required(false),
                string("progress_source").description("Source of the progress information.")
                    .required(false),
                date("status_changed_on").description("Date when the status of the feature last changed.")
                    .required(false),
                object("created_by_user").properties(string("id").description("ID of the user who created the feature.")
                    .required(false),
                    string("name").description("Name of the user who created the feature.")
                        .required(false),
                    string("email").description("Email of the user who created the feature.")
                        .required(false),
                    dateTime("created_at").description("Timestamp when the user account was created.")
                        .required(false),
                    dateTime("updated_at").description("Timestamp when the user account was last updated.")
                        .required(false))
                    .required(false),
                object("workflow_kind").properties(string("id").description("Identifier for the workflow kind.")
                    .required(false),
                    string("name").description("Name of the workflow kind.")
                        .required(false))
                    .required(false),
                object("workflow_status").properties(string("id").description("ID of the status.")
                    .required(false),
                    string("name").description("Name of the status.")
                        .required(false),
                    integer("position").description("Position of the status.")
                        .required(false),
                    bool("complete").description("Indicates if the status is marked as complete.")
                        .required(false),
                    string("color").description("Color code associated with the status.")
                        .required(false))
                    .description("Status of the feature.")
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private AhaCreateFeatureAction() {
    }
}
