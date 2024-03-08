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

import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.CATEGORY;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.DESCRIPTION;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.DETAIL;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.DUE_ON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class CapsuleCRMCreateTaskActionTest extends AbstractCapsuleCRMActionTest {

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(DESCRIPTION))
            .thenReturn((String) propertyStubsMap.get(DESCRIPTION));
        when(mockedParameters.getDate(DUE_ON))
            .thenReturn((Date) propertyStubsMap.get(DUE_ON));
        when(mockedParameters.getString(DETAIL))
            .thenReturn((String) propertyStubsMap.get(DETAIL));
        when(mockedParameters.get(CATEGORY))
            .thenReturn(propertyStubsMap.get(CATEGORY));

        Object result = CapsuleCRMCreateTaskAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("task", propertyStubsMap), body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(DESCRIPTION, "description");
        propertyStubsMap.put(DUE_ON, new Date());
        propertyStubsMap.put(DETAIL, "detail");
        propertyStubsMap.put(CATEGORY, Map.of("name", "name", "colour", "#ffffff"));

        return propertyStubsMap;
    }

}
