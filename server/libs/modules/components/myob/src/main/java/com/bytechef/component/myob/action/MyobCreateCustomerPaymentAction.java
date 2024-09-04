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

package com.bytechef.component.myob.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.myob.constant.MyobConstants.ACCOUNT;
import static com.bytechef.component.myob.constant.MyobConstants.COMPANY_FILE;
import static com.bytechef.component.myob.constant.MyobConstants.COMPANY_FILE_PROPERTY;
import static com.bytechef.component.myob.constant.MyobConstants.CUSTOMER;
import static com.bytechef.component.myob.constant.MyobConstants.PAY_FROM;
import static com.bytechef.component.myob.constant.MyobConstants.PAY_FROM_PROPERTY;
import static com.bytechef.component.myob.constant.MyobConstants.UID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.myob.util.MyobUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MyobCreateCustomerPaymentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createCustomerPayment")
        .title("Create Customer Payment")
        .description("Creates a new customer payment.")
        .properties(
            COMPANY_FILE_PROPERTY,
            PAY_FROM_PROPERTY,
            string(ACCOUNT)
                .label("Account")
                .maxLength(36)
                .required(true),
            string(CUSTOMER)
                .label("Customer")
                .options((ActionOptionsFunction<String>) MyobUtils::getCustomerOptions)
                .optionsLookupDependsOn(COMPANY_FILE)
                .required(true))
        .perform(MyobCreateCustomerPaymentAction::perform);

    private MyobCreateCustomerPaymentAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        actionContext
            .http(http -> http.post(inputParameters.getRequiredString(COMPANY_FILE) + "/Sale/CustomerPayment"))
            .body(
                Http.Body.of(
                    PAY_FROM, inputParameters.getRequiredString(PAY_FROM),
                    ACCOUNT, Map.of(UID, inputParameters.getRequiredString(ACCOUNT)),
                    CUSTOMER, Map.of(UID, inputParameters.getRequiredString(CUSTOMER))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }
}
