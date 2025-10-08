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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
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
public class AhaCreateIdeaAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createIdea")
        .title("Create Idea")
        .description("Creates a new idea.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/products/{productId}/ideas", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("productId").label("Product ID")
            .description("Numeric ID or key of the product.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) AhaUtils::getProductIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Name")
                .description("Name of the idea.")
                .required(true),
            string("description").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Description")
                .description("Description of the idea.")
                .required(false))
        .output(outputSchema(object().properties(object("idea").properties(string("id").description("ID of the idea.")
            .required(false),
            string("name").description("Name of the idea.")
                .required(false),
            string("reference_number").description("Reference number of the idea.")
                .required(false),
            integer("score").description("Score of the idea.")
                .required(false),
            dateTime("created_at").description("Date and time when the idea was created.")
                .required(false),
            dateTime("updated_at").description("Timestamp when the idea was last updated.")
                .required(false),
            string("product_id").description("ID of the product to which the idea belongs.")
                .required(false),
            integer("votes").description("Number of votes the idea has received.")
                .required(false),
            integer("initial_votes").description("Initial number of votes the idea had.")
                .required(false),
            dateTime("status_changed_at").description("Timestamp when the status of the idea last changed.")
                .required(false),
            object("workflow_status").properties(string("id").description("ID of the status.")
                .required(false),
                string("name").description("Name of the status.")
                    .required(false),
                integer("position").description("Position of the status in the workflow.")
                    .required(false),
                bool("complete").description("Indicates if the status is marked as complete.")
                    .required(false),
                string("color").description("Color code associated with the status.")
                    .required(false))
                .description("Status of the idea.")
                .required(false),
            object("description").properties(string("id").description("Identifier for the description.")
                .required(false),
                string("body").description("HTML content of the description.")
                    .required(false),
                dateTime("created_at").description("Timestamp when the description was created.")
                    .required(false),
                dateTime("updated_at").description("Timestamp when the description was last updated.")
                    .required(false))
                .description("Description of the idea.")
                .required(false),
            string("visibility").description("Visibility level of the idea.")
                .required(false),
            string("url").description("URL to view the idea.")
                .required(false),
            string("resource").description("API resource URL for the idea.")
                .required(false),
            object("product").properties(string("id").description("ID for the product.")
                .required(false),
                string("reference_prefix").description("Reference prefix for the product.")
                    .required(false),
                string("name").description("Name of the product.")
                    .required(false),
                bool("product_line").description("Indicates if the product is part of a product line.")
                    .required(false),
                dateTime("created_at").description("Timestamp when the product was created.")
                    .required(false),
                string("workspace_type").description("Type of workspace for the product.")
                    .required(false),
                string("url").description("URL to view the product.")
                    .required(false))
                .required(false),
            object("created_by_user").properties(string("id").description("ID of the user who created the idea.")
                .required(false),
                string("name").description("Name of the user who created the idea.")
                    .required(false),
                string("email").description("Email of the user who created the idea.")
                    .required(false),
                dateTime("created_at").description("Timestamp when the user account was created.")
                    .required(false),
                dateTime("updated_at").description("Timestamp when the user account was last updated.")
                    .required(false))
                .required(false),
            integer("endorsements_count").description("Count of endorsements for the idea.")
                .required(false),
            integer("comments_count").description("Count of comments on the idea.")
                .required(false),
            array("workflow_status_times")
                .items(object().properties(string("status_id").description("ID for the status.")
                    .required(false),
                    string("status_name").description("Name of the status.")
                        .required(false),
                    dateTime("started_at").description("Timestamp when the status started.")
                        .required(false),
                    dateTime("ended_at").description("Timestamp when the status ended, if applicable.")
                        .required(false)))
                .required(false),
            string("submitted_idea_portal_record_url").description("URL to the submitted idea portal record.")
                .required(false))
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private AhaCreateIdeaAction() {
    }
}
