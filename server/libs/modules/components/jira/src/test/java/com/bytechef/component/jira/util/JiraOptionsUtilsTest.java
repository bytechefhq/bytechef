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

package com.bytechef.component.jira.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.jira.constant.JiraConstants.FIELDS;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUES;
import static com.bytechef.component.jira.constant.JiraConstants.JQL;
import static com.bytechef.component.jira.constant.JiraConstants.NAME;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.SUMMARY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class JiraOptionsUtilsTest {

    protected ArgumentCaptor<ActionContext> actionContextArgumentCaptor =
        ArgumentCaptor.forClass(ActionContext.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(PROJECT, "test"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private List<Option<String>> result;
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final List<Option<String>> expectedOptions = List.of(option("abc", "123"));

    @BeforeEach
    void beforeEach() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetIssueIdOptions() {
        Map<String, Object> valuesMap = Map.of(ISSUES, List.of(Map.of(FIELDS, Map.of(SUMMARY, "abc"), ID, "123")));

        when(mockedExecutor.queryParameters(
            stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
            stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(valuesMap);

        try (MockedStatic<JiraUtils> jiraUtilsMockedStatic = mockStatic(JiraUtils.class)) {
            jiraUtilsMockedStatic
                .when(() -> JiraUtils.getProjectName(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture(),
                    actionContextArgumentCaptor.capture()))
                .thenReturn("PROJECT");

            result = JiraOptionsUtils.getIssueIdOptions(
                mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

            assertEquals(expectedOptions, result);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
            assertEquals(List.of(JQL, PROJECT + "=\"PROJECT\"", FIELDS, SUMMARY), stringArgumentCaptor.getAllValues());
        }
    }

    @Test
    void testGetIssueTypesIdOptions() {
        List<Object> list = List.of(Map.of(NAME, "abc", ID, "123"));

        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(list);

        result = JiraOptionsUtils.getIssueTypesIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        assertEquals(expectedOptions, result);
        assertEquals(List.of("projectId", "test"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testPriorityIdOptions() {
        List<Object> list = List.of(Map.of(NAME, "abc", ID, "123"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(list);

        result = JiraOptionsUtils.getPriorityIdOptions(mockedParameters, mockedParameters, Map.of(), "",
            mockedActionContext);

        assertEquals(expectedOptions, result);
    }

    @Test
    void testGetProjectIdOptions() {
        Map<String, Object> valuesMap = Map.of("values", List.of(Map.of(NAME, "abc", ID, "123")));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(valuesMap);

        result = JiraOptionsUtils.getProjectIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        assertEquals(expectedOptions, result);
    }

    @Test
    void testGetUserIdOptions() {
        List<Object> list = List.of(Map.of("displayName", "abc", "accountId", "123"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(list);

        result = JiraOptionsUtils.getUserIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedActionContext);

        assertEquals(expectedOptions, result);
    }
}
