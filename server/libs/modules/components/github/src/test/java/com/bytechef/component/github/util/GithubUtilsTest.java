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

package com.bytechef.component.github.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Luka Ljubic
 */
class GithubUtilsTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @BeforeEach
    public void beforeEach() {

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetRepositoryOptions() {
        List<Map<String, Object>> body = new ArrayList<>();
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("name", "taskName");
        items.put("id", "123");
        body.add(items);

        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(body);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("taskName", "taskName"));

        assertEquals(expectedOptions,
            GithubUtils.getRepositoryOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetIssueOptions() {
        List<Map<String, Object>> body = new ArrayList<>();
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("title", "taskName");
        items.put("number", 123);
        body.add(items);

        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(body);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("taskName", "123"));

        assertEquals(expectedOptions,
            GithubUtils.getIssueOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));
    }

    @Test
    void testGetOwnerName() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("login", "name");

        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(body);

        String actualOwnerName = GithubUtils.getOwnerName(mockedContext);
        String expectedOwnerName = "name";

        assertEquals(expectedOwnerName, actualOwnerName);
    }
}
