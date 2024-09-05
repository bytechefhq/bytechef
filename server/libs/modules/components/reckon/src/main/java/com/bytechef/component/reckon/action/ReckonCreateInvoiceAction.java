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

package com.bytechef.component.reckon.action;

import static com.bytechef.component.OpenAPIComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ReckonCreateInvoiceAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createInvoice")
        .title("Create Invoice")
        .description("Create a new Invoice.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/{bookId}/invoices", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("bookId").label("Book")
            .description("Book where new invoice will be created.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("__item").properties(string("customer").label("Customer")
                .description("The customer that is being invoiced.")
                .required(true),
                date("invoiceDate").label("Invoice   Date")
                    .description("The date of the invoice.")
                    .required(true),
                string("amountTaxStatus").label("Amount   Tax   Status")
                    .description("The amount tax status of the amounts in the invoice.")
                    .options(option("NonTaxed", "NonTaxed"), option("Inclusive", "Inclusive"),
                        option("Exclusive", "Exclusive"))
                    .required(true),
                array("lineItems").items(object().properties(integer("lineNumber").label("Line Number")
                    .required(false))
                    .description("The individual items that make up the invoice."))
                    .placeholder("Add to Line Items")
                    .label("Line   Items")
                    .description("The individual items that make up the invoice.")
                    .required(true))
                .label("Invoice")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .output(outputSchema(object().properties(object("body").properties(string("id").required(false))
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ReckonCreateInvoiceAction() {
    }
}
