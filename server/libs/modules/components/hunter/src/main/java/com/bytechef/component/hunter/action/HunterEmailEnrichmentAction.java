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
import static com.bytechef.component.definition.ComponentDsl.number;
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
public class HunterEmailEnrichmentAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("emailEnrichment")
        .title("Email Enrichment")
        .description(
            "Returns all the information associated with an email address, such as a person's name, location and social handles.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/people/find"

            ))
        .properties(string("email").label("Email Address")
            .description("The email address name for which you to find associated information.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)))
        .output(outputSchema(object().properties(object("data").properties(string("id").description("ID of the person.")
            .required(false),
            object("name").properties(string("fullName").description("Full name of the person.")
                .required(false),
                string("givenName").description("First name of the person.")
                    .required(false),
                string("familyName").description("Last name of the person.")
                    .required(false))
                .required(false),
            string("email").description("Email address of the person.")
                .required(false),
            string("location").description("Location of the person.")
                .required(false),
            object("geo").properties(string("city").description("City")
                .required(false),
                string("state").description("State")
                    .required(false),
                integer("stateCode").description("State code.")
                    .required(false),
                string("country").description("Country")
                    .required(false),
                string("countryCode").description("Country code")
                    .required(false),
                number("lat").description("Latitude")
                    .required(false),
                number("lng").description("Longitude")
                    .required(false))
                .description("Geographical information.")
                .required(false),
            object("meta").properties(string("email").description("Email address of the person.")
                .required(false))
                .required(false))
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private HunterEmailEnrichmentAction() {
    }
}
