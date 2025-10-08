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

package com.bytechef.component.bitbucket.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.bitbucket.property.BitbucketRepositoryObjectProperties;
import com.bytechef.component.bitbucket.util.BitbucketUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class BitbucketCreateRepositoryAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createRepository")
        .title("Create Repository")
        .description("Creates a repository in a selected workspace.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/repositories/{workspace}/{repo_slug}", "bodyContentType", BodyContentType.JSON, "mimeType",
                "application/json"

            ))
        .properties(string("workspace").label("Workspace")
            .description("Workspace in which repository will be created.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) BitbucketUtils::getWorkspaceOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("repo_slug").label("Repository Slug")
                .description("Repository slug that is used as identifier for the repository.")
                .required(true)
                .metadata(
                    Map.of(
                        "type", PropertyType.PATH)),
            string("name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Name")
                .description("The name of the repository.")
                .required(true),
            string("scm").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Source Control Management.")
                .description("Specifies the version control system that your repository will use.")
                .options(option("Git", "git"))
                .required(true),
            object("project").properties(string("key").label("Key")
                .description("The key of the parent project.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<String>) BitbucketUtils::getKeyOptions)
                .optionsLookupDependsOn("workspace"))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Project")
                .description("Parent project of the repository.")
                .required(true),
            bool("is_private").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Is Private")
                .description("Whether the repository is private or not.")
                .required(false),
            string("description").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Description")
                .description("The description of repository.")
                .required(false),
            string("fork_policy").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Fork Policy")
                .description("Specifies the fork policy for the repository.")
                .options(option("Allow_forks", "allow_forks"), option("No_public_forks", "no_public_forks"),
                    option("No_forks", "no_forks"))
                .required(false),
            string("language").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Language")
                .description("Main programming language of the repository")
                .required(false))
        .output(outputSchema(object().properties(BitbucketRepositoryObjectProperties.PROPERTIES)
            .description("Bitbucket repository object returned from the API.")
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private BitbucketCreateRepositoryAction() {
    }
}
