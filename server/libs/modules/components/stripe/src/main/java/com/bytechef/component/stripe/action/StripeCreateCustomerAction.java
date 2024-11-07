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
        .properties(object("__item").properties(string("email").maxLength(512)
            .label("Email")
            .description("Customer’s email address.")
            .required(false),
            string("name").label("Name")
                .description("The customer's full name.")
                .required(false),
            string("description").label("Description")
                .required(false),
            string("phone").label("Phone")
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
                .label("Address")
                .required(false))
            .label("Customer")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY)))
        .output(outputSchema(object()
            .properties(object("body")
                .properties(string("id").required(false), string("description").required(false),
                    string("email").required(false), string("name").required(false), string("phone").required(false),
                    object("address")
                        .properties(string("city").required(false), string("country").required(false),
                            string("line1").required(false), string("line2").required(false),
                            string("postal_code").required(false), string("state").required(false))
                        .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private StripeCreateCustomerAction() {
    }
}
