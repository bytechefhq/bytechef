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

package com.bytechef.component.quickbooks.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CUSTOMER;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INVOICE_OUTPUT_PROPERTY;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.MAX_RESULTS;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TXN_DATE_FROM;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TXN_DATE_TO;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.UNPAID_ONLY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.quickbooks.constant.Entity;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class QuickbooksSearchInvoicesAction {

    private static final int DEFAULT_MAX_RESULTS = 100;
    private static final int MAX_RESULTS_LIMIT = 1000;

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchInvoices")
        .title("Search Invoices")
        .description(
            "Searches invoices, most recent transaction date first, optionally filtered by transaction date, " +
                "customer and open balance.")
        .properties(
            date(TXN_DATE_FROM)
                .label("Transaction Date From")
                .description("Only return invoices with a transaction date on or after this date.")
                .required(false),
            date(TXN_DATE_TO)
                .label("Transaction Date To")
                .description("Only return invoices with a transaction date on or before this date.")
                .required(false),
            string(CUSTOMER)
                .label("Customer")
                .description("Only return invoices for this customer.")
                .options(QuickbooksUtils.getOptions(Entity.CUSTOMER, null))
                .required(false),
            bool(UNPAID_ONLY)
                .label("Unpaid Only")
                .description("Only return invoices that still have an open balance.")
                .defaultValue(false)
                .required(false),
            integer(MAX_RESULTS)
                .label("Max Results")
                .description("Maximum number of invoices to return, between 1 and 1000.")
                .defaultValue(DEFAULT_MAX_RESULTS)
                .minValue(1)
                .maxValue(MAX_RESULTS_LIMIT)
                .required(false))
        .output(
            outputSchema(
                array()
                    .description("The invoices, most recent transaction date first.")
                    .items(INVOICE_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/quickbooks_v1#search-invoices")
        .perform(QuickbooksSearchInvoicesAction::perform);

    private QuickbooksSearchInvoicesAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, Object> body = actionContext
            .http(http -> http.get("/query"))
            .queryParameter("query", buildQuery(inputParameters))
            .header("Accept", "application/json")
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body != null && body.get("QueryResponse") instanceof Map<?, ?> queryResponse &&
            queryResponse.get("Invoice") instanceof List<?> invoices) {

            return invoices;
        }

        return List.of();
    }

    private static String buildQuery(Parameters inputParameters) {
        List<String> conditions = new ArrayList<>();

        LocalDate txnDateFrom = inputParameters.getLocalDate(TXN_DATE_FROM);

        if (txnDateFrom != null) {
            conditions.add("TxnDate >= '" + txnDateFrom + "'");
        }

        LocalDate txnDateTo = inputParameters.getLocalDate(TXN_DATE_TO);

        if (txnDateTo != null) {
            conditions.add("TxnDate <= '" + txnDateTo + "'");
        }

        String customer = inputParameters.getString(CUSTOMER);

        if (customer != null && !customer.isBlank()) {
            conditions.add("CustomerRef = '" + escape(customer) + "'");
        }

        if (inputParameters.getBoolean(UNPAID_ONLY, false)) {
            conditions.add("Balance > '0'");
        }

        int maxResults = Math.clamp(
            inputParameters.getInteger(MAX_RESULTS, DEFAULT_MAX_RESULTS), 1, MAX_RESULTS_LIMIT);

        StringBuilder query = new StringBuilder("SELECT * FROM Invoice");

        if (!conditions.isEmpty()) {
            query.append(" WHERE ");
            query.append(String.join(" AND ", conditions));
        }

        query.append(" ORDERBY TxnDate DESC MAXRESULTS ");
        query.append(maxResults);

        return query.toString();
    }

    private static String escape(String value) {
        String escapedBackslashes = value.replace("\\", "\\\\");

        return escapedBackslashes.replace("'", "\\'");
    }
}
