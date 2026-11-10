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

package com.bytechef.component.xero.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_IDS;
import static com.bytechef.component.xero.constant.XeroConstants.DATE_FROM;
import static com.bytechef.component.xero.constant.XeroConstants.DATE_TO;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICES;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICE_OUTPUT_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.MODIFIED_SINCE;
import static com.bytechef.component.xero.constant.XeroConstants.PAGE;
import static com.bytechef.component.xero.constant.XeroConstants.STATUSES;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.xero.util.XeroUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class XeroListInvoicesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listInvoices")
        .title("List Invoices")
        .description("Lists invoices, optionally filtered by status, contact, date range and modification date.")
        .properties(
            array(STATUSES)
                .label("Statuses")
                .description("Only return invoices with one of these statuses.")
                .items(
                    string()
                        .options(
                            option("Draft", "DRAFT"),
                            option("Submitted", "SUBMITTED"),
                            option("Authorised", "AUTHORISED"),
                            option("Paid", "PAID"),
                            option("Voided", "VOIDED"),
                            option("Deleted", "DELETED")))
                .required(false),
            array(CONTACT_IDS)
                .label("Contact IDs")
                .description("Only return invoices for these contacts.")
                .items(
                    string()
                        .options((OptionsFunction<String>) XeroUtils::getContactIdOptions))
                .required(false),
            date(DATE_FROM)
                .label("Date From")
                .description("Only return invoices dated on or after this date.")
                .required(false),
            date(DATE_TO)
                .label("Date To")
                .description("Only return invoices dated on or before this date.")
                .required(false),
            dateTime(MODIFIED_SINCE)
                .label("Modified Since")
                .description("Only return invoices created or modified since this date and time (UTC).")
                .required(false),
            integer(PAGE)
                .label("Page")
                .description("Page of results to return. Each page contains up to 100 invoices. Defaults to 1.")
                .minValue(1)
                .required(false))
        .output(outputSchema(array().items(INVOICE_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/xero_v1#list-invoices")
        .perform(XeroListInvoicesAction::perform);

    private static final DateTimeFormatter MODIFIED_SINCE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private XeroListInvoicesAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, List<String>> queryParameters = new HashMap<>();

        queryParameters.put(PAGE, List.of(String.valueOf(inputParameters.getInteger(PAGE, 1))));

        List<String> statuses = inputParameters.getList(STATUSES, String.class, List.of());

        if (!statuses.isEmpty()) {
            queryParameters.put(STATUSES, List.of(String.join(",", statuses)));
        }

        List<String> contactIds = inputParameters.getList(CONTACT_IDS, String.class, List.of());

        if (!contactIds.isEmpty()) {
            queryParameters.put(CONTACT_IDS, List.of(String.join(",", contactIds)));
        }

        List<String> whereConditions = new ArrayList<>();

        LocalDate dateFrom = inputParameters.getLocalDate(DATE_FROM);

        if (dateFrom != null) {
            whereConditions.add("Date >= " + toXeroDateTime(dateFrom));
        }

        LocalDate dateTo = inputParameters.getLocalDate(DATE_TO);

        if (dateTo != null) {
            whereConditions.add("Date <= " + toXeroDateTime(dateTo));
        }

        if (!whereConditions.isEmpty()) {
            queryParameters.put("where", List.of(String.join(" && ", whereConditions)));
        }

        Map<String, List<String>> headers = new HashMap<>();

        LocalDateTime modifiedSince = inputParameters.getLocalDateTime(MODIFIED_SINCE);

        if (modifiedSince != null) {
            headers.put("If-Modified-Since", List.of(modifiedSince.format(MODIFIED_SINCE_FORMATTER)));
        }

        Map<String, Object> body = actionContext.http(http -> http.get("/" + INVOICES))
            .queryParameters(queryParameters)
            .headers(headers)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body != null && body.get(INVOICES) instanceof List<?> invoices) {
            return invoices;
        }

        return List.of();
    }

    private static String toXeroDateTime(LocalDate localDate) {
        return String.format(
            "DateTime(%d,%02d,%02d)", localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
    }
}
