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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.QTY_ON_HAND;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

/**
 * @author Mario Cvjetojevic
 * @author Luka Ljubić
 */
public class QuickbooksCreateItemAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createItem")
        .title("Create Item")
        .description("Creates a new item.")
        .properties(
            string(NAME)
                .label("Name")
                .description("Name of the item.")
                .maxLength(100)
                .required(true),
            number(QTY_ON_HAND)
                .label("Quantity on Hand")
                .description("Current quantity of the Inventory items available for sale.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id"),
                        string("name"),
                        string("description"),
                        number("unitPrice"))))
        .perform(QuickbooksCreateItemAction::perform);

    private QuickbooksCreateItemAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext
            .http(http -> http.post("/item"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    QTY_ON_HAND, inputParameters.getRequiredDouble(QTY_ON_HAND)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
