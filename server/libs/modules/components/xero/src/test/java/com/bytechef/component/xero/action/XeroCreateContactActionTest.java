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

import static com.bytechef.component.xero.constant.XeroConstants.ACCOUNT_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.ADDRESSES;
import static com.bytechef.component.xero.constant.XeroConstants.ADDRESS_TYPE;
import static com.bytechef.component.xero.constant.XeroConstants.BANK_ACCOUNT_DETAILS;
import static com.bytechef.component.xero.constant.XeroConstants.CITY;
import static com.bytechef.component.xero.constant.XeroConstants.COMPANY_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACTS;
import static com.bytechef.component.xero.constant.XeroConstants.CONTACT_STATUS;
import static com.bytechef.component.xero.constant.XeroConstants.COUNTRY;
import static com.bytechef.component.xero.constant.XeroConstants.EMAIL_ADDRESS;
import static com.bytechef.component.xero.constant.XeroConstants.FIRST_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.LAST_NAME;
import static com.bytechef.component.xero.constant.XeroConstants.NAME;
import static com.bytechef.component.xero.constant.XeroConstants.PHONES;
import static com.bytechef.component.xero.constant.XeroConstants.PHONE_AREA_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.PHONE_COUNTRY_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.PHONE_NUMBER;
import static com.bytechef.component.xero.constant.XeroConstants.PHONE_TYPE;
import static com.bytechef.component.xero.constant.XeroConstants.POSTAL_CODE;
import static com.bytechef.component.xero.constant.XeroConstants.REGION;
import static com.bytechef.component.xero.constant.XeroConstants.TAX_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Mario Cvjetojevic
 * @author Monika Domiter
 */
class XeroCreateContactActionTest extends AbstractXeroActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Object mockedObject = mock(Object.class);

    private final List<Map<String, String>> addresses = List.of(Map.of(
        ADDRESS_TYPE, "STREET",
        CITY, "city",
        REGION, "region",
        POSTAL_CODE, "123",
        COUNTRY, "country"));

    private final List<Map<String, String>> phones = List.of(Map.of(
        PHONE_TYPE, "DEFAULT",
        PHONE_NUMBER, "123",
        PHONE_AREA_CODE, "01",
        PHONE_COUNTRY_CODE, "333"));

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(NAME))
            .thenReturn((String) propertyStubsMap.get(NAME));
        when(mockedParameters.getString(COMPANY_NUMBER))
            .thenReturn((String) propertyStubsMap.get(COMPANY_NUMBER));
        when(mockedParameters.getString(ACCOUNT_NUMBER))
            .thenReturn((String) propertyStubsMap.get(ACCOUNT_NUMBER));
        when(mockedParameters.getString(CONTACT_STATUS))
            .thenReturn((String) propertyStubsMap.get(CONTACT_STATUS));
        when(mockedParameters.getString(FIRST_NAME))
            .thenReturn((String) propertyStubsMap.get(FIRST_NAME));
        when(mockedParameters.getString(LAST_NAME))
            .thenReturn((String) propertyStubsMap.get(LAST_NAME));
        when(mockedParameters.getString(EMAIL_ADDRESS))
            .thenReturn((String) propertyStubsMap.get(EMAIL_ADDRESS));
        when(mockedParameters.getString(BANK_ACCOUNT_DETAILS))
            .thenReturn((String) propertyStubsMap.get(BANK_ACCOUNT_DETAILS));
        when(mockedParameters.getString(TAX_NUMBER))
            .thenReturn((String) propertyStubsMap.get(TAX_NUMBER));
        when((List<Map<String, String>>) mockedParameters.getList(PHONES))
            .thenReturn(phones);
        when((List<Map<String, String>>) mockedParameters.getList(ADDRESSES))
            .thenReturn(addresses);

        when(mockedContext.http(XeroCreateContactAction.POST_CONTACTS_CONTEXT_FUNCTION))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(CONTACTS, List.of(mockedObject)));

        Object result = XeroCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Http.Body bodyValue = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, bodyValue.getContent());
    }

    private Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(NAME, "name");
        propertyStubsMap.put(COMPANY_NUMBER, "123");
        propertyStubsMap.put(ACCOUNT_NUMBER, "123");
        propertyStubsMap.put(CONTACT_STATUS, "ACTIVE");
        propertyStubsMap.put(FIRST_NAME, "abc");
        propertyStubsMap.put(LAST_NAME, "def");
        propertyStubsMap.put(EMAIL_ADDRESS, "test@mail.com");
        propertyStubsMap.put(BANK_ACCOUNT_DETAILS, "123");
        propertyStubsMap.put(TAX_NUMBER, "123");
        propertyStubsMap.put(ADDRESSES, addresses);
        propertyStubsMap.put(PHONES, phones);

        return propertyStubsMap;
    }
}
