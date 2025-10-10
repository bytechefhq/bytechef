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

package com.bytechef.component.zoho.books.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.Body;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.SALES_ORDER_NUMBER;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.SHIPMENT_DATE;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.USE_CUSTOM_SALES_ORDER_NUMBER;
import static com.bytechef.component.zoho.commons.ZohoConstants.CURRENCY_ID;
import static com.bytechef.component.zoho.commons.ZohoConstants.CUSTOMER_ID;
import static com.bytechef.component.zoho.commons.ZohoConstants.DATE;
import static com.bytechef.component.zoho.commons.ZohoConstants.LINE_ITEMS;
import static com.bytechef.component.zoho.commons.ZohoConstants.PAYMENT_TERMS;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.zoho.commons.ZohoUtils;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
public class ZohoBooksCreateSalesOrderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSalesOrder")
        .title("Create Sales Order")
        .description("Create a sales order for your customer.")
        .properties(
            string(CUSTOMER_ID)
                .label("Customer ID")
                .description("ID of the customer the invoice has to be created.")
                .required(true)
                .options((OptionsFunction<String>) ZohoUtils::getCustomersOptions),
            bool(USE_CUSTOM_SALES_ORDER_NUMBER)
                .label("Use Custom Sales Order Number")
                .description(
                    "If true, create custom sales order number, if false, use auto sales order number generation.")
                .defaultValue(false)
                .required(true),
            string(SALES_ORDER_NUMBER)
                .label("Sales Order Number")
                .description("Number of sales order.")
                .displayCondition("%s == true".formatted(USE_CUSTOM_SALES_ORDER_NUMBER))
                .required(true),
            array(LINE_ITEMS)
                .label("Line Items")
                .description("Items in invoice.")
                .required(true)
                .items(
                    object()
                        .properties(
                            string("item_id")
                                .label("Item ID")
                                .description("ID of item.")
                                .options((OptionsFunction<String>) ZohoUtils::getItemsOptions)
                                .required(true),
                            number("quantity")
                                .label("Quantity")
                                .description("Quantity of item.")
                                .required(false))),
            string(CURRENCY_ID)
                .label("Currency ID")
                .description("Currency ID of the customer's currency.")
                .options((OptionsFunction<String>) ZohoUtils::getCurrencyOptions)
                .required(false),
            date(DATE)
                .label("Sales Order Date")
                .description("The date the sales order was created.")
                .required(false),
            date(SHIPMENT_DATE)
                .label("Sales Order Shipment Date")
                .description("Shipping date of sales order.")
                .required(false),
            integer(PAYMENT_TERMS)
                .label("Payment Terms")
                .description(
                    "Payment terms in days e.g. 15, 30, 60. Invoice due date will be calculated based on this.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        number("code")
                            .description(
                                "Zoho Books error code. This will be zero for a success response and non-zero in " +
                                    "case of an error."),
                        string("message")
                            .description("Message for the invoked API."),
                        object("salesorder")
                            .description("Created sales order."))))
        .perform(ZohoBooksCreateSalesOrderAction::perform);

    private ZohoBooksCreateSalesOrderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters conectionParameters, Context context) {
        return context.http(http -> http.post("/salesorders"))
            .queryParameter(
                "ignore_auto_number_generation",
                inputParameters.getRequiredString(USE_CUSTOM_SALES_ORDER_NUMBER))
            .body(
                Body.of(
                    CUSTOMER_ID, inputParameters.getRequiredString(CUSTOMER_ID),
                    SALES_ORDER_NUMBER, inputParameters.getString(SALES_ORDER_NUMBER),
                    CURRENCY_ID, inputParameters.getString(CURRENCY_ID),
                    SHIPMENT_DATE, inputParameters.getString(SHIPMENT_DATE),
                    DATE, inputParameters.getString(DATE),
                    PAYMENT_TERMS, inputParameters.getInteger(PAYMENT_TERMS),
                    LINE_ITEMS, inputParameters.getList(LINE_ITEMS)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
