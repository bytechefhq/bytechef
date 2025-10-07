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

package com.bytechef.component.xero.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.xero.constant.XeroConstants.BRANDING_THEME_ID;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_ID;
import static com.bytechef.component.xero.constant.XeroConstants.CURRENCY_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.DATE;
import static com.bytechef.component.xero.constant.XeroConstants.DESCRIPTION;
import static com.bytechef.component.xero.constant.XeroConstants.DISCOUNT_RATE;
import static com.bytechef.component.xero.constant.XeroConstants.EMAIL_ADDRESS;
import static com.bytechef.component.xero.constant.XeroConstants.EXPIRY_DATE;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_AMOUNT_TYPES;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_AMOUNT_TYPE_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS_ACCREC_PROPERTY;
import static com.bytechef.component.xero.constant.XeroConstants.NAME;
import static com.bytechef.component.xero.constant.XeroConstants.QUANTITY;
import static com.bytechef.component.xero.constant.XeroConstants.QUOTE_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.REFERENCE;
import static com.bytechef.component.xero.constant.XeroConstants.STATUS;
import static com.bytechef.component.xero.constant.XeroConstants.SUMMARY;
import static com.bytechef.component.xero.constant.XeroConstants.TERMS;
import static com.bytechef.component.xero.constant.XeroConstants.TITLE;
import static com.bytechef.component.xero.constant.XeroConstants.UNIT_AMOUNT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.xero.util.XeroUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class XeroCreateQuoteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createQuote")
        .title("Create Quote")
        .description("Creates a new quote draft.")
        .properties(
            string(CONTACT_ID)
                .label("Contact ID")
                .description("ID of the contact that the quote is being raised for.")
                .options((OptionsFunction<String>) XeroUtils::getContactIdOptions)
                .required(true),
            date(DATE)
                .label("Date")
                .description("Date quote was issued.")
                .required(true),
            LINE_ITEMS_ACCREC_PROPERTY,
            LINE_AMOUNT_TYPE_PROPERTY,
            date(EXPIRY_DATE)
                .label("Expiry Date")
                .description("Date quote expires")
                .required(false),
            string(CURRENCY_CODE)
                .label("Currency Code")
                .description("The currency code that quote has been raised in.")
                .options((OptionsFunction<String>) XeroUtils::getCurrencyCodeOptions)
                .required(false),
            string(QUOTE_NUMBER)
                .label("Quote Number")
                .description("Unique alpha numeric code identifying a quote.")
                .maxLength(255)
                .required(false),
            string(REFERENCE)
                .label(REFERENCE)
                .description("Additional reference number")
                .required(false),
            string(BRANDING_THEME_ID)
                .label("Branding Theme ID")
                .description("The branding theme ID to be applied to this quote.")
                .options((OptionsFunction<String>) XeroUtils::getBrandingThemeIdOptions)
                .required(false),
            string(TITLE)
                .label(TITLE)
                .description("The title of the quote.")
                .maxLength(100)
                .required(false),
            string(SUMMARY)
                .label(SUMMARY)
                .description("The summary of the quote.")
                .maxLength(3000)
                .required(false),
            string(TERMS)
                .label("Terms")
                .description("The terms of the quote.")
                .controlType(ControlType.TEXT_AREA)
                .maxLength(4000)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("QuoteID"),
                        string(QUOTE_NUMBER),
                        string(REFERENCE),
                        string(TERMS),
                        object(CONTACT)
                            .properties(
                                string(CONTACT_ID),
                                string(NAME),
                                string(EMAIL_ADDRESS)),
                        array(LINE_ITEMS)
                            .items(
                                object()
                                    .properties(
                                        string("LineItemID"),
                                        string(DESCRIPTION),
                                        number(UNIT_AMOUNT),
                                        integer(DISCOUNT_RATE),
                                        integer(QUANTITY))),
                        string("DateString"),
                        string("ExpiryDateString"),
                        string(STATUS),
                        string(CURRENCY_CODE),
                        string(TITLE),
                        string(BRANDING_THEME_ID),
                        string(SUMMARY),
                        string(LINE_AMOUNT_TYPES))))
        .perform(XeroCreateQuoteAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_QUOTES_CONTEXT_FUNCTION =
        http -> http.post("/Quotes");

    private XeroCreateQuoteAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        Map<String, Object> body = actionContext.http(POST_QUOTES_CONTEXT_FUNCTION)
            .body(
                Http.Body.of(
                    QUOTE_NUMBER, inputParameters.getString(QUOTE_NUMBER),
                    REFERENCE, inputParameters.getString(REFERENCE),
                    TERMS, inputParameters.getString(TERMS),
                    CONTACT, Map.of(CONTACT_ID, inputParameters.getRequiredString(CONTACT_ID)),
                    LINE_ITEMS, inputParameters.getRequiredList(LINE_ITEMS),
                    DATE, inputParameters.getLocalDate(DATE),
                    EXPIRY_DATE, inputParameters.getLocalDate(EXPIRY_DATE),
                    CURRENCY_CODE, inputParameters.getString(CURRENCY_CODE),
                    TITLE, inputParameters.getString(TITLE),
                    SUMMARY, inputParameters.getString(SUMMARY),
                    LINE_AMOUNT_TYPES, inputParameters.getString(LINE_AMOUNT_TYPES)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("Quotes") instanceof List<?> list) {
            return list.getFirst();
        } else {
            return body.get("Message");
        }
    }
}
