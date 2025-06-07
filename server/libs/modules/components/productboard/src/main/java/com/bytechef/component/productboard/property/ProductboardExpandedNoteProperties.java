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

package com.bytechef.component.productboard.property;

import static com.bytechef.component.definition.ComponentDsl.array;
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
public class ProductboardExpandedNoteProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(string("id").label("Id")
        .description("ID of the note.")
        .required(false),
        string("title").label("Title")
            .description("Title of note.")
            .required(false),
        string("content").label("Content")
            .description("HTML-encoded rich text supported by certain tags; unsupported tags will be stripped out.")
            .required(false),
        string("displayUrl").label("Display Url")
            .description("Note display url leading to Productboard detail page.")
            .required(false),
        string("externalDisplayUrl").label("External Display Url")
            .description("URL in an external system where the note originated.")
            .required(false),
        object("company").properties(string("id").label("Id")
            .description("ID of the company.")
            .required(false))
            .label("Company")
            .required(false),
        object("user").properties(string("id").label("Id")
            .description("ID of the user.")
            .required(false))
            .label("User")
            .required(false),
        object("owner").properties(ProductboardOwnerProperties.PROPERTIES)
            .label("Owner")
            .required(false),
        array("followers").items(object().properties(string("memberId").label("Member Id")
            .description("ID of the member.")
            .required(false),
            string("memberName").label("Member Name")
                .description("Name of the member.")
                .required(false),
            string("memberEmail").label("Member Email")
                .description("Email of the member.")
                .required(false),
            string("teamId").label("Team Id")
                .description("ID of the team.")
                .required(false),
            string("teamName").label("Team Name")
                .description("Name of the team.")
                .required(false))
            .description("The followers of the note."))
            .placeholder("Add to Followers")
            .label("Followers")
            .description("The followers of the note.")
            .required(false),
        string("state").label("State")
            .description("State of the note.")
            .required(false),
        object("source").properties(string("origin").label("Origin")
            .description("A unique string identifying the external system from which the data came.")
            .required(false),
            string("record_id").label("Record Id")
                .description("The unique id of the record in the origin system.")
                .required(false))
            .label("Source")
            .required(false),
        string("tags").label("Tags")
            .description("Comma-separated list of tags.")
            .required(false),
        array("features").items(object().properties(string("id").label("Id")
            .description("ID of the feature.")
            .required(false),
            string("type").label("Type")
                .description("Type of the feature.")
                .required(false),
            integer("importance").label("Importance")
                .description("Importance of the feature.")
                .required(false))
            .description("All features related to a given note."))
            .placeholder("Add to Features")
            .label("Features")
            .description("All features related to a given note.")
            .required(false),
        dateTime("createdAt").label("Created At")
            .description("Date and time when the note was created.")
            .required(false),
        dateTime("updatedAt").label("Updated At")
            .description("Date and time when the note was last updated.")
            .required(false),
        object("createdBy").properties(string("email").label("Email")
            .description("Email of the user who created the note.")
            .required(false),
            string("name").label("Name")
                .description("Name of the user who created the note.")
                .required(false),
            string("uuid").label("Uuid")
                .description("ID of the user who created the note.")
                .required(false))
            .label("Created By")
            .required(false));

    private ProductboardExpandedNoteProperties() {
    }
}
