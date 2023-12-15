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

package com.bytechef.component.quickbooks.constant;

/**
 * @author Mario Cvjetojevic
 */
public final class QuickbooksConstants {

    public static final String QUICKBOOKS = "quickbooks";

    // action input property names

    public static final String DISPLAY_NAME = "displayName";
    public static final String SUFFIX = "suffix";
    public static final String TITLE = "title";
    public static final String MIDDLE_NAME = "middleName";
    public static final String FAMILY_NAME = "familyName";
    public static final String GIVEN_NAME = "givenName";
    public static final String ITEM_NAME = "itemName";
    public static final String QUANTITY_ON_HAND = "quantityOnHand";
    public static final String INCOME_ACCOUNT_ID = "incomeAccountId";
    public static final String ASSET_ACCOUNT_ID = "assetAccountId";
    public static final String EXPENSE_ACCOUNT_ID = "expenseAccountId";
    public static final String CUSTOMER_ID = "customerId";
    public static final String TYPE = "type";
    public static final String INVENTORY_START_DATE = "inventoryStartDate";

    // actions

    public static final String CREATECUSTOMER = "createCustomer";
    public static final String CREATEITEM = "createItem";
    public static final String DOWNLOADCUSTOMERPDF = "downloadCustomerPdf";

    private QuickbooksConstants() {
    }
}
