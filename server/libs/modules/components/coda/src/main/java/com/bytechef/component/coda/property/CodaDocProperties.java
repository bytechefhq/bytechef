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

package com.bytechef.component.coda.property;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class CodaDocProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(string("id").label("Id")
        .description("ID of the Coda doc.")
        .required(false),
        string("type").label("Type")
            .description("The type of this resource.")
            .required(false),
        string("href").label("Href")
            .description("API link to the Coda doc.")
            .required(false),
        string("browserLink").label("Browser Link")
            .description("Browser-friendly link to the Coda doc.")
            .required(false),
        string("name").label("Name")
            .description("Name of the doc.")
            .required(false),
        string("owner").label("Owner")
            .description("Email address of the doc owner.")
            .required(false),
        string("ownerName").label("Owner Name")
            .description("Name of the doc owner.")
            .required(false),
        string("createdAt").label("Created At")
            .description("Timestamp for when the doc was created.")
            .required(false),
        string("updatedAt").label("Updated At")
            .description("Timestamp for when the doc was last modified.")
            .required(false),
        object("icon").properties(string("name").label("Name")
            .description("Name of the icon.")
            .required(false),
            string("type").label("Type")
                .description("MIME type of the icon.")
                .required(false),
            string("browserLink").label("Browser Link")
                .description("Browser-friendly link to an icon.")
                .required(false))
            .label("Icon")
            .description("Info about the icon.")
            .required(false),
        object("docSize").properties(number("totalRowCount").label("Total Row Count")
            .description("The number of rows contained within all tables of the doc.")
            .required(false),
            number("tableAndViewCount").label("Table And View Count")
                .description("The total number of tables and views contained within the doc.")
                .required(false),
            number("pageCount").label("Page Count")
                .description("The total number of page contained within the doc.")
                .required(false),
            bool("overApiSizeLimit").label("Over Api Size Limit")
                .description("If true, indicates that the doc is over the API size limit.")
                .required(false))
            .label("Doc Size")
            .description("The number of components within a Coda doc.")
            .required(false),
        object("sourceDoc").properties(string("id").label("Id")
            .description("ID of the Coda doc.")
            .required(false),
            string("type").label("Type")
                .description("The type of this resource.")
                .required(false),
            string("href").label("Href")
                .description("API link to the Coda doc.")
                .required(false),
            string("browserLink").label("Browser Link")
                .description("Browser-friendly link to the Coda doc.")
                .required(false))
            .label("Source Doc")
            .description("Reference to a Coda doc from which this doc was copied, if any.")
            .required(false),
        string("workspaceId").label("Workspace Id")
            .description("ID of the Coda workspace containing this doc.")
            .required(false),
        string("folderId").label("Folder Id")
            .description("ID of the Coda folder containing this doc.")
            .required(false),
        object("workspace").properties(string("id").label("Id")
            .description("ID of the Coda workspace.")
            .required(false),
            string("type").label("Type")
                .description("The type of this resource.")
                .required(false),
            string("browserLink").label("Browser Link")
                .description("Browser-friendly link to the Coda workspace.")
                .required(false),
            string("name").label("Name")
                .description("Name of the workspace; included if the user has access to the workspace.")
                .required(false))
            .label("Workspace")
            .description("Reference to a Coda workspace.")
            .required(false),
        object("folder").properties(string("id").label("Id")
            .description("ID of the Coda folder.")
            .required(false),
            string("type").label("Type")
                .description("The type of this resource.")
                .required(false),
            string("browserLink").label("Browser Link")
                .description("Browser-friendly link to the folder.")
                .required(false),
            string("name").label("Name")
                .description("Name of the folder; included if the user has access to the folder.")
                .required(false))
            .label("Folder")
            .description("Reference to a Coda folder.")
            .required(false));

    private CodaDocProperties() {
    }
}
