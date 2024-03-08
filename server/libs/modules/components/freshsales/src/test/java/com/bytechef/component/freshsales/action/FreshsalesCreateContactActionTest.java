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

package com.bytechef.component.freshsales.action;

import static com.bytechef.component.freshsales.constant.FreshsalesConstants.ADDRESS;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.CITY;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.COUNTRY;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.EMAIL;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.FACEBOOK;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.FIRST_NAME;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.JOB_TITLE;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.LAST_NAME;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.LINKEDIN;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.MEDIUM;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.MOBILE_NUMBER;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.STATE;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.TWITTER;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.WORK_NUMBER;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.ZIPCODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class FreshsalesCreateContactActionTest extends AbstractFreshsalesActionTest {

    @Test
    void testPerform() {
        Map<String, String> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getString(FIRST_NAME))
            .thenReturn(propertyStubsMap.get(FIRST_NAME));
        when(mockedParameters.getString(LAST_NAME))
            .thenReturn(propertyStubsMap.get(LAST_NAME));
        when(mockedParameters.getString(JOB_TITLE))
            .thenReturn(propertyStubsMap.get(JOB_TITLE));
        when(mockedParameters.getRequiredString(EMAIL))
            .thenReturn(propertyStubsMap.get(EMAIL));
        when(mockedParameters.getString(WORK_NUMBER))
            .thenReturn(propertyStubsMap.get(WORK_NUMBER));
        when(mockedParameters.getString(MOBILE_NUMBER))
            .thenReturn(propertyStubsMap.get(MOBILE_NUMBER));
        when(mockedParameters.getString(ADDRESS))
            .thenReturn(propertyStubsMap.get(ADDRESS));
        when(mockedParameters.getString(CITY))
            .thenReturn(propertyStubsMap.get(CITY));
        when(mockedParameters.getString(STATE))
            .thenReturn(propertyStubsMap.get(STATE));
        when(mockedParameters.getString(ZIPCODE))
            .thenReturn(propertyStubsMap.get(ZIPCODE));
        when(mockedParameters.getString(COUNTRY))
            .thenReturn(propertyStubsMap.get(COUNTRY));
        when(mockedParameters.getString(MEDIUM))
            .thenReturn(propertyStubsMap.get(MEDIUM));
        when(mockedParameters.getString(FACEBOOK))
            .thenReturn(propertyStubsMap.get(FACEBOOK));
        when(mockedParameters.getString(TWITTER))
            .thenReturn(propertyStubsMap.get(TWITTER));
        when(mockedParameters.getString(LINKEDIN))
            .thenReturn(propertyStubsMap.get(LINKEDIN));

        Object result = FreshsalesCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, String> createPropertyStubsMap() {
        Map<String, String> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(FIRST_NAME, "firstName");
        propertyStubsMap.put(LAST_NAME, "lastName");
        propertyStubsMap.put(JOB_TITLE, "jobTitle");
        propertyStubsMap.put(EMAIL, "email");
        propertyStubsMap.put(WORK_NUMBER, "workNumber");
        propertyStubsMap.put(MOBILE_NUMBER, "mobileNumber");
        propertyStubsMap.put(ADDRESS, "address");
        propertyStubsMap.put(CITY, "city");
        propertyStubsMap.put(STATE, "state");
        propertyStubsMap.put(ZIPCODE, "zipcode");
        propertyStubsMap.put(COUNTRY, "country");
        propertyStubsMap.put(MEDIUM, "medium");
        propertyStubsMap.put(FACEBOOK, "facebook");
        propertyStubsMap.put(TWITTER, "twitter");
        propertyStubsMap.put(LINKEDIN, "linkedin");

        return propertyStubsMap;
    }
}
