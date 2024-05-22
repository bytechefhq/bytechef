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

package com.bytechef.component.copper.action;

import static com.bytechef.component.copper.action.CopperCreateCompanyAction.POST_COMPANIES_CONTEXT_FUNCTION;
import static com.bytechef.component.copper.constant.CopperConstants.ADDRESS;
import static com.bytechef.component.copper.constant.CopperConstants.ASSIGNEE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.CATEGORY;
import static com.bytechef.component.copper.constant.CopperConstants.CITY;
import static com.bytechef.component.copper.constant.CopperConstants.CONTACT_TYPE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.EMAIL_DOMAIN;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.NUMBER;
import static com.bytechef.component.copper.constant.CopperConstants.PHONE_NUMBERS;
import static com.bytechef.component.copper.constant.CopperConstants.SOCIALS;
import static com.bytechef.component.copper.constant.CopperConstants.STREET;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS;
import static com.bytechef.component.copper.constant.CopperConstants.URL;
import static com.bytechef.component.copper.constant.CopperConstants.WEBSITES;
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
class CopperCreateCompanyActionTest extends AbstractCopperActionTest {

    private static final List<Map<String, String>> phoneNumbers = List.of(Map.of(NUMBER, "1234", CATEGORY, "work"));
    private static final List<Map<String, String>> socials = List.of(Map.of(URL, "url", CATEGORY, "youtube"));
    private static final List<Map<String, String>> websites = List.of(Map.of(URL, "url", CATEGORY, "personal"));

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getString(NAME))
            .thenReturn((String) propertyStubsMap.get(NAME));
        when(mockedParameters.getString(ASSIGNEE_ID))
            .thenReturn((String) propertyStubsMap.get(ASSIGNEE_ID));
        when(mockedParameters.getString(EMAIL_DOMAIN))
            .thenReturn((String) propertyStubsMap.get(EMAIL_DOMAIN));
        when(mockedParameters.getString(CONTACT_TYPE_ID))
            .thenReturn((String) propertyStubsMap.get(CONTACT_TYPE_ID));
        when(mockedParameters.getString(DETAILS))
            .thenReturn((String) propertyStubsMap.get(DETAILS));
        when((List<Map<String, String>>) mockedParameters.getList(PHONE_NUMBERS))
            .thenReturn(phoneNumbers);
        when((List<Map<String, String>>) mockedParameters.getList(SOCIALS))
            .thenReturn(socials);
        when((List<Map<String, String>>) mockedParameters.getList(WEBSITES))
            .thenReturn(websites);
        when(mockedParameters.get(ADDRESS))
            .thenReturn(propertyStubsMap.get(ADDRESS));
        when(mockedParameters.getList(TAGS, String.class))
            .thenReturn((List<String>) propertyStubsMap.get(TAGS));

        Object result = CopperCreateCompanyAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        verify(mockedContext, times(1)).http(POST_COMPANIES_CONTEXT_FUNCTION);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());

    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(NAME, "name");
        propertyStubsMap.put(ASSIGNEE_ID, "assigneeId");
        propertyStubsMap.put(EMAIL_DOMAIN, "emailDomain");
        propertyStubsMap.put(DETAILS, "details");
        propertyStubsMap.put(CONTACT_TYPE_ID, "contactType");
        propertyStubsMap.put(PHONE_NUMBERS, phoneNumbers);
        propertyStubsMap.put(SOCIALS, socials);
        propertyStubsMap.put(WEBSITES, websites);
        propertyStubsMap.put(ADDRESS, Map.of(STREET, "street", CITY, "city"));
        propertyStubsMap.put(TAGS, List.of("tag1", "tag2"));

        return propertyStubsMap;
    }
}
