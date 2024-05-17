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

package com.bytechef.component.xero.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.xero.constant.XeroConstants.BASE_URL;
import static com.bytechef.component.xero.constant.XeroConstants.CODE;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACTS;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_ID;
import static com.bytechef.component.xero.constant.XeroConstants.CURRENCY_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.DATE;
import static com.bytechef.component.xero.constant.XeroConstants.DUE_DATE;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_AMOUNT_TYPES;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS;
import static com.bytechef.component.xero.constant.XeroConstants.MESSAGE;
import static com.bytechef.component.xero.constant.XeroConstants.NAME;
import static com.bytechef.component.xero.constant.XeroConstants.REFERENCE;
import static com.bytechef.component.xero.constant.XeroConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class XeroUtils {

    protected static final ContextFunction<Http, Http.Executor> GET_CONTACTS_CONTEXT_FUNCTION =
        http -> http.get(BASE_URL + "/" + CONTACTS);

    protected static final ContextFunction<Http, Http.Executor> GET_CURRENCIES_CONTEXT_FUNCTION =
        http -> http.get(BASE_URL + "/Currencies");

    private XeroUtils() {
    }

    public static Object createInvoice(
        Parameters inputParameters, ActionContext actionContext, ContextFunction<Http, Http.Executor> contextFunction,
        String accpay) {

        Map<String, Object> body = actionContext.http(contextFunction)
            .body(
                Http.Body.of(TYPE, accpay,
                    CONTACT, Map.of(CONTACT_ID, inputParameters.getRequiredString(CONTACT_ID)),
                    DATE, inputParameters.getLocalDate(DATE),
                    DUE_DATE, inputParameters.getLocalDate(DUE_DATE),
                    REFERENCE, inputParameters.getString(REFERENCE),
                    CURRENCY_CODE, inputParameters.getString(CURRENCY_CODE),
                    LINE_AMOUNT_TYPES, inputParameters.getString(LINE_AMOUNT_TYPES),
                    LINE_ITEMS, inputParameters.getList(LINE_ITEMS)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get("Invoices") instanceof List<?> list) {
            return list.getFirst();
        } else {
            return body.get(MESSAGE);
        }
    }

    public static List<Option<String>> getContactIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(GET_CONTACTS_CONTEXT_FUNCTION)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get(CONTACTS) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get("ContactID")));
                }
            }
        }
        return options;

    }

    public static List<Option<String>> getCurrencyCodeOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, Object> body = context
            .http(GET_CURRENCIES_CONTEXT_FUNCTION)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("Currencies") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("Description"), (String) map.get(CODE)));
                }
            }
        }

        return options;
    }
}
