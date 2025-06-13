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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ADDRESS_1;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ADDRESS_2;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.BILLING;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CITY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.COMPANY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.COUNTRY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CUSTOMER_OUTPUT_PROPERTY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.EMAIL;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.FIRST_NAME;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.LAST_NAME;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PHONE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.POSTCODE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.SHIPPING;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STATE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.USERNAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class WoocommerceCreateCustomerAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCustomer")
        .title("Create Customer")
        .description("Create a new customer.")
        .properties(
            string(EMAIL)
                .label("Email")
                .description("The email address for the customer.")
                .required(true),
            string(FIRST_NAME)
                .label("First Name")
                .description("Customer first name.")
                .required(true),
            string(LAST_NAME)
                .label("Last Name")
                .description("Customer last name.")
                .required(true),
            string(USERNAME)
                .label("Username")
                .description("Customer login name.")
                .required(true),
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
                    string(PHONE)
                        .label("Phone")
                        .description("Phone number.")))
        .output(outputSchema(CUSTOMER_OUTPUT_PROPERTY))
        .perform(WoocommerceCreateCustomerAction::perform);

    private WoocommerceCreateCustomerAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters conectionParameters, Context context) {
        return context.http(http -> http.post("/customers"))
            .body(
                Http.Body.of(
                    EMAIL, inputParameters.getRequiredString(EMAIL),
                    FIRST_NAME, inputParameters.getRequiredString(FIRST_NAME),
                    LAST_NAME, inputParameters.getRequiredString(LAST_NAME),
                    USERNAME, inputParameters.getRequiredString(USERNAME),
                    BILLING, inputParameters.getMap(BILLING),
                    SHIPPING, inputParameters.getMap(SHIPPING)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
