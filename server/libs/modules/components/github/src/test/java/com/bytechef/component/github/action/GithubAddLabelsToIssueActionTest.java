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

import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.LABELS;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
class GithubAddLabelsToIssueActionTest extends AbstractGithubActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(REPOSITORY, "testRepo", ISSUE, "testIssue", LABELS, List.of("help-wanted", "docs")));
    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform() throws Exception {
        try (MockedStatic<GithubUtils> githubUtilsMockedStatic = mockStatic(GithubUtils.class)) {
            githubUtilsMockedStatic.when(() -> GithubUtils.getOwnerName(contextArgumentCaptor.capture()))
                .thenReturn("testOwner");

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(mockedObject);

            Object result = executePerformFunction(GithubAddLabelsToIssueAction.ACTION_DEFINITION, mockedParameters);

            assertEquals(mockedObject, result);
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
            assertEquals("/repos/testOwner/testRepo/issues/testIssue/labels", stringArgumentCaptor.getValue());

            Http.Body body = bodyArgumentCaptor.getValue();

            assertEquals(Map.of(LABELS, List.of("help-wanted", "docs")), body.getContent());
        }
    }
}
