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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.config.TestOwnerContributors;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwner;
import com.bytechef.automation.configuration.ownership.ProjectDeploymentOwnerContributor;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.ownership.ProjectOwnerContributor;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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
    private ProjectDeploymentRepository projectDeploymentRepository;

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

        projectDeploymentRepository.deleteAll();
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
    public void testCombinedOwnerConditionsAgainstRealRows() {
        long firstProjectId = saveProject("p1");
        long secondProjectId = saveProject("p2");
        long thirdProjectId = saveProject("p3");
        long plainProjectId = saveProject("p4");

        ProjectOwnershipService service = new ProjectOwnershipServiceImpl(
            jdbcClient, List.of(),
            List.of(
                ProjectOwnerContributor.of(
                    ProjectOwner.EMBEDDED_CATALOG, "%1$s = " + firstProjectId + " OR %1$s = " + secondProjectId),
                ProjectOwnerContributor.of(ProjectOwner.EMBEDDED_CONNECTED_USER, "%1$s = " + thirdProjectId)));

        List<Long> unownedProjectIds = jdbcClient
            .sql(
                "SELECT id FROM project WHERE " +
                    service.getExcludeProjectsCondition(EnumSet.allOf(ProjectOwner.class), "project.id"))
            .query(Long.class)
            .list();
        List<Long> catalogOnlyUnownedProjectIds = jdbcClient
            .sql(
                "SELECT id FROM project WHERE " +
                    service.getExcludeProjectsCondition(Set.of(ProjectOwner.EMBEDDED_CATALOG), "project.id"))
            .query(Long.class)
            .list();

        assertThat(unownedProjectIds).containsExactly(plainProjectId);
        assertThat(catalogOnlyUnownedProjectIds).containsExactlyInAnyOrder(thirdProjectId, plainProjectId);
        assertThat(
            service.getOwnedProjectIds(
                List.of(firstProjectId, secondProjectId, thirdProjectId, plainProjectId),
                EnumSet.allOf(ProjectOwner.class)))
                    .isEqualTo(Set.of(firstProjectId, secondProjectId, thirdProjectId));
    }

    @Test
    public void testCombinedDeploymentOwnerConditionsAgainstRealRows() {
        long projectId = saveProject("p1");

        long firstDeploymentId = saveDeployment(projectId, "d1");
        long secondDeploymentId = saveDeployment(projectId, "d2");
        long plainDeploymentId = saveDeployment(projectId, "d3");

        ProjectOwnershipService service = new ProjectOwnershipServiceImpl(
            jdbcClient,
            List.of(
                ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.EMBEDDED, "%1$s = " + firstDeploymentId),
                ProjectDeploymentOwnerContributor.of(ProjectDeploymentOwner.MCP_SERVER,
                    "%1$s = " + secondDeploymentId)),
            List.of());

        List<Long> unownedDeploymentIds = jdbcClient
            .sql(
                "SELECT id FROM project_deployment WHERE " +
                    service.getExcludeProjectDeploymentsCondition(
                        EnumSet.allOf(ProjectDeploymentOwner.class), "project_deployment.id"))
            .query(Long.class)
            .list();
        List<Long> embeddedDeploymentIds = jdbcClient
            .sql(
                "SELECT id FROM project_deployment WHERE " +
                    service.getSelectProjectDeploymentsCondition(
                        ProjectDeploymentOwner.EMBEDDED, "project_deployment.id"))
            .query(Long.class)
            .list();

        assertThat(unownedDeploymentIds).containsExactly(plainDeploymentId);
        assertThat(embeddedDeploymentIds).containsExactly(firstDeploymentId);
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
    public void testConditionsWithoutContributorsAgainstRealRows() {
        ProjectOwnershipService emptyService = new ProjectOwnershipServiceImpl(jdbcClient, List.of(), List.of());

        long firstProjectId = saveProject("p1");
        long secondProjectId = saveProject("p2");

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName("deployment");
        projectDeployment.setProjectId(firstProjectId);
        projectDeployment.setProjectVersion(1);

        projectDeploymentRepository.save(projectDeployment);

        List<Long> projectIds = jdbcClient
            .sql(
                "SELECT id FROM project WHERE " +
                    emptyService.getExcludeProjectsCondition(EnumSet.allOf(ProjectOwner.class), "project.id"))
            .query(Long.class)
            .list();
        List<Long> excludedProjectDeploymentIds = jdbcClient
            .sql(
                "SELECT id FROM project_deployment WHERE " +
                    emptyService.getExcludeProjectDeploymentsCondition(
                        EnumSet.allOf(ProjectDeploymentOwner.class), "project_deployment.id"))
            .query(Long.class)
            .list();
        List<Long> selectedProjectDeploymentIds = jdbcClient
            .sql(
                "SELECT id FROM project_deployment WHERE " +
                    emptyService.getSelectProjectDeploymentsCondition(
                        ProjectDeploymentOwner.EMBEDDED, "project_deployment.id"))
            .query(Long.class)
            .list();

        assertThat(projectIds).containsExactlyInAnyOrder(firstProjectId, secondProjectId);
        assertThat(excludedProjectDeploymentIds).containsExactly(projectDeployment.getId());
        assertThat(selectedProjectDeploymentIds).isEmpty();
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
    public void testGetOwnedProjectIdsForSeveralOwnersReturnsIdsClaimedByAny() {
        long catalogProjectId = saveProject("p1");
        long connectedUserProjectId = saveProject("p2");
        long plainProjectId = saveProject("p3");

        testOwnerContributors.addCatalogId(catalogProjectId);
        testOwnerContributors.addConnectedUserId(connectedUserProjectId);

        assertThat(
            projectOwnershipService.getOwnedProjectIds(
                List.of(catalogProjectId, connectedUserProjectId, plainProjectId),
                EnumSet.allOf(ProjectOwner.class)))
                    .isEqualTo(Set.of(catalogProjectId, connectedUserProjectId));
        assertThat(
            projectOwnershipService.getOwnedProjectIds(List.of(catalogProjectId), Set.of())).isEmpty();
    }

    @Test
    public void testGetOwnedProjectIdsHandlesMoreIdsThanOneChunk() {
        long ownedProjectId = saveProject("p1");

        testOwnerContributors.addCatalogId(ownedProjectId);

        List<Long> projectIds = new ArrayList<>();

        for (long projectId = ownedProjectId + 1; projectId <= ownedProjectId + 2499; projectId++) {
            projectIds.add(projectId);
        }

        projectIds.add(1500, ownedProjectId);

        assertThat(projectOwnershipService.getOwnedProjectIds(projectIds, ProjectOwner.EMBEDDED_CATALOG))
            .isEqualTo(Set.of(ownedProjectId));
    }

    @Test
    public void testGetOwnedProjectIdsWithEmptyIdsReturnsEmpty() {
        assertThat(projectOwnershipService.getOwnedProjectIds(List.of(), ProjectOwner.EMBEDDED_CATALOG)).isEmpty();
    }

    @Test
    public void testAfterSingletonsInstantiatedThrowsWhenTheEnterpriseEditionLacksEmbeddedContributors() {
        ProjectOwnershipServiceImpl projectOwnershipServiceImpl = new ProjectOwnershipServiceImpl(
            jdbcClient, List.of(deploymentOwnerContributor(ProjectDeploymentOwner.API_COLLECTION)),
            List.of(projectOwnerContributor(ProjectOwner.EMBEDDED_CONNECTED_USER)), true);

        assertThatThrownBy(projectOwnershipServiceImpl::afterSingletonsInstantiated)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("[EMBEDDED_CATALOG]")
            .hasMessageContaining("[EMBEDDED]");
    }

    @Test
    public void testAfterSingletonsInstantiatedDoesNotThrowWhenTheEnterpriseEditionHasEmbeddedContributors() {
        ProjectOwnershipServiceImpl projectOwnershipServiceImpl = new ProjectOwnershipServiceImpl(
            jdbcClient, List.of(deploymentOwnerContributor(ProjectDeploymentOwner.EMBEDDED)),
            List.of(
                projectOwnerContributor(ProjectOwner.EMBEDDED_CATALOG),
                projectOwnerContributor(ProjectOwner.EMBEDDED_CONNECTED_USER)),
            true);

        assertThatCode(projectOwnershipServiceImpl::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    @Test
    public void testAfterSingletonsInstantiatedDoesNotThrowInTheCommunityEditionWithoutContributors() {
        ProjectOwnershipServiceImpl projectOwnershipServiceImpl = new ProjectOwnershipServiceImpl(
            jdbcClient, List.of(), List.of(), false);

        assertThatCode(projectOwnershipServiceImpl::afterSingletonsInstantiated).doesNotThrowAnyException();
    }

    @Test
    public void testAfterSingletonsInstantiatedDoesNotWarnInTheCommunityEditionWithoutEmbeddedChangesets() {
        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            new ProjectOwnershipServiceImpl(jdbcClient, List.of(), List.of(), false).afterSingletonsInstantiated();

            assertThat(appender.list).noneMatch(event -> event.getLevel() == Level.WARN);
        } finally {
            detachAppender(appender);
        }
    }

    @Test
    public void testAfterSingletonsInstantiatedWarnsInTheCommunityEditionWhenEmbeddedChangesetsWereApplied() {
        jdbcClient
            .sql(
                "INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype) " +
                    "VALUES ('owner-test-embedded', 'test', " +
                    "'config/liquibase/changelog/embedded/test/embedded_test.xml', CURRENT_TIMESTAMP, 999999, " +
                    "'EXECUTED')")
            .update();

        ListAppender<ILoggingEvent> appender = attachAppender();

        try {
            new ProjectOwnershipServiceImpl(jdbcClient, List.of(), List.of(), false).afterSingletonsInstantiated();

            assertThat(appender.list)
                .filteredOn(event -> event.getLevel() == Level.WARN)
                .singleElement()
                .satisfies(event -> assertThat(event.getFormattedMessage())
                    .contains("EMBEDDED_CATALOG", "EMBEDDED_CONNECTED_USER", "EMBEDDED"));
        } finally {
            detachAppender(appender);

            jdbcClient
                .sql("DELETE FROM databasechangelog WHERE id = 'owner-test-embedded'")
                .update();
        }
    }

    private static ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(ProjectOwnershipServiceImpl.class);

        ListAppender<ILoggingEvent> appender = new ListAppender<>();

        appender.start();

        logger.addAppender(appender);

        return appender;
    }

    private static void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(ProjectOwnershipServiceImpl.class);

        logger.detachAppender(appender);
    }

    private static ProjectDeploymentOwnerContributor deploymentOwnerContributor(
        ProjectDeploymentOwner projectDeploymentOwner) {

        return ProjectDeploymentOwnerContributor.of(projectDeploymentOwner, "%1$s = 0");
    }

    private static ProjectOwnerContributor projectOwnerContributor(ProjectOwner projectOwner) {
        return ProjectOwnerContributor.of(projectOwner, "%1$s = 0");
    }

    private long saveDeployment(long projectId, String name) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.DEVELOPMENT);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(1);

        return projectDeploymentRepository.save(projectDeployment)
            .getId();
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
