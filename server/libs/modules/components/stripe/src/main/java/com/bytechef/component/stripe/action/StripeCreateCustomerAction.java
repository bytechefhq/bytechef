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

package com.bytechef.component.stripe.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class StripeCreateCustomerAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createCustomer")
        .title("Create Customer")
        .description("Creates a new customer.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/customers", "bodyContentType", BodyContentType.FORM_URL_ENCODED, "mimeType",
                "application/x-www-form-urlencoded"

            ))
        .properties(string("email").maxLength(512)
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("Email")
            .description("Customer’s email address.")
            .required(false),
            string("name").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Name")
                .description("The customer's full name.")
                .required(false),
            string("description").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Description")
                .required(false),
            string("phone").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Phone")
                .required(false),
            object("address").properties(string("city").label("City")
                .required(false),
                string("country").label("Country")
                    .required(false),
                string("line1").label("Address Line 1")
                    .required(false),
                string("line2").label("Address Line 2")
                    .required(false),
                string("postal_code").label("Postal Code")
                    .required(false),
                string("state").label("State")
                    .description("State, country, province, or region.")
                    .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Address")
                .required(false))
        .output(outputSchema(object().properties(string("id").description("ID of the customer.")
            .required(false),
            string("description").description("Description of the customer.")
                .required(false),
            string("email").description("Email address of the customer.")
                .required(false),
            string("name").description("The customer's full name.")
                .required(false),
            string("phone").description("Phone number of the customer.")
                .required(false),
            object("address").properties(string("city").description("City, district, suburb, town, or village.")
                .required(false),
                string("country").description("Country.")
                    .required(false),
                string("line1").description("Address line 1 (e.g., street, PO Box, or company name).")
                    .required(false),
                string("line2").description("Address line 2 (e.g., apartment, suite, unit, or building).")
                    .required(false),
                string("postal_code").description("ZIP or postal code.")
                    .required(false),
                string("state").description("State, country, province, or region.")
                    .required(false))
                .description("Customer's address.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private StripeCreateCustomerAction() {
    }
}
