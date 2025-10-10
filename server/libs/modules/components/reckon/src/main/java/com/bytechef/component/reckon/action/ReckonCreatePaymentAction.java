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
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
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
public class ReckonCreatePaymentAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createPayment")
        .title("Create Payment")
        .description("Creates a new payment.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/{bookId}/payments", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("bookId").label("Book ID")
            .description("ID of the book where new payment will be created.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) ReckonUtils::getBookIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("supplier").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Supplier")
                .description("The supplier that is being paid.")
                .required(true)
                .options((ActionDefinition.OptionsFunction<String>) ReckonUtils::getSupplierOptions),
            date("paymentDate").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Payment Date")
                .description("The date of the payment.")
                .required(true),
            number("totalAmount").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Total Amount")
                .description("The total amount of the payment applied.")
                .required(true))
        .output(outputSchema(object().properties(string("id").required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ReckonCreatePaymentAction() {
    }
}
