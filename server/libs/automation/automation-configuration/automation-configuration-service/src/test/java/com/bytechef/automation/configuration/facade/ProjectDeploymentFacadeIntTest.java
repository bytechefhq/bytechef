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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
public class ProjectDeploymentFacadeIntTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectFacade projectFacade;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectDeploymentRepository projectDeploymentRepository;

    @Autowired
    private ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;

    @Autowired
    private ProjectDeploymentFacade projectDeploymentFacade;

    @Autowired
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Autowired
    private ProjectWorkflowRepository projectWorkflowRepository;

    @Autowired
    TagRepository tagRepository;

    private Workspace workspace;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private ProjectDeploymentFacadeHelper projectDeploymentFacadeHelper;

    @Autowired
    private JobFacade jobFacade;

    @Autowired
    private JobService jobService;

    @Autowired
    private PrincipalJobService principalJobService;

    @AfterEach
    public void afterEach() {
        projectDeploymentWorkflowRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceRepository.deleteAll();

        categoryRepository.deleteAll();
        tagRepository.deleteAll();

    }

    @BeforeEach
    void beforeEach() {
        workspace = workspaceRepository.save(new Workspace("test"));

        projectDeploymentFacadeHelper = new ProjectDeploymentFacadeHelper(
            categoryRepository, projectFacade, projectRepository, projectDeploymentFacade, projectWorkflowFacade,
            projectWorkflowRepository);

        // Default stub: no running jobs unless explicitly mocked by a test
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyInt()))
            .thenReturn(Page.empty());
    }

    @Disabled
    @Test
    public void testCreateProjectDeployment() {
        // TODO
    }

    @Disabled
    @Test
    public void testCreateProjectDeploymentJob() {
        // TODO
    }

    @Test
    public void testDeleteProjectDeployment() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());
        ProjectDeploymentDTO projectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);

        ProjectDeploymentDTO projectDeploymentToDelete =
            projectDeploymentFacade.getProjectDeployment(projectDeploymentDTO.id());

        projectDeploymentFacade.deleteProjectDeployment(projectDeploymentToDelete.id());

        List<ProjectDeploymentDTO> workspaceProjectDeployments = projectDeploymentFacade.getWorkspaceProjectDeployments(
            workspace.getId(), (long) Environment.DEVELOPMENT.ordinal(), projectDeploymentToDelete.projectId(), null,
            true);

        assertThat(workspaceProjectDeployments).hasSize(0);
    }

    @Disabled
    @Test
    public void testGetProjectDeployment() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetProjectDeploymentTags() {
        // TODO
    }

    @Disabled
    @Test
    public void testSearchProjectDeployments() {
        // TODO
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowEnabledToEnabledShouldDisableAndReEnable() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // When - Update the same workflow (enabled to enabled transition)
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // Then - Verify the workflow is still enabled
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isTrue();
                assertThat(workflow.workflowId()).isEqualTo(workflowId);
            });
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowDisabledToEnabledShouldOnlyEnable() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, false);

        // When - Enable the workflow (disabled to enabled transition)
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // Then - Verify the workflow is enabled
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isTrue();
                assertThat(workflow.workflowId()).isEqualTo(workflowId);
            });
    }

    @Test
    public void testDisablingWorkflowStopsRunningJobs() {
        // Given - Create a project with a workflow and enable deployment + workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        long deploymentId = projectDeploymentDTO.id();

        projectDeploymentFacade.enableProjectDeployment(deploymentId, true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(deploymentId, workflowId, true);

        // And mock PrincipalJobService to return STARTED job IDs for this deployment/workflow
        List<Long> runningJobIds = List.of(101L, 202L);

        when(
            principalJobService.getJobIds(
                eq(Job.Status.STARTED), eq(null), eq(null), eq(List.of(deploymentId)), eq(PlatformType.AUTOMATION),
                eq(List.of(workflowId)), eq(0)))
                    .thenReturn(new PageImpl<>(runningJobIds, PageRequest.of(0, 20), runningJobIds.size()));

        // When - Disable the workflow
        projectDeploymentFacade.enableProjectDeploymentWorkflow(deploymentId, workflowId, false);

        // Then - verify each running job was stopped
        verify(jobFacade).stopJob(101L);
        verify(jobFacade).stopJob(202L);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowEnabledToDisabledShouldDisable() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // When - Disable the workflow (enabled to disabled transition)
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, false);

        // Then - Verify the workflow is disabled
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isFalse();
                assertThat(workflow.workflowId()).isEqualTo(workflowId);
            });
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowProjectDeploymentDisabledShouldNotAffectTriggers() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), false);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        // When - Enable the workflow while project deployment is disabled
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        // Then - Verify the workflow is enabled but triggers should not be affected
        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.enabled()).isFalse();
        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isTrue();
                assertThat(workflow.workflowId()).isEqualTo(workflowId);
            });
    }

    @Disabled
    @Test
    public void testUpdateProjectDeploymentTags() {
        // TODO
    }

    @Test
    public void testWorkflowLastExecutionDateFiltersJobsByDeploymentId() {
        // Given - Create a project with a workflow and deploy it
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        long deploymentId = projectDeploymentDTO.id();

        // And mock job execution - simulate a job that ran in this deployment
        long jobIdForThisDeployment = 12345L;
        Instant executionTime = Instant.parse("2024-06-15T10:30:00Z");

        Job mockJob = new Job();

        mockJob.setEndDate(executionTime);

        when(principalJobService.fetchLastWorkflowJobId(eq(deploymentId), anyList(), eq(PlatformType.AUTOMATION)))
            .thenReturn(Optional.of(jobIdForThisDeployment));
        when(jobService.getJob(jobIdForThisDeployment))
            .thenReturn(mockJob);

        // When - Get the project deployment (which fetches workflow execution dates)
        ProjectDeploymentDTO result = projectDeploymentFacade.getProjectDeployment(deploymentId);

        // Then - Verify that fetchLastWorkflowJobId was called with THIS deployment's ID
        // This ensures execution dates are filtered by deployment, not fetched globally
        verify(principalJobService, atLeastOnce()).fetchLastWorkflowJobId(
            eq(deploymentId), anyList(), eq(PlatformType.AUTOMATION));

        // And the workflow should have the execution date from the mocked job
        assertThat(result.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> assertThat(workflow.lastExecutionDate()).isEqualTo(executionTime));
    }

    @Test
    public void testWorkflowLastExecutionDateIsolatedBetweenDeployments() {
        // Given - Create a project with a workflow
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        // And create TWO deployments (simulating different environments)
        ProjectDeploymentDTO deployment1 = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        ProjectDeploymentDTO deployment2 = projectDeploymentFacadeHelper.createProjectDeploymentForEnvironment(
            workspace.getId(), projectDTO, Environment.STAGING);

        long deployment1Id = deployment1.id();
        long deployment2Id = deployment2.id();

        // And mock different job executions for each deployment
        long jobIdForDeployment1 = 1001L;
        long jobIdForDeployment2 = 2002L;
        Instant executionTime1 = Instant.parse("2024-06-01T10:00:00Z");
        Instant executionTime2 = Instant.parse("2024-06-15T15:00:00Z");

        Job mockJob1 = new Job();

        mockJob1.setEndDate(executionTime1);

        Job mockJob2 = new Job();

        mockJob2.setEndDate(executionTime2);

        // Reset mocks to clear default stub
        reset(principalJobService);

        when(principalJobService.fetchLastWorkflowJobId(eq(deployment1Id), anyList(), eq(PlatformType.AUTOMATION)))
            .thenReturn(Optional.of(jobIdForDeployment1));
        when(principalJobService.fetchLastWorkflowJobId(eq(deployment2Id), anyList(), eq(PlatformType.AUTOMATION)))
            .thenReturn(Optional.of(jobIdForDeployment2));

        when(jobService.getJob(jobIdForDeployment1))
            .thenReturn(mockJob1);
        when(jobService.getJob(jobIdForDeployment2))
            .thenReturn(mockJob2);

        // Re-stub getJobIds for other tests
        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyInt()))
            .thenReturn(Page.empty());

        // When - Get each deployment
        ProjectDeploymentDTO result1 = projectDeploymentFacade.getProjectDeployment(deployment1Id);
        ProjectDeploymentDTO result2 = projectDeploymentFacade.getProjectDeployment(deployment2Id);

        // Then - Each deployment should have its OWN execution date, not mixed up
        assertThat(result1.projectDeploymentWorkflows())
            .first()
            .satisfies(workflow -> assertThat(workflow.lastExecutionDate()).isEqualTo(executionTime1));

        assertThat(result2.projectDeploymentWorkflows())
            .first()
            .satisfies(workflow -> assertThat(workflow.lastExecutionDate()).isEqualTo(executionTime2));

        // Verify the correct deployment IDs were used in the queries
        verify(principalJobService, atLeastOnce()).fetchLastWorkflowJobId(
            eq(deployment1Id), anyList(), eq(PlatformType.AUTOMATION));
        verify(principalJobService, atLeastOnce()).fetchLastWorkflowJobId(
            eq(deployment2Id), anyList(), eq(PlatformType.AUTOMATION));
    }
}
