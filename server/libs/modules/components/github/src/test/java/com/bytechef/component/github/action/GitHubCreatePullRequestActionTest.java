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

import static com.bytechef.component.github.constant.GithubConstants.BASE;
import static com.bytechef.component.github.constant.GithubConstants.BODY;
import static com.bytechef.component.github.constant.GithubConstants.HEAD;
import static com.bytechef.component.github.constant.GithubConstants.HEAD_REPO;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Anas Elgarhy (@0x61nas)
 * @author Monika Kušter
 */
class GitHubCreatePullRequestActionTest extends AbstractGithubActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            OWNER, "testOwner", REPOSITORY, "testRepo", TITLE, "name", HEAD, "feat/123",
            HEAD_REPO, "head-repo", BASE, "master", BODY, "description"));

    @Test
    void testPerform() throws Exception {
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of());

        Object result = executePerformFunction(GitHubCreatePullRequestAction.ACTION_DEFINITION, mockedParameters);

        assertEquals(Map.of(), result);
        assertEquals("/repos/testOwner/testRepo/pulls", stringArgumentCaptor.getValue());
        assertEquals(
            Http.Body.of(
                Map.of(TITLE, "name", HEAD, "feat/123", HEAD_REPO, "head-repo", BASE, "master", BODY, "description"),
                Http.BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}
