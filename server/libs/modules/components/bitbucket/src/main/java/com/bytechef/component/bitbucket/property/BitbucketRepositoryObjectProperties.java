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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
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
public class BitbucketRepositoryObjectProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("type").label("Type")
            .description("The type of the object (usually \"repository\").")
            .required(false),
        object("links").properties(object("self").properties(BitbucketLinkObjectProperties.PROPERTIES)
            .label("Self")
            .description("A hyperlink reference with optional name.")
            .required(false),
            object("html").properties(BitbucketLinkObjectProperties.PROPERTIES)
                .label("Html")
                .description("A hyperlink reference with optional name.")
                .required(false),
            object("avatar").properties(BitbucketLinkObjectProperties.PROPERTIES)
                .label("Avatar")
                .description("A hyperlink reference with optional name.")
                .required(false),
            object("pullrequests").properties(BitbucketLinkObjectProperties.PROPERTIES)
                .label("Pullrequests")
                .description("A hyperlink reference with optional name.")
                .required(false),
            object("commits").properties(BitbucketLinkObjectProperties.PROPERTIES)
                .label("Commits")
                .description("A hyperlink reference with optional name.")
                .required(false),
            object("forks").properties(BitbucketLinkObjectProperties.PROPERTIES)
                .label("Forks")
                .description("A hyperlink reference with optional name.")
                .required(false),
            object("watchers").properties(BitbucketLinkObjectProperties.PROPERTIES)
                .label("Watchers")
                .description("A hyperlink reference with optional name.")
                .required(false),
            object("downloads").properties(BitbucketLinkObjectProperties.PROPERTIES)
                .label("Downloads")
                .description("A hyperlink reference with optional name.")
                .required(false),
            array("clone").items(object().properties(BitbucketLinkObjectProperties.PROPERTIES)
                .description("A hyperlink reference with optional name."))
                .placeholder("Add to Clone")
                .label("Clone")
                .description("List of clone URLs (HTTPS and/or SSH).")
                .required(false),
            object("hooks").properties(BitbucketLinkObjectProperties.PROPERTIES)
                .label("Hooks")
                .description("A hyperlink reference with optional name.")
                .required(false))
            .label("Links")
            .description("A collection of relevant resource links.")
            .required(false),
        string("uuid").label("Uuid")
            .description("The globally unique identifier for the repository.")
            .required(false),
        string("full_name").label("Full Name")
            .description("The full name of the repository (workspace/repo_slug).")
            .required(false),
        bool("is_private").label("Is Private")
            .description("Indicates whether the repository is private.")
            .required(false),
        string("scm").label("Scm")
            .description("The source control system (only \"git\" is supported).")
            .required(false),
        object("owner").properties(string("type").label("Type")
            .description("The type of the owner (usually \"user\" or \"team\").")
            .required(false))
            .label("Owner")
            .description("The user or team that owns the repository.")
            .required(false),
        string("name").label("Name")
            .description("The display name of the repository.")
            .required(false),
        string("description").label("Description")
            .description("A short description of the repository.")
            .required(false),
        string("created_on").label("Created On")
            .description("Timestamp of when the repository was created.")
            .required(false),
        string("updated_on").label("Updated On")
            .description("Timestamp of the last repository update.")
            .required(false),
        integer("size").label("Size")
            .description("Total size of the repository in bytes.")
            .required(false),
        string("language").label("Language")
            .description("The primary programming language of the repository.")
            .required(false),
        bool("has_issues").label("Has Issues")
            .description("Indicates whether the issue tracker is enabled.")
            .required(false),
        bool("has_wiki").label("Has Wiki")
            .description("Indicates whether the wiki is enabled.")
            .required(false),
        string("fork_policy").label("Fork Policy")
            .description("Repository fork policy.")
            .required(false),
        object("project").properties(string("type").label("Type")
            .description("Type of the project object.")
            .required(false))
            .label("Project")
            .description("Project that the repository belongs to.")
            .required(false),
        object("mainbranch").properties(string("type").label("Type")
            .description("Type of the branch object.")
            .required(false))
            .label("Mainbranch")
            .description("The default branch of the repository.")
            .required(false));

    private BitbucketRepositoryObjectProperties() {
    }
}
