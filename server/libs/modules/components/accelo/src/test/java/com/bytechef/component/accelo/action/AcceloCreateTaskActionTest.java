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

import static com.bytechef.component.accelo.constant.AcceloConstants.AGAINST_ID;
import static com.bytechef.component.accelo.constant.AcceloConstants.AGAINST_TYPE;
import static com.bytechef.component.accelo.constant.AcceloConstants.DATE_STARTED;
import static com.bytechef.component.accelo.constant.AcceloConstants.TITLE;
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
class AcceloCreateTaskActionTest extends AbstractAcceloActionTest {

    private static final Date date = new Date();

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(TITLE))
            .thenReturn((String) propertyStubsMap.get(TITLE));
        when(mockedParameters.getRequiredString(AGAINST_TYPE))
            .thenReturn((String) propertyStubsMap.get(AGAINST_TYPE));
        when(mockedParameters.getRequiredString(AGAINST_ID))
            .thenReturn((String) propertyStubsMap.get(AGAINST_ID));
        when(mockedParameters.getRequiredDate(DATE_STARTED))
            .thenReturn(date);

        Object result = AcceloCreateTaskAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(TITLE, "title");
        propertyStubsMap.put(AGAINST_TYPE, "type");
        propertyStubsMap.put(AGAINST_ID, "id");
        propertyStubsMap.put(DATE_STARTED, date.toInstant()
            .getEpochSecond());

        return propertyStubsMap;
    }
}
