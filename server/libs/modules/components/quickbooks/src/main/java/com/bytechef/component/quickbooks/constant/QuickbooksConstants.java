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

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Mario Cvjetojevic
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class QuickbooksConstants {

    public static final String ACCOUNT = "Account";
    public static final String ACTIVE = "Active";
    public static final String ASSET_ACCOUNT_REF = "AssetAccountRef";
    public static final String BASE = "base";
    public static final String COMPANY_ID = "companyId";
    public static final String CUSTOMER = "Customer";
    public static final String CUSTOMER_REF = "CustomerRef";
    public static final String DISPLAY_NAME = "DisplayName";
    public static final String DOMAIN = "domain";
    public static final String EXPENSE_ACCOUNT_REF = "ExpenseAccountRef";
    public static final String FAMILY_NAME = "FamilyName";
    public static final String FULLY_QUALIFIED_NAME = "FullyQualifiedName";
    public static final String GIVEN_NAME = "GivenName";
    public static final String ID = "Id";
    public static final String INCOME_ACCOUNT_REF = "IncomeAccountRef";
    public static final String INVENTORY = "Inventory";
    public static final String INVOICE = "Invoice";
    public static final String INV_START_DATE = "InvStartDate";
    public static final String ITEM = "Item";
    public static final String MIDDLE_NAME = "MiddleName";
    public static final String NAME = "Name";
    public static final String PAYMENT = "Payment";
    public static final String QTY_ON_HAND = "QtyOnHand";
    public static final String SERVICE = "Service";
    public static final String SUFFIX = "Suffix";
    public static final String TITLE = "Title";
    public static final String TOTAL_AMT = "TotalAmt";
    public static final String TYPE = "Type";
    public static final String VALUE = "value";

    private QuickbooksConstants() {
    }

    public static final ModifiableObjectProperty CUSTOMER_OUTPUT_PROPERTY = object()
        .properties(
            object(CUSTOMER)
                .properties(
                    string(DOMAIN),
                    string(ID),
                    string(TITLE),
                    string(GIVEN_NAME),
                    string(MIDDLE_NAME),
                    string(FAMILY_NAME),
                    string(SUFFIX),
                    string(FULLY_QUALIFIED_NAME),
                    string(DISPLAY_NAME),
                    string(ACTIVE)));

    public static final ModifiableObjectProperty ITEM_OUTPUT_PROPERTY =
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
                                string("name"))));

    public static final ModifiableObjectProperty PAYMENT_OUTPUT_PROPERTY =
        object()
            .properties(
                object(PAYMENT)
                    .properties(
                        string(DOMAIN),
                        string(ID),
                        object("CurrencyRef")
                            .properties(
                                string("name")),
                        object(CUSTOMER_REF)
                            .properties(
                                string("name")),
                        string(TOTAL_AMT)));
}
