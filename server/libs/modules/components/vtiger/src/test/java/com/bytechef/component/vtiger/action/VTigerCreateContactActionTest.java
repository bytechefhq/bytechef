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

package com.bytechef.component.vtiger.action;

import static com.bytechef.component.vtiger.constant.VTigerConstants.EMAIL;
import static com.bytechef.component.vtiger.constant.VTigerConstants.FIRSTNAME;
import static com.bytechef.component.vtiger.constant.VTigerConstants.LASTNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Luka Ljubić
 */
class VTigerCreateContactActionTest {

    private final ArgumentCaptor<Context.Http.Body> bodyArgumentCaptor =
        ArgumentCaptor.forClass(Context.Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    private final Map<String, Object> responseMap = Map.of("key", "value");

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
            .thenReturn(responseMap);

        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getString("elementType"))
            .thenReturn((String) propertyStubsMap.get("elementType"));
        when(mockedParameters.getRequired("element"))
            .thenReturn(propertyStubsMap.get("element"));
        when(mockedParameters.getRequiredString(FIRSTNAME))
            .thenReturn((String) propertyStubsMap.get(FIRSTNAME));
        when(mockedParameters.getRequiredString(LASTNAME))
            .thenReturn((String) propertyStubsMap.get(LASTNAME));
        when(mockedParameters.getRequiredString(EMAIL))
            .thenReturn((String) propertyStubsMap.get(EMAIL));

        Object result = VTigerCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new LinkedHashMap<>();
        Map<String, String> bodyMap = new LinkedHashMap<>();

        bodyMap.put(FIRSTNAME, null);
        bodyMap.put(LASTNAME, null);
        bodyMap.put(EMAIL, null);

        propertyStubsMap.put("elementType", "Contacts");
        propertyStubsMap.put("element", bodyMap);

        return propertyStubsMap;
    }
}
