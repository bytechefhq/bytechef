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

package com.bytechef.component.capsule.crm.action;

import static com.bytechef.component.capsule.crm.action.CapsuleCRMCreateContactAction.POST_PARTIES_CONTEXT_FUNCTION;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ABOUT;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ADDRESS;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.ADDRESSES;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.CITY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.EMAIL_ADDRESSES;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.FIRST_NAME;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.LAST_NAME;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.NUMBER;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.PHONE_NUMBERS;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.STREET;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class CapsuleCRMCreateContactActionTest extends AbstractCapsuleCRMActionTest {

    private static final List<Map<String, String>> emails = List.of(
        Map.of(ADDRESS, "test@mail.com", TYPE, "Home"));

    private static final List<Map<String, String>> addresses = List.of(
        Map.of(STREET, "street", TYPE, "Home", CITY, "city"));

    private static final List<Map<String, String>> phoneNumbers = List.of(
        Map.of(NUMBER, "12345678", TYPE, "Home"));

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getString(FIRST_NAME))
            .thenReturn((String) propertyStubsMap.get(FIRST_NAME));
        when(mockedParameters.getString(LAST_NAME))
            .thenReturn((String) propertyStubsMap.get(LAST_NAME));
        when(mockedParameters.getRequiredString(TYPE))
            .thenReturn((String) propertyStubsMap.get(TYPE));
        when(mockedParameters.getString(ABOUT))
            .thenReturn((String) propertyStubsMap.get(ABOUT));
        when((List<Map<String, String>>) mockedParameters.getList(EMAIL_ADDRESSES))
            .thenReturn(emails);
        when((List<Map<String, String>>) mockedParameters.getList(ADDRESSES))
            .thenReturn(addresses);
        when((List<Map<String, String>>) mockedParameters.getList(PHONE_NUMBERS))
            .thenReturn(phoneNumbers);

        Object result = CapsuleCRMCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        verify(mockedContext, times(1)).http(POST_PARTIES_CONTEXT_FUNCTION);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("party", propertyStubsMap), body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(FIRST_NAME, "fname");
        propertyStubsMap.put(LAST_NAME, "lname");
        propertyStubsMap.put(ABOUT, "about");
        propertyStubsMap.put(EMAIL_ADDRESSES, emails);
        propertyStubsMap.put(ADDRESSES, addresses);
        propertyStubsMap.put(PHONE_NUMBERS, phoneNumbers);

        return propertyStubsMap;
    }
}
