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
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectCategoryFacadeImplTest {

    private static final long OTHER_WORKSPACE_ID = 6L;
    private static final long WORKSPACE_ID = 5L;

    private final CategoryService categoryService = mock(CategoryService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    /**
     * The third project in the fixture is filed under no category at all, so this also pins that a null category id
     * never reaches the lookup.
     */
    @Test
    void testGetProjectCategoriesScopesToWorkspace() {
        when(categoryService.getCategories(List.of(10L, 11L)))
            .thenReturn(List.of(new Category(10, "a"), new Category(11, "b")));

        List<Category> categories = facade().getProjectCategories(WORKSPACE_ID);

        assertThat(categories).hasSize(2);

        verify(projectService).getWorkspaceProjectIds(WORKSPACE_ID);
        verify(categoryService).getCategories(List.of(10L, 11L));
    }

    /**
     * The assertion is on the ids handed to {@code CategoryService}: the facade previously read every project in the
     * instance, so a workspace holding no projects still offered another workspace's categories.
     */
    @Test
    void testGetProjectCategoriesOfAWorkspaceWithoutProjectsIsEmpty() {
        when(projectService.getWorkspaceProjectIds(OTHER_WORKSPACE_ID)).thenReturn(List.of());
        when(projectService.getProjects(List.of())).thenReturn(List.of());

        facade().getProjectCategories(OTHER_WORKSPACE_ID);

        verify(categoryService).getCategories(List.of());
    }

    private ProjectCategoryFacadeImpl facade() {
        when(projectService.getWorkspaceProjectIds(WORKSPACE_ID)).thenReturn(List.of(1L, 2L, 3L));
        when(projectService.getProjects(List.of(1L, 2L, 3L)))
            .thenReturn(List.of(project(1L, 10L), project(2L, 11L), project(3L, null)));

        return new ProjectCategoryFacadeImpl(categoryService, projectService);
    }

    private static Project project(long id, Long categoryId) {
        Project project = new Project();

        project.setId(id);
        project.setCategoryId(categoryId);

        return project;
    }
}
