/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.apollo.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ApolloEnrichPersonAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("enrichPerson")
        .title("Enrich Person")
        .description("Enriches data for a person.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/people/match"

            ))
        .properties(string("first_name").label("First Name")
            .description("The first name of the person.")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            string("last_name").label("Last Name")
                .description("The lst name of the person.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("name").label("Name")
                .description("The full name of the person.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("email").label("Email")
                .description("The email address of the person.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("organization_name").label("Organization Name")
                .description("The name of the person's employer.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("domain").label("Domain")
                .description(
                    "The domain name for the person's employer. This can be the current employer or a previous employer. Do not include www., the @ symbol, or similar.")
                .required(false)
                .exampleValue("apollo.io")
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("linkedin_url").label("LinkedIn URL")
                .description("The URL for the person's LinkedIn profile.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object()
            .properties(object("body")
                .properties(object("person")
                    .properties(string("id").required(false), string("first_name").required(false),
                        string("last_name").required(false), string("name").required(false),
                        string("linkedin_url").required(false), string("title").required(false),
                        string("email_status").required(false), string("photo_url").required(false),
                        string("twitter_url").required(false), string("github_url").required(false),
                        string("facebook_url").required(false), string("headline").required(false),
                        string("email").required(false), string("organization_id").required(false))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ApolloEnrichPersonAction() {
    }
}
