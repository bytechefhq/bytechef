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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Vihar Shah
 */
class JiraCreateIssueCommentActionTest extends AbstractJiraActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        when(mockedParameters.getRequiredString("comment"))
            .thenReturn("This is a comment");

        Object result = JiraCreateIssueCommentAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of("body", Map.of(
            "content", List.of(
                Map.of(
                    "content", List.of("text", "This is a comment", "type", "text"),
                    "type", "paragraph"))));

        assertEquals(expectedBody, body.getContent());
    }
}
