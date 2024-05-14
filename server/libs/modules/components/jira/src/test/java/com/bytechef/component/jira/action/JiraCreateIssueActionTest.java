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

package com.bytechef.component.jira.action;

import static com.bytechef.component.jira.constant.JiraConstants.ASSIGNEE;
import static com.bytechef.component.jira.constant.JiraConstants.CONTENT;
import static com.bytechef.component.jira.constant.JiraConstants.DESCRIPTION;
import static com.bytechef.component.jira.constant.JiraConstants.FIELDS;
import static com.bytechef.component.jira.constant.JiraConstants.ID;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_TYPE;
import static com.bytechef.component.jira.constant.JiraConstants.PRIORITY;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.SUMMARY;
import static com.bytechef.component.jira.constant.JiraConstants.TEXT;
import static com.bytechef.component.jira.constant.JiraConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class JiraCreateIssueActionTest extends AbstractJiraActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredString(PROJECT))
            .thenReturn("1");
        when(mockedParameters.getRequiredString(ISSUE_TYPE))
            .thenReturn("1");
        when(mockedParameters.getRequiredString(SUMMARY))
            .thenReturn("summary");
        when(mockedParameters.getString(ASSIGNEE))
            .thenReturn("1");
        when(mockedParameters.getString(PRIORITY))
            .thenReturn("1");
        when(mockedParameters.getString(DESCRIPTION))
            .thenReturn("description");

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responseMap);

        Object result = JiraCreateIssueAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedFieldsMap = getExpectedFieldsMap();

        assertEquals(Map.of(FIELDS, expectedFieldsMap), body.getContent());
    }

    private static Map<String, Object> getExpectedFieldsMap() {
        Map<String, String> idMap = Map.of(ID, "1");

        return Map.of(PROJECT, idMap, ISSUE_TYPE, idMap, SUMMARY, "summary", ASSIGNEE, idMap, PRIORITY, idMap,
            DESCRIPTION, Map.of(CONTENT, List.of(
                Map.of(
                    CONTENT, List.of(
                        Map.of(
                            TEXT, "description",
                            TYPE, TEXT)),
                    TYPE, "paragraph")),
                TYPE, "doc",
                "version", 1));
    }
}
