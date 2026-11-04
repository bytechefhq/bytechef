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

package com.bytechef.automation.configuration.instance.accessor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.instance.config.ProjectDeploymentJobPrincipalAccessorIntTestConfiguration;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalSummary;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectDeploymentJobPrincipalAccessorIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class ProjectDeploymentJobPrincipalAccessorIntTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectDeploymentJobPrincipalAccessor projectDeploymentJobPrincipalAccessor;

    @Autowired
    private ProjectDeploymentService projectDeploymentService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Test
    void testGetJobPrincipalSummaryDescribesTheDeployedProject() {
        Project project = createProject("CRM " + UUID.randomUUID());

        ProjectDeployment projectDeployment = createProjectDeployment(project);

        assertThat(
            projectDeploymentJobPrincipalAccessor.getJobPrincipalSummary(projectDeployment.getId(), "missing-workflow"))
                .isEqualTo(
                    new JobPrincipalSummary(Environment.PRODUCTION.ordinal(), project.getId(), project.getName(), 3));
    }

    @Test
    void testGetJobPrincipalSummaryReportsTheProjectVersionTheJobRan() {
        Project project = createProject("CRM " + UUID.randomUUID());

        ProjectDeployment projectDeployment = createProjectDeployment(project);

        String workflowId = "workflow-" + UUID.randomUUID();

        projectWorkflowService.addWorkflow(project.getId(), 2, workflowId, UUID.randomUUID());

        assertThat(projectDeploymentJobPrincipalAccessor.getJobPrincipalSummary(projectDeployment.getId(), workflowId))
            .isEqualTo(
                new JobPrincipalSummary(Environment.PRODUCTION.ordinal(), project.getId(), project.getName(), 2));
    }

    @Test
    void testGetJobPrincipalSummaryOmitsTheNameOfAMissingProject() {
        Project project = createProject("CRM " + UUID.randomUUID());

        ProjectDeployment projectDeployment = createProjectDeployment(project);

        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.execute("SET LOCAL session_replication_role = replica");

            jdbcTemplate.update("DELETE FROM project WHERE id = ?", project.getId());
        });

        assertThat(
            projectDeploymentJobPrincipalAccessor.getJobPrincipalSummary(projectDeployment.getId(), "missing-workflow"))
                .isEqualTo(new JobPrincipalSummary(Environment.PRODUCTION.ordinal(), project.getId(), null, 3));
    }

    private Project createProject(String name) {
        Workspace workspace = workspaceRepository.save(new Workspace("workspace " + UUID.randomUUID()));

        Project project = new Project();

        project.setName(name);
        project.setUuid(UUID.randomUUID());
        project.setWorkspaceId(workspace.getId());

        return projectService.create(project);
    }

    private ProjectDeployment createProjectDeployment(Project project) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(Environment.PRODUCTION);
        projectDeployment.setName("deployment " + UUID.randomUUID());
        projectDeployment.setProjectId(project.getId());
        projectDeployment.setProjectVersion(3);

        return projectDeploymentService.create(projectDeployment);
    }
}
