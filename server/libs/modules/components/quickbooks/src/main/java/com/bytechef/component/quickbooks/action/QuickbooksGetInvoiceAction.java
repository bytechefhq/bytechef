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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CUSTOMER_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DOMAIN;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INVOICE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;

/**
 * @author Monika Kušter
 */
public class QuickbooksGetInvoiceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getInvoice")
        .title("Get Invoice")
        .description("Gets details about a specific invoice.")
        .properties(
            string(INVOICE)
                .label("Invoice")
                .description("Invoice to get.")
                .options(QuickbooksUtils.getOptions(INVOICE, null))
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object(INVOICE)
                            .properties(
                                string(DOMAIN),
                                string(ID),
                                string("DocNumber"),
                                object(CUSTOMER_REF)
                                    .properties(
                                        string("name")),
                                string("Balance")))))
        .perform(QuickbooksGetInvoiceAction::perform);

    private QuickbooksGetInvoiceAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.get("/invoice/" + inputParameters.getRequiredString(INVOICE)))
            .configuration(responseType(Http.ResponseType.XML))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
