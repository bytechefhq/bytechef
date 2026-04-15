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

package com.bytechef.component.hubspot.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.hubspot.property.HubspotListProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class HubspotCreateListAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createList")
        .title("Create List")
        .description("Create a new list.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/crm/lists/2026-03", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("name").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Name")
            .description("The name of the list, which must be globally unique across all public lists in the portal.")
            .required(true),
            string("objectTypeId").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Object Type ID")
                .description(
                    "The object type ID of the type of objects that the list will store. (e.g., 0-1 for contacts).")
                .defaultValue("0-1")
                .required(true),
            string("processingType").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Processing Type")
                .description("The processing type of the list.")
                .options(option("MANUAL", "MANUAL"), option("DYNAMIC", "DYNAMIC"), option("SNAPSHOT", "SNAPSHOT"))
                .required(true),
            integer("listFolderId").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("List Folder ID")
                .description(
                    "The ID of the folder that the list should be created in. If left blank, then the list will be created in the root of the list folder structure.")
                .required(false),
            object("membershipSettings").properties(bool("includeUnassigned").label("Include Unassigned")
                .description("Whether to include unassigned records.")
                .required(false),
                integer("membershipTeamId").label("Membership Team Id")
                    .description("The team ID for membership filtering.")
                    .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Membership Settings")
                .description("Settings controlling list membership.")
                .required(false))
        .output(outputSchema(object().properties(object("list").properties(HubspotListProperties.PROPERTIES)
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private HubspotCreateListAction() {
    }
}
