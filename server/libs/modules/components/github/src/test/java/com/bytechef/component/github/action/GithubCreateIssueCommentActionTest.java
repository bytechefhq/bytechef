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

package com.bytechef.component.github.action;

import static com.bytechef.component.definition.Context.Http.Body;
import static com.bytechef.component.definition.Context.Http.Executor;
import static com.bytechef.component.definition.Context.Http.Response;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Luka Ljubic
 */
class GithubCreateIssueCommentActionTest {

    ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    ActionContext mockedContext = mock(ActionContext.class);
    Executor mockedExecutor = mock(Executor.class);
    Parameters mockedParameters = mock(Parameters.class);
    Response mockedResponse = mock(Response.class);
    Map<String, Object> responeseMap = Map.of("key", "value");

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = new LinkedHashMap<>();
        propertyStubsMap.put(BODY, "body");

        when(mockedParameters.getRequiredString(BODY))
            .thenReturn((String) propertyStubsMap.get(BODY));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responeseMap);

        Object result = GithubCreateIssueCommentAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }
}
