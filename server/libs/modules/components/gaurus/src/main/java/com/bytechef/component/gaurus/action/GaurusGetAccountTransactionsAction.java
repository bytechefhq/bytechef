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

package com.bytechef.component.gaurus.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.gaurus.property.GaurusMyResponseProperties;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class GaurusGetAccountTransactionsAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getAccountTransactions")
        .title("Gets transactions for provided IBAN and query parameters.")
        .description(
            "General rules for query parameters: <ul> <li>If lastTransactionid is provided without date parameters system <strong>will not set default values for date range</strong> (only lastTransactionId will be used for data filtering).</li> <li>Max range between dateFrom and dateTo is 7 days. If provided date range is greater, dateFrom is set to the dateTo minus 7 days.</li> <li>If dateFrom and dateTo are not provided: <ul> <li>dateFrom is set to the current date minus 7 days</li> <li>dateTo is set to the current date</li> </ul> </li> <li>If dateFrom is provided and dateTo is not provided: <ol> <li>If dateFrom is set to future date, it is set to the current date.</li> <li>dateTo is set to dateFrom plus 1 day</li> </ol> </li> <li>If dateFrom is not provided and dateTo is provided: <ol> <li>If dateTo is set to future date, it is set to the current date.</li> <li>dateFrom is set to dateTo minus 1 day</li> </ol> </li> </ul>")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/accounts/{iban}/transactions"

            ))
        .properties(string("iban").label("IBAN")
            .description("Account IBAN.")
            .required(true)
            .exampleValue("HR1210010051863000160")
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("dateFrom").label("Date From")
                .description("Oldest transaction execution date.")
                .required(false)
                .exampleValue("2021-01-01")
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("dateTo").label("Date To")
                .description("Newest transaction execution date.")
                .required(false)
                .exampleValue("2021-01-01")
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("lastTransactionId").label("Last Transaction Id")
                .description(
                    "If this parameter is specified, system will return transactions stored after the corresponding transaction.")
                .required(false)
                .exampleValue(123)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object().properties(GaurusMyResponseProperties.PROPERTIES)
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))),
            sampleOutput(Map.<String, Object>ofEntries(Map.entry("code", "OK"), Map.entry("message", ""),
                Map.entry("hasMoreResults", true), Map.entry("data", "TransactionResult[]"))));

    private GaurusGetAccountTransactionsAction() {
    }
}
