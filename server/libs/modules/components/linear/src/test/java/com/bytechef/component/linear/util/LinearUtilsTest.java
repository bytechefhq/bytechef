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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class LinearUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final Parameters mockedInputParameters = mock(Parameters.class);
    private final Parameters mockedConnectionParameters = mock(Parameters.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final List<Option<String>> expected = List.of(
        option("test1", "1"),
        option("test2", "2"));

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetAssigneeOptions() {
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

        assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    void testGetIssueOptions() {
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

        assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    void testGetProjectOptions() {
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
    void testGetProjectStateOptions() {
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

        assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    void testGetIssueStateOptions() {
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

        assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    void testGetTeamOptions() {
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

        assertThat(result, Matchers.containsInAnyOrder(expected.toArray()));
    }

    @Test
    void testExecuteGraphQLQuery() {
        String query = "{users{nodes{id displayName}}}";
        Map<String, Object> expectedResponse = Map.of("data", Map.of("key", "value"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(expectedResponse);

        Map<String, Object> result = LinearUtils.executeGraphQLQuery(query, mockedContext);

        assertEquals(expectedResponse, result);
    }

    @Test
    void testExecuteIssueTriggerQuery() {
        WebhookBody mockedBody = mock(WebhookBody.class);

        Map<String, Object> mockIssue = Map.of("id", "123");
        Map<String, Object> graphqlResponse = Map.of("data", Map.of("issue", mockIssue));
        Map<String, Object> webhookPayload = Map.of(
            "action", "create",
            "data", Map.of("id", "123"));

        when(mockedBody.getContent(any(TypeReference.class)))
            .thenReturn(webhookPayload);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(graphqlResponse);

        Object result = LinearUtils.executeIssueTriggerQuery("create", mockedBody, mockedTriggerContext);

        assertEquals(mockIssue, result);
    }

    @Test
    void testCreateWebhook() {
        String url = "https://example.com/webhook";
        Map<String, Object> graphqlResponse = Map.of(
            "data", Map.of(
                "webhookCreate", Map.of(
                    "webhook", Map.of("id", "abc123"))));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(graphqlResponse);

        WebhookEnableOutput result = LinearUtils.createWebhook(url, mockedTriggerContext, mockedInputParameters);

        assertEquals(Map.of("id", "abc123"), result.parameters());
        assertNull(result.webhookExpirationDate());
    }
}
