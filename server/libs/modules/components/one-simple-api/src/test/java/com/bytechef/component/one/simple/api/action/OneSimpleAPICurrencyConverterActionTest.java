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

package com.bytechef.component.one.simple.api.action;

import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FROM_CURRENCY;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FROM_VALUE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TO_CURRENCY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Luka Ljubić
 */
class OneSimpleAPICurrencyConverterActionTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    private final Map<String, Object> responeseMap = Map.of("key", "value");

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);

        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(FROM_CURRENCY))
            .thenReturn((String) propertyStubsMap.get(FROM_CURRENCY));
        when(mockedParameters.getRequiredString(TO_CURRENCY))
            .thenReturn((String) propertyStubsMap.get(TO_CURRENCY));
        when(mockedParameters.getRequiredString(FROM_VALUE))
            .thenReturn((String) propertyStubsMap.get(FROM_VALUE));

        Object result = OneSimpleAPICurrencyConverterAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(FROM_CURRENCY, FROM_CURRENCY);
        propertyStubsMap.put(TO_CURRENCY, TO_CURRENCY);
        propertyStubsMap.put(FROM_VALUE, FROM_VALUE);

        return propertyStubsMap;
    }
}
