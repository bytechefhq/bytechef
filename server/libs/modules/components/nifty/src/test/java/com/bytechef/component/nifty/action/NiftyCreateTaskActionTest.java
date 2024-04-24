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

package com.bytechef.component.nifty.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.nifty.constant.NiftyConstants;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Luka Ljubić
 */
public class NiftyCreateTaskActionTest {

    Parameters mockedParameters = Mockito.mock(Parameters.class);
    ArgumentCaptor<Http.Body> bodyArgumentCaptor =
        ArgumentCaptor.forClass(Http.Body.class);
    ActionContext mockedContext = Mockito.mock(ActionContext.class);
    Map<String, Object> responeseMap = Map.of("key", "value");
    Context.Http.Executor mockedExecutor = Mockito.mock(Context.Http.Executor.class);
    Context.Http.Response mockedResponse = Mockito.mock(Context.Http.Response.class);

    @BeforeEach
    public void beforeEach() {

        Mockito.when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        Mockito.when(mockedExecutor.headers(anyMap()))
            .thenReturn(mockedExecutor);
        Mockito.when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        Mockito.when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        Mockito.when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        Mockito.when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);
    }

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        Mockito.when(mockedParameters.getString(NiftyConstants.NAME))
            .thenReturn((String) propertyStubsMap.get(NiftyConstants.NAME));
        Mockito.when(mockedParameters.getString(NiftyConstants.DESCRIPTION))
            .thenReturn((String) propertyStubsMap.get(NiftyConstants.DESCRIPTION));
        Mockito.when(mockedParameters.getString(NiftyConstants.PROJECT))
            .thenReturn((String) propertyStubsMap.get(NiftyConstants.PROJECT));
        Mockito.when(mockedParameters.getString(NiftyConstants.TASK_GROUP_ID))
            .thenReturn((String) propertyStubsMap.get(NiftyConstants.TASK_GROUP_ID));
        Mockito.when(mockedParameters.getString(NiftyConstants.DUE_DATE))
            .thenReturn((String) propertyStubsMap.get(NiftyConstants.DUE_DATE));

        Object result = NiftyCreateTaskAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(NiftyConstants.NAME, "name");
        propertyStubsMap.put(NiftyConstants.DESCRIPTION, "description");
        propertyStubsMap.put(NiftyConstants.TASK_GROUP_ID, "task_group_id");
        propertyStubsMap.put(NiftyConstants.DUE_DATE, "due_date");

        return propertyStubsMap;
    }
}
