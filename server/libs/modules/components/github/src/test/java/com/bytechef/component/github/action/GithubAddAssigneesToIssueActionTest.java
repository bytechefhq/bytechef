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

package com.bytechef.component.github.action;

import static com.bytechef.component.github.constant.GithubConstants.ASSIGNEES;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.github.util.GithubUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mayank Madan
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class GithubAddAssigneesToIssueActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(REPOSITORY, "testRepo", ISSUE, "testIssue", ASSIGNEES, List.of("githubUsername", "githubUsername2")));
    private final Map<String, Object> responseMap = Map.of("result", List.of("123", "abc"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (MockedStatic<GithubUtils> githubUtilsMockedStatic = mockStatic(GithubUtils.class)) {
            githubUtilsMockedStatic.when(() -> GithubUtils.getOwnerName(contextArgumentCaptor.capture()))
                .thenReturn("testOwner");

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

            Object result = GithubAddAssigneesToIssueAction.perform(mockedParameters, null, mockedContext);

            assertEquals(responseMap, result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
            assertEquals("/repos/testOwner/testRepo/issues/testIssue/assignees", stringArgumentCaptor.getValue());
            assertEquals(
                Body.of(Map.of(ASSIGNEES, List.of("githubUsername", "githubUsername2")), BodyContentType.JSON),
                bodyArgumentCaptor.getValue());
        }
    }
}
