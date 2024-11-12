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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DISPLAY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.FAMILY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.GIVEN_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.MIDDLE_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.SUFFIX;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TITLE;
import static com.bytechef.component.quickbooks.util.QuickbooksUtils.getCompanyId;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Mario Cvjetojevic
 * @author Luka Ljubić
 */
public class QuickbooksCreateCustomerAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCustomer")
        .title("Create Customer")
        .description("Creates a new customer.")
        .properties(
            string(DISPLAY_NAME)
                .label("Display Name")
                .description("The name of the person or organization as displayed.")
                .maxLength(500)
                .required(true),
            string(GIVEN_NAME)
                .label("First Name")
                .description("Given name or first name of a person.")
                .maxLength(100)
                .required(false),
            string(FAMILY_NAME)
                .label("Last Name")
                .description("Family name or the last name of the person.")
                .maxLength(100)
                .required(false),
            string(SUFFIX)
                .label("Suffix")
                .description("Suffix of the name.")
                .exampleValue("Jr")
                .maxLength(16)
                .required(false),
            string(TITLE)
                .label("Title")
                .description("Title of the person.")
                .maxLength(16)
                .required(false),
            string(MIDDLE_NAME)
                .label("Middle Name")
                .description("Middle name of the person.")
                .maxLength(100)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id"),
                        string("contactName"),
                        object("creditChargeInfo")
                            .properties(
                                string("number"),
                                string("nameOnAcct"),
                                integer("ccExpiryMonth"),
                                integer("ccExpiryYear"),
                                string("billAddrStreet"),
                                string("postalCode"),
                                number("amount")),
                        number("balance"),
                        string("acctNum"),
                        string("businessNumber"))))
        .perform(QuickbooksCreateCustomerAction::perform);

    private QuickbooksCreateCustomerAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/v3/company/" + getCompanyId(connectionParameters) + "/customer"))
            .body(
                Http.Body.of(
                    DISPLAY_NAME, inputParameters.getRequiredString(DISPLAY_NAME),
                    SUFFIX, inputParameters.getString(SUFFIX),
                    TITLE, inputParameters.getString(TITLE),
                    MIDDLE_NAME, inputParameters.getString(MIDDLE_NAME),
                    FAMILY_NAME, inputParameters.getString(FAMILY_NAME),
                    GIVEN_NAME, inputParameters.getString(GIVEN_NAME)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
