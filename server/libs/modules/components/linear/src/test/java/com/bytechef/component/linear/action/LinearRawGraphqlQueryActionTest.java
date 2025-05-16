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

package com.bytechef.component.linear.action;

import static com.bytechef.component.linear.constant.LinearConstants.QUERY;
import static com.bytechef.component.linear.constant.LinearConstants.VARIABLES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class LinearRawGraphqlQueryActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(QUERY,
            "query GetIssue($issueId: String!) {issue(id: $issueId) {id title description state {name} assignee {name}}}",
            VARIABLES, "{\"issueId\": \"1\"}"));

    @Test
    void testPerform() {
        Map<String, Object> expectedResponse = Map.of("data", Map.of("id", "abc"));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(expectedResponse);

        Object jsonObject = "{\"issueId\": \"1\"}";

        when(mockedContext.json(any())).thenReturn(jsonObject);

        Object result = LinearRawGraphqlQueryAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(expectedResponse, result);

        Map<String, Object> expectedBody = Map.of(
            "query",
            "query GetIssue($issueId: String!) {issue(id: $issueId) {id title description state {name} assignee {name}}}",
            "variables", "{\"issueId\": \"1\"}");

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(expectedBody, body.getContent());
    }
}
