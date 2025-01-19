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
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CUSTOMER;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CUSTOMER_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.PAYMENT_OUTPUT_PROPERTY;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TOTAL_AMT;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.quickbooks.constant.Entity;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class QuickbooksCreatePaymentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createPayment")
        .title("Create Payment")
        .description("Creates a new payment.")
        .properties(
            string(CUSTOMER)
                .label("Customer")
                .options(QuickbooksUtils.getOptions(Entity.CUSTOMER, null))
                .required(true),
            number(TOTAL_AMT)
                .label("Total Amount")
                .description("Total amount of the transaction.")
                .required(true))
        .output(outputSchema(PAYMENT_OUTPUT_PROPERTY))
        .perform(QuickbooksCreatePaymentAction::perform);

    private QuickbooksCreatePaymentAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/payment"))
            .body(
                Http.Body.of(
                    TOTAL_AMT, inputParameters.getDouble(TOTAL_AMT),
                    CUSTOMER_REF, Map.of(VALUE, inputParameters.getRequiredString(CUSTOMER))))
            .configuration(responseType(Http.ResponseType.XML))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
