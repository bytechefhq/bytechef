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

    public static final String ACCOUNT = "account";
    public static final String ACTIVE = "active";
    public static final String ASSET_ACCOUNT_REF = "assetAccountRef";
    public static final String BASE = "base";
    public static final String COMPANY_ID = "companyId";
    public static final String CUSTOMER = "customer";
    public static final String CUSTOMER_REF = "customerRef";
    public static final String DISPLAY_NAME = "displayName";
    public static final String DOMAIN = "domain";
    public static final String EXPENSE_ACCOUNT_REF = "expenseAccountRef";
    public static final String FAMILY_NAME = "familyName";
    public static final String FULLY_QUALIFIED_NAME = "fullyQualifiedName";
    public static final String GIVEN_NAME = "givenName";
    public static final String ID = "id";
    public static final String INCOME_ACCOUNT_REF = "incomeAccountRef";
    public static final String INVOICE = "invoice";
    public static final String INV_START_DATE = "invStartDate";
    public static final String ITEM = "item";
    public static final String MIDDLE_NAME = "middleName";
    public static final String NAME = "name";
    public static final String PAYMENT = "payment";
    public static final String QTY_ON_HAND = "qtyOnHand";
    public static final String SUFFIX = "suffix";
    public static final String TITLE = "title";
    public static final String TOTAL_AMT = "totalAmt";
    public static final String TYPE = "type";
    public static final String VALUE = "value";

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

    private QuickbooksConstants() {
    }
}
