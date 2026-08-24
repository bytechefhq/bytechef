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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.DefaultResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.search.SearchAssetProvider;
import com.bytechef.automation.search.SearchAssetType;
import com.bytechef.automation.search.SearchResult;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The provider fan-out runs on the common ForkJoinPool, which inherits no {@code SecurityContext}. The three
 * project-backed providers resolve project visibility, and both {@code ResourceVisibilityResolver} implementations read
 * the current principal through {@code SecurityUtils.getCurrentUserLogin()}, which <em>throws</em> when there is none —
 * so without the facade re-establishing the context inside each pooled task, the whole search fails for every user on
 * any tenant that owns at least one project.
 *
 * <p>
 * This test therefore goes through the REAL {@link ProjectVisibilityFilter} over the REAL CE
 * {@link DefaultResourceVisibilityResolver}, and through the real {@code CompletableFuture.supplyAsync} fan-out. A
 * lambda or mocked resolver would never read the principal and so could not catch this.
 *
 * @author Ivica Cardic
 */
class AutomationSearchFacadeSecurityContextTest {

    private static final long ACCESSIBLE_WORKSPACE_ID = 10L;
    private static final long COLLEAGUES_PROJECT_ID = 2L;
    private static final String CURRENT_USER_LOGIN = "ivica@localhost.com";
    private static final long OWNED_PROJECT_ID = 1L;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testVisibilityResolvingProviderSeesTheCallerPrincipalOnTheFanOutPool() {
        authenticateCurrentUser();

        AutomationSearchFacadeImpl automationSearchFacade = new AutomationSearchFacadeImpl(
            List.of(new ProjectVisibilityResolvingSearchAssetProvider()), userService(), workspaceFacade());

        // Returning at all is half the assertion: without the propagation the resolver throws on the pooled thread
        // and CompletableFuture.allOf(...).join() rethrows it out of search(). The other half is that the filtering
        // still happened — the colleague's PRIVATE project is decided by comparing its owner to the principal the
        // pooled thread was handed, so a context that arrived empty could not have excluded it either.
        List<SearchResult<?>> searchResults = automationSearchFacade.search("project", 10, null);

        List<Object> ids = searchResults.stream()
            .map(searchResult -> (Object) searchResult.id())
            .toList();

        assertThat(ids).containsExactly(OWNED_PROJECT_ID);
    }

    private static void authenticateCurrentUser() {
        // Deliberately not an admin: the admin branch short-circuits ahead of the ownership comparison, which is the
        // branch that has to see the caller's login.
        SecurityContextHolder.setContext(
            new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(CURRENT_USER_LOGIN, "password", List.of())));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ResourceVisibilityResolver> objectProvider(
        ResourceVisibilityResolver resourceVisibilityResolver) {

        ObjectProvider<ResourceVisibilityResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resourceVisibilityResolver);

        return objectProvider;
    }

    private static Project project(long id, String createdBy) {
        Project project = new Project();

        project.setId(id);
        project.setName("project-" + id);
        project.setWorkspaceId(ACCESSIBLE_WORKSPACE_ID);
        project.setVisibility(ResourceVisibility.PRIVATE);

        ReflectionTestUtils.setField(project, "createdBy", createdBy);

        return project;
    }

    private static UserService userService() {
        UserService userService = mock(UserService.class);
        User user = mock(User.class);

        when(user.getId()).thenReturn(1L);
        when(userService.getCurrentUser()).thenReturn(user);

        return userService;
    }

    private static WorkspaceFacade workspaceFacade() {
        WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);
        Workspace workspace = mock(Workspace.class);

        when(workspace.getId()).thenReturn(ACCESSIBLE_WORKSPACE_ID);
        when(workspaceFacade.getUserWorkspaces(1L)).thenReturn(List.of(workspace));

        return workspaceFacade;
    }

    /**
     * Stands in for the three real project-backed providers, resolving visibility exactly as they do.
     */
    private static final class ProjectVisibilityResolvingSearchAssetProvider implements SearchAssetProvider {

        private final ProjectVisibilityFilter projectVisibilityFilter = new ProjectVisibilityFilter(
            objectProvider(new DefaultResourceVisibilityResolver()));

        @Override
        public List<ProjectResult> search(String query, int limit) {
            List<Project> projects = List.of(
                project(OWNED_PROJECT_ID, CURRENT_USER_LOGIN),
                project(COLLEAGUES_PROJECT_ID, "colleague@localhost.com"));

            return projectVisibilityFilter.filterVisible(projects)
                .stream()
                .map(project -> new ProjectResult(project.getId(), project.getWorkspaceId()))
                .toList();
        }

        @Override
        public SearchAssetType getAssetType() {
            return SearchAssetType.PROJECT;
        }
    }

    private record ProjectResult(Long id, Long workspaceId) implements SearchResult<Long> {

        @Override
        public String name() {
            return "project-" + id;
        }

        @Override
        public String description() {
            return null;
        }

        @Override
        public SearchAssetType type() {
            return SearchAssetType.PROJECT;
        }
    }
}
