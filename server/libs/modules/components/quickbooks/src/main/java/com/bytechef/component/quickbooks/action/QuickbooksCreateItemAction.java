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
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.BASE_URL;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.COMPANY_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CREATE_ITEM;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.QUANTITY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Mario Cvjetojevic
 * @author Luka Ljubić
 */
public final class QuickbooksCreateItemAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_ITEM)
        .title("Create item")
        .description("Creates a new item.")
        .properties(
            string(NAME)
                .label("Name")
                .description("Name of the item. This value must be unique. Required for create.")
                .maxLength(100)
                .required(true),
            number(QUANTITY)
                .label("Quantity on hand")
                .description(
                    "Current quantity of the Inventory items available for sale. Not used for Service or " +
                        "NonInventory type items.Required for Inventory type items."))
        .outputSchema(
            object()
                .properties(
                    string("id")
                        .label("ID")
                        .required(true),
                    string("name")
                        .label("Name"),
                    string("description")
                        .label("Description"),
                    number("unitPrice")
                        .label("Unit price")))
        .perform(QuickbooksCreateItemAction::perform);

    private QuickbooksCreateItemAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context
            .http(http -> http.post(BASE_URL + "/v3/company/" + getCompanyId(connectionParameters) + "/item"))
            .body(
                Context.Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    QUANTITY, inputParameters.getRequired(QUANTITY)))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});
    }

    private static String getCompanyId(Parameters connectionParameters) {
        String companyId = connectionParameters.getRequiredString(COMPANY_ID);

        return companyId.replace(" ", "");
    }
}
