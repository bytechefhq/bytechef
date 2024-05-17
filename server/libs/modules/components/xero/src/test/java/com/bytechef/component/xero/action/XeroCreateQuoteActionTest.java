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

import static com.bytechef.component.xero.constant.XeroConstants.CONTACT;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_ID;
import static com.bytechef.component.xero.constant.XeroConstants.CURRENCY_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.DATE;
import static com.bytechef.component.xero.constant.XeroConstants.DESCRIPTION;
import static com.bytechef.component.xero.constant.XeroConstants.EXPIRY_DATE;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_AMOUNT_TYPES;
import static com.bytechef.component.xero.constant.XeroConstants.LINE_ITEMS;
import static com.bytechef.component.xero.constant.XeroConstants.QUOTE_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.REFERENCE;
import static com.bytechef.component.xero.constant.XeroConstants.SUMMARY;
import static com.bytechef.component.xero.constant.XeroConstants.TERMS;
import static com.bytechef.component.xero.constant.XeroConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class XeroCreateQuoteActionTest extends AbstractXeroActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final List<Map<String, String>> lineItems = List.of(Map.of(DESCRIPTION, "some description"));
    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getString(QUOTE_NUMBER))
            .thenReturn((String) propertyStubsMap.get(QUOTE_NUMBER));
        when(mockedParameters.getString(REFERENCE))
            .thenReturn((String) propertyStubsMap.get(REFERENCE));
        when(mockedParameters.getString(TERMS))
            .thenReturn((String) propertyStubsMap.get(TERMS));
        when(mockedParameters.getRequiredString(CONTACT_ID))
            .thenReturn("123");
        when(mockedParameters.getLocalDate(DATE))
            .thenReturn((LocalDate) propertyStubsMap.get(DATE));
        when(mockedParameters.getLocalDate(EXPIRY_DATE))
            .thenReturn((LocalDate) propertyStubsMap.get(EXPIRY_DATE));
        when(mockedParameters.getString(CURRENCY_CODE))
            .thenReturn((String) propertyStubsMap.get(CURRENCY_CODE));
        when(mockedParameters.getString(TITLE))
            .thenReturn((String) propertyStubsMap.get(TITLE));
        when(mockedParameters.getString(SUMMARY))
            .thenReturn((String) propertyStubsMap.get(SUMMARY));
        when(mockedParameters.getString(LINE_AMOUNT_TYPES))
            .thenReturn((String) propertyStubsMap.get(LINE_AMOUNT_TYPES));
        when((List<Map<String, String>>) mockedParameters.getRequiredList(LINE_ITEMS))
            .thenReturn(lineItems);

        when(mockedContext.http(XeroCreateQuoteAction.POST_QUOTES_CONTEXT_FUNCTION))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("Quotes", List.of(mockedObject)));

        Object result = XeroCreateQuoteAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private Map<String, Object> createPropertyStubsMap() {
        LocalDate localDate = LocalDate.of(2000, 1, 1);

        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(QUOTE_NUMBER, "123");
        propertyStubsMap.put(REFERENCE, "reference");
        propertyStubsMap.put(TERMS, "terms");
        propertyStubsMap.put(CONTACT, Map.of(CONTACT_ID, "123"));
        propertyStubsMap.put(LINE_ITEMS, lineItems);
        propertyStubsMap.put(DATE, localDate);
        propertyStubsMap.put(EXPIRY_DATE, localDate);
        propertyStubsMap.put(CURRENCY_CODE, "EU");
        propertyStubsMap.put(TITLE, "terms");
        propertyStubsMap.put(SUMMARY, "terms");
        propertyStubsMap.put(LINE_AMOUNT_TYPES, "NoTax");

        return propertyStubsMap;
    }
}
