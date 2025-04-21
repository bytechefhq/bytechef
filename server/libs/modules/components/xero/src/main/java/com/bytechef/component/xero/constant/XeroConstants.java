/*
 * Copyright 2025 ByteChef
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

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
public final class XeroConstants {

    public static final String ACCOUNT_NUMBER = "AccountNumber";
    public static final String ACCPAY = "ACCPAY";
    public static final String ACCREC = "ACCREC";
    public static final String ADDRESSES = "Addresses";
    public static final String ADDRESS_TYPE = "AddressType";
    public static final String BANK_ACCOUNT_DETAILS = "BankAccountDetails";
    public static final String BRANDING_THEME_ID = "BrandingThemeID";
    public static final String CITY = "City";
    public static final String CODE = "Code";
    public static final String COMPANY_NUMBER = "CompanyNumber";
    public static final String CONTACT_ID = "ContactID";
    public static final String CONTACT_STATUS = "ContactStatus";
    public static final String CONTACT = "Contact";
    public static final String CONTACTS = "Contacts";
    public static final String COUNTRY = "Country";
    public static final String CURRENCY_CODE = "CurrencyCode";
    public static final String DATE = "Date";
    public static final String DESCRIPTION = "Description";
    public static final String DISCOUNT_RATE = "DiscountRate";
    public static final String DUE_DATE = "DueDate";
    public static final String EMAIL_ADDRESS = "EmailAddress";
    public static final String EXPIRY_DATE = "ExpiryDate";
    public static final String FIRST_NAME = "FirstName";
    public static final String INVOICE = "INVOICE";
    public static final String INVOICES = "Invoices";
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
    public static final String QUOTE_NUMBER = "QuoteNumber";
    public static final String REFERENCE = "Reference";
    public static final String REGION = "Region";
    public static final String STATUS = "Status";
    public static final String SUMMARY = "Summary";
    public static final String TAX_NUMBER = "TaxNumber";
    public static final String TERMS = "Terms";
    public static final String TITLE = "Title";
    public static final String TYPE = "Type";
    public static final String UNIT_AMOUNT = "UnitAmount";
    public static final String WEBHOOK_KEY = "webhookKey";

    public static final ModifiableObjectProperty CONTACT_OUTPUT_PROPERTY = object()
        .properties(
            string(CONTACT_ID)
                .description("ID of the contact."),
            string(COMPANY_NUMBER)
                .description("Company registration number."),
            string(ACCOUNT_NUMBER)
                .description("A user defined account number."),
            string(CONTACT_STATUS)
                .description("Status of the contact."),
            string(NAME)
                .description("Full name of contact/organisation."),
            string(FIRST_NAME)
                .description("First name of contact person."),
            string(LAST_NAME)
                .description("Last name of contact person."),
            string(EMAIL_ADDRESS)
                .description("Email address of contact person."),
            string(BANK_ACCOUNT_DETAILS)
                .description("Bank account number of contact."),
            string(TAX_NUMBER)
                .description("Tax number of contact."),
            array(ADDRESSES)
                .description("List of addresses associated with the contact.")
                .items(
                    object()
                        .properties(
                            string(ADDRESS_TYPE),
                            string(CITY),
                            string(REGION),
                            string(POSTAL_CODE),
                            string(COUNTRY))),
            array(PHONES)
                .items(
                    object()
                        .properties(
                            string(PHONE_TYPE),
                            string(PHONE_NUMBER),
                            string(PHONE_AREA_CODE),
                            string(PHONE_COUNTRY_CODE))));

    public static final ModifiableObjectProperty INVOICE_OUTPUT_PROPERTY = object()
        .properties(
            string(TYPE)
                .description("Type of the invoice."),
            object(CONTACT)
                .description("Contact associated with the invoice.")
                .properties(
                    string(CONTACT_ID)
                        .description("ID of the contact."),
                    string(NAME)
                        .description("Full name of contact/organization."),
                    string(EMAIL_ADDRESS)
                        .description("Email address of contact person")),
            string("DateString")
                .description("Date of the invoice – YYYY-MM-DDThh-mm-ss"),
            string("DueDateString")
                .description("Due date of the invoice – YYYY-MM-DDThh-mm-ss"),
            string(STATUS)
                .description("Status of the invoice."),
            string(LINE_AMOUNT_TYPES)
                .description("Line Amount Type"),
            array(LINE_ITEMS)
                .description("Line items on the invoice.")
                .items(
                    object()
                        .properties(
                            string(DESCRIPTION)
                                .description("The sales description of the item."),
                            integer(QUANTITY)
                                .description("Line item quantity."),
                            number(UNIT_AMOUNT)
                                .description("Line item unit amount."))),
            string(CURRENCY_CODE)
                .description("The currency that invoice has been raised in."));

    public static final ModifiableStringProperty LINE_AMOUNT_TYPE_PROPERTY = string(LINE_AMOUNT_TYPES)
        .label("Line Amount Type")
        .options(
            option("Exclusive", "Exclusive"),
            option("Inclusive", "Inclusive"),
            option("NoTax", "NoTax"))
        .required(false);

    public static final ModifiableArrayProperty LINE_ITEMS_ACCREC_PROPERTY = array(LINE_ITEMS)
        .label("Line Items")
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

    public static final ModifiableStringProperty WEBHOOK_KEY_PROPERTY = string(WEBHOOK_KEY)
        .label("Webhook Key")
        .description("The key used to sign the webhook request.")
        .required(true);

    private XeroConstants() {
    }

}
