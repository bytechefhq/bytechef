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

package com.bytechef.component.hubspot.property;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class HubspotListProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("listId").label("List Id")
            .description("ID of the list.")
            .required(false),
        integer("listVersion").label("List Version")
            .description("Version of the list.")
            .required(false),
        string("name").label("Name")
            .description("Name of the list.")
            .required(false),
        string("objectTypeId").label("Object Type Id")
            .description("Object type ID of the list.")
            .required(false),
        string("processingStatus").label("Processing Status")
            .description("Current processing status of the list.")
            .required(false),
        string("processingType").label("Processing Type")
            .description("Processing type of the list.")
            .required(false),
        dateTime("createdAt").label("Created At")
            .description("When the list was created.")
            .required(false),
        string("createdById").label("Created By Id")
            .description("ID of the user who created the list.")
            .required(false),
        dateTime("deletedAt").label("Deleted At")
            .description("When the list was deleted.")
            .required(false),
        object("filterBranch").label("Filter Branch")
            .description("Filter branch defining membership criteria.")
            .required(false),
        dateTime("filtersUpdatedAt").label("Filters Updated At")
            .description("When the filters were last updated.")
            .required(false),
        object("listPermissions")
            .properties(array("teamsWithEditAccess").items(integer(null).description("Team IDs with edit access."))
                .placeholder("Add to Teams With Edit Access")
                .label("Teams With Edit Access")
                .description("Team IDs with edit access.")
                .required(false),
                array("usersWithEditAccess").items(integer(null).description("User IDs with edit access."))
                    .placeholder("Add to Users With Edit Access")
                    .label("Users With Edit Access")
                    .description("User IDs with edit access.")
                    .required(false))
            .label("List Permissions")
            .description("Permissions for the list.")
            .required(false),
        object("membershipSettings").properties(bool("includeUnassigned").label("Include Unassigned")
            .description("Whether unassigned records are included.")
            .required(false),
            integer("membershipTeamId").label("Membership Team Id")
                .description("The team ID for membership filtering.")
                .required(false))
            .label("Membership Settings")
            .description("Settings controlling list membership.")
            .required(false),
        integer("size").label("Size")
            .description("Number of members in the list.")
            .required(false),
        dateTime("updatedAt").label("Updated At")
            .description("When the list was last updated.")
            .required(false),
        string("updatedById").label("Updated By Id")
            .description("ID of the user who last updated the list.")
            .required(false));

    private HubspotListProperties() {
    }
}
