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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.LongPredicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ProjectTagFacadeImplTest {

    private static final long HIDDEN_PROJECT_ID = 2L;
    private static final long VISIBLE_PROJECT_ID = 1L;
    private static final long WORKSPACE_ID = 5L;

    private final ProjectService projectService = mock(ProjectService.class);
    private final TagService tagService = mock(TagService.class);

    @Test
    void testGetProjectTagsScopesToWorkspace() {
        when(tagService.getTags(List.of(10L, 11L, 12L)))
            .thenReturn(List.of(new Tag("a"), new Tag("b"), new Tag("c")));

        List<Tag> tags = facadeWith(id -> true).getProjectTags(WORKSPACE_ID);

        assertThat(tags).hasSize(3);
    }

    /**
     * A tag name aggregated off a project the caller cannot see is a name disclosed from a withheld project, and a
     * dropdown option that selects nothing in the listing this feeds.
     *
     * <p>
     * The assertion is on the ids handed to {@code TagService} rather than on the returned tags: the two projects here
     * carry different tag ids, so a facade that stopped filtering would ask for both and fail, whereas asserting the
     * returned list would only pin whatever the stub was told to return.
     */
    @Test
    void testGetProjectTagsDropsTheTagsOfAProjectTheResolverHides() {
        facadeWith(id -> id != HIDDEN_PROJECT_ID).getProjectTags(WORKSPACE_ID);

        verify(tagService).getTags(List.of(10L, 11L));
    }

    private ProjectTagFacadeImpl facadeWith(LongPredicate visible) {
        when(projectService.getWorkspaceProjectIds(WORKSPACE_ID)).thenReturn(List.of(1L, 2L));
        when(projectService.getProjects(List.of(1L, 2L)))
            .thenReturn(
                List.of(project(VISIBLE_PROJECT_ID, List.of(10L, 11L)), project(HIDDEN_PROJECT_ID, List.of(12L))));

        return new ProjectTagFacadeImpl(projectService, projectVisibilityFilter(visible), tagService);
    }

    private static Project project(long id, List<Long> tagIds) {
        Project project = new Project();

        project.setId(id);
        project.setTagIds(tagIds);

        return project;
    }

    /**
     * The production {@link ProjectVisibilityFilter} over a stubbed resolver, not a mock of the filter — so the test
     * fails if this facade stops routing through the one component every project list surface shares.
     */
    @SuppressWarnings("unchecked")
    private static ProjectVisibilityFilter projectVisibilityFilter(LongPredicate visible) {
        ResourceVisibilityResolver resourceVisibilityResolver =
            (resourceType, workspaceId, candidates) -> candidates.stream()
                .map(ResourceVisibilityResolver.VisibilityRecord::id)
                .filter(visible::test)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        ObjectProvider<ResourceVisibilityResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resourceVisibilityResolver);

        return new ProjectVisibilityFilter(objectProvider);
    }
}
