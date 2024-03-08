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

package com.bytechef.component.capsule.crm.action;

import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ABOUT;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ADDRESS;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ADDRESSES;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.BASE_URL;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.CITY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.COUNTRY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.CREATE_CONTACT;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.EMAIL_ADDRESSES;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.FIRST_NAME;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.LAST_NAME;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.NAME;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.NUMBER;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.PHONE_NUMBERS;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.STATE;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.STREET;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.TYPE;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ZIP;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.dynamicProperties;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.component.capsule.crm.util.CapsuleCRMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class CapsuleCRMCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CONTACT)
        .title("Create Contact")
        .description("Creates a new Person or Organization")
        .properties(
            string(TYPE)
                .label("Type")
                .description("Represents if this party is a person or an organisation.")
                .options(
                    option("Person", "person"),
                    option("Organization", "organization"))
                .required(true),
            dynamicProperties("name")
                .loadPropertiesDependsOn(TYPE)
                .properties(CapsuleCRMUtils::createNameProperties)
                .required(false),
            string(ABOUT)
                .label("About")
                .description("A short description of the party.")
                .required(false),
            array(EMAIL_ADDRESSES)
                .label("Email addresses")
                .description("An array of all the email addresses associated with this party.")
                .items(
                    object()
                        .properties(
                            string(ADDRESS)
                                .label("Email address")
                                .description("The email address string.")
                                .required(true),
                            string(TYPE)
                                .label("Type")
                                .description("The type of the email address")
                                .options(
                                    option("Home", "Home"),
                                    option("Work", "Work"))
                                .required(false)))
                .required(false),
            array(ADDRESSES)
                .label("Addresses")
                .description("An array of all the addresses associated with this party.")
                .items(
                    object()
                        .properties(
                            string(TYPE)
                                .label("Type")
                                .description("The address type.")
                                .options(
                                    option("Home", "Home"),
                                    option("Postal", "Postal"),
                                    option("Office", "Office"),
                                    option("Billing", "Billing"),
                                    option("Shipping", "Shipping"))
                                .required(false),
                            string(STREET)
                                .label("Street")
                                .description("Street address.")
                                .required(false),
                            string(CITY)
                                .label("City")
                                .description("The city of the address.")
                                .required(false),
                            string(STATE)
                                .label("State")
                                .description("The state or province of the address.")
                                .required(false),
                            string(COUNTRY)
                                .label("Country")
                                .description("The country of the address.")
                                .options(
                                    (OptionsDataSource.ActionOptionsFunction<String>) CapsuleCRMUtils::getCountryOptions)
                                .required(false),
                            string(ZIP)
                                .label("Zip")
                                .description("The zip/postal code.")
                                .required(false)))
                .required(false),
            array(PHONE_NUMBERS)
                .label("Phone numbers")
                .description("An array of all the phone numbers associated with this party.")
                .items(
                    object()
                        .properties(
                            string(TYPE)
                                .label("Type")
                                .description("The type of the phone number.")
                                .options(
                                    option("Home", "Home"),
                                    option("Work", "Work"),
                                    option("Mobile", "Mobile"),
                                    option("Fax", "Fax"),
                                    option("Direct", "Direct"))
                                .required(false),
                            string(NUMBER)
                                .label("Number")
                                .description("The actual phone number.")
                                .required(true)))
                .required(false))
        .outputSchema(
            object()
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time()))
        .perform(CapsuleCRMCreateContactAction::perform);

    private CapsuleCRMCreateContactAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/parties"))
            .body(
                Http.Body.of(
                    "party",
                    new Object[] {
                        TYPE, inputParameters.getRequiredString(TYPE),
                        FIRST_NAME, inputParameters.getString(FIRST_NAME),
                        LAST_NAME, inputParameters.getString(LAST_NAME),
                        NAME, inputParameters.getString(NAME),
                        ABOUT, inputParameters.getString(ABOUT),
                        EMAIL_ADDRESSES, inputParameters.getList(EMAIL_ADDRESSES),
                        ADDRESSES, inputParameters.getList(ADDRESSES),
                        PHONE_NUMBERS, inputParameters.getList(PHONE_NUMBERS)
                    }))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
