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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.BILL_PROPERTY;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DUE_DATE_FROM;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DUE_DATE_TO;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.MAX_RESULTS;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.OPEN_BALANCE_ONLY;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.VENDOR_ID;

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

/**
 * @author Ivica Cardic
 */
public class QuickbooksSearchBillsAction {

    private static final int DEFAULT_MAX_RESULTS = 100;

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchBills")
        .title("Search Bills")
        .description(
            "Searches vendor bills, optionally filtered by vendor, due date range and open balance.")
        .properties(
            string(VENDOR_ID)
                .label("Vendor ID")
                .description("ID of the vendor whose bills should be returned.")
                .options(QuickbooksUtils.getOptions(Entity.VENDOR, null))
                .required(false),
            date(DUE_DATE_FROM)
                .label("Due Date From")
                .description("Return only bills due on or after this date.")
                .required(false),
            date(DUE_DATE_TO)
                .label("Due Date To")
                .description("Return only bills due on or before this date.")
                .required(false),
            bool(OPEN_BALANCE_ONLY)
                .label("Open Balance Only")
                .description("If true, return only bills with a balance greater than zero.")
                .defaultValue(false)
                .required(false),
            integer(MAX_RESULTS)
                .label("Max Results")
                .description("Maximum number of bills to return.")
                .defaultValue(DEFAULT_MAX_RESULTS)
                .minValue(1)
                .maxValue(1000)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("QueryResponse")
                            .properties(
                                array("Bill")
                                    .items(BILL_PROPERTY),
                                string("startPosition"),
                                string("maxResults")))))
        .help("", "https://docs.bytechef.io/reference/components/quickbooks_v1#search-bills")
        .perform(QuickbooksSearchBillsAction::perform);

    private QuickbooksSearchBillsAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.get("/query"))
            .queryParameter("query", buildQuery(inputParameters))
            .configuration(responseType(Http.ResponseType.XML))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    protected static String buildQuery(Parameters inputParameters) {
        List<String> conditions = new ArrayList<>();

        String vendorId = inputParameters.getString(VENDOR_ID);

        if (vendorId != null && !vendorId.isBlank()) {
            conditions.add("VendorRef = '" + escape(vendorId) + "'");
        }

        LocalDate dueDateFrom = inputParameters.getLocalDate(DUE_DATE_FROM);

        if (dueDateFrom != null) {
            conditions.add("DueDate >= '" + dueDateFrom + "'");
        }

        LocalDate dueDateTo = inputParameters.getLocalDate(DUE_DATE_TO);

        if (dueDateTo != null) {
            conditions.add("DueDate <= '" + dueDateTo + "'");
        }

        if (inputParameters.getBoolean(OPEN_BALANCE_ONLY, false)) {
            conditions.add("Balance > '0'");
        }

        StringBuilder query = new StringBuilder("SELECT * FROM Bill");

        if (!conditions.isEmpty()) {
            query.append(" WHERE ");
            query.append(String.join(" AND ", conditions));
        }

        query.append(" MAXRESULTS ");
        query.append(inputParameters.getInteger(MAX_RESULTS, DEFAULT_MAX_RESULTS));

        return query.toString();
    }

    private static String escape(String value) {
        String escapedBackslashes = value.replace("\\", "\\\\");

        return escapedBackslashes.replace("'", "\\'");
    }
}
