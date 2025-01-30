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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.PAYMENT;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.PAYMENT_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.quickbooks.constant.Entity;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;

/**
 * @author Monika Kušter
 */
public class QuickbooksGetPaymentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getPayment")
        .title("Get Payment")
        .description("Gets details about a specific payment.")
        .properties(
            string(PAYMENT)
                .label("Payment ID")
                .description("ID of the payment to get.")
                .options(QuickbooksUtils.getOptions(Entity.PAYMENT, null))
                .required(true))
        .output(outputSchema(PAYMENT_OUTPUT_PROPERTY))
        .perform(QuickbooksGetPaymentAction::perform);

    private QuickbooksGetPaymentAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.get("/payment/" + inputParameters.getRequiredString(PAYMENT)))
            .configuration(responseType(Http.ResponseType.XML))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
