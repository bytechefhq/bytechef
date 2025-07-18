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

package com.bytechef.component.retable.action;

import static com.bytechef.component.retable.constant.RetableConstants.PROJECT_ID;
import static com.bytechef.component.retable.constant.RetableConstants.RETABLE_ID;
import static com.bytechef.component.retable.constant.RetableConstants.ROWS_IDS;
import static com.bytechef.component.retable.constant.RetableConstants.ROW_ID;
import static com.bytechef.component.retable.constant.RetableConstants.WORKSPACE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class RetableUpdateRowActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(WORKSPACE_ID, "1", PROJECT_ID, "2", RETABLE_ID, "3", ROW_ID, 4, ROWS_IDS, Map.of("5", "test")));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = RetableUpdateRowAction.perform(parameters, parameters, mockedContext);

        Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            "rows", List.of(
                Map.of(
                    ROW_ID, 4,
                    "columns", List.of(Map.of("column_id", "5", "update_cell_value", "test")))));

        assertEquals(expectedBody, body.getContent());
        assertEquals(mockedObject, result);
    }
}
