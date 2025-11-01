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

package com.bytechef.component.capsule.crm.action;

import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ABOUT;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ADDRESS;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ADDRESSES;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.CITY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.EMAIL_ADDRESSES;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.FIRST_NAME;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.LAST_NAME;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.NAME;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.NUMBER;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.PHONE_NUMBERS;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.STREET;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.TYPE;
import static com.bytechef.component.capsule.crm.constant.ContactType.ORGANIZATION;
import static com.bytechef.component.capsule.crm.constant.ContactType.PERSON;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.capsule.crm.constant.ContactType;
import com.bytechef.component.capsule.crm.util.CapsuleCRMUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Monika Domiter
 */
public class CapsuleCRMCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new Person or Organization")
        .properties(
            string(TYPE)
                .label("Type")
                .description("Represents if this party is a person or an organisation.")
                .options(
                    option("Person", PERSON.getValue()),
                    option("Organization", ORGANIZATION.getValue()))
                .required(true),
            string(FIRST_NAME)
                .label("First Name")
                .description("The first name of the person.")
                .displayCondition("%s == '%s'".formatted(TYPE, PERSON.getValue()))
                .required(true),
            string(LAST_NAME)
                .label("Last Name")
                .description("The last name of the person.")
                .displayCondition("%s == '%s'".formatted(TYPE, PERSON.getValue()))
                .required(true),
            string(NAME)
                .label("Name")
                .description("The name of the organisation.")
                .displayCondition("%s == '%s'".formatted(TYPE, ORGANIZATION.getValue()))
                .required(true),
            string(ABOUT)
                .label("About")
                .description("A short description of the party.")
                .required(false),
            array(EMAIL_ADDRESSES)
                .label("Email Addresses")
                .description("An array of all the email addresses associated with this party.")
                .items(
                    object()
                        .properties(
                            string(ADDRESS)
                                .label("Email Address")
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
                            string("state")
                                .label("State")
                                .description("The state or province of the address.")
                                .required(false),
                            string("country")
                                .label("Country")
                                .description("The country of the address.")
                                .options((OptionsFunction<String>) CapsuleCRMUtils::getCountryOptions)
                                .required(false),
                            string("zip")
                                .label("Zip")
                                .description("The zip/postal code.")
                                .required(false)))
                .required(false),
            array(PHONE_NUMBERS)
                .label("Phone Numbers")
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
        .output()
        .perform(CapsuleCRMCreateContactAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_PARTIES_CONTEXT_FUNCTION =
        http -> http.post("/parties");

    private CapsuleCRMCreateContactAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ContactType contactType = inputParameters.getRequired(TYPE, ContactType.class);

        return actionContext.http(POST_PARTIES_CONTEXT_FUNCTION)
            .body(
                Http.Body.of(
                    "party",
                    new Object[] {
                        TYPE, contactType.getValue(),
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
