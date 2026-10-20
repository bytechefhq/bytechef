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

package com.bytechef.automation.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.config.TestOwnerContributors;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
public class ProjectDeploymentRepositoryIntTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private TestOwnerContributors testOwnerContributors;

    private Workspace workspace;

    @BeforeEach
    public void beforeEach() {
        testOwnerContributors.clear();

        workspace = workspaceRepository.save(new Workspace("test"));
    }

    @AfterEach
    public void afterEach() {
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();

        jdbcClient.sql("DELETE FROM tag")
            .update();

        workspaceRepository.deleteAll();
    }

    @Nested
    class FindAllProjectDeploymentsTest {

        @Test
        public void testEmbeddedFilterSelectsEmbeddedOwnedDeployments() {
            Project catalogProject = projectRepository.save(getProject("Catalog"));
            Project connectedUserProject = projectRepository.save(getProject("ext"));
            Project project = projectRepository.save(getProject("project"));

            ProjectDeployment referenceDeployment = saveProjectDeployment("ref", catalogProject);
            ProjectDeployment connectedUserDeployment = saveProjectDeployment("user", connectedUserProject);

            saveProjectDeployment("deployment", project);
            saveProjectDeployment("xxEMBEDDEDyyext", project);

            testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.EMBEDDED, referenceDeployment.getId());
            testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.EMBEDDED, connectedUserDeployment.getId());

            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(true, null, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getName)
                    .containsExactly("ref", "user");
            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(false, null, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getName)
                    .containsExactly("deployment", "xxEMBEDDEDyyext");
        }

        @Test
        public void testEmbeddedFilterCombinesWithEnvironmentProjectAndTagFilters() {
            Project firstProject = projectRepository.save(getProject("first"));
            Project secondProject = projectRepository.save(getProject("second"));

            long tagId = saveTag("combined");

            ProjectDeployment plainDevelopment = saveProjectDeployment(
                "a", firstProject, Environment.DEVELOPMENT, List.of(tagId));

            saveProjectDeployment("b", firstProject, Environment.PRODUCTION, List.of());

            ProjectDeployment embeddedSecondProject = saveProjectDeployment(
                "c", secondProject, Environment.DEVELOPMENT, List.of(tagId));
            ProjectDeployment embeddedProduction = saveProjectDeployment(
                "d", firstProject, Environment.PRODUCTION, List.of(tagId));

            testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.EMBEDDED, embeddedSecondProject.getId());
            testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.EMBEDDED, embeddedProduction.getId());

            int production = Environment.PRODUCTION.ordinal();
            int development = Environment.DEVELOPMENT.ordinal();

            assertThat(findNames(true, production, null, null)).containsExactly("d");
            assertThat(findNames(false, development, null, null)).containsExactly("a");
            assertThat(findNames(true, null, secondProject.getId(), null)).containsExactly("c");
            assertThat(findNames(false, null, firstProject.getId(), null)).containsExactly("a", "b");
            assertThat(findNames(true, null, null, tagId)).containsExactly("c", "d");
            assertThat(findNames(false, null, null, tagId)).containsExactly(plainDevelopment.getName());
            assertThat(findNames(false, production, firstProject.getId(), tagId)).isEmpty();
            assertThat(findNames(true, production, firstProject.getId(), tagId)).containsExactly("d");
        }

        @Test
        public void testFindAllProjectDeploymentsWithoutTheEmbeddedFilterKeepsEmbeddedDeployments() {
            Project project = projectRepository.save(getProject("project"));

            ProjectDeployment embeddedDeployment = saveProjectDeployment("embedded", project);
            ProjectDeployment mcpDeployment = saveProjectDeployment("mcp", project);

            saveProjectDeployment("plain", project);

            testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.EMBEDDED, embeddedDeployment.getId());
            testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.MCP_SERVER, mcpDeployment.getId());

            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(null, null, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getName)
                    .containsExactly("embedded", "plain");
        }

        @Test
        public void testNonEmbeddedFilterExcludesPlainDeploymentsOfCatalogProjects() {
            Project catalogProject = projectRepository.save(getProject("Catalog"));
            Project project = projectRepository.save(getProject("project"));

            saveProjectDeployment("catalog", catalogProject);
            saveProjectDeployment("plain", project);

            testOwnerContributors.addCatalogId(catalogProject.getId());

            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(false, null, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getName)
                    .containsExactly("plain");
            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(null, null, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getName)
                    .containsExactly("catalog", "plain");
        }

        @Test
        public void testNonEmbeddedFilterExcludesPlainDeploymentsOfConnectedUserProjects() {
            Project connectedUserProject = projectRepository.save(getProject("Connected User"));
            Project project = projectRepository.save(getProject("project"));

            saveProjectDeployment("connected user", connectedUserProject);
            saveProjectDeployment("plain", project);

            testOwnerContributors.addConnectedUserId(connectedUserProject.getId());

            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(false, null, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getName)
                    .containsExactly("plain");
        }

        @Test
        public void testFindAllProjectDeploymentsListsLegacyPrefixedDeploymentsWithoutAnOwner() {
            Project project = projectRepository.save(getProject("project"));

            saveProjectDeployment("__MCP_SERVER__legacy", project);
            saveProjectDeployment("__A2A_SERVER__legacy", project);
            saveProjectDeployment("__API_COLLECTION__legacy", project);

            assertThat(
                projectDeploymentRepository.findAllProjectDeployments(false, null, null, null, workspace.getId()))
                    .extracting(ProjectDeployment::getName)
                    .containsExactlyInAnyOrder("__MCP_SERVER__legacy", "__A2A_SERVER__legacy",
                        "__API_COLLECTION__legacy");
        }

        @Test
        public void testFindAllProjectDeploymentsWithoutAnyFilterExcludesOwnedDeployments() {
            Project project = projectRepository.save(getProject("project"));

            ProjectDeployment mcpDeployment = saveProjectDeployment("mcp", project);

            saveProjectDeployment("plain", project);

            testOwnerContributors.addDeploymentId(ProjectDeploymentOwner.MCP_SERVER, mcpDeployment.getId());

            assertThat(projectDeploymentRepository.findAllProjectDeployments(null, null, null, null, null))
                .extracting(ProjectDeployment::getName)
                .containsExactly("plain");
        }
    }

    private List<String> findNames(Boolean embedded, Integer environment, Long projectId, Long tagId) {
        return projectDeploymentRepository
            .findAllProjectDeployments(embedded, environment, projectId, tagId, workspace.getId())
            .stream()
            .map(ProjectDeployment::getName)
            .toList();
    }

    private Project getProject(String name) {
        return Project.builder()
            .name(name)
            .workspaceId(workspace.getId())
            .build();
    }

    private long saveTag(String name) {
        return jdbcClient
            .sql(
                "INSERT INTO tag (name, created_date, created_by, last_modified_date, last_modified_by, version) " +
                    "VALUES (:name, now(), 'test', now(), 'test', 0) RETURNING id")
            .param("name", name)
            .query(Long.class)
            .single();
    }

    private ProjectDeployment saveProjectDeployment(String name, Project project) {
        return saveProjectDeployment(name, project, Environment.DEVELOPMENT, List.of());
    }

    private ProjectDeployment saveProjectDeployment(
        String name, Project project, Environment environment, List<Long> tagIds) {

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(1);
        projectDeployment.setTagIds(tagIds);

        return projectDeploymentRepository.save(projectDeployment);
    }
}
