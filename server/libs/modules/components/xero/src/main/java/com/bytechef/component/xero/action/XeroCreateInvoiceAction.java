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
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.xero.constant.XeroConstants.ACCPAY;
import static com.bytechef.component.xero.constant.XeroConstants.ACCREC;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_ID;
import static com.bytechef.component.xero.constant.XeroConstants.CREATE_INVOICE;
import static com.bytechef.component.xero.constant.XeroConstants.DESCRIPTION;
import static com.bytechef.component.xero.constant.XeroConstants.ITEM_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_AMOUNT;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEM;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEM_ID;
import static com.bytechef.component.xero.constant.XeroConstants.QUANTITY;
import static com.bytechef.component.xero.constant.XeroConstants.TAX_AMOUNT;
import static com.bytechef.component.xero.constant.XeroConstants.TAX_TYPE;
import static com.bytechef.component.xero.constant.XeroConstants.TYPE;
import static com.bytechef.component.xero.constant.XeroConstants.UNIT_AMOUNT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 */
public final class XeroCreateInvoiceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_INVOICE)
        .title("Create invoice")
        .description("Description")
        .properties(
            string(TYPE)
                .label("Invoice type")
                .description(
                    "Type of an invoice.")
                .options(
                    option(ACCPAY, ACCPAY),
                    option(ACCREC, ACCREC))
                .required(true),
            string(CONTACT_ID)
                .label("Contact")
                .description(
                    "Full name of a contact or organisation.")
                .options((ActionOptionsFunction<String>) XeroCreateInvoiceAction::getContactOptions)
                .required(true),
            array(LINE_ITEMS)
                .label("Line items")
                .description(
                    "The LineItems collection can contain any number of individual LineItem sub-elements. At least " +
                        "one is required to create a complete Invoice.")
                .required(true)
                .minItems(1)
                .items(
                    object(LINE_ITEM)
                        .properties(
                            string(DESCRIPTION)
                                .label("Description")
                                .description(
                                    "Description needs to be at least 1 char long. A line item with just a description "
                                        +
                                        "(i.e no unit amount or quantity) can be created by specifying just a Description "
                                        +
                                        "element that contains at least 1 character.")
                                .maxLength(4000)
                                .required(true),
                            string(QUANTITY)
                                .label("Quantity")
                                .description("LineItem quantity")
                                .maxLength(13),
                            number(UNIT_AMOUNT)
                                .label("Unit amount")
                                .description(
                                    "Lineitem unit amount. By default, unit amount will be rounded to two decimal " +
                                        "places. You can opt in to use four decimal places by adding the querystring parameter "
                                        +
                                        "unitdp=4 to your query. See the Rounding in Xero guide for more information."),
                            string(ITEM_CODE)
                                .label("Item code")
                                .description(
                                    "If an item is tracked it means Xero tracks the quantity on hand and value of " +
                                        "the item. There are stricter business rules around tracked items to facilitate this "
                                        +
                                        "e.g. you can't create a sales invoice for that item if you don't have sufficient "
                                        +
                                        "quantity on hand."),
                            string(LINE_ITEM_ID)
                                .label("Line item ID")
                                .description(
                                    "The Xero generated identifier for a LineItem. It is recommended that you include "
                                        +
                                        "LineItemIDs on update requests. If LineItemIDs are not included with line items in "
                                        +
                                        "an update request then the line items are deleted and recreated."),
                            string(TAX_TYPE)
                                .label("Tax type")
                                .description(
                                    "Used as an override if the default Tax Code for the selected AccountCode " +
                                        "is not correct"),
                            number(TAX_AMOUNT)
                                .label("Tax amount")
                                .description(
                                    "The tax amount is auto calculated as a percentage of the line amount (see below) based "
                                        +
                                        "on the tax rate. This value can be overriden if the calculated TaxAmount is " +
                                        "not correct."),
                            number(LINE_AMOUNT)
                                .label("Line amount")
                                .description(
                                    "The line amount reflects the discounted price if a DiscountRate has been used i.e "
                                        +
                                        "LineAmount = Quantity * Unit Amount * ((100 – DiscountRate)/100)")
                                .maxValue(9999999999.99))))
        .outputSchema(string())
        .perform(XeroCreateInvoiceAction::perform);

    private XeroCreateInvoiceAction() {
    }

    public static Map<String, ?> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post("https://api.xero.com/api.xro/2.0/Invoices"))
            .body(
                Http.Body.of(
                    TYPE, inputParameters.getRequiredString(TYPE),
                    "Contact", Map.of(CONTACT_ID, inputParameters.getRequiredString(CONTACT_ID)),
                    LINE_ITEMS, inputParameters.getList(LINE_ITEMS)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static List<Option<String>> getContactOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, ?> response = context.http(http -> http.get("https://api.xero.com/api.xro/2.0/Contacts"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<?> contactList = (List<?>) response.get("Contacts");

        return contactList.stream()
            .map(contact -> (Option<String>) option(
                String.valueOf(((Map<?, ?>) contact).get("Name")),
                String.valueOf(((Map<?, ?>) contact).get("ContactID"))))
            .toList();
    }
}
