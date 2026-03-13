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

package com.bytechef.component.asana.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
@ExtendWith(MockContextSetupExtension.class)
class AsanaUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("name", "gid"));
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of("data", Map.of("workspace", "data.workspace", "project", "data.project")));
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach(Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("data", List.of(Map.of("name", "name", "gid", "gid"))));
    }

    @Test
    void testGetAssigneeOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Option<String>> result = AsanaUtils.getAssigneeOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/users", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            "limit", 100, "offset", null, "workspace", "data.workspace",
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetProjectOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Option<String>> result =
            AsanaUtils.getProjectOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/projects", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            "limit", 100, "offset", null, "workspace", "data.workspace",
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetTagsOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Option<String>> result =
            AsanaUtils.getTagsOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/tags", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            "limit", 100, "offset", null,
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetTeamOptionsOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Option<String>> result =
            AsanaUtils.getTeamOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/workspaces/data.workspace/teams", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            "limit", 100, "offset", null,
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetWorkspaceOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Option<String>> workspaceOptions = AsanaUtils.getWorkspaceOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, workspaceOptions);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/workspaces", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            "limit", 100, "offset", null,
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetTaskGidOptions(
        Context mockedContext, ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Option<String>> taskGidOptions = AsanaUtils.getTaskGidOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        assertEquals(expectedOptions, taskGidOptions);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/projects/data.project/tasks", stringArgumentCaptor.getValue());

        Object[] expectedQueryParameters = {
            "limit", 100, "offset", null, "opt_fields", "gid,name"
        };

        assertArrayEquals(expectedQueryParameters, objectsArgumentCaptor.getValue());
    }
}
