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

package com.bytechef.component.woocommerce.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.AFTER;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.BEFORE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CUSTOMER;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ORDER_OUTPUT_PROPERTY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PER_PAGE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.SEARCH;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STATUS;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class WoocommerceSearchOrdersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchOrders")
        .title("Search Orders")
        .description(
            "Search orders, most recent first, optionally filtered by status, creation date, customer and a " +
                "search term.")
        .properties(
            string(STATUS)
                .label("Status")
                .description("Only return orders with this status.")
                .options(
                    option("Pending", "pending"),
                    option("Processing", "processing"),
                    option("On-hold", "on-hold"),
                    option("Completed", "completed"),
                    option("Cancelled", "cancelled"),
                    option("Refunded", "refunded"),
                    option("Failed", "failed"),
                    option("Trash", "trash"))
                .required(false),
            dateTime(AFTER)
                .label("Created After")
                .description("Only return orders created after this date and time, in the site's timezone.")
                .required(false),
            dateTime(BEFORE)
                .label("Created Before")
                .description("Only return orders created before this date and time, in the site's timezone.")
                .required(false),
            integer(CUSTOMER)
                .label("Customer ID")
                .description("Only return orders of the customer with this ID.")
                .required(false),
            string(SEARCH)
                .label("Search")
                .description(
                    "Only return orders matching this term, for example an order number or a customer email.")
                .required(false),
            integer(PER_PAGE)
                .label("Limit")
                .description("Maximum number of orders to return, between 1 and 100. Defaults to 10.")
                .minValue(1)
                .maxValue(100)
                .required(false))
        .output(outputSchema(array().items(ORDER_OUTPUT_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/woocommerce_v1#search-orders")
        .perform(WoocommerceSearchOrdersAction::perform);

    private WoocommerceSearchOrdersAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, List<String>> queryParameters = new HashMap<>();

        String status = inputParameters.getString(STATUS);

        if (status != null) {
            queryParameters.put(STATUS, List.of(status));
        }

        LocalDateTime after = inputParameters.getLocalDateTime(AFTER);

        if (after != null) {
            queryParameters.put(AFTER, List.of(after.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }

        LocalDateTime before = inputParameters.getLocalDateTime(BEFORE);

        if (before != null) {
            queryParameters.put(BEFORE, List.of(before.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }

        Integer customer = inputParameters.getInteger(CUSTOMER);

        if (customer != null) {
            queryParameters.put(CUSTOMER, List.of(String.valueOf(customer)));
        }

        String search = inputParameters.getString(SEARCH);

        if (search != null) {
            queryParameters.put(SEARCH, List.of(search));
        }

        Integer perPage = inputParameters.getInteger(PER_PAGE);

        if (perPage != null) {
            queryParameters.put(PER_PAGE, List.of(String.valueOf(perPage)));
        }

        return context.http(http -> http.get("/orders"))
            .queryParameters(queryParameters)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
