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
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_ID;
import static com.bytechef.component.jira.constant.JiraConstants.JQL;
import static com.bytechef.component.jira.constant.JiraConstants.MAX_RESULTS;
import static com.bytechef.component.jira.constant.JiraConstants.NAME;
import static com.bytechef.component.jira.constant.JiraConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.SUMMARY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
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
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class JiraOptionsUtilsTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final List<Option<String>> expectedOptions = List.of(option("abc", "123"));
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(PROJECT, "test"));
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach(Http.Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
    }

    @Test
    void testGetIssueIdOptions(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ISSUES, List.of(Map.of(FIELDS, Map.of(SUMMARY, "abc"), ID, "123"))));

        try (MockedStatic<JiraUtils> jiraUtilsMockedStatic = mockStatic(JiraUtils.class)) {
            jiraUtilsMockedStatic
                .when(() -> JiraUtils.getProjectName(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture(),
                    contextArgumentCaptor.capture()))
                .thenReturn("PROJECT");

            List<Option<String>> result = JiraOptionsUtils.getIssueIdOptions(
                mockedParameters, mockedParameters, Map.of(), "", mockedContext);

            assertEquals(expectedOptions, result);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(List.of("/search/jql"), stringArgumentCaptor.getAllValues());

            Object[] objects = {
                JQL, PROJECT + "=\"PROJECT\"", FIELDS, SUMMARY, MAX_RESULTS, 5000, NEXT_PAGE_TOKEN, null
            };

            assertArrayEquals(objects, objectsArgumentCaptor.getValue());

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

            Http.Configuration configuration = configurationBuilder.build();

            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        }
    }

    @Test
    void testGetIssueTypesIdOptions(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of(NAME, "abc", ID, "123")));

        List<Option<String>> result = JiraOptionsUtils.getIssueTypesIdOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(expectedOptions, result);
        assertEquals(List.of("/issuetype/project", "projectId", "test"), stringArgumentCaptor.getAllValues());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
    }

    @Test
    void testGetProjectIdOptions(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("values", List.of(Map.of(NAME, "abc", ID, "123"))));

        List<Option<String>> result = JiraOptionsUtils.getProjectIdOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(expectedOptions, result);
        assertEquals("/project/search", stringArgumentCaptor.getValue());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());

        Object[] objects = {
            MAX_RESULTS, 100
        };

        assertArrayEquals(objects, objectsArgumentCaptor.getValue());
    }

    @Test
    void testGetStatusIdOptions(
        Context mockedContext, Http.Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> transition = Map.of("id", "123", "to", Map.of("name", "In Progress"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("transitions", List.of(transition)));

        Parameters parameters = MockParametersFactory.create(Map.of(ISSUE_ID, "testIssue"));

        List<Option<String>> result = JiraOptionsUtils.getStatusIdOptions(
            parameters, null, null, null, mockedContext);

        List<Option<String>> expected = List.of(option("In Progress", "123"));
        assertEquals(expected, result);

        assertEquals(List.of("/issue/testIssue/transitions"), stringArgumentCaptor.getAllValues());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
    }

    @Test
    void testGetUserIdOptions(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(
                Map.of("displayName", "abc", "accountId", "123", "accountType", "atlassian"),
                Map.of("displayName", "xyz", "accountId", "cde", "accountType", "apps")));

        List<Option<String>> result = JiraOptionsUtils.getUserIdOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(expectedOptions, result);
        assertEquals("/users/search", stringArgumentCaptor.getValue());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());

        Object[] objects = {
            MAX_RESULTS, 1000
        };

        assertArrayEquals(objects, objectsArgumentCaptor.getValue());
    }

    @Test
    void testPriorityIdOptions(
        Context mockedContext, Http.Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of(NAME, "abc", ID, "123")));

        List<Option<String>> result = JiraOptionsUtils.getPriorityIdOptions(
            mockedParameters, null, null, null, mockedContext);

        assertEquals(expectedOptions, result);
        assertEquals("/priority", stringArgumentCaptor.getValue());

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
    }
}
