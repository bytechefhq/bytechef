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

package com.bytechef.component.retable.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class RetableUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);

    List<Option<String>> expected = List.of(
        option("One", "1"),
        option("Two", "2"));

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
    }

    @Test
    void testGetWorkspaceOptions() {

        Map<String, Object> mockResponse = Map.of(
            "data",
            Map.of("workspaces",
                List.of(
                    Map.of("name", "One", "id", "1"),
                    Map.of("name", "Two", "id", "2"))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = RetableUtils.getWorkspaceOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expected, result);
    }

    @Test
    void testGetProjectOptions() {

        Map<String, Object> mockResponse = Map.of(
            "data",
            Map.of("projects",
                List.of(
                    Map.of("name", "One", "id", "1"),
                    Map.of("name", "Two", "id", "2"))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = RetableUtils.getProjectOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expected, result);
    }

    @Test
    void testGetRetableOptions() {

        Map<String, Object> mockResponse = Map.of(
            "data",
            Map.of("retables",
                List.of(
                    Map.of("title", "One", "id", "1"),
                    Map.of("title", "Two", "id", "2"))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = RetableUtils.getRetableOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expected, result);
    }
}
