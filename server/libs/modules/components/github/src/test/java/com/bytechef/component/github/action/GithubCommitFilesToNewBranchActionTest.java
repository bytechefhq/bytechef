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

import static com.bytechef.component.github.constant.GithubConstants.BASE_BRANCH;
import static com.bytechef.component.github.constant.GithubConstants.COMMIT_MESSAGE;
import static com.bytechef.component.github.constant.GithubConstants.CONTENT;
import static com.bytechef.component.github.constant.GithubConstants.DELETE;
import static com.bytechef.component.github.constant.GithubConstants.FILES;
import static com.bytechef.component.github.constant.GithubConstants.NEW_BRANCH;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.PATH;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.UPDATE_EXISTING_BRANCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class GithubCommitFilesToNewBranchActionTest {

    private static final String REPOSITORY_URL = "/repos/bytechefhq/bytechef";

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> getUrlArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<String> postUrlArgumentCaptor = forClass(String.class);

    @Test
    void testPerformCreatesTheBranch(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(mockedExecutor, mockedHttp);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("ref", "refs/heads/docs/release-other", "object", Map.of("sha", "other"))))
            .thenReturn(Map.of("object", Map.of("sha", "head1")))
            .thenReturn(Map.of("tree", Map.of("sha", "tree1")))
            .thenReturn(Map.of("sha", "tree2"))
            .thenReturn(Map.of("html_url", "https://github.com/commit/commit1", "sha", "commit1"))
            .thenReturn(Map.of("ref", "refs/heads/docs/release"));

        Map<String, Object> result = GithubCommitFilesToNewBranchAction.perform(
            createParameters(false), null, mockedContext);

        assertEquals(
            Map.of(
                "branch", "docs/release", "commitSha", "commit1", "commitUrl", "https://github.com/commit/commit1",
                "treeSha", "tree2"),
            result);
        assertEquals(
            List.of(
                REPOSITORY_URL + "/git/matching-refs/heads/docs/release", REPOSITORY_URL + "/git/ref/heads/main",
                REPOSITORY_URL + "/git/commits/head1"),
            getUrlArgumentCaptor.getAllValues());
        assertEquals(
            List.of(REPOSITORY_URL + "/git/trees", REPOSITORY_URL + "/git/commits", REPOSITORY_URL + "/git/refs"),
            postUrlArgumentCaptor.getAllValues());

        List<Body> bodies = bodyArgumentCaptor.getAllValues();

        Map<String, Object> deletedEntry = new HashMap<>();

        deletedEntry.put("mode", "100644");
        deletedEntry.put(PATH, "old.md");
        deletedEntry.put("sha", null);
        deletedEntry.put("type", "blob");

        assertEquals(
            Body.of(
                Map.of(
                    "base_tree", "tree1", "tree",
                    List.of(
                        Map.of(CONTENT, "# Release", "mode", "100644", PATH, "docs/release.md", "type", "blob"),
                        deletedEntry)),
                BodyContentType.JSON),
            bodies.get(0));
        assertEquals(
            Body.of(
                Map.of("message", "Add release notes", "parents", List.of("head1"), "tree", "tree2"),
                BodyContentType.JSON),
            bodies.get(1));
        assertEquals(
            Body.of(Map.of("ref", "refs/heads/docs/release", "sha", "commit1"), BodyContentType.JSON),
            bodies.get(2));
        verify(mockedHttp, never()).patch(anyString());
    }

    @Test
    void testPerformFailsWhenTheBranchExists(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(mockedExecutor, mockedHttp);

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("ref", "refs/heads/docs/release", "object", Map.of("sha", "existing1"))));

        ProviderException exception = assertThrows(
            ProviderException.class,
            () -> GithubCommitFilesToNewBranchAction.perform(createParameters(false), null, mockedContext));

        assertTrue(exception.getMessage()
            .contains("Branch 'docs/release' already exists in bytechefhq/bytechef"));
        verify(mockedHttp, never()).post(anyString());
    }

    @Test
    void testPerformFastForwardsAnExistingBranch(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        stubHttp(mockedExecutor, mockedHttp);

        ArgumentCaptor<String> patchUrlArgumentCaptor = forClass(String.class);

        when(mockedHttp.patch(patchUrlArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("ref", "refs/heads/docs/release", "object", Map.of("sha", "existing1"))))
            .thenReturn(Map.of("tree", Map.of("sha", "tree1")))
            .thenReturn(Map.of("sha", "tree2"))
            .thenReturn(Map.of("html_url", "https://github.com/commit/commit1", "sha", "commit1"));

        Map<String, Object> result = GithubCommitFilesToNewBranchAction.perform(
            createParameters(true), null, mockedContext);

        assertEquals("commit1", result.get("commitSha"));
        assertEquals(
            List.of(
                REPOSITORY_URL + "/git/matching-refs/heads/docs/release", REPOSITORY_URL + "/git/commits/existing1"),
            getUrlArgumentCaptor.getAllValues());
        assertEquals(REPOSITORY_URL + "/git/refs/heads/docs/release", patchUrlArgumentCaptor.getValue());

        List<Body> bodies = bodyArgumentCaptor.getAllValues();

        assertEquals(
            Body.of(
                Map.of("message", "Add release notes", "parents", List.of("existing1"), "tree", "tree2"),
                BodyContentType.JSON),
            bodies.get(1));
        assertEquals(Body.of(Map.of("force", false, "sha", "commit1"), BodyContentType.JSON), bodies.get(2));
    }

    private static Parameters createParameters(boolean updateExistingBranch) {
        return MockParametersFactory.create(
            Map.of(
                OWNER, "bytechefhq", REPOSITORY, "bytechef", BASE_BRANCH, "main", NEW_BRANCH, "docs/release",
                COMMIT_MESSAGE, "Add release notes", UPDATE_EXISTING_BRANCH, updateExistingBranch, FILES,
                List.of(
                    Map.of(PATH, "docs/release.md", CONTENT, "# Release"),
                    Map.of(PATH, "old.md", DELETE, true))));
    }

    private void stubHttp(Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(getUrlArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.post(postUrlArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
    }
}
