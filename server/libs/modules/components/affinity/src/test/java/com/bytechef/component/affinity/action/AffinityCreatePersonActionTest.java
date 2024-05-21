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

package com.bytechef.component.affinity.action;

import static com.bytechef.component.affinity.constant.AffinityConstants.EMAILS;
import static com.bytechef.component.affinity.constant.AffinityConstants.FIRST_NAME;
import static com.bytechef.component.affinity.constant.AffinityConstants.LAST_NAME;
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
class AffinityCreatePersonActionTest extends AbstractAffinityActionTest {

    private static final List<String> emails = List.of("test@mail.com", "test2@mail.com");

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(FIRST_NAME))
            .thenReturn((String) propertyStubsMap.get(FIRST_NAME));
        when(mockedParameters.getRequiredString(LAST_NAME))
            .thenReturn((String) propertyStubsMap.get(LAST_NAME));
        when(mockedParameters.getList(EMAILS, String.class))
            .thenReturn(emails);

        Object result = AffinityCreatePersonAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        verify(mockedContext, times(1)).http(AffinityCreatePersonAction.POST_PERSONS_CONTEXT_FUNCTION);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(FIRST_NAME, "firstName");
        propertyStubsMap.put(LAST_NAME, "lastName");
        propertyStubsMap.put(EMAILS, emails);

        return propertyStubsMap;
    }
}
