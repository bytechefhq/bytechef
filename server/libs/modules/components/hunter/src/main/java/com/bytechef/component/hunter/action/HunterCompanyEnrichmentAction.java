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

package com.bytechef.component.hunter.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
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
public class HunterCompanyEnrichmentAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("companyEnrichment")
        .title("Company Enrichment")
        .description(
            "Returns all the information associated with a domain name, such as the industry, the description, or headquarters' location.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/companies/find"

            ))
        .properties(string("domain").label("Domain")
            .description("The domain name for which you to find associated information.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)))
        .output(outputSchema(object()
            .properties(object("data").properties(string("id").description("ID of the company.")
                .required(false),
                string("name").description("Name of the company.")
                    .required(false),
                string("legalName").description("Legal name of the company.")
                    .required(false),
                string("domain").description("Domain name of the company.")
                    .required(false),
                string("description").description("Description of the company.")
                    .required(false),
                integer("foundedYear").description("Year the company was founded.")
                    .required(false),
                string("location").description("Location of the company.")
                    .required(false),
                string("timeZone").description("Time zone the company is located in.")
                    .required(false),
                string("logo").description("URL of the company's logo.")
                    .required(false),
                string("emailProvider").description("Email provider the company is using.")
                    .required(false),
                string("phone").description("Phone number of the company.")
                    .required(false),
                object("meta").properties(string("domain").required(false))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private HunterCompanyEnrichmentAction() {
    }
}
