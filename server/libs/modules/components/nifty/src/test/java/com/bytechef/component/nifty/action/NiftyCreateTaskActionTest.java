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

import static com.bytechef.component.nifty.constant.NiftyConstants.DESCRIPTION;
import static com.bytechef.component.nifty.constant.NiftyConstants.DUE_DATE;
import static com.bytechef.component.nifty.constant.NiftyConstants.NAME;
import static com.bytechef.component.nifty.constant.NiftyConstants.PROJECT;
import static com.bytechef.component.nifty.constant.NiftyConstants.TASK_GROUP_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Luka Ljubić
 */
class NiftyCreateTaskActionTest {

    ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    ActionContext mockedContext = mock(ActionContext.class);
    Http.Executor mockedExecutor = mock(Http.Executor.class);
    Parameters mockedParameters = mock(Parameters.class);
    Http.Response mockedResponse = mock(Http.Response.class);
    Map<String, Object> responeseMap = Map.of("key", "value");

    @BeforeEach
    public void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responeseMap);
    }

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(NAME))
            .thenReturn((String) propertyStubsMap.get(NAME));
        when(mockedParameters.getString(DESCRIPTION))
            .thenReturn((String) propertyStubsMap.get(DESCRIPTION));
        when(mockedParameters.getString(PROJECT))
            .thenReturn((String) propertyStubsMap.get(PROJECT));
        when(mockedParameters.getRequiredString(TASK_GROUP_ID))
            .thenReturn((String) propertyStubsMap.get(TASK_GROUP_ID));
        when(mockedParameters.getLocalDateTime(DUE_DATE))
            .thenReturn((LocalDateTime) propertyStubsMap.get(DUE_DATE));

        Object result = NiftyCreateTaskAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(NAME, "name");
        propertyStubsMap.put(DESCRIPTION, "description");
        propertyStubsMap.put(TASK_GROUP_ID, "task_group_id");
        propertyStubsMap.put(DUE_DATE, LocalDateTime.of(2000, 1, 1, 1, 1, 1));

        return propertyStubsMap;
    }
}
