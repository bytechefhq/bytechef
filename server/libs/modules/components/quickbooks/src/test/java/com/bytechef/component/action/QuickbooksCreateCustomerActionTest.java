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

package com.bytechef.component.action;

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DISPLAY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.FAMILY_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.GIVEN_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.MIDDLE_NAME;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.SUFFIX;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.quickbooks.action.QuickbooksCreateCustomerAction;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuickbooksCreateCustomerActionTest {

    private final ArgumentCaptor<Context.Http.Body> bodyArgumentCaptor =
        ArgumentCaptor.forClass(Context.Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    private final Map<String, Object> responeseMap = Map.of("key", "value");

    @Test
    void testPerform() {

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);

        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(DISPLAY_NAME))
            .thenReturn((String) propertyStubsMap.get(DISPLAY_NAME));
        when(mockedParameters.getString(SUFFIX))
            .thenReturn((String) propertyStubsMap.get(SUFFIX));
        when(mockedParameters.getString(TITLE))
            .thenReturn((String) propertyStubsMap.get(TITLE));
        when(mockedParameters.getString(MIDDLE_NAME))
            .thenReturn((String) propertyStubsMap.get(MIDDLE_NAME));
        when(mockedParameters.getString(FAMILY_NAME))
            .thenReturn((String) propertyStubsMap.get(FAMILY_NAME));
        when(mockedParameters.getString(GIVEN_NAME))
            .thenReturn((String) propertyStubsMap.get(GIVEN_NAME));


        Object result = QuickbooksCreateCustomerAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(DISPLAY_NAME, DISPLAY_NAME);
        propertyStubsMap.put(SUFFIX, SUFFIX);
        propertyStubsMap.put(TITLE, TITLE);
        propertyStubsMap.put(MIDDLE_NAME, MIDDLE_NAME);
        propertyStubsMap.put(FAMILY_NAME, FAMILY_NAME);
        propertyStubsMap.put(GIVEN_NAME, GIVEN_NAME);

        return propertyStubsMap;
    }
}
