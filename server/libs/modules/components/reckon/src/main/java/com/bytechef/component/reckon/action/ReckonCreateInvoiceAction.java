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

package com.bytechef.component.reckon.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.reckon.util.ReckonUtils;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ReckonCreateInvoiceAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createInvoice")
        .title("Create Invoice")
        .description("Creates a new invoice.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/{bookId}/invoices", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("bookId").label("Book ID")
            .description("ID of the book where new invoice will be created.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) ReckonUtils::getBookIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("customer").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Customer")
                .description("The customer that is being invoiced.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) ReckonUtils::getCustomerOptions),
            date("invoiceDate").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Invoice Date")
                .description("The date of the invoice.")
                .required(true),
            string("amountTaxStatus").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Amount Tax Status")
                .description("The amount tax status of the amounts in the invoice.")
                .options(option("NonTaxed", "NonTaxed"), option("Inclusive", "Inclusive"),
                    option("Exclusive", "Exclusive"))
                .required(true),
            array("lineItems").items(object().properties(integer("lineNumber").label("Line Number")
                .required(false))
                .description("The individual items that make up the invoice."))
                .placeholder("Add to Line Items")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Line Items")
                .description("The individual items that make up the invoice.")
                .required(true))
        .output(outputSchema(object().properties(string("id").required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ReckonCreateInvoiceAction() {
    }
}
