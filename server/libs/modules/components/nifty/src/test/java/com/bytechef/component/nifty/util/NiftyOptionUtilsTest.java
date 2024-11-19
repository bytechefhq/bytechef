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

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.nifty.constant.NiftyConstants.ID;
import static com.bytechef.component.nifty.constant.NiftyConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Luka Ljubić
 */
class NiftyOptionUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("abc", "123"));
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> queryNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> queryValueArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);

    @BeforeEach
    public void beforeEach() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(queryNameArgumentCaptor.capture(), queryValueArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetTaskAppIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("apps", List.of(Map.of(NAME, "abc", ID, "123"))));

        assertEquals(
            expectedOptions,
            NiftyOptionUtils.getAppIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedTriggerContext));

        Object[] query = queryArgumentCaptor.getValue();

        assertEquals(List.of("limit", 100, "offset", 0), Arrays.asList(query));
    }

    @Test
    void testGetTaskGroupIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("items", List.of(Map.of(NAME, "abc", ID, "123"))));

        assertEquals(
            expectedOptions,
            NiftyOptionUtils.getTaskGroupIdOptions(mockedParameters, mockedParameters, Map.of(), "",
                mockedActionContext));
    }

    @Test
    void testGetProjectIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("projects", List.of(Map.of(NAME, "abc", ID, "123"))));

        assertEquals(
            expectedOptions,
            NiftyOptionUtils.getProjectIdOptions(mockedParameters, mockedParameters, Map.of(), "",
                mockedActionContext));
    }

    @Test
    void testGetProjectTemplateOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("items", List.of(Map.of(NAME, "abc", ID, "123"))));

        assertEquals(
            expectedOptions,
            NiftyOptionUtils.getProjectTemplateOptions(mockedParameters, mockedParameters, Map.of(), "",
                mockedActionContext));

        assertEquals("type", queryNameArgumentCaptor.getValue());
        assertEquals("project", queryValueArgumentCaptor.getValue());
    }
}
