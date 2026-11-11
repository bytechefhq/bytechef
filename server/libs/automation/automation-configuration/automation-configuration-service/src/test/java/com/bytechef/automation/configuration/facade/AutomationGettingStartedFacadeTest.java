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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.AutomationGettingStartedDTO;
import com.bytechef.automation.configuration.dto.AutomationGettingStartedRunDTO;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AutomationGettingStartedFacadeTest {

    private static final long OTHER_WORKSPACE_ID = 2L;
    private static final long WORKSPACE_ID = 1L;

    private AutomationGettingStartedFacadeImpl automationGettingStartedFacade;
    private JobService jobService;
    private PrincipalJobService principalJobService;
    private ProjectDeploymentService projectDeploymentService;
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private ProjectService projectService;
    private ProjectWorkflowService projectWorkflowService;
    private PropertyService propertyService;
    private TaskExecutionService taskExecutionService;
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        jobService = mock(JobService.class);
        principalJobService = mock(PrincipalJobService.class);
        projectDeploymentService = mock(ProjectDeploymentService.class);
        projectDeploymentWorkflowService = mock(ProjectDeploymentWorkflowService.class);
        projectService = mock(ProjectService.class);
        projectWorkflowService = mock(ProjectWorkflowService.class);
        propertyService = mock(PropertyService.class);
        taskExecutionService = mock(TaskExecutionService.class);
        workflowService = mock(WorkflowService.class);

        automationGettingStartedFacade = new AutomationGettingStartedFacadeImpl(
            jobService, principalJobService, projectDeploymentService, projectDeploymentWorkflowService,
            projectService, projectWorkflowService, propertyService, taskExecutionService, workflowService);

        when(propertyService.getProperties(List.of("gettingStarted"), Scope.AUTOMATION, null))
            .thenReturn(List.of());
    }

    @Test
    void testFirstRunCountsDeploymentsOfAllEnvironments() {
        ProjectDeployment developmentDeployment = deployment(10L, true);
        ProjectDeployment productionDeployment = deployment(20L, true);

        when(developmentDeployment.getEnvironment()).thenReturn(Environment.DEVELOPMENT);
        when(productionDeployment.getEnvironment()).thenReturn(Environment.PRODUCTION);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(developmentDeployment, productionDeployment));

        mockJobs(Job.Status.COMPLETED, 100L, 20L, null);
        mockNoJobs(Job.Status.FAILED);
        mockProjects(project(5L, false));

        when(projectDeploymentService.fetchProjectDeployment(20L)).thenReturn(Optional.of(productionDeployment));

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.firstRun()).isNotNull();
        assertThat(automationGettingStartedDTO.firstRun()
            .projectDeploymentId()).isEqualTo(20L);
        assertThat(automationGettingStartedDTO.firstRun()
            .projectDeploymentEnabled()).isTrue();
        assertThat(automationGettingStartedDTO.completed()).isTrue();

        verify(propertyService).save("gettingStarted", Map.of("completionType", "COMPLETED"), Scope.AUTOMATION, null);
        verify(principalJobService).getJobIds(
            eq(Job.Status.COMPLETED), any(), any(), eq(List.of(10L, 20L)), eq(PlatformType.AUTOMATION), eq(List.of()),
            eq(true), eq(0));
    }

    @Test
    void testNoDeploymentsMeansNoRunAndNoJobQuery() {
        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of());

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.firstRun()).isNull();
        assertThat(automationGettingStartedDTO.lastFailedRun()).isNull();
        assertThat(automationGettingStartedDTO.completed()).isFalse();

        verifyNoInteractions(principalJobService);
    }

    @Test
    void testLastFailedRunNullAfterSuccess() {
        ProjectDeployment projectDeployment = deployment(10L, true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentService.fetchProjectDeployment(10L)).thenReturn(Optional.of(projectDeployment));

        mockJobs(Job.Status.COMPLETED, 100L, 10L, null);
        mockJobs(Job.Status.FAILED, 101L, 10L, "boom");

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.firstRun()).isNotNull();
        assertThat(automationGettingStartedDTO.lastFailedRun()).isNull();
    }

    @Test
    void testLastFailedRunCarriesError() {
        ProjectDeployment projectDeployment = deployment(10L, true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentService.fetchProjectDeployment(10L)).thenReturn(Optional.of(projectDeployment));

        mockNoJobs(Job.Status.COMPLETED);
        mockJobs(Job.Status.FAILED, 101L, 10L, "boom");
        mockProjects(project(5L, false));

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.firstRun()).isNull();
        assertThat(automationGettingStartedDTO.lastFailedRun()).isNotNull();
        assertThat(automationGettingStartedDTO.lastFailedRun()
            .errorMessage()).isEqualTo("boom");
        assertThat(automationGettingStartedDTO.completed()).isFalse();

        verify(propertyService, never()).save(any(), any(), any(), any());
    }

    @Test
    void testLastFailedRunFallsBackToTheFailedTaskError() {
        ProjectDeployment projectDeployment = deployment(10L, true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentService.fetchProjectDeployment(10L)).thenReturn(Optional.of(projectDeployment));

        mockNoJobs(Job.Status.COMPLETED);
        mockJobs(Job.Status.FAILED, 101L, 10L, null);
        mockProjects(project(5L, false));

        TaskExecution failedTaskExecution = new TaskExecution();

        failedTaskExecution.setError(new ExecutionError("Connection refused", List.of()));

        when(taskExecutionService.getLatestAttemptFailedLeafJobTaskExecutions(101L, List.of(), 0, 1))
            .thenReturn(List.of(failedTaskExecution));

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.lastFailedRun()).isNotNull();
        assertThat(automationGettingStartedDTO.lastFailedRun()
            .errorMessage()).isEqualTo("Connection refused");
    }

    @Test
    void testLastFailedRunInAnotherWorkspaceHidesItsDetails() {
        ProjectDeployment projectDeployment = deployment(10L, true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentService.fetchProjectDeployment(10L)).thenReturn(Optional.of(projectDeployment));

        mockNoJobs(Job.Status.COMPLETED);
        mockJobs(Job.Status.FAILED, 101L, 10L, "boom");
        mockProjects(project(5L, false));

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(OTHER_WORKSPACE_ID);

        AutomationGettingStartedRunDTO lastFailedRun = automationGettingStartedDTO.lastFailedRun();

        assertThat(lastFailedRun).isNotNull();
        assertThat(lastFailedRun.status()).isEqualTo(Job.Status.FAILED);
        assertThat(lastFailedRun.errorMessage()).isNull();
        assertThat(lastFailedRun.projectDeploymentEnabled()).isNull();
    }

    @Test
    void testFirstRunOfDeletedDeploymentHasNullEnabled() {
        ProjectDeployment projectDeployment = deployment(10L, true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentService.fetchProjectDeployment(10L)).thenReturn(Optional.empty());

        mockJobs(Job.Status.COMPLETED, 100L, 10L, null);

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.firstRun()).isNotNull();
        assertThat(automationGettingStartedDTO.firstRun()
            .projectDeploymentEnabled()).isNull();
    }

    @Test
    void testIgnoresDeploymentsOutsideDevelopment() {
        ProjectDeployment stagingDeployment = deployment(10L, true);

        when(projectDeploymentService.getProjectDeployments(false, null, null, null, null))
            .thenReturn(List.of(stagingDeployment));
        when(projectDeploymentService.getProjectDeployments(false, Environment.STAGING, null, null, null))
            .thenReturn(List.of(stagingDeployment));

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.deployed()).isFalse();
        assertThat(automationGettingStartedDTO.live()).isFalse();
        assertThat(automationGettingStartedFacade.isAutomationGettingStartedCompleted(WORKSPACE_ID)).isFalse();

        verify(propertyService, never()).save(any(), any(), any(), any());
    }

    @Test
    void testCompletedFlagUsesStoredRowWithoutQuerying() {
        when(propertyService.getProperties(List.of("gettingStarted"), Scope.AUTOMATION, null))
            .thenReturn(List.of(new Property()));

        assertThat(automationGettingStartedFacade.isAutomationGettingStartedCompleted(WORKSPACE_ID)).isTrue();

        verifyNoInteractions(projectDeploymentService);
    }

    @Test
    void testCompletedFlagEvaluatesAndPersists() {
        ProjectDeployment projectDeployment = deployment(10L, true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentService.fetchProjectDeployment(10L)).thenReturn(Optional.of(projectDeployment));

        mockJobs(Job.Status.COMPLETED, 100L, 10L, null);

        assertThat(automationGettingStartedFacade.isAutomationGettingStartedCompleted(WORKSPACE_ID)).isTrue();

        verify(propertyService).save("gettingStarted", Map.of("completionType", "COMPLETED"), Scope.AUTOMATION, null);
    }

    @Test
    void testDismissStoresDismissed() {
        automationGettingStartedFacade.dismissAutomationGettingStarted(WORKSPACE_ID);

        verify(propertyService).save("gettingStarted", Map.of("completionType", "DISMISSED"), Scope.AUTOMATION, null);
    }

    @Test
    void testRecipeSignals() {
        mockWorkflows(
            new Workflow(
                "w1", """
                    {"triggers": [{"name": "trigger_1", "type": "manual/v1/manual"}],
                     "tasks": [{"name": "task_1", "type": "httpClient/v2/get"},
                        {"name": "task_2", "type": "logger/v1/info"}]}
                    """, Workflow.Format.JSON),
            new Workflow(
                "w2", """
                    {"triggers": [{"name": "trigger_1", "type": "manual/v1/manual"}], "tasks": []}
                    """, Workflow.Format.JSON));

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.httpClientAdded()).isTrue();
        assertThat(automationGettingStartedDTO.loggerAdded()).isTrue();
    }

    @Test
    void testRecipeSignalsAreFalseWithoutMatchingNodes() {
        mockWorkflows(
            new Workflow(
                "w2", """
                    {"triggers": [{"name": "trigger_1", "type": "manual/v1/manual"}],
                     "tasks": [{"name": "task_1", "type": "httpClient/v2/post"},
                        {"name": "task_2", "type": "loggerAudit/v1/info"}]}
                    """, Workflow.Format.JSON));

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.httpClientAdded()).isFalse();
        assertThat(automationGettingStartedDTO.loggerAdded()).isFalse();
    }

    @Test
    void testRecipeSignalsSkipWorkflowLookupWithoutProjects() {
        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.httpClientAdded()).isFalse();
        assertThat(automationGettingStartedDTO.loggerAdded()).isFalse();

        verifyNoInteractions(projectWorkflowService, workflowService);
    }

    @Test
    void testStageFlagsAreFalseForAnEmptyTenant() {
        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.projectCreated()).isFalse();
        assertThat(automationGettingStartedDTO.workflowCreated()).isFalse();
        assertThat(automationGettingStartedDTO.published()).isFalse();
        assertThat(automationGettingStartedDTO.deployed()).isFalse();
        assertThat(automationGettingStartedDTO.live()).isFalse();
    }

    @Test
    void testProjectCreatedWhenAnyProjectExists() {
        Project project = project(5L, false);

        mockProjects(project);

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.projectCreated()).isTrue();
        assertThat(automationGettingStartedDTO.workflowCreated()).isFalse();
        assertThat(automationGettingStartedDTO.published()).isFalse();
    }

    @Test
    void testWorkflowCreatedWhenAnyProjectHasAWorkflow() {
        mockWorkflows(
            new Workflow(
                "w1", """
                    {"triggers": [{"name": "trigger_1", "type": "manual/v1/manual"}], "tasks": []}
                    """, Workflow.Format.JSON));

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.workflowCreated()).isTrue();
    }

    @Test
    void testPublishedWhenAnyProjectIsPublished() {
        Project draftProject = project(5L, false);
        Project publishedProject = project(6L, true);

        mockProjects(draftProject, publishedProject);

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.published()).isTrue();
    }

    @Test
    void testDeployedButNotLiveWhenNoDeploymentIsEnabled() {
        ProjectDeployment projectDeployment = deployment(10L, false);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));

        mockNoJobs(Job.Status.COMPLETED);
        mockNoJobs(Job.Status.FAILED);

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.deployed()).isTrue();
        assertThat(automationGettingStartedDTO.live()).isFalse();

        verifyNoInteractions(projectDeploymentWorkflowService);
    }

    @Test
    void testNotLiveWhenEnabledDeploymentHasNoEnabledWorkflow() {
        ProjectDeployment projectDeployment = deployment(10L, true);
        ProjectDeploymentWorkflow disabledDeploymentWorkflow = deploymentWorkflow(false);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(List.of(10L)))
            .thenReturn(List.of(disabledDeploymentWorkflow));

        mockNoJobs(Job.Status.COMPLETED);
        mockNoJobs(Job.Status.FAILED);

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.deployed()).isTrue();
        assertThat(automationGettingStartedDTO.live()).isFalse();
    }

    @Test
    void testLiveWhenEnabledDeploymentHasAnEnabledWorkflow() {
        ProjectDeployment disabledDeployment = deployment(10L, false);
        ProjectDeployment enabledDeployment = deployment(20L, true);
        ProjectDeploymentWorkflow disabledDeploymentWorkflow = deploymentWorkflow(false);
        ProjectDeploymentWorkflow enabledDeploymentWorkflow = deploymentWorkflow(true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(disabledDeployment, enabledDeployment));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(List.of(20L)))
            .thenReturn(List.of(disabledDeploymentWorkflow, enabledDeploymentWorkflow));

        mockNoJobs(Job.Status.COMPLETED);
        mockNoJobs(Job.Status.FAILED);

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.live()).isTrue();
    }

    @Test
    void testEmptyWorkspaceReportsProgressMadeInAnotherWorkspace() {
        ProjectDeployment projectDeployment = deployment(10L, true);
        ProjectDeploymentWorkflow projectDeploymentWorkflow = deploymentWorkflow(true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentService.fetchProjectDeployment(10L)).thenReturn(Optional.of(projectDeployment));
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflows(List.of(10L)))
            .thenReturn(List.of(projectDeploymentWorkflow));

        mockJobs(Job.Status.COMPLETED, 100L, 10L, null);
        mockWorkflows(
            new Workflow(
                "w1", """
                    {"triggers": [{"name": "trigger_1", "type": "manual/v1/manual"}],
                     "tasks": [{"name": "task_1", "type": "httpClient/v2/get"},
                        {"name": "task_2", "type": "logger/v1/info"}]}
                    """, Workflow.Format.JSON));
        Project publishedProject = project(5L, true);

        mockProjects(publishedProject);

        AutomationGettingStartedDTO automationGettingStartedDTO =
            automationGettingStartedFacade.getAutomationGettingStarted(OTHER_WORKSPACE_ID);

        assertThat(automationGettingStartedDTO.completed()).isTrue();
        assertThat(automationGettingStartedDTO.firstRun()).isNotNull();
        assertThat(automationGettingStartedDTO.httpClientAdded()).isTrue();
        assertThat(automationGettingStartedDTO.loggerAdded()).isTrue();
        assertThat(automationGettingStartedDTO.projectCreated()).isTrue();
        assertThat(automationGettingStartedDTO.workflowCreated()).isTrue();
        assertThat(automationGettingStartedDTO.published()).isTrue();
        assertThat(automationGettingStartedDTO.deployed()).isTrue();
        assertThat(automationGettingStartedDTO.live()).isTrue();

        verify(projectService, never()).getWorkspaceProjectIds(anyLong());
        verify(projectDeploymentService, never()).getProjectDeployments(any(), any(), any(), any(), eq(WORKSPACE_ID));
        verify(projectDeploymentService, never())
            .getProjectDeployments(any(), any(), any(), any(), eq(OTHER_WORKSPACE_ID));
    }

    @Test
    void testCompletedRunInAnotherWorkspaceCompletesTheGuide() {
        ProjectDeployment projectDeployment = deployment(10L, true);

        when(projectDeploymentService.getProjectDeployments(false, Environment.DEVELOPMENT, null, null, null))
            .thenReturn(List.of(projectDeployment));
        when(projectDeploymentService.fetchProjectDeployment(10L)).thenReturn(Optional.of(projectDeployment));

        mockJobs(Job.Status.COMPLETED, 100L, 10L, null);

        assertThat(automationGettingStartedFacade.isAutomationGettingStartedCompleted(OTHER_WORKSPACE_ID)).isTrue();

        verify(propertyService).save("gettingStarted", Map.of("completionType", "COMPLETED"), Scope.AUTOMATION, null);
    }

    @Test
    void testDismissInOneWorkspaceCompletesEveryWorkspace() {
        automationGettingStartedFacade.dismissAutomationGettingStarted(WORKSPACE_ID);

        verify(propertyService).save("gettingStarted", Map.of("completionType", "DISMISSED"), Scope.AUTOMATION, null);

        when(propertyService.getProperties(List.of("gettingStarted"), Scope.AUTOMATION, null))
            .thenReturn(List.of(new Property()));

        assertThat(automationGettingStartedFacade.isAutomationGettingStartedCompleted(OTHER_WORKSPACE_ID)).isTrue();
        assertThat(automationGettingStartedFacade.getAutomationGettingStarted(OTHER_WORKSPACE_ID)
            .completed()).isTrue();
    }

    private static ProjectDeployment deployment(long id, boolean enabled) {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);

        when(projectDeployment.getId()).thenReturn(id);
        when(projectDeployment.getProjectId()).thenReturn(5L);
        when(projectDeployment.isEnabled()).thenReturn(enabled);

        return projectDeployment;
    }

    private static ProjectDeploymentWorkflow deploymentWorkflow(boolean enabled) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = mock(ProjectDeploymentWorkflow.class);

        when(projectDeploymentWorkflow.isEnabled()).thenReturn(enabled);

        return projectDeploymentWorkflow;
    }

    private static Project project(long id, boolean published) {
        Project project = mock(Project.class);

        when(project.getId()).thenReturn(id);
        when(project.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(project.isPublished()).thenReturn(published);

        return project;
    }

    private void mockJobs(Job.Status status, long jobId, long projectDeploymentId, String errorMessage) {
        when(principalJobService.getJobIds(
            eq(status), any(), any(), anyList(), eq(PlatformType.AUTOMATION), eq(List.of()), eq(true), eq(0)))
                .thenReturn(new PageImpl<>(List.of(jobId)));
        when(principalJobService.getJobPrincipalId(jobId, PlatformType.AUTOMATION)).thenReturn(projectDeploymentId);

        Job job = new Job(jobId);

        job.setStatus(status);
        job.setStartDate(Instant.parse("2026-01-01T00:00:00Z"));
        job.setEndDate(Instant.parse("2026-01-01T00:00:05Z"));

        if (errorMessage != null) {
            job.setError(new ExecutionError(errorMessage, List.of()));
        }

        when(jobService.getJob(jobId)).thenReturn(job);
    }

    private void mockNoJobs(Job.Status status) {
        when(principalJobService.getJobIds(
            eq(status), any(), any(), anyList(), eq(PlatformType.AUTOMATION), eq(List.of()), eq(true), eq(0)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    private void mockWorkflows(Workflow... workflows) {
        ProjectWorkflow firstProjectWorkflow = mock(ProjectWorkflow.class);
        ProjectWorkflow secondProjectWorkflow = mock(ProjectWorkflow.class);

        when(firstProjectWorkflow.getWorkflowId()).thenReturn("w1");
        when(secondProjectWorkflow.getWorkflowId()).thenReturn("w2");

        Project project = project(5L, false);

        mockProjects(project);

        when(projectWorkflowService.getProjectWorkflows(List.of(5L)))
            .thenReturn(List.of(firstProjectWorkflow, secondProjectWorkflow));
        when(workflowService.getWorkflows(anyList())).thenReturn(List.of(workflows));
    }

    private void mockProjects(Project... projects) {
        when(projectService.getProjects(null, null, null, null, null, null)).thenReturn(List.of(projects));
    }
}
