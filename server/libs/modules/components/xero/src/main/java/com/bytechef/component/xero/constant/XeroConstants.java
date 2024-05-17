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

package com.bytechef.component.xero.constant;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
public final class XeroConstants {

    public static final String ACCOUNT_NUMBER = "AccountNumber";
    public static final String ACCREC = "ACCREC";
    public static final String ADDRESSES = "Addresses";
    public static final String ADDRESS_TYPE = "AddressType";
    public static final String BANK_ACCOUNT_DETAILS = "BankAccountDetails";
    public static final String BASE_URL = "https://api.xero.com/api.xro/2.0";
    public static final String CITY = "City";
    public static final String CODE = "Code";
    public static final String COMPANY_NUMBER = "CompanyNumber";
    public static final String CONTACT_ID = "ContactID";
    public static final String CONTACT_STATUS = "ContactStatus";
    public static final String CONTACT = "Contact";
    public static final String CONTACTS = "Contacts";
    public static final String COUNTRY = "Country";
    public static final String CREATE_CONTACT = "createContact";
    public static final String CREATE_SALES_INVOICE = "createSalesInvoice";
    public static final String CURRENCY_CODE = "CurrencyCode";
    public static final String DATE = "Date";
    public static final String DESCRIPTION = "Description";
    public static final String DISCOUNT_RATE = "DiscountRate";
    public static final String DUE_DATE = "DueDate";
    public static final String EMAIL_ADDRESS = "EmailAddress";
    public static final String FIRST_NAME = "FirstName";
    public static final String LAST_NAME = "LastName";
    public static final String LINE_AMOUNT_TYPES = "LineAmountTypes";
    public static final String LINE_ITEMS = "LineItems";
    public static final String MESSAGE = "Message";
    public static final String NAME = "Name";
    public static final String PHONES = "Phones";
    public static final String PHONE_TYPE = "PhoneType";
    public static final String PHONE_NUMBER = "PhoneNumber";
    public static final String PHONE_AREA_CODE = "PhoneAreaCode";
    public static final String PHONE_COUNTRY_CODE = "PhoneCountryCode";
    public static final String POSTAL_CODE = "PostalCode";
    public static final String QUANTITY = "Quantity";
    public static final String REFERENCE = "Reference";
    public static final String REGION = "Region";
    public static final String STATUS = "Status";
    public static final String TAX_NUMBER = "TaxNumber";
    public static final String TYPE = "Type";
    public static final String UNIT_AMOUNT = "UnitAmount";
    public static final String XERO = "xero";

    public static final ModifiableObjectProperty INVOICE_OUTPUT_PROPERTY = object()
        .properties(
            string(TYPE),
            string(REFERENCE),
            object(CONTACT)
                .properties(
                    string(CONTACT_ID),
                    string(NAME),
                    string(EMAIL_ADDRESS)),
            string("DateString"),
            string("DueDateString"),
            string(STATUS),
            string(LINE_AMOUNT_TYPES),
            array(LINE_ITEMS)
                .items(
                    object()
                        .properties(
                            string(DESCRIPTION),
                            integer(QUANTITY),
                            number(UNIT_AMOUNT))),
            string(CURRENCY_CODE));

    public static final ModifiableStringProperty LINE_AMOUNT_TYPE_PROPERTY = string(LINE_AMOUNT_TYPES)
        .label("Line amount type")
        .options(
            option("Exclusive", "Exclusive"),
            option("Inclusive", "Inclusive"),
            option("NoTax", "NoTax"))
        .required(false);

    public static final ModifiableArrayProperty LINE_ITEMS_ACCREC_PROPERTY = array(LINE_ITEMS)
        .label("Line items")
        .description("Line items on the invoice.")
        .required(true)
        .minItems(1)
        .items(
            object()
                .properties(
                    string(DESCRIPTION)
                        .label(DESCRIPTION)
                        .maxLength(4000)
                        .required(true),
                    integer(QUANTITY)
                        .label(QUANTITY)
                        .description("LineItem quantity")
                        .required(false),
                    number(UNIT_AMOUNT)
                        .label("Price")
                        .required(false),
                    number(DISCOUNT_RATE)
                        .label("Discount (%)")
                        .maxNumberPrecision(2)
                        .required(false)));

    private XeroConstants() {
    }

}
