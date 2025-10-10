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

package com.bytechef.component.zoho.invoice.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zoho.commons.ZohoConstants.BILLING_ADDRESS;
import static com.bytechef.component.zoho.commons.ZohoConstants.COMPANY_NAME;
import static com.bytechef.component.zoho.commons.ZohoConstants.CONTACT_NAME;
import static com.bytechef.component.zoho.commons.ZohoConstants.CURRENCY_ID;
import static com.bytechef.component.zoho.commons.ZohoConstants.SHIPPING_ADDRESS;
import static com.bytechef.component.zoho.commons.ZohoConstants.WEBSITE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.zoho.commons.ZohoUtils;

/**
 * @author Marija Horvat
 */
public class ZohoInvoiceCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Create a contact.")
        .properties(
            string(CONTACT_NAME)
                .label("Contact Name")
                .description("Name of the contact.")
                .maxLength(200)
                .required(true),
            string(COMPANY_NAME)
                .label("Company Name")
                .description("Name of the contact's company. ")
                .maxLength(200)
                .required(false),
            string(WEBSITE)
                .label("Website")
                .description("Website of the contact.")
                .required(false),
            string(CURRENCY_ID)
                .label("Currency ID")
                .description("Currency ID of the customer's currency.")
                .options((OptionsFunction<String>) ZohoUtils::getCurrencyOptions)
                .required(false),
            object(BILLING_ADDRESS)
                .label("Billing Address")
                .description("Billing address of the contact.")
                .required(false)
                .properties(
                    string("attention"),
                    string("address")
                        .maxLength(500),
                    string("street2")
                        .label("Additional Address"),
                    string("state_code")
                        .label("State Code"),
                    string("city"),
                    string("state"),
                    string("zip"),
                    string("country"),
                    string("fax"),
                    string("phone")),
            object(SHIPPING_ADDRESS)
                .label("Shipping Address")
                .description("Customer's shipping address to which the goods must be delivered.")
                .required(false)
                .properties(
                    string("attention"),
                    string("address")
                        .maxLength(200),
                    string("street2")
                        .label("Additional Address"),
                    string("state_code")
                        .label("State Code"),
                    string("city"),
                    string("state"),
                    string("zip"),
                    string("country"),
                    string("fax"),
                    string("phone")))
        .output(
            outputSchema(
                object()
                    .properties(
                        number("code")
                            .description(
                                "Zoho Invoice error code. This will be zero for a success response and non-zero in " +
                                    "case of an error."),
                        string("message")
                            .description("Message for the invoked API."),
                        object("contact")
                            .description("Created contact."))))
        .perform(ZohoInvoiceCreateContactAction::perform);

    private ZohoInvoiceCreateContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters conectionParameters, Context context) {
        return context.http(http -> http.post("/contacts"))
            .body(
                Body.of(
                    CONTACT_NAME, inputParameters.getRequiredString(CONTACT_NAME),
                    COMPANY_NAME, inputParameters.getString(COMPANY_NAME),
                    WEBSITE, inputParameters.getString(WEBSITE),
                    CURRENCY_ID, inputParameters.getString(CURRENCY_ID),
                    BILLING_ADDRESS, inputParameters.getMap(BILLING_ADDRESS),
                    SHIPPING_ADDRESS, inputParameters.getMap(SHIPPING_ADDRESS)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
