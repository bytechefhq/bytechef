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

package com.bytechef.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.config.TestOwnerContributors;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class ProjectOwnershipServiceIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ProjectOwnershipService projectOwnershipService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestOwnerContributors testOwnerContributors;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Category category;
    private Workspace workspace;

    @BeforeEach
    public void beforeEach() {
        testOwnerContributors.clear();

        category = categoryRepository.save(new Category("name"));
        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @AfterEach
    public void afterEach() {
        testOwnerContributors.clear();

        projectRepository.deleteAll();
        workspaceRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    public void testExcludeProjectsCombinesContributorsWithOr() {
        long firstProjectId = saveProject("p1");
        long secondProjectId = saveProject("p2");
        long thirdProjectId = saveProject("p3");

        testOwnerContributors.addCatalogId(firstProjectId);
        testOwnerContributors.addConnectedUserId(secondProjectId);

        String condition = projectOwnershipService.getExcludeProjectsCondition(
            Set.of(ProjectOwner.EMBEDDED_CATALOG, ProjectOwner.EMBEDDED_CONNECTED_USER), "project.id");

        List<Long> projectIds = jdbcClient.sql("SELECT id FROM project WHERE " + condition)
            .query(Long.class)
            .list();

        assertThat(projectIds).contains(thirdProjectId)
            .doesNotContain(firstProjectId, secondProjectId);
    }

    @Test
    public void testSelectWithoutContributorMatchesNothing() {
        ProjectOwnershipService emptyService = new ProjectOwnershipServiceImpl(jdbcClient, List.of(), List.of());

        assertThat(
            emptyService.getSelectProjectDeploymentsCondition(ProjectDeploymentOwner.EMBEDDED, "project_deployment.id"))
                .isEqualTo("1 = 0");
    }

    @Test
    public void testExcludeWithoutContributorKeepsEverything() {
        ProjectOwnershipService emptyService = new ProjectOwnershipServiceImpl(jdbcClient, List.of(), List.of());

        assertThat(
            emptyService.getExcludeProjectDeploymentsCondition(
                Set.of(ProjectDeploymentOwner.EMBEDDED), "project_deployment.id"))
                    .isEqualTo("1 = 1");
    }

    @Test
    public void testGetOwnedProjectIdsReturnsOnlyClaimedIds() {
        long firstProjectId = saveProject("p1");
        long secondProjectId = saveProject("p2");

        testOwnerContributors.addCatalogId(firstProjectId);

        assertThat(
            projectOwnershipService.getOwnedProjectIds(
                List.of(firstProjectId, secondProjectId), ProjectOwner.EMBEDDED_CATALOG))
                    .isEqualTo(Set.of(firstProjectId));
    }

    @Test
    public void testGetOwnedProjectIdsWithEmptyIdsReturnsEmpty() {
        assertThat(projectOwnershipService.getOwnedProjectIds(List.of(), ProjectOwner.EMBEDDED_CATALOG)).isEmpty();
    }

    private long saveProject(String name) {
        Project project = projectRepository.save(
            Project.builder()
                .categoryId(category.getId())
                .description("description")
                .name(name)
                .workspaceId(workspace.getId())
                .build());

        return project.getId();
    }
}
