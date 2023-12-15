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

import static com.bytechef.component.quickbooks.action.QuickbooksOutputPropertyDefinitions.CREDIT_CHARGE_INFO_PROPERTIES;
import static com.bytechef.component.quickbooks.action.QuickbooksOutputPropertyDefinitions.PHYSICAL_ADDRESS_PROPERTIES;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CREATECUSTOMER;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DISPLAY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.FAMILY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.GIVEN_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.MIDDLE_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.SUFFIX;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TITLE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;

import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.Parameters;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;

/**
 * @author Mario Cvjetojevic
 */
public final class QuickbooksCreateCustomerAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATECUSTOMER)
        .title("Create customer")
        .description("Creates a new customer.")
        .properties(
            string(DISPLAY_NAME)
                .label("Display name")
                .description(
                    "The name of the person or organization as displayed. " +
                        "Must be unique across all Customer, Vendor, and Employee objects. " +
                        "Cannot be removed with sparse update. If not supplied, the system generates DisplayName by " +
                        "concatenating customer name components supplied in the request from the following list: " +
                        "Title, GivenName, MiddleName, FamilyName, and Suffix.")
                .maxLength(500),
            string(SUFFIX)
                .label("Suffix")
                .description(
                    "Suffix of the name. For example, Jr. The DisplayName attribute or at least one of " +
                        "Title, GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.")
                .maxLength(16),
            string(TITLE)
                .label("Title")
                .description(
                    "Title of the person. This tag supports i18n, all locales. The DisplayName attribute or " +
                        "at least one of Title, GivenName, MiddleName, FamilyName, Suffix, or FullyQualifiedName " +
                        "attributes are required during create.")
                .maxLength(16),
            string(MIDDLE_NAME)
                .label("Middle name")
                .description(
                    "Middle name of the person. The person can have zero or more middle names. " +
                        "The DisplayName attribute or at least one of Title, GivenName, MiddleName, FamilyName, " +
                        "or Suffix attributes is required for object create.")
                .maxLength(100),
            string(FAMILY_NAME)
                .label("Last/Family name")
                .description(
                    "Family name or the last name of the person. The DisplayName attribute or at least one " +
                        "of Title, GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.")
                .maxLength(100),
            string(GIVEN_NAME)
                .label("First/Given name")
                .description(
                    "Given name or first name of a person. The DisplayName attribute or at least one of " +
                        "Title, GivenName, MiddleName, FamilyName, or Suffix attributes is required for object create.")
                .maxLength(100))
        .outputSchema(
            object()
                .properties(
                    string("id")
                        .label("ID")
                        .required(true),
                    object("billAddr")
                        .properties(PHYSICAL_ADDRESS_PROPERTIES)
                        .label("Billing address"),
                    object("shipAddr")
                        .properties(PHYSICAL_ADDRESS_PROPERTIES)
                        .label("Shiping address"),
                    string("contactName")
                        .label("Contact name"),
                    object("creditChargeInfo")
                        .properties(CREDIT_CHARGE_INFO_PROPERTIES)
                        .label("Credit card"),
                    number("balance")
                        .label("Balance"),
                    string("acctNum")
                        .label("Account number"),
                    string("businessNumber")
                        .label("Business number")))
        .description("Has conditionally required parameters.")
        .perform(QuickbooksCreateCustomerAction::perform);

    private QuickbooksCreateCustomerAction() {
    }

    public static Customer perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws FMSException {

        DataService service = QuickbooksUtils.getDataService(connectionParameters.getRequiredString(ACCESS_TOKEN));

        Customer createdCustomer;
        Customer toBeCreatedCustomer = new Customer();

        toBeCreatedCustomer.setDisplayName(inputParameters.getRequiredString(DISPLAY_NAME));
        toBeCreatedCustomer.setSuffix(inputParameters.getRequiredString(SUFFIX));
        toBeCreatedCustomer.setTitle(inputParameters.getRequiredString(TITLE));
        toBeCreatedCustomer.setMiddleName(inputParameters.getRequiredString(MIDDLE_NAME));
        toBeCreatedCustomer.setFamilyName(inputParameters.getRequiredString(FAMILY_NAME));
        toBeCreatedCustomer.setGivenName(inputParameters.getRequiredString(GIVEN_NAME));

        createdCustomer = service.add(toBeCreatedCustomer);

        return createdCustomer;
    }
}
