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

package com.bytechef.component.linear.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.linear.constant.LinearConstants.ALL_PUBLIC_TEAMS;
import static com.bytechef.component.linear.constant.LinearConstants.QUERY;
import static com.bytechef.component.linear.constant.LinearConstants.TEAM_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class LinearUtilsTest {

    private Parameters mockedInputParameters = mock(Parameters.class);
    private final Parameters mockedConnectionParameters = mock(Parameters.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final List<Option<String>> expected = List.of(option("test1", "1"), option("test2", "2"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach(Http mockedHttp, Executor mockedExecutor) {
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
    }

    @Test
    void testGetAssigneeOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> graphqlResponse = Map.of(
            "data", Map.of(
                "users", Map.of(
                    "nodes", List.of(
                        Map.of("id", "1", "displayName", "test1"),
                        Map.of("id", "2", "displayName", "test2")))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(graphqlResponse);

        List<Option<String>> result = LinearUtils.getAssigneeOptions(
            mockedInputParameters, mockedConnectionParameters, Map.of(), "", mockedContext);

        assertEquals(result, expected);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/graphql", stringArgumentCaptor.getValue());
        assertEquals(Body.of(Map.of(QUERY, "{users{nodes{id displayName}}}")), bodyArgumentCaptor.getValue());
    }

    @Test
    void testGetIssueOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedInputParameters = MockParametersFactory.create(Map.of(TEAM_ID, "abc"));

        Map<String, Object> graphqlResponse = Map.of(
            "data", Map.of(
                "issues", Map.of(
                    "nodes", List.of(
                        Map.of("id", "1", "title", "test1"),
                        Map.of("id", "2", "title", "test2")))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(graphqlResponse);

        List<Option<String>> result = LinearUtils.getIssueOptions(
            mockedInputParameters, mockedConnectionParameters, Map.of(), "", mockedContext);

        assertEquals(result, expected);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/graphql", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(Map.of(QUERY, "{issues(filter: {team: {id: {eq: \"abc\" }}}){nodes{id title}}} ")),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testGetProjectOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> graphqlResponse = Map.of(
            "data", Map.of(
                "projects", Map.of(
                    "nodes", List.of(
                        Map.of("id", "1", "name", "test1"),
                        Map.of("id", "2", "name", "test2")))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(graphqlResponse);

        List<Option<String>> result = LinearUtils.getProjectOptions(
            mockedInputParameters, mockedConnectionParameters, Map.of(), "", mockedContext);

        assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    void testGetProjectStateOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> graphqlResponse = Map.of(
            "data", Map.of(
                "projectStatuses", Map.of(
                    "nodes", List.of(
                        Map.of("id", "1", "name", "test1"),
                        Map.of("id", "2", "name", "test2")))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(graphqlResponse);

        List<Option<String>> result = LinearUtils.getProjectStateOptions(
            mockedInputParameters, mockedConnectionParameters, Map.of(), "", mockedContext);

        assertEquals(result, expected);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/graphql", stringArgumentCaptor.getValue());
        assertEquals(Body.of(Map.of(QUERY, "{projectStatuses {nodes {id name}}}")), bodyArgumentCaptor.getValue());
    }

    @Test
    void testGetIssueStateOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> graphqlResponse = Map.of(
            "data", Map.of(
                "workflowStates", Map.of(
                    "nodes", List.of(
                        Map.of("id", "1", "name", "test1"),
                        Map.of("id", "2", "name", "test2")))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(graphqlResponse);

        List<Option<String>> result = LinearUtils.getIssueStateOptions(
            mockedInputParameters, mockedConnectionParameters, Map.of(), "", mockedContext);

        assertEquals(result, expected);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/graphql", stringArgumentCaptor.getValue());
        assertEquals(Body.of(Map.of(QUERY, "{workflowStates{nodes{id name}}}")), bodyArgumentCaptor.getValue());
    }

    @Test
    void testGetTeamOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> graphqlResponse = Map.of(
            "data", Map.of(
                "teams", Map.of(
                    "nodes", List.of(
                        Map.of("id", "1", "name", "test1"),
                        Map.of("id", "2", "name", "test2")))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(graphqlResponse);

        List<Option<String>> result = LinearUtils.getTeamOptions(
            mockedInputParameters, mockedConnectionParameters, Map.of(), "", mockedContext);

        assertEquals(result, expected);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/graphql", stringArgumentCaptor.getValue());
        assertEquals(Body.of(Map.of(QUERY, "{teams{nodes{id name}}}")), bodyArgumentCaptor.getValue());
    }

    @Test
    void testExecuteGraphQLQuery(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String query = "{users{nodes{id displayName}}}";
        Map<String, Object> expectedResponse = Map.of("data", Map.of("key", "value"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(expectedResponse);

        Map<String, Object> result = LinearUtils.executeGraphQLQuery(query, mockedContext);

        assertEquals(result, expectedResponse);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/graphql", stringArgumentCaptor.getValue());
        assertEquals(Body.of(Map.of(QUERY, query)), bodyArgumentCaptor.getValue());
    }

    @Test
    void testExecuteIssueTriggerQuery() {
        WebhookBody mockedBody = mock(WebhookBody.class);

        Map<String, Object> mockIssue = Map.of("id", "123");
        Map<String, Object> webhookPayload = Map.of(
            "action", "create",
            "data", Map.of("id", "123"));

        when(mockedBody.getContent(any(TypeReference.class)))
            .thenReturn(webhookPayload);

        Object result = LinearUtils.executeIssueTriggerQuery("create", mockedBody);

        assertEquals(mockIssue, result);
    }

    @Test
    void testCreateWebhook(
        TriggerContext mockedTriggerContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedInputParameters = MockParametersFactory.create(Map.of(ALL_PUBLIC_TEAMS, false, TEAM_ID, "xy"));

        String url = "https://example.com/webhook";
        Map<String, Object> graphqlResponse = Map.of(
            "data", Map.of(
                "webhookCreate", Map.of(
                    "webhook", Map.of("id", "abc123"))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(graphqlResponse);

        WebhookEnableOutput result = LinearUtils.createWebhook(url, mockedTriggerContext, mockedInputParameters);

        WebhookEnableOutput webhookEnableOutput = new WebhookEnableOutput(Map.of("id", "abc123"), null);

        assertEquals(webhookEnableOutput, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/graphql", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                Map.of(
                    QUERY,
                    "mutation {webhookCreate(input: {url: \"" + url
                        + "\", teamId: \"xy\", resourceTypes: [\"Issue\"]}) {webhook {id}}}")),
            bodyArgumentCaptor.getValue());
    }
}
