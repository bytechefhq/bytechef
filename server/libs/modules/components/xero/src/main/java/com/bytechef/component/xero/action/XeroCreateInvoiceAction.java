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
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.xero.constant.XeroConstants.ACCREC;
import static com.bytechef.component.xero.constant.XeroConstants.BASE_URL;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_ID;
import static com.bytechef.component.xero.constant.XeroConstants.CREATE_SALES_INVOICE;
import static com.bytechef.component.xero.constant.XeroConstants.CURRENCY_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.DATE;
import static com.bytechef.component.xero.constant.XeroConstants.DUE_DATE;
import static com.bytechef.component.xero.constant.XeroConstants.INVOICE_OUTPUT_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_AMOUNT_TYPE_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS_ACCREC_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.REFERENCE;
import static com.bytechef.component.xero.util.XeroUtils.createInvoice;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.xero.util.XeroUtils;
import java.time.LocalDate;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
public class XeroCreateInvoiceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_SALES_INVOICE)
        .title("Create invoice")
        .description("Creates draft invoice (Acount Receivable).")
        .properties(
            string(CONTACT_ID)
                .label("Contact")
                .description("Contact to create the invoice for.")
                .options((ActionOptionsFunction<String>) XeroUtils::getContactIdOptions)
                .required(true),
            date(DATE)
                .label("Date")
                .description("Date invoice was issued. If no date is specified, the current date will be used.")
                .defaultValue(LocalDate.now())
                .required(false),
            date(DUE_DATE)
                .label("Due Date")
                .description("Date invoice is due. If no date is specified, the current date will be used.")
                .defaultValue(LocalDate.now())
                .required(false),
            LINE_AMOUNT_TYPE_PROPERTY,
            LINE_ITEMS_ACCREC_PROPERTY,
            string(CURRENCY_CODE)
                .label("Currency")
                .description("Currency that invoice is raised in.")
                .options((ActionOptionsFunction<String>) XeroUtils::getCurrencyCodeOptions)
                .required(false),
            string(REFERENCE)
                .label("Invoice Reference")
                .description("Reference number of the invoice.")
                .required(false))
        .outputSchema(INVOICE_OUTPUT_PROPERTY)
        .perform(XeroCreateInvoiceAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_INVOICES_CONTEXT_FUNCTION =
        http -> http.post(BASE_URL + "/Invoices");

    private XeroCreateInvoiceAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return createInvoice(inputParameters, actionContext, POST_INVOICES_CONTEXT_FUNCTION, ACCREC);
    }
}
