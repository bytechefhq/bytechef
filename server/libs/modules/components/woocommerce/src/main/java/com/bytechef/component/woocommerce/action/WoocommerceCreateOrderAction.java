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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.Body;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ADDRESS_1;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ADDRESS_2;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.BILLING;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CITY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.COMPANY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.COUNTRY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CUSTOMER_ID;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CUSTOMER_NOTE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.EMAIL;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.FIRST_NAME;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.LAST_NAME;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.LINE_ITEMS;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ORDER_OUTPUT_PROPERTY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PAYMENT_METHOD;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PHONE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.POSTCODE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PRODUCT_ID;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.QUANTITY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.SET_PAID;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.SHIPPING;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STATE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STATUS;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.woocommerce.util.WoocommerceUtils;

/**
 * @author Marija Horvat
 */
public class WoocommerceCreateOrderAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createOrder")
        .title("Create Order")
        .description("Create a new order.")
        .properties(
            string(CUSTOMER_ID)
                .label("Customer Id")
                .description("User ID who owns the order. 0 for guests.")
                .options((OptionsFunction<String>) WoocommerceUtils::getCustomerIdOptions)
                .required(true),
            array(LINE_ITEMS)
                .label("Line Items")
                .description("Line items data.")
                .required(true)
                .items(
                    object()
                        .properties(
                            string(PRODUCT_ID)
                                .label("Product Id")
                                .description("Product ID.")
                                .options((OptionsFunction<String>) WoocommerceUtils::getProductIdOptions),
                            integer(QUANTITY)
                                .label("Quantity")
                                .description("Quantity ordered."))),
            string(STATUS)
                .label("Status")
                .description("Order status.")
                .options(
                    option("Pending", "pending"),
                    option("Processing", "processing"),
                    option("On-hold", "on-hold"),
                    option("Completed", "completed"),
                    option("Cancelled", "cancelled"),
                    option("Refunded", "refunded"),
                    option("Failed", "failed"))
                .required(false),
            string(CUSTOMER_NOTE)
                .label("Customer Note")
                .description("Note left by customer during checkout.")
                .required(false),
            object(BILLING)
                .label("Billing")
                .description("List of billing address data.")
                .required(false)
                .properties(
                    string(FIRST_NAME)
                        .label("First Name")
                        .description("First name."),
                    string(LAST_NAME)
                        .label("Last Name")
                        .description("Last name."),
                    string(COMPANY)
                        .label("Company")
                        .description("Company name."),
                    string(ADDRESS_1)
                        .label("Address 1")
                        .description("Address line 1."),
                    string(ADDRESS_2)
                        .label("Address 2")
                        .description("Address line 2."),
                    string(CITY)
                        .label("City")
                        .description("City name."),
                    string(STATE)
                        .label("State")
                        .description("ISO code or name of the state, province or district."),
                    string(POSTCODE)
                        .label("Postcode")
                        .description("Postal code."),
                    string(COUNTRY)
                        .label("Country")
                        .description("ISO code of the country."),
                    string(EMAIL)
                        .label("Email")
                        .description("Email address."),
                    string(PHONE)
                        .label("Phone")
                        .description("Phone number.")),
            object(SHIPPING)
                .label("Shipping")
                .description("List of shipping address data.")
                .required(false)
                .properties(string(FIRST_NAME)
                    .label("First Name")
                    .description("First name."),
                    string(LAST_NAME)
                        .label("Last Name")
                        .description("Last name."),
                    string(COMPANY)
                        .label("Company")
                        .description("Company name."),
                    string(ADDRESS_1)
                        .label("Address 1")
                        .description("Address line 1."),
                    string(ADDRESS_2)
                        .label("Address 2")
                        .description("Address line 2."),
                    string(CITY)
                        .label("City")
                        .description("City name."),
                    string(STATE)
                        .label("State")
                        .description("ISO code or name of the state, province or district."),
                    string(POSTCODE)
                        .label("Postcode")
                        .description("Postal code."),
                    string(COUNTRY)
                        .label("Country")
                        .description("ISO code of the country."),
                    string(PHONE)
                        .label("Phone")
                        .description("Phone number.")),
            string(PAYMENT_METHOD)
                .label("Payment Method")
                .description("Payment method ID.")
                .options((OptionsFunction<String>) WoocommerceUtils::getPaymentIdOptions)
                .required(false),
            bool(SET_PAID)
                .label("Set Paid")
                .description(
                    "Define if the order is paid. It will set the status to processing and reduce stock items.")
                .required(false))
        .output(outputSchema(ORDER_OUTPUT_PROPERTY))
        .perform(WoocommerceCreateOrderAction::perform);

    private WoocommerceCreateOrderAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters conectionParameters, Context context) {
        return context.http(http -> http.post("/orders"))
            .body(
                Body.of(
                    CUSTOMER_ID, inputParameters.getRequiredString(CUSTOMER_ID),
                    LINE_ITEMS, inputParameters.getRequiredList(LINE_ITEMS),
                    STATUS, inputParameters.getString(STATUS),
                    CUSTOMER_NOTE, inputParameters.getString(CUSTOMER_NOTE),
                    BILLING, inputParameters.getMap(BILLING),
                    SHIPPING, inputParameters.getMap(SHIPPING),
                    PAYMENT_METHOD, inputParameters.getString(PAYMENT_METHOD),
                    SET_PAID, inputParameters.getBoolean(SET_PAID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
