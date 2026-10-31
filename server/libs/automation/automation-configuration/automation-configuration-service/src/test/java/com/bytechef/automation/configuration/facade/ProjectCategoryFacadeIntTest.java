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

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
class ProjectCategoryFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectCategoryFacade projectCategoryFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Category firstCategory;
    private Category otherWorkspaceCategory;
    private Category secondCategory;
    private Workspace emptyWorkspace;
    private Workspace workspace;

    @BeforeEach
    void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("project-category-workspace"));
        emptyWorkspace = workspaceRepository.save(new Workspace("project-category-empty-workspace"));

        Workspace otherWorkspace = workspaceRepository.save(new Workspace("project-category-other-workspace"));

        firstCategory = categoryRepository.save(new Category("project-category-a"));
        secondCategory = categoryRepository.save(new Category("project-category-b"));
        otherWorkspaceCategory = categoryRepository.save(new Category("project-category-other"));

        saveProject("project-1", workspace, firstCategory);
        saveProject("project-2", workspace, secondCategory);
        saveProject("project-3", workspace, null);
        saveProject("other-project", otherWorkspace, otherWorkspaceCategory);
    }

    @AfterEach
    void afterEach() {
        projectRepository.deleteAll();
        categoryRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    /**
     * The third project of the workspace is filed under no category at all, so this also pins that a null category id
     * never reaches the lookup, and the other workspace's category stays out.
     */
    @Test
    void testGetProjectCategoriesScopesToWorkspace() {
        List<Category> categories = projectCategoryFacade.getProjectCategories(workspace.getId());

        assertThat(categories)
            .extracting(Category::getId)
            .containsExactlyInAnyOrder(firstCategory.getId(), secondCategory.getId())
            .doesNotContain(otherWorkspaceCategory.getId());
    }

    /**
     * The facade previously read every project in the instance, so a workspace holding no projects still offered
     * another workspace's categories.
     */
    @Test
    void testGetProjectCategoriesOfAWorkspaceWithoutProjectsIsEmpty() {
        List<Category> categories = projectCategoryFacade.getProjectCategories(emptyWorkspace.getId());

        assertThat(categories).isEmpty();
    }

    private void saveProject(String name, Workspace projectWorkspace, Category category) {
        projectRepository.save(
            Project.builder()
                .categoryId(category == null ? null : category.getId())
                .name(name)
                .workspaceId(projectWorkspace.getId())
                .build());
    }
}
