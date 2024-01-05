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

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ASSET_ACCOUNT_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CREATE_ITEM;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.EXPENSE_ACCOUNT_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INCOME_ACCOUNT_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.INVENTORY_START_DATE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.ITEM_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.QUANTITY_ON_HAND;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TYPE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.date;
import static com.bytechef.hermes.component.definition.ComponentDSL.number;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableOption;
import com.bytechef.hermes.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.hermes.component.definition.Parameters;
import com.intuit.ipp.data.Account;
import com.intuit.ipp.data.AccountTypeEnum;
import com.intuit.ipp.data.Item;
import com.intuit.ipp.data.ItemTypeEnum;
import com.intuit.ipp.data.ReferenceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Mario Cvjetojevic
 */
public final class QuickbooksCreateItemAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_ITEM)
        .title("Create item")
        .description("Creates a new item.")
        .properties(
            string(ITEM_NAME)
                .label("Name")
                .description("Name of the item. This value must be unique. Required for create.")
                .maxLength(100)
                .required(true),
            string(QUANTITY_ON_HAND)
                .label("Quantity on hand")
                .description(
                    "Current quantity of the Inventory items available for sale. Not used for Service or " +
                        "NonInventory type items.Required for Inventory type items."),
            string(INCOME_ACCOUNT_ID)
                .label("Income account ID")
                .description("Income account id.")
                .options((ActionOptionsFunction) QuickbooksCreateItemAction::getIncomeAccountIdOptions),
            string(TYPE)
                .label("Type")
                .description("Type of item.")
                .options(
                    option("Inventory", "inventory"),
                    option("Service", "service"),
                    option("Non-inventory", "nonInventory")),
            string(ASSET_ACCOUNT_ID)
                .label("Asset account ID")
                .description("Asset account id.")
                .options((ActionOptionsFunction) QuickbooksCreateItemAction::getAssetAccountIdOptions),
            date(INVENTORY_START_DATE)
                .label("Inventory start date")
                .description(
                    "Date of opening balance for the inventory transaction. Required when creating an " +
                        "Item.Type=Inventory. Required for Inventory item types."),
            string(EXPENSE_ACCOUNT_ID)
                .label("Expense account ID")
                .description("Expense account id")
                .options((ActionOptionsFunction) QuickbooksCreateItemAction::getExpenseAccountIdOptions))
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

    public static Item perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws FMSException {

        DataService dataService = QuickbooksUtils.getDataService(connectionParameters);

        Item item = new Item();

        ReferenceType referenceType = new ReferenceType();

        referenceType.setValue(inputParameters.getRequiredString(ASSET_ACCOUNT_ID));

        item.setAssetAccountRef(referenceType);

        referenceType = new ReferenceType();

        referenceType.setValue(inputParameters.getRequiredString(EXPENSE_ACCOUNT_ID));

        item.setExpenseAccountRef(referenceType);

        referenceType = new ReferenceType();

        referenceType.setValue(inputParameters.getRequiredString(INCOME_ACCOUNT_ID));

        item.setIncomeAccountRef(referenceType);

        item.setInvStartDate(inputParameters.getDate(INVENTORY_START_DATE));
        item.setName(inputParameters.getRequiredString(ITEM_NAME));
        item.setQtyOnHand(new BigDecimal(inputParameters.getRequiredInteger(QUANTITY_ON_HAND)));
        item.setTrackQtyOnHand(true);

        item.setType(
            switch (inputParameters.getRequiredString(TYPE)) {
                case "inventory" -> ItemTypeEnum.INVENTORY;
                case "service" -> ItemTypeEnum.SERVICE;
                case "nonInventory" -> ItemTypeEnum.NON_INVENTORY;
                default -> throw new IllegalArgumentException(
                    "Invalid Quickbooks item type input: " + inputParameters.getRequiredString(TYPE));
            });

        return dataService.add(item);
    }

    @SuppressWarnings("unchecked")
    private static List<Account> getAllAccounts(Parameters connectionParameters) throws FMSException {
        DataService dataService = QuickbooksUtils.getDataService(connectionParameters);

        QueryResult queryResult = dataService.executeQuery("select * from Account");

        return (List<Account>) queryResult.getEntities();
    }

    private static List<ModifiableOption<String>> getAssetAccountIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws FMSException {

        return getAllAccounts(connectionParameters)
            .stream()
            .filter(account -> AccountTypeEnum.OTHER_CURRENT_ASSET.equals(account.getAccountType()))
            .map(account -> option(account.getName(), account.getId()))
            .toList();
    }

    private static List<ModifiableOption<String>> getExpenseAccountIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws FMSException {

        return getAllAccounts(connectionParameters)
            .stream()
            .filter(account -> AccountTypeEnum.COST_OF_GOODS_SOLD.equals(account.getAccountType()))
            .map(account -> option(account.getName(), account.getId()))
            .toList();
    }

    private static List<ModifiableOption<String>> getIncomeAccountIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws FMSException {

        return getAllAccounts(connectionParameters)
            .stream()
            .filter(account -> AccountTypeEnum.INCOME.equals(account.getAccountType()))
            .map(account -> option(account.getName(), account.getId()))
            .toList();
    }
}
