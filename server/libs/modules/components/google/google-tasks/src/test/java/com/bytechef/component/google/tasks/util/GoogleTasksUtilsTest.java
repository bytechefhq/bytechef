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

package com.bytechef.component.google.tasks.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
public class GoogleTasksUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private Parameters mockedParameters;
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    private final Map<String, Object> responseMap = Map.of(
        "items", List.of(
            Map.of("title", "List 1", "id", "list1"),
            Map.of("title", "List 2", "id", "list2")));

    @Test
    void getListsIdOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        List<Option<String>> result = GoogleTasksUtils.getListsIdOptions(
            mock(Parameters.class), mock(Parameters.class), Map.of(), "", mockedContext);

        assertEquals(2, result.size());
        assertEquals("List 1", result.get(0).getLabel());
        assertEquals("list1", result.get(0).getValue());
        assertEquals("List 2", result.get(1).getLabel());
        assertEquals("list2", result.get(1).getValue());
    }

    @Test
    void getTasksIdOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        List<Option<String>> result = GoogleTasksUtils.getTasksIdOptions(
            mockedParameters, mock(Parameters.class), Map.of(), "", mockedContext);

        assertEquals(2, result.size());
        assertEquals("List 1", result.getFirst().getLabel());
        assertEquals("list1", result.getFirst().getValue());
    }

    @Test
    void createTasksRequestBody() {
        mockedParameters = MockParametersFactory.create(
            Map.of("title", "Test Task", "status", "needsAction", "notes", "Some notes"));

        Map<String, Object> result = GoogleTasksUtils.createTaskRequestBody(mockedParameters);

        assertEquals(3, result.size());
        assertEquals("Test Task", result.get("title"));
        assertEquals("needsAction", result.get("status"));
        assertEquals("Some notes", result.get("notes"));
    }
}
