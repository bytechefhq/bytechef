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

package com.bytechef.component.beamer.action;

import static com.bytechef.component.beamer.constant.BeamerConstants.CONTENT;
import static com.bytechef.component.beamer.constant.BeamerConstants.FEATURE_REQUEST_ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_EMAIL;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_FIRST_NAME;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_LAST_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class BeamerNewVoteActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FEATURE_REQUEST_ID, 2, CONTENT, "testContent", USER_ID, 1010, USER_EMAIL, "testUserEmail",
            USER_FIRST_NAME, "Jane", USER_LAST_NAME, "Doe"));

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> mockedMap =
        Map.of(FEATURE_REQUEST_ID, 2, CONTENT, "testContent", USER_ID, 1010, USER_EMAIL, "testUserEmail",
            USER_FIRST_NAME, "Jane", USER_LAST_NAME, "Doe");

    @Test
    void perform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedMap);

        Map<String, Object> result = BeamerNewVoteAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedMap.get(FEATURE_REQUEST_ID), result.get(FEATURE_REQUEST_ID));
        assertEquals(mockedMap.get(CONTENT), result.get(CONTENT));
        assertEquals(mockedMap.get(USER_ID), result.get(USER_ID));
        assertEquals(mockedMap.get(USER_EMAIL), result.get(USER_EMAIL));
        assertEquals(mockedMap.get(USER_FIRST_NAME), result.get(USER_FIRST_NAME));
        assertEquals(mockedMap.get(USER_LAST_NAME), result.get(USER_LAST_NAME));
    }
}
