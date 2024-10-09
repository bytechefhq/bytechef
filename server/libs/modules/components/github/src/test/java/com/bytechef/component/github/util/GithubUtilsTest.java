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

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Luka Ljubic
 * @author Monika Kušter
 */
class GithubUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final TriggerDefinition.WebhookBody mockedWebhookBody = mock(TriggerDefinition.WebhookBody.class);

    @BeforeEach()
    void beforeEach() {
        when(mockedActionContext.http(any()))
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
    void testGetContent() {
        Map<String, String> content = Map.of("action", "opened");

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(content);

        assertEquals(content, GithubUtils.getContent(mockedWebhookBody));
    }

    @Test
    void testGetRepositoryOptions() {
        List<Map<String, Object>> body = new ArrayList<>();
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("name", "taskName");
        items.put("id", "123");
        body.add(items);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("taskName", "taskName"));

        assertEquals(expectedOptions,
            GithubUtils.getRepositoryOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetIssueOptions() {
        List<Map<String, Object>> body = new ArrayList<>();
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("title", "taskName");
        items.put("number", 123);
        body.add(items);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("taskName", "123"));

        assertEquals(expectedOptions,
            GithubUtils.getIssueOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetCollaborators() {
        List<Map<String, Object>> body = new ArrayList<>();
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("name", "John Doe");
        items.put("login", "jdTest123");
        body.add(items);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        List<Option<String>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("John Doe", "jdTest123"));

        assertEquals(expectedOptions,
            GithubUtils.getCollaborators(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testGetOwnerName() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("login", "name");

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        String actualOwnerName = GithubUtils.getOwnerName(mockedActionContext);
        String expectedOwnerName = "name";

        assertEquals(expectedOwnerName, actualOwnerName);
    }

    @Test
    void testSubscribeWebhook() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, 123));

        Integer id = GithubUtils.subscribeWebhook("", "event", "webhookUrl", mockedTriggerContext);

        assertEquals(123, id);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            "events", List.of("event"),
            "config", Map.of("url", "webhookUrl", "content_type", "json"));

        assertEquals(expectedBody, body.getContent());
    }

    @Test
    void testUnsubscribeWebhook() {
        GithubUtils.unsubscribeWebhook("", 123, mockedTriggerContext);

        verify(mockedTriggerContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }
}
