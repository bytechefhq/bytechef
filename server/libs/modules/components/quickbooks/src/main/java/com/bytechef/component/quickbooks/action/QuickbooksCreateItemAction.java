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
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CREATEITEM;
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
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;

import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OptionsDataSource;
import com.bytechef.hermes.component.definition.OptionsDataSource.OptionsResponse;
import com.bytechef.hermes.component.definition.Parameters;
import com.intuit.ipp.data.Account;
import com.intuit.ipp.data.AccountTypeEnum;
import com.intuit.ipp.data.Item;
import com.intuit.ipp.data.ItemTypeEnum;
import com.intuit.ipp.data.ReferenceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author Mario Cvjetojevic
 */
public final class QuickbooksCreateItemAction {
    private static List<Account> allAccountsList;

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATEITEM)
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
                    "Current quantity of the Inventory items available for sale. Not used for Service or" +
                        " NonInventory type items.Required for Inventory type items."),
            string(INCOME_ACCOUNT_ID)
                .label("Income account ID")
                .description("Income account id.")
                .options((OptionsDataSource.ActionOptionsFunction) QuickbooksCreateItemAction::getIncomeAccountIdList),
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
                .options((OptionsDataSource.ActionOptionsFunction) QuickbooksCreateItemAction::getAssetAccountIdList),
            date(INVENTORY_START_DATE)
                .label("Inventory start date")
                .description(
                    "Date of opening balance for the inventory transaction. Required when creating an" +
                        " Item.Type=Inventory. Required for Inventory item types."),
            string(EXPENSE_ACCOUNT_ID)
                .label("Expense account ID")
                .description("Expense account id")
                .options((OptionsDataSource.ActionOptionsFunction) QuickbooksCreateItemAction::getExpenseAccountIdList))
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

        DataService service = QuickbooksUtils.getDataService(connectionParameters.getRequiredString(ACCESS_TOKEN));

        Item createdItem;

        Item toBeCreatedItem = new Item();
        ReferenceType tempReference;

        toBeCreatedItem.setName(inputParameters.getRequiredString(ITEM_NAME));
        toBeCreatedItem.setQtyOnHand(new BigDecimal(inputParameters.getRequiredInteger(QUANTITY_ON_HAND)));

        tempReference = new ReferenceType();
        tempReference.setValue(inputParameters.getRequiredString(INCOME_ACCOUNT_ID));
        toBeCreatedItem.setIncomeAccountRef(tempReference);

        ItemTypeEnum type = switch (inputParameters.getRequiredString(TYPE)) {
            case "inventory" -> ItemTypeEnum.INVENTORY;
            case "service" -> ItemTypeEnum.SERVICE;
            case "nonInventory" -> ItemTypeEnum.NON_INVENTORY;
            default -> throw new IllegalArgumentException(
                "Invalid Quickbooks item type input: " + inputParameters.getRequiredString(TYPE));
        };
        toBeCreatedItem.setType(type);

        tempReference = new ReferenceType();
        tempReference.setValue(inputParameters.getRequiredString(ASSET_ACCOUNT_ID));
        toBeCreatedItem.setAssetAccountRef(tempReference);

        toBeCreatedItem.setInvStartDate(inputParameters.getDate(INVENTORY_START_DATE));

        tempReference = new ReferenceType();
        tempReference.setValue(inputParameters.getRequiredString(EXPENSE_ACCOUNT_ID));
        toBeCreatedItem.setExpenseAccountRef(tempReference);

        toBeCreatedItem.setTrackQtyOnHand(true);

        createdItem = service.add(toBeCreatedItem);

        return createdItem;
    }

    private static OptionsResponse getIncomeAccountIdList(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws FMSException {

        List<ComponentDSL.ModifiableOption<String>> options =
            getAllAccounts(connectionParameters).stream()
                .filter(account -> account.getAccountType()
                    .equals(AccountTypeEnum.INCOME))
                .map(account -> option(account.getName(), account.getId()))
                .toList();

        return new OptionsResponse(options);
    }

    private static OptionsResponse getAssetAccountIdList(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws FMSException {

        List<ComponentDSL.ModifiableOption<String>> options =
            lazyGetAllAccounts(connectionParameters).stream()
                .filter(account -> account.getAccountType()
                    .equals(AccountTypeEnum.OTHER_CURRENT_ASSET))
                .map(account -> option(account.getName(), account.getId()))
                .toList();

        return new OptionsResponse(options);
    }

    private static OptionsResponse getExpenseAccountIdList(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws FMSException {

        List<ComponentDSL.ModifiableOption<String>> options =
            lazyGetAllAccounts(connectionParameters).stream()
                .filter(account -> account.getAccountType()
                    .equals(AccountTypeEnum.COST_OF_GOODS_SOLD))
                .map(account -> option(account.getName(), account.getId()))
                .toList();

        return new OptionsResponse(options);
    }

    private static List<Account> getAllAccounts(Parameters connectionParameters) throws FMSException {
        fetchAllAccounts(connectionParameters);
        return allAccountsList;
    }

    private static List<Account> lazyGetAllAccounts(Parameters connectionParameters) throws FMSException {
        if (allAccountsList == null) {
            fetchAllAccounts(connectionParameters);
        }
        return allAccountsList;
    }

    private static void fetchAllAccounts(Parameters connectionParameters) throws FMSException {
        allAccountsList = (List<Account>) (QuickbooksUtils
            .getDataService(connectionParameters.getRequiredString(ACCESS_TOKEN))
            .executeQuery("select * from Account")
            .getEntities());
    }
}
