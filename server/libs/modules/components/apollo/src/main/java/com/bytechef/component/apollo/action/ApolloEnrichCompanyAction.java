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
import static com.bytechef.component.definition.ComponentDsl.array;
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
public class ApolloEnrichCompanyAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("enrichCompany")
        .title("Enrich Company")
        .description("Enriches data for company.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/organizations/enrich"

            ))
        .properties(string("domain").label("Domain")
            .description(
                "The domain of the company that you want to enrich. Do not include www., the @ symbol, or similar.")
            .required(true)
            .exampleValue("apollo.io")
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)))
        .output(outputSchema(object()
            .properties(object("organization").properties(string("id").description("The ID of the company.")
                .required(false),
                string("name").description("The name of the company.")
                    .required(false),
                string("website_url").description("The URL for the company's website.")
                    .required(false),
                string("blog_url").description("The URL for the company's blog.")
                    .required(false),
                string("linkedin_url").description("The URL for the company's LinkedIn profile.")
                    .required(false),
                string("twitter_url").description("The URL for the company's Twitter profile.")
                    .required(false),
                string("facebook_url").description("The URL for the company's Facebook profile.")
                    .required(false),
                string("phone").description("The phone number of the company.")
                    .required(false),
                string("logo_url").description("The URL for the company's logo.")
                    .required(false),
                string("primary_domain").description("The primary domain of the company.")
                    .required(false),
                string("industry").description("The industry of the company.")
                    .required(false),
                array("keywords").items(string().description("Keywords associated with the company."))
                    .description("Keywords associated with the company.")
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ApolloEnrichCompanyAction() {
    }
}
