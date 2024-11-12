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
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ACCOUNT;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ACTIVE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ASSET_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DOMAIN;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.EXPENSE_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.FULLY_QUALIFIED_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INCOME_ACCOUNT_REF;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INVENTORY;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INV_START_DATE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ITEM;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.QTY_ON_HAND;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.SERVICE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TYPE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Luka Ljubić
 * @author Monika Kušter
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
            string(TYPE)
                .label("Type")
                .description("Type of item.")
                .options(
                    option(INVENTORY, INVENTORY),
                    option(SERVICE, SERVICE),
                    option("Non-inventory", "NonInventory"))
                .defaultValue(SERVICE)
                .required(true),
            dynamicProperties(ACCOUNT)
                .propertiesLookupDependsOn(TYPE)
                .properties(QuickbooksUtils::addPropertiesForItem),
            string(EXPENSE_ACCOUNT_REF)
                .label("Expense Account")
                .options(QuickbooksUtils.getAccountOptions("Expense"))
                .required(true),
            number(QTY_ON_HAND)
                .label("Quantity on Hand")
                .description("Current quantity of the inventory items available for sale.")
                .displayCondition("%s == '%s'".formatted(TYPE, INVENTORY))
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object(ITEM)
                            .properties(
                                string(DOMAIN),
                                string(ID),
                                string(NAME),
                                string(ACTIVE),
                                string(FULLY_QUALIFIED_NAME),
                                string(TYPE),
                                object(INCOME_ACCOUNT_REF)
                                    .properties(
                                        string("name")),
                                object(ASSET_ACCOUNT_REF)
                                    .properties(
                                        string("name")),
                                object(EXPENSE_ACCOUNT_REF)
                                    .properties(
                                        string("name"))))))
        .perform(QuickbooksCreateItemAction::perform);

    private QuickbooksCreateItemAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, String> account = inputParameters.getMap(ACCOUNT, String.class);

        String incomeAccount = account.get(INCOME_ACCOUNT_REF);
        String assetAccount = account.get(ASSET_ACCOUNT_REF);
        String invStartDate = account.get(INV_START_DATE);
        String expenseAccount = inputParameters.getString(EXPENSE_ACCOUNT_REF);

        return actionContext
            .http(http -> http.post("/item"))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    QTY_ON_HAND, inputParameters.getDouble(QTY_ON_HAND),
                    INCOME_ACCOUNT_REF, incomeAccount == null ? null : Map.of(VALUE, incomeAccount),
                    ASSET_ACCOUNT_REF, assetAccount == null ? null : Map.of(VALUE, assetAccount),
                    INV_START_DATE, invStartDate,
                    EXPENSE_ACCOUNT_REF, expenseAccount == null ? null : Map.of(VALUE, expenseAccount)))
            .configuration(responseType(Http.ResponseType.XML))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
