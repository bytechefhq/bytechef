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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.xero.constant.XeroConstants.ACCOUNT_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.ADDRESSES;
import static com.bytechef.component.xero.constant.XeroConstants.ADDRESS_TYPE;
import static com.bytechef.component.xero.constant.XeroConstants.BANK_ACCOUNT_DETAILS;
import static com.bytechef.component.xero.constant.XeroConstants.BASE_URL;
import static com.bytechef.component.xero.constant.XeroConstants.CITY;
import static com.bytechef.component.xero.constant.XeroConstants.COMPANY_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACTS;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_OUTPUT_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_STATUS;
import static com.bytechef.component.xero.constant.XeroConstants.COUNTRY;
import static com.bytechef.component.xero.constant.XeroConstants.CREATE_CONTACT;
import static com.bytechef.component.xero.constant.XeroConstants.EMAIL_ADDRESS;
import static com.bytechef.component.xero.constant.XeroConstants.FIRST_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.LAST_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.MESSAGE;
import static com.bytechef.component.xero.constant.XeroConstants.NAME;
import static com.bytechef.component.xero.constant.XeroConstants.PHONES;
import static com.bytechef.component.xero.constant.XeroConstants.PHONE_AREA_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.PHONE_COUNTRY_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.PHONE_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.PHONE_TYPE;
import static com.bytechef.component.xero.constant.XeroConstants.POSTAL_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.REGION;
import static com.bytechef.component.xero.constant.XeroConstants.TAX_NUMBER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
public class XeroCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CONTACT)
        .title("Create contact")
        .description("Creates a new contact.")
        .properties(
            string(NAME)
                .label("Name")
                .description("Full name of a contact or organisation.")
                .maxLength(255)
                .required(true),
            string(COMPANY_NUMBER)
                .label("Company Number")
                .description("Company registration number.")
                .maxLength(50)
                .required(false),
            string(ACCOUNT_NUMBER)
                .label("Account number")
                .description("Unique account number to identify, reference and search for the contact.")
                .maxLength(50)
                .required(false),
            string(CONTACT_STATUS)
                .label("Contact status")
                .description("Current status of a contact.")
                .options(
                    option("Active", "ACTIVE", "The Contact is active and can be used in transactions."),
                    option("Archived", "ARCHIVED",
                        "The Contact is archived and can no longer be used in transactions."),
                    option("GDPR erasure request", "GDPRREQUEST",
                        "The Contact is the subject of a GDPR erasure request and can no longer be used in transctions."))
                .required(false),
            string(FIRST_NAME)
                .label("First name")
                .description("First name of primary person.")
                .maxLength(255)
                .required(false),
            string(LAST_NAME)
                .label("Last name")
                .description("Last name of primary person.")
                .maxLength(255)
                .required(false),
            string(EMAIL_ADDRESS)
                .label("Email address")
                .description("Email address of contact person.")
                .controlType(ControlType.EMAIL)
                .maxLength(255)
                .required(false),
            string(BANK_ACCOUNT_DETAILS)
                .label("Bank account number")
                .description("Bank account number of contact.")
                .required(false),
            string(TAX_NUMBER)
                .label("Tax number")
                .description(
                    "Tax number of contact – this is also known as the ABN (Australia), GST Number (New Zealand), " +
                        "VAT Number (UK) or Tax ID Number (US and global) in the Xero UI depending on which " +
                        "regionalized version of Xero you are using.")
                .maxLength(50)
                .required(false),
            array(PHONES)
                .label("Phones")
                .items(
                    object()
                        .properties(
                            string(PHONE_TYPE)
                                .options(
                                    option("Default", "DEFAULT"),
                                    option("Fax", "FAX"),
                                    option("Mobile", "MOBILE"),
                                    option("Direct Dial", "DDI"))
                                .required(true),
                            string(PHONE_NUMBER)
                                .label("Phone Number")
                                .maxLength(50)
                                .required(false),
                            string(PHONE_AREA_CODE)
                                .label("Phone Area Code")
                                .maxLength(10)
                                .required(false),
                            string(PHONE_COUNTRY_CODE)
                                .label("Phone Country Code")
                                .maxLength(20)
                                .required(false)))
                .maxItems(4)
                .required(false),
            array(ADDRESSES)
                .label("Addresses")
                .items(
                    object()
                        .properties(
                            string(ADDRESS_TYPE)
                                .label("Address type")
                                .options(
                                    option("POBOX", "POBOX"),
                                    option("STREET", "STREET"))
                                .required(true),
                            string(CITY)
                                .label("City/Town")
                                .maxLength(255)
                                .required(false),
                            string(REGION)
                                .label("State/Region")
                                .maxLength(255)
                                .required(false),
                            string(POSTAL_CODE)
                                .label("Postal/Zip code")
                                .maxLength(50)
                                .required(false),
                            string(COUNTRY)
                                .label("Country")
                                .maxLength(50)
                                .required(false)))
                .maxItems(2)
                .required(false))
        .outputSchema(CONTACT_OUTPUT_PROPERTY)
        .perform(XeroCreateContactAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_CONTACTS_CONTEXT_FUNCTION =
        http -> http.post(BASE_URL + "/" + CONTACTS);

    private XeroCreateContactAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, Object> body = actionContext.http(POST_CONTACTS_CONTEXT_FUNCTION)
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    COMPANY_NUMBER, inputParameters.getString(COMPANY_NUMBER),
                    ACCOUNT_NUMBER, inputParameters.getString(ACCOUNT_NUMBER),
                    CONTACT_STATUS, inputParameters.getString(CONTACT_STATUS),
                    FIRST_NAME, inputParameters.getString(FIRST_NAME),
                    LAST_NAME, inputParameters.getString(LAST_NAME),
                    EMAIL_ADDRESS, inputParameters.getString(EMAIL_ADDRESS),
                    BANK_ACCOUNT_DETAILS, inputParameters.getString(BANK_ACCOUNT_DETAILS),
                    TAX_NUMBER, inputParameters.getString(TAX_NUMBER),
                    ADDRESSES, inputParameters.getList(ADDRESSES),
                    PHONES, inputParameters.getList(PHONES)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(CONTACTS) instanceof List<?> list) {
            return list.getFirst();
        } else {
            return body.get(MESSAGE);
        }
    }
}
