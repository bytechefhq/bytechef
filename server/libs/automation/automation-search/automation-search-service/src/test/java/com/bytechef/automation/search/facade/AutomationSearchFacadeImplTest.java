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

package com.bytechef.automation.search.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.automation.search.SearchResult;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the aggregator drops results from workspaces the caller cannot access, while passing through results
 * with a null workspaceId (providers not yet workspace-scoped, or workspace-independent results).
 *
 * @author Ivica Cardic
 */
class AutomationSearchFacadeImplTest {

    @Test
    void testFiltersOutInaccessibleWorkspaceResults() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace accessible = mock(Workspace.class);

        when(accessible.getId()).thenReturn(10L);
        when(workspaceFacade.getUserWorkspaces(1L)).thenReturn(List.of(accessible));

        SearchAssetProvider provider = new SearchAssetProvider() {

            @Override
            public List<? extends SearchResult> search(String query, int limit) {
                return List.of(
                    new TestResult(1L, 10L), // accessible
                    new TestResult(2L, 99L), // not accessible
                    new TestResult(3L, null)); // unscoped / workspace-independent
            }

            @Override
            public SearchAssetType getAssetType() {
                return SearchAssetType.WORKFLOW;
            }
        };

        AutomationSearchFacadeImpl facade = new AutomationSearchFacadeImpl(
            List.of(provider), userService, workspaceFacade);

        List<SearchResult<?>> results = facade.search("q", 10, null);

        List<Object> ids = results.stream()
            .map(searchResult -> (Object) searchResult.id())
            .toList();

        assertThat(ids).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void testOnlyRequestedTypeProvidersAreCalled() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace accessible = mock(Workspace.class);

        when(accessible.getId()).thenReturn(10L);
        when(workspaceFacade.getUserWorkspaces(1L)).thenReturn(List.of(accessible));

        AtomicBoolean connectionProviderCalled = new AtomicBoolean();

        SearchAssetProvider workflowProvider = new RecordingProvider(SearchAssetType.WORKFLOW, new AtomicBoolean());
        SearchAssetProvider connectionProvider = new RecordingProvider(
            SearchAssetType.CONNECTION, connectionProviderCalled);

        AutomationSearchFacadeImpl facade = new AutomationSearchFacadeImpl(
            List.of(workflowProvider, connectionProvider), userService, workspaceFacade);

        facade.search("q", 10, Set.of(SearchAssetType.WORKFLOW));

        assertThat(connectionProviderCalled).isFalse();
    }

    @Test
    void testNullTypesMeansEveryProvider() {
        UserService userService = mock(UserService.class);
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userService.getCurrentUser()).thenReturn(user);

        Workspace accessible = mock(Workspace.class);

        when(accessible.getId()).thenReturn(10L);
        when(workspaceFacade.getUserWorkspaces(1L)).thenReturn(List.of(accessible));

        AtomicBoolean connectionProviderCalled = new AtomicBoolean();

        AutomationSearchFacadeImpl facade = new AutomationSearchFacadeImpl(
            List.of(new RecordingProvider(SearchAssetType.CONNECTION, connectionProviderCalled)), userService,
            workspaceFacade);

        facade.search("q", 10, null);

        assertThat(connectionProviderCalled).isTrue();
    }

    private record TestResult(Long id, Long workspaceId) implements SearchResult<Long> {

        @Override
        public String name() {
            return "name-" + id;
        }

        @Override
        public String description() {
            return null;
        }

        @Override
        public SearchAssetType type() {
            return SearchAssetType.WORKFLOW;
        }
    }

    private record RecordingProvider(SearchAssetType assetType, AtomicBoolean called) implements SearchAssetProvider {

        @Override
        public List<? extends SearchResult> search(String query, int limit) {
            called.set(true);

            return List.of();
        }

        @Override
        public SearchAssetType getAssetType() {
            return assetType;
        }
    }
}
