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

package com.bytechef.component.asana.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class AsanaUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private static final Map<String, List<Map<String, String>>> map = new LinkedHashMap<>();
    private static final List<Option<String>> expectedOptions = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        List<Map<String, String>> data = new ArrayList<>();
        Map<String, String> dataMap = new LinkedHashMap<>();

        dataMap.put("name", "name");
        dataMap.put("gid", "gid");

        data.add(dataMap);

        map.put("data", data);

        expectedOptions.add(option("name", "gid"));
    }

    @BeforeEach
     void beforeEach() {
         when(mockedContext.http(any()))
             .thenReturn(mockedExecutor);
         when(mockedExecutor.configuration(any()))
             .thenReturn(mockedExecutor);
         when(mockedExecutor.execute())
             .thenReturn(mockedResponse);
         when(mockedResponse.getBody(any(TypeReference.class)))
             .thenReturn(map);
     }

    @Test
    void testGetAssigneeOptions() {
        assertEquals(
            expectedOptions,
            AsanaUtils.getAssigneeOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetProjectIdOptions() {
        assertEquals(
            expectedOptions,
            AsanaUtils.getProjectIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetTagOptions() {
        assertEquals(
            expectedOptions,
            AsanaUtils.getTagOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetTeamOptionsOptions() {
        assertEquals(
            expectedOptions,
            AsanaUtils.getTeamOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetWorkspaceIdOptions() {
        assertEquals(
            expectedOptions,
            AsanaUtils.getWorkspaceIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }
}
