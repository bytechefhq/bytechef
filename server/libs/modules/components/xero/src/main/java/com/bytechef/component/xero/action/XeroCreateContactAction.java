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

package com.bytechef.component.xero.action;

import static com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.xero.constant.XeroConstants.ACCOUNT_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.BANK_ACCOUNT_DETAILS;
import static com.bytechef.component.xero.constant.XeroConstants.COMPANY_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.CREATE_CONTACT;
import static com.bytechef.component.xero.constant.XeroConstants.EMAIL_ADDRESS;
import static com.bytechef.component.xero.constant.XeroConstants.FIRST_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.IS_CUSTOMER;
import static com.bytechef.component.xero.constant.XeroConstants.IS_SUPPLIER;
import static com.bytechef.component.xero.constant.XeroConstants.LAST_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.NAME;
import static com.bytechef.component.xero.constant.XeroConstants.TAX_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.TRACKING_CATEGORY_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.TRACKING_OPTION_NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.LinkedHashMap;

/**
 * @author Mario Cvjetojevic
 */
public final class XeroCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CONTACT)
        .title("Create contact")
        .description("Description")
        .properties(
            string(NAME)
                .label("Name")
                .description(
                    "Full name of a contact or organisation.")
                .maxLength(255)
                .required(true),
            string(CONTACT_NUMBER)
                .label("Contact number")
                .description(
                    "This can be updated via the API only i.e. This field is read only on the Xero contact screen, " +
                        "used to identify contacts in external systems. If the Contact Number is used, this is " +
                        "displayed as Contact Code in the Contacts UI in Xero.")
                .maxLength(50),
            string(ACCOUNT_NUMBER)
                .label("Account number")
                .description(
                    "A user defined account number. This can be updated via the API and the Xero UI.")
                .maxLength(50),
            string(FIRST_NAME)
                .label("First name")
                .description(
                    "First name of contact person.")
                .maxLength(255),
            string(LAST_NAME)
                .label("Last name")
                .description(
                    "Last name of contact person.")
                .maxLength(255),
            string(COMPANY_NUMBER)
                .label("Company Number")
                .description(
                    "Company registration number.")
                .maxLength(50),
            string(EMAIL_ADDRESS)
                .label("Email address")
                .description(
                    "Email address of contact person (umlauts not supported). ")
                .maxLength(255),
            string(BANK_ACCOUNT_DETAILS)
                .label("Bank account details")
                .description(
                    "Bank account number of contact."),
            string(TAX_NUMBER)
                .label("Tax number")
                .description(
                    "Tax number of contact – this is also known as the ABN (Australia), GST Number (New Zealand), " +
                        "VAT Number (UK) or Tax ID Number (US and global) in the Xero UI depending on which " +
                        "regionalized version of Xero you are using.")
                .maxLength(50),
            bool(IS_SUPPLIER)
                .label("Is supplier")
                .description(
                    "Boolean that describes if a contact that has any AP invoices entered against them. Cannot be " +
                        "set via PUT or POST – it is automatically set when an accounts payable invoice is generated " +
                        "against this contact."),
            bool(IS_CUSTOMER)
                .label("Is customer")
                .description(
                    "Boolean that describes if a contact has any AR invoices entered against them. Cannot be set " +
                        "via PUT or POST – it is automatically set when an accounts receivable invoice is generated " +
                        "against this contact."),
            string(TRACKING_CATEGORY_NAME)
                .label("Tracking category name")
                .description(
                    "The name of the Tracking Category assigned to the contact under SalesTrackingCategories and " +
                        "PurchasesTrackingCategories"),
            string(TRACKING_OPTION_NAME)
                .label("Tracking option name")
                .description(
                    "The name of the Tracking Option assigned to the contact under SalesTrackingCategories and " +
                        "PurchasesTrackingCategories"))
        .outputSchema(
            object()
                .properties(
                    string(NAME),
                    string(CONTACT_NUMBER),
                    string(ACCOUNT_NUMBER),
                    string(FIRST_NAME),
                    string(LAST_NAME),
                    string(COMPANY_NUMBER),
                    string(EMAIL_ADDRESS),
                    string(BANK_ACCOUNT_DETAILS),
                    string(TAX_NUMBER),
                    bool(IS_SUPPLIER),
                    bool(IS_CUSTOMER),
                    string(TRACKING_CATEGORY_NAME),
                    string(TRACKING_OPTION_NAME)))
        .perform(XeroCreateContactAction::perform);

    private XeroCreateContactAction() {
    }

    public static LinkedHashMap<String, ?> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post("https://api.xero.com/api.xro/2.0/Contacts"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    CONTACT_NUMBER, inputParameters.getString(CONTACT_NUMBER),
                    FIRST_NAME, inputParameters.getString(FIRST_NAME),
                    LAST_NAME, inputParameters.getString(LAST_NAME),
                    COMPANY_NUMBER, inputParameters.getString(COMPANY_NUMBER),
                    EMAIL_ADDRESS, inputParameters.getString(EMAIL_ADDRESS),
                    BANK_ACCOUNT_DETAILS, inputParameters.getString(BANK_ACCOUNT_DETAILS),
                    TAX_NUMBER, inputParameters.getString(TAX_NUMBER),
                    IS_SUPPLIER, inputParameters.getBoolean(IS_SUPPLIER),
                    IS_CUSTOMER, inputParameters.getBoolean(IS_CUSTOMER),
                    TRACKING_CATEGORY_NAME, inputParameters.getString(TRACKING_CATEGORY_NAME),
                    TRACKING_OPTION_NAME, inputParameters.getString(TRACKING_OPTION_NAME)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
