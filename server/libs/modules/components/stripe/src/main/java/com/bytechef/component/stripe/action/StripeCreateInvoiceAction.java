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

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.stripe.util.StripeUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class StripeCreateInvoiceAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createInvoice")
        .title("Create Invoice")
        .description("Creates a new invoice.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/invoices", "bodyContentType", BodyContentType.FORM_URL_ENCODED, "mimeType",
                "application/x-www-form-urlencoded"

            ))
        .properties(string("customer").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Customer ID")
            .description("ID of the customer who will be billed.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) StripeUtils::getCustomerOptions),
            string("currency").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Currency")
                .description("Currency used for invoice.")
                .required(true),
            string("description").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Description")
                .description("Description for the invoice.")
                .required(false))
        .output(outputSchema(object().properties(string("id").description("ID of the invoice.")
            .required(false),
            string("customer").description("ID of the customer who will be billed.")
                .required(false),
            string("currency").description("Currency used for invoice.")
                .required(false),
            string("description").description("Description for the invoice.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private StripeCreateInvoiceAction() {
    }
}
