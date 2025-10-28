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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.github.util.GithubUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mayank Madan
 * @author Monika Kušter
 */
class GithubAddAssigneesToIssueActionTest extends AbstractGithubActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(REPOSITORY, "testRepo", ISSUE, "testIssue", ASSIGNEES, List.of("githubUsername", "githubUsername2")));
    protected final Map<String, Object> responseMap = Map.of("result", List.of("123", "abc"));

    @Test
    void testPerform() throws Exception {
        try (MockedStatic<GithubUtils> githubUtilsMockedStatic = mockStatic(GithubUtils.class)) {
            githubUtilsMockedStatic.when(() -> GithubUtils.getOwnerName(contextArgumentCaptor.capture()))
                .thenReturn("testOwner");

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

            Object result = executePerformFunction(GithubAddAssigneesToIssueAction.ACTION_DEFINITION, mockedParameters);

            assertEquals(responseMap, result);
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
            assertEquals("/repos/testOwner/testRepo/issues/testIssue/assignees", stringArgumentCaptor.getValue());

            Http.Body body = bodyArgumentCaptor.getValue();

            assertEquals(Map.of(ASSIGNEES, List.of("githubUsername", "githubUsername2")), body.getContent());
        }
    }
}
