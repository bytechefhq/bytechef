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

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
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
public class ReckonCreatePaymentAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("createPayment")
        .title("Create Payment")
        .description("Creates a new payment.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/{bookId}/payments", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("bookId").label("Book")
            .description("Book where new payment will be created.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            object("__item").properties(string("supplier").label("Supplier")
                .description("The supplier that is being paid.")
                .required(true),
                date("paymentDate").label("Payment Date")
                    .description("The date of the payment.")
                    .required(true),
                number("totalAmount").label("Total Amount")
                    .description("The total amount of the payment applied.")
                    .required(true))
                .label("Payment")
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY)))
        .output(outputSchema(object().properties(object("body").properties(string("id").required(false))
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ReckonCreatePaymentAction() {
    }
}
