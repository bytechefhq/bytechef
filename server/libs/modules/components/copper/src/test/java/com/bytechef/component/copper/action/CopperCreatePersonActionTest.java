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

import static com.bytechef.component.copper.constant.CopperConstants.ADDRESS;
import static com.bytechef.component.copper.constant.CopperConstants.ASSIGNEE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.COMPANY_ID;
import static com.bytechef.component.copper.constant.CopperConstants.CONTACT_TYPE_ID;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.EMAILS;
import static com.bytechef.component.copper.constant.CopperConstants.NAME;
import static com.bytechef.component.copper.constant.CopperConstants.PHONE_NUMBERS;
import static com.bytechef.component.copper.constant.CopperConstants.SOCIALS;
import static com.bytechef.component.copper.constant.CopperConstants.TAGS;
import static com.bytechef.component.copper.constant.CopperConstants.TITLE;
import static com.bytechef.component.copper.constant.CopperConstants.WEBSITES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class CopperCreatePersonActionTest extends AbstractCopperActionTest {

    @Test
    void testPerform() {

        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getString(NAME))
            .thenReturn((String) propertyStubsMap.get(NAME));
        when(mockedParameters.getList(EMAILS))
            .thenReturn(null);
        when(mockedParameters.getString(ASSIGNEE_ID))
            .thenReturn((String) propertyStubsMap.get(ASSIGNEE_ID));
        when(mockedParameters.getString(TITLE))
            .thenReturn((String) propertyStubsMap.get(TITLE));
        when(mockedParameters.getString(COMPANY_ID))
            .thenReturn((String) propertyStubsMap.get(COMPANY_ID));
        when(mockedParameters.getString(CONTACT_TYPE_ID))
            .thenReturn((String) propertyStubsMap.get(CONTACT_TYPE_ID));
        when(mockedParameters.getString(DETAILS))
            .thenReturn((String) propertyStubsMap.get(DETAILS));
        when(mockedParameters.getList(PHONE_NUMBERS))
            .thenReturn(null);
        when(mockedParameters.getList(SOCIALS))
            .thenReturn(null);
        when(mockedParameters.getList(WEBSITES))
            .thenReturn(null);
        when(mockedParameters.get(ADDRESS))
            .thenReturn(null);
        when(mockedParameters.getList(TAGS))
            .thenReturn(null);

        Object result = CopperCreatePersonAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(NAME, "name");
        propertyStubsMap.put(ASSIGNEE_ID, "assigneId");
        propertyStubsMap.put(TITLE, "title");
        propertyStubsMap.put(COMPANY_ID, "companyId");
        propertyStubsMap.put(CONTACT_TYPE_ID, "contactType");
        propertyStubsMap.put(DETAILS, "details");

        return propertyStubsMap;
    }
}
