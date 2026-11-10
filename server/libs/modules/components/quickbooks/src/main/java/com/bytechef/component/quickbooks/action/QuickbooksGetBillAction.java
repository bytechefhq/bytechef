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

package com.bytechef.component.quickbooks.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.BILL;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.BILL_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.quickbooks.constant.Entity;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;

/**
 * @author Ivica Cardic
 */
public class QuickbooksGetBillAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getBill")
        .title("Get Bill")
        .description("Gets details about a specific vendor bill.")
        .properties(
            string(BILL)
                .label("Bill ID")
                .description("ID of the bill to get.")
                .options(QuickbooksUtils.getOptions(Entity.BILL, null))
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(BILL_PROPERTY)))
        .help("", "https://docs.bytechef.io/reference/components/quickbooks_v1#get-bill")
        .perform(QuickbooksGetBillAction::perform);

    private QuickbooksGetBillAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.get("/bill/" + inputParameters.getRequiredString(BILL)))
            .configuration(responseType(Http.ResponseType.XML))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
