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

package com.bytechef.component.xero.action;

import static com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.xero.action.XeroCreateInvoiceAction.POST_INVOICES_CONTEXT_FUNCTION;
import static com.bytechef.component.xero.constant.XeroConstants.ACCOUNT_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.ACCPAY;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_ID;
import static com.bytechef.component.xero.constant.XeroConstants.CREATE_BILL;
import static com.bytechef.component.xero.constant.XeroConstants.CURRENCY_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.DATE;
import static com.bytechef.component.xero.constant.XeroConstants.DESCRIPTION;
import static com.bytechef.component.xero.constant.XeroConstants.DUE_DATE;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICE_OUTPUT_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_AMOUNT_TYPE_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEM;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS;
import static com.bytechef.component.xero.constant.XeroConstants.QUANTITY;
import static com.bytechef.component.xero.constant.XeroConstants.REFERENCE;
import static com.bytechef.component.xero.constant.XeroConstants.UNIT_AMOUNT;
import static com.bytechef.component.xero.util.XeroUtils.createInvoice;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.xero.util.XeroUtils;

/**
 * @author Monika Domiter
 */
public class XeroCreateBillAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_BILL)
        .title("Create bill")
        .description("Creates draft bill (Accounts Payable).")
        .properties(
            string(CONTACT_ID)
                .label("Contact")
                .description("Contact to create the bill for.")
                .options((ActionOptionsFunction<String>) XeroUtils::getContactIdOptions)
                .required(true),
            date(DATE)
                .label("Date")
                .description("Date of the bill. If no date is specified, the current date will be used.")
//                .defaultValue(LocalDate.now())
                .required(true),
            date(DUE_DATE)
                .label("Due Date")
                .description("Date bill is due.If no date is specified, the current date will be used. ")
//                .defaultValue(LocalDate.now())
                .required(false),
            LINE_AMOUNT_TYPE_PROPERTY,
            array(LINE_ITEMS)
                .label("Line items")
                .description("Line items on the bill.")
                .minItems(1)
                .items(
                    object(LINE_ITEM)
                        .properties(
                            string(DESCRIPTION)
                                .label(DESCRIPTION)
                                .maxLength(4000)
                                .required(true),
                            number(QUANTITY)
                                .label(QUANTITY)
                                .description("LineItem quantity")
                                .required(false),
                            number(UNIT_AMOUNT)
                                .label("Price")
                                .required(false),
                            string(ACCOUNT_CODE)
                                .label("Account")
                                .options((ActionOptionsFunction<String>) XeroUtils::getAccountCodeOptions)
                                .required(false)))
                .required(true),
            string(CURRENCY_CODE)
                .label("Currency")
                .description("Currency that bill is raised in.")
                .options((ActionOptionsFunction<String>) XeroUtils::getCurrencyCodeOptions)
                .required(false),
            string(REFERENCE)
                .label("Invoice Reference")
                .description("Reference number of the bill.")
                .required(false))
        .output(outputSchema(INVOICE_OUTPUT_PROPERTY))
        .perform(XeroCreateBillAction::perform);

    private XeroCreateBillAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return createInvoice(inputParameters, actionContext, POST_INVOICES_CONTEXT_FUNCTION, ACCPAY);
    }
}
