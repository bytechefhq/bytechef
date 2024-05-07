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

import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.REPO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GithubGetIssueActionTest {
    ActionContext mockedContext = mock(ActionContext.class);
    Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    Parameters mockedParameters = mock(Parameters.class);
    Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    Map<String, Object> responeseMap = Map.of("key", "value");

    @BeforeEach
    public void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);
    }

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(REPO))
            .thenReturn((String) propertyStubsMap.get(REPO));
        when(mockedParameters.getRequiredString(ISSUE))
            .thenReturn((String) propertyStubsMap.get(ISSUE));

        Object result = GithubGetIssueAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(REPO, "repo");
        propertyStubsMap.put(ISSUE, "issue");

        return propertyStubsMap;
    }
}
