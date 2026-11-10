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
import static com.bytechef.component.github.constant.GithubConstants.DIRECTION;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.SORT;
import static com.bytechef.component.github.constant.GithubConstants.STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.github.util.GithubUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Ivica Cardic
 */
class GithubListPullRequestsActionTest {

    private final Context mockedContext = mock(Context.class);
    private final List<Map<String, ?>> pullRequests = List.of(Map.of("number", 1));

    @Test
    void testPerform() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                OWNER, "bytechefhq", REPOSITORY, "bytechef", STATE, "closed", BASE, "master", SORT, "updated",
                DIRECTION, "asc"));

        try (MockedStatic<GithubUtils> githubUtilsMockedStatic = mockStatic(GithubUtils.class)) {
            githubUtilsMockedStatic.when(() -> GithubUtils.getItems(
                eq(mockedContext), eq("/repos/bytechefhq/bytechef/pulls"), eq(false), eq(STATE), eq("closed"),
                eq(BASE), eq("master"), eq(SORT), eq("updated"), eq(DIRECTION), eq("asc")))
                .thenReturn(pullRequests);

            List<Map<String, ?>> result = GithubListPullRequestsAction.perform(parameters, null, mockedContext);

            assertEquals(pullRequests, result);
        }
    }

    @Test
    void testPerformWithDefaultState() {
        Parameters parameters = MockParametersFactory.create(Map.of(OWNER, "bytechefhq", REPOSITORY, "bytechef"));

        try (MockedStatic<GithubUtils> githubUtilsMockedStatic = mockStatic(GithubUtils.class)) {
            githubUtilsMockedStatic.when(() -> GithubUtils.getItems(
                eq(mockedContext), eq("/repos/bytechefhq/bytechef/pulls"), eq(false), eq(STATE), eq("open"),
                eq(BASE), isNull(), eq(SORT), isNull(), eq(DIRECTION), isNull()))
                .thenReturn(pullRequests);

            List<Map<String, ?>> result = GithubListPullRequestsAction.perform(parameters, null, mockedContext);

            assertEquals(pullRequests, result);
        }
    }
}
