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

package com.bytechef.component.freshsales.action;

import static com.bytechef.component.freshsales.constant.FreshsalesConstants.NAME;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.PHONE;
import static com.bytechef.component.freshsales.constant.FreshsalesConstants.WEBSITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class FreshsalesCreateAccountActionTest extends AbstractFreshsalesActionTest {

    @Test
    void testPerform() {
        Map<String, String> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(NAME))
            .thenReturn(propertyStubsMap.get(NAME));
        when(mockedParameters.getString(WEBSITE))
            .thenReturn(propertyStubsMap.get(WEBSITE));
        when(mockedParameters.getString(PHONE))
            .thenReturn(propertyStubsMap.get(PHONE));

        Object result = FreshsalesCreateAccountAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, String> createPropertyStubsMap() {
        Map<String, String> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(NAME, "name");
        propertyStubsMap.put(WEBSITE, "website");
        propertyStubsMap.put(PHONE, "phone");

        return propertyStubsMap;
    }
}
