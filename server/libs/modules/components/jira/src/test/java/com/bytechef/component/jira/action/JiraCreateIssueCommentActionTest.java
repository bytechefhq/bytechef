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

package com.bytechef.component.jira.action;

import static com.bytechef.component.jira.constant.JiraConstants.COMMENT;
import static com.bytechef.component.jira.constant.JiraConstants.ISSUE_ID;
import static com.bytechef.component.jira.constant.JiraConstants.TEXT;
import static com.bytechef.component.jira.constant.JiraConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Vihar Shah
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class JiraCreateIssueCommentActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ISSUE_ID, "xy", COMMENT, "This is a comment"));
    private final Map<String, Object> responseMap = Map.of("key", "value");
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Http.Response mockedResponse, Http.Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(responseMap);

        Object result = JiraCreateIssueCommentAction.perform(mockedParameters, null, mockedContext);

        assertEquals(responseMap, result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/issue/xy/comment", stringArgumentCaptor.getValue());

        Map<String, Object> expectedBody = Map.of("body", Map.of(
            "version", 1,
            TYPE, "doc",
            "content", List.of(
                Map.of(
                    "content", List.of(Map.of(TEXT, "This is a comment", TYPE, TEXT)),
                    TYPE, "paragraph"))));

        assertEquals(Http.Body.of(expectedBody, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }
}
