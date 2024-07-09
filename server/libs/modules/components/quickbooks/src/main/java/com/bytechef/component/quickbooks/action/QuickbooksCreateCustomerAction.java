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

package com.bytechef.component.quickbooks.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.BASE_URL;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.COMPANY_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CREATE_CUSTOMER;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DISPLAY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.FAMILY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.GIVEN_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.MIDDLE_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.SUFFIX;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TITLE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Mario Cvjetojevic
 * @author Luka Ljubić
 */
public final class QuickbooksCreateCustomerAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CUSTOMER)
        .title("Create customer")
        .description("Creates a new customer.")
        .properties(
            string(DISPLAY_NAME)
                .label("Display name")
                .description(
                    "The name of the person or organization as displayed. Must be unique across all Customer, " +
                        "Vendor, and Employee objects. Cannot be removed with sparse update. If not supplied, " +
                        "the system generates DisplayName by concatenating customer name components supplied in the " +
                        "request from the following list: Title, GivenName, MiddleName, FamilyName, and Suffix.")
                .maxLength(500)
                .required(true),
            string(SUFFIX)
                .label("Suffix")
                .description(
                    "Suffix of the name. For example, Jr. The DisplayName attribute or at least one of Title, " +
                        "GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.")
                .maxLength(16),
            string(TITLE)
                .label("Title")
                .description(
                    "Title of the person. This tag supports i18n, all locales. The DisplayName attribute or at least " +
                        "one of Title, GivenName, MiddleName, FamilyName, Suffix, or FullyQualifiedName attributes " +
                        "are required during create.")
                .maxLength(16),
            string(MIDDLE_NAME)
                .label("Middle name")
                .description(
                    "Middle name of the person. The person can have zero or more middle names. The DisplayName " +
                        "attribute or at least one of Title, GivenName, MiddleName, FamilyName, or Suffix attributes " +
                        "is required for object create.")
                .maxLength(100),
            string(FAMILY_NAME)
                .label("Last/Family name")
                .description(
                    "Family name or the last name of the person. The DisplayName attribute or at least one of Title, " +
                        "GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.")
                .maxLength(100),
            string(GIVEN_NAME)
                .label("First/Given name")
                .description(
                    "Given name or first name of a person. The DisplayName attribute or at least one of Title, " +
                        "GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.")
                .maxLength(100))
        .description("Has conditionally required parameters.")
        .outputSchema(
            object()
                .properties(
                    string("id")
                        .label("ID")
                        .required(true),
                    string("contactName")
                        .label("Contact name"),
                    object("creditChargeInfo")
                        .properties(
                            string("number")
                                .label("Number"),
                            string("nameOnAcct")
                                .label("Name on account"),
                            integer("ccExpiryMonth")
                                .label("Expiry month"),
                            integer("ccExpiryYear")
                                .label("Expiry year"),
                            string("billAddrStreet")
                                .label("Billing address street"),
                            string("postalCode")
                                .label("Postal code"),
                            number("amount")
                                .label("Amount"))
                        .label("Credit card"),
                    number("balance")
                        .label("Balance"),
                    string("acctNum")
                        .label("Account number"),
                    string("businessNumber")
                        .label("Business number")))
        .perform(QuickbooksCreateCustomerAction::perform);

    private QuickbooksCreateCustomerAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context
            .http(http -> http.post(BASE_URL + "/v3/company/" +
                connectionParameters.getRequiredString(COMPANY_ID)
                    .replace(" ", "")
                + "/customer"))
            .body(
                Context.Http.Body.of(
                    DISPLAY_NAME, inputParameters.getRequiredString(DISPLAY_NAME),
                    SUFFIX, inputParameters.getString(SUFFIX),
                    TITLE, inputParameters.getString(TITLE),
                    MIDDLE_NAME, inputParameters.getString(MIDDLE_NAME),
                    FAMILY_NAME, inputParameters.getString(FAMILY_NAME),
                    GIVEN_NAME, inputParameters.getString(GIVEN_NAME)))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});
    }
}
