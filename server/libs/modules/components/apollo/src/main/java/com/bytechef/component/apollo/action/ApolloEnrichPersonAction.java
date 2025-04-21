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
            .properties(object("person").properties(string("id").description("The ID of the person.")
                .required(false),
                string("first_name").description("The first name of the person.")
                    .required(false),
                string("last_name").description("The last name of the person.")
                    .required(false),
                string("name").description("The full name of the person.")
                    .required(false),
                string("linkedin_url").description("The URL for the person's LinkedIn profile.")
                    .required(false),
                string("title").description("The person's job title.")
                    .required(false),
                string("email_status").description("The status of the person's email address.")
                    .required(false),
                string("photo_url").description("The URL for the person's profile photo.")
                    .required(false),
                string("twitter_url").description("The URL for the person's Twitter profile.")
                    .required(false),
                string("github_url").description("The URL for the person's GitHub profile.")
                    .required(false),
                string("facebook_url").description("The URL for the person's Facebook profile.")
                    .required(false),
                string("headline").description("The person's headline or summary.")
                    .required(false),
                string("email").description("The person's email address.")
                    .required(false),
                string("organization_id").description("The ID of the person's employer.")
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ApolloEnrichPersonAction() {
    }
}
