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

package com.bytechef.component.nifty.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.nifty.constant.NiftyConstants.ID;
import static com.bytechef.component.nifty.constant.NiftyConstants.NAME;
import static com.bytechef.component.nifty.constant.NiftyConstants.PROJECT;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.bytechef.component.definition.TriggerContext;
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
 * @author Luka Ljubić
 */
@ExtendWith(MockContextSetupExtension.class)
class NiftyUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("abc", "123"));
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @BeforeEach
    void beforeEach(Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
    }

    @Test
    void testGetTaskAppIdOptions(
        TriggerContext mockedTriggerContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "apps", List.of(Map.of(NAME, "abc", ID, "123")),
                "hasMore", false));

        assertEquals(
            expectedOptions,
            NiftyUtils.getAppIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedTriggerContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/apps", stringArgumentCaptor.getValue());

        Object[] query = {
            "limit", 100, "offset", 0
        };

        assertArrayEquals(query, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetTaskGroupIdOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedInputParameters = MockParametersFactory.create(Map.of(PROJECT, "123"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "items", List.of(Map.of(NAME, "abc", ID, "123")),
                "hasMore", false));

        assertEquals(
            expectedOptions,
            NiftyUtils.getTaskGroupIdOptions(mockedInputParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/taskgroups", stringArgumentCaptor.getValue());

        Object[] query = {
            "limit", 100, "offset", 0, "project_id", "123", "archived", "false"
        };

        assertArrayEquals(query, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetProjectIdOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "projects", List.of(Map.of(NAME, "abc", ID, "123")),
                "hasMore", false));

        assertEquals(
            expectedOptions,
            NiftyUtils.getProjectIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/projects", stringArgumentCaptor.getValue());

        Object[] query = {
            "limit", 100, "offset", 0
        };

        assertArrayEquals(query, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetTemplateIdOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("items", List.of(Map.of(NAME, "abc", ID, "123"))));

        assertEquals(
            expectedOptions,
            NiftyUtils.getTemplateIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/templates", stringArgumentCaptor.getValue());

        Object[] query = {
            "limit", 100, "offset", 0, "type", "project"
        };

        assertArrayEquals(query, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetTaskIdOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "tasks", List.of(Map.of(NAME, "abc", ID, "123")),
                "hasMore", false));

        assertEquals(
            expectedOptions,
            NiftyUtils.getTaskIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/tasks", stringArgumentCaptor.getValue());

        Object[] query = {
            "limit", 100, "offset", 0
        };

        assertArrayEquals(query, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetLabelsOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "items", List.of(Map.of(NAME, "abc", ID, "123")),
                "hasMore", false));

        assertEquals(
            expectedOptions,
            NiftyUtils.getLabelsOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/labels", stringArgumentCaptor.getValue());

        Object[] query = {
            "limit", 100, "offset", 0
        };

        assertArrayEquals(query, objectsArgumentCaptor.getValue());
    }
}
