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

package com.bytechef.component.accelo.action;

import static com.bytechef.component.accelo.constant.AcceloConstants.COMPANY;
import static com.bytechef.component.accelo.constant.AcceloConstants.EMAIL;
import static com.bytechef.component.accelo.constant.AcceloConstants.FIRST_NAME;
import static com.bytechef.component.accelo.constant.AcceloConstants.LAST_NAME;
import static com.bytechef.component.accelo.constant.AcceloConstants.PHONE;
import static com.bytechef.component.accelo.constant.AcceloConstants.POSITION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class AcceloCreateContactActionTest extends AbstractAcceloActionTest {

    @Test
    void testPerform() {
        Map<String, String> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getString(FIRST_NAME))
            .thenReturn(propertyStubsMap.get(FIRST_NAME));
        when(mockedParameters.getString(LAST_NAME))
            .thenReturn(propertyStubsMap.get(LAST_NAME));
        when(mockedParameters.getString(COMPANY))
            .thenReturn(propertyStubsMap.get(COMPANY));
        when(mockedParameters.getString(PHONE))
            .thenReturn(propertyStubsMap.get(PHONE));
        when(mockedParameters.getString(EMAIL))
            .thenReturn(propertyStubsMap.get(EMAIL));
        when(mockedParameters.getString(POSITION))
            .thenReturn(propertyStubsMap.get(POSITION));

        Object result = AcceloCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, String> createPropertyStubsMap() {
        Map<String, String> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(FIRST_NAME, "firstName");
        propertyStubsMap.put(LAST_NAME, "lastName");
        propertyStubsMap.put(COMPANY, "company");
        propertyStubsMap.put(PHONE, "phone");
        propertyStubsMap.put(EMAIL, "email");
        propertyStubsMap.put(POSITION, "position");

        return propertyStubsMap;
    }
}
