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

import static com.bytechef.component.jira.constant.JiraConstants.CONTENT;
import static com.bytechef.component.jira.constant.JiraConstants.DESCRIPTION;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUETYPE;
import static com.bytechef.component.jira.constant.JiraConstants.NAME;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.TEXT;
import static com.bytechef.component.jira.constant.JiraConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class JiraUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);

    @Test
    void testGetProjectName() {
        Map<String, Object> valuesMap = Map.of(NAME, "name");

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(valuesMap);

        assertEquals("name", JiraUtils.getProjectName(mockedParameters, mockedParameters, mockedActionContext));
    }

    @Test
    void testSubscribeWebhok() {
        when(mockedParameters.getRequiredString(PROJECT))
            .thenReturn("new");
        when(mockedParameters.getString(ISSUETYPE))
            .thenReturn("task");

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("webhookRegistrationResult", List.of(Map.of("createdWebhookId", 123))));

        assertEquals(123,
            JiraUtils.subscribeWebhook(mockedParameters, "webhookUrl", mockedTriggerContext, "event"));

        Http.Body body = bodyArgumentCaptor.getValue();

        HashMap<String, Object> stringHashMap = new HashMap<>();

        stringHashMap.put("url", "webhookUrl");
        stringHashMap.put("webhooks", List.of(
            Map.of(
                "events", List.of("event"),
                "jqlFilter", PROJECT + " = new AND " + ISSUETYPE + " = task")));

        assertEquals(stringHashMap, body.getContent());
    }

    @Test
    void testUnsubscribeWebhook() {
        when(mockedParameters.getInteger(ID))
            .thenReturn(123);

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        JiraUtils.unsubscribeWebhook(mockedParameters, mockedTriggerContext);

        verify(mockedTriggerContext, times(1)).http(any());

        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("webhookIds", List.of(123)), body.getContent());
    }

    @Test
    void testAddDescriptionField() {
        Map<String, Object> fields = new HashMap<>();
        String description = "description";

        JiraUtils.addDescriptionField(fields, description);

        Map<String, Object> expected = Map.of(DESCRIPTION, Map.of(
            CONTENT, List.of(
                Map.of(
                    CONTENT, List.of(
                        Map.of(
                            TEXT, description,
                            TYPE, TEXT)),
                    TYPE, "paragraph")),
            TYPE, "doc",
            "version", 1));

        assertEquals(expected, fields);
    }
}
