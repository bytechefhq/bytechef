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
import static com.bytechef.component.xero.constant.XeroConstants.ACCREC;
import static com.bytechef.component.xero.constant.XeroConstants.CODE;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACTS;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_ID;
import static com.bytechef.component.xero.constant.XeroConstants.CURRENCY_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.DATE;
import static com.bytechef.component.xero.constant.XeroConstants.DESCRIPTION;
import static com.bytechef.component.xero.constant.XeroConstants.DUE_DATE;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_AMOUNT_TYPES;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS;
import static com.bytechef.component.xero.constant.XeroConstants.NAME;
import static com.bytechef.component.xero.constant.XeroConstants.REFERENCE;
import static com.bytechef.component.xero.constant.XeroConstants.TYPE;
import static com.bytechef.component.xero.util.XeroUtils.GET_ACCOUNTS_CONTEXT_FUNCTION;
import static com.bytechef.component.xero.util.XeroUtils.GET_BRANDING_THEME_CONTEXT_FUNCTION;
import static com.bytechef.component.xero.util.XeroUtils.GET_CONTACTS_CONTEXT_FUNCTION;
import static com.bytechef.component.xero.util.XeroUtils.GET_CURRENCIES_CONTEXT_FUNCTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class XeroUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final List<Option<String>> expectedOptions = List.of(option("name", "123"));
    private final List<Map<String, String>> lineItems = List.of(Map.of(DESCRIPTION, "some description"));
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final ContextFunction mockedContextFunction = mock(ContextFunction.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testCreateInvoice() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(CONTACT_ID))
            .thenReturn("123");
        when(mockedParameters.getLocalDate(DATE))
            .thenReturn((LocalDate) propertyStubsMap.get(DATE));
        when(mockedParameters.getLocalDate(DUE_DATE))
            .thenReturn((LocalDate) propertyStubsMap.get(DUE_DATE));
        when(mockedParameters.getString(REFERENCE))
            .thenReturn((String) propertyStubsMap.get(REFERENCE));
        when(mockedParameters.getString(CURRENCY_CODE))
            .thenReturn((String) propertyStubsMap.get(CURRENCY_CODE));
        when(mockedParameters.getString(LINE_AMOUNT_TYPES))
            .thenReturn((String) propertyStubsMap.get(LINE_AMOUNT_TYPES));
        when((List<Map<String, String>>) mockedParameters.getList(LINE_ITEMS))
            .thenReturn(lineItems);

        when(mockedContext.http(mockedContextFunction))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("Invoices", List.of(mockedObject)));

        Object result = XeroUtils.createInvoice(mockedParameters, mockedContext, mockedContextFunction, ACCREC);

        assertEquals(mockedObject, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    @Test
    void testGetAccountCodeOptions() {
        Map<String, Object> account = Map.of(NAME, "name", CODE, "123");

        Map<String, Object> bodyMap = Map.of("Accounts", List.of(account));

        when(mockedContext.http(GET_ACCOUNTS_CONTEXT_FUNCTION))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(bodyMap);

        List<Option<String>> resultOptions = XeroUtils.getAccountCodeOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, resultOptions);
    }

    @Test
    void testGetBrandingThemeIdOptions() {
        Map<String, Object> brandingTheme = Map.of(NAME, "name", "BrandingThemeID", "123");

        Map<String, Object> bodyMap = Map.of("BrandingThemes", List.of(brandingTheme));

        when(mockedContext.http(GET_BRANDING_THEME_CONTEXT_FUNCTION))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(bodyMap);

        List<Option<String>> resultOptions = XeroUtils.getBrandingThemeIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, resultOptions);
    }

    @Test
    void testGetContactIdOptions() {
        Map<String, Object> contact = Map.of(NAME, "name", "ContactID", "123");

        Map<String, Object> bodyMap = Map.of(CONTACTS, List.of(contact));

        when(mockedContext.http(GET_CONTACTS_CONTEXT_FUNCTION))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(bodyMap);

        List<Option<String>> resultOptions = XeroUtils.getContactIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, resultOptions);
    }

    @Test
    void testGetCurrencyCodeOptions() {
        Map<String, Object> currency = Map.of("Description", "name", CODE, "123");

        Map<String, Object> bodyMap = Map.of("Currencies", List.of(currency));

        when(mockedContext.http(GET_CURRENCIES_CONTEXT_FUNCTION))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(bodyMap);

        List<Option<String>> resultOptions = XeroUtils.getCurrencyCodeOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, resultOptions);
    }

    private Map<String, Object> createPropertyStubsMap() {
        LocalDate localDate = LocalDate.of(2000, 1, 1);

        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(TYPE, ACCREC);
        propertyStubsMap.put(CONTACT, Map.of(CONTACT_ID, "123"));
        propertyStubsMap.put(DATE, localDate);
        propertyStubsMap.put(DUE_DATE, localDate);
        propertyStubsMap.put(REFERENCE, "reference");
        propertyStubsMap.put(CURRENCY_CODE, "EU");
        propertyStubsMap.put(LINE_AMOUNT_TYPES, "NoTax");
        propertyStubsMap.put(LINE_ITEMS, lineItems);

        return propertyStubsMap;
    }
}
