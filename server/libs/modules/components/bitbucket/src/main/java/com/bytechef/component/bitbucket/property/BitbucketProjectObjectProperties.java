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

package com.bytechef.component.bitbucket.property;

import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class BitbucketProjectObjectProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("type").label("Type")
            .description("The type of the object (typically 'project').")
            .required(false),
        object("links").properties(object("html").properties(BitbucketLinkObjectProperties.PROPERTIES)
            .label("Html")
            .description("A hyperlink reference with optional name.")
            .required(false),
            object("avatar").properties(BitbucketLinkObjectProperties.PROPERTIES)
                .label("Avatar")
                .description("A hyperlink reference with optional name.")
                .required(false))
            .label("Links")
            .description("Relevant links for the project.")
            .required(false),
        string("uuid").label("Uuid")
            .description("Globally unique identifier for the project.")
            .required(false),
        string("key").label("Key")
            .description("Unique key identifying the project within the workspace.")
            .required(false),
        object("owner").properties(string("type").label("Type")
            .description("Type of the owner (usually 'user' or 'workspace').")
            .required(false))
            .label("Owner")
            .description("The workspace or user who owns the project.")
            .required(false),
        string("name").label("Name")
            .description("Human-readable name of the project.")
            .required(false),
        string("description").label("Description")
            .description("Description of the project.")
            .required(false),
        bool("is_private").label("Is Private")
            .description("Indicates whether the project is private.")
            .required(false),
        dateTime("created_on").label("Created On")
            .description("Timestamp of when the project was created.")
            .required(false),
        dateTime("updated_on").label("Updated On")
            .description("Timestamp of the last update to the project.")
            .required(false),
        bool("has_publicly_visible_repos").label("Has Publicly Visible Repos")
            .description("Indicates if the project contains any public repositories.")
            .required(false));

    private BitbucketProjectObjectProperties() {
    }
}
