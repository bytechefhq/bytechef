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

package com.bytechef.component.nifty.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Luka Ljubić
 */
class NiftyOptionUtilsTest {

    private final ActionContext mockedContext = Mockito.mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = Mockito.mock(Context.Http.Executor.class);
    private final Parameters mockedParameters = Mockito.mock(Parameters.class);
    private final Context.Http.Response mockedResponse = Mockito.mock(Context.Http.Response.class);

    @BeforeEach
    public void beforeEach() {

        Mockito.when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        Mockito.when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);
        Mockito.when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        Mockito.when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetTaskGroupIdOptions() {
        Map<String, Object> body = new HashMap<>();
        Map<String, String> task = new LinkedHashMap<>();
        task.put("name", "taskName");
        task.put("id", "123");
        body.put("items", List.of(task));

        Mockito.when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(body);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("taskName", "123"));

        assertEquals(expectedOptions,
            NiftyOptionUtils.getTaskGroupIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetProjectIdOptions() {
        Map<String, Object> body = new HashMap<>();
        Map<String, String> task = new LinkedHashMap<>();
        task.put("name", "ProjectName");
        task.put("id", "123");
        body.put("projects", List.of(task));

        Mockito.when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(body);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("ProjectName", "123"));

        assertEquals(expectedOptions,
            NiftyOptionUtils.getProjectIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }
}
