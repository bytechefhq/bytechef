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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.calls;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.ProjectIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.util.ProjectDeploymentFacadeHelper;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.platform.category.repository.CategoryRepository;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Autowired
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private TriggerLifecycleFacade triggerLifecycleFacade;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private TriggerStateService triggerStateService;

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

        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());

        when(triggerDefinitionService.getTriggerDefinition(anyString(), anyInt(), anyString()))
            .thenThrow(new IllegalArgumentException("Trigger definition not found"));
    }

    @Test
    public void testCreateProjectDeploymentWithRequiredIntegerInputs() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        projectDeploymentFacadeHelper.addTestWorkflow(
            projectDTO,
            """
                {
                    "label": "BUG - RunDaily - Scheduler",
                    "inputs": [
                        {"name": "destinationFolderName", "label": "Destination folder name", "type": "string"},
                        {"name": "serviceProviderEmail", "label": "Email", "type": "string", "required": true},
                        {"name": "hourToRun", "label": "Hour", "type": "integer", "required": true},
                        {"name": "minutesToRun", "label": "Minute", "type": "integer", "required": true}
                    ],
                    "triggers": [
                        {
                            "label": "Every Work Day",
                            "name": "trigger_1",
                            "type": "schedule/v1/everyDay",
                            "parameters": {
                                "hour": "${hourToRun}",
                                "minute": "${minutesToRun}",
                                "timezone": "Europe/Zagreb",
                                "dayOfWeek": [2, 3, 4, 5, 6]
                            }
                        }
                    ],
                    "tasks": [
                        {
                            "label": "Logger",
                            "name": "logger_1",
                            "type": "logger/v1/info",
                            "parameters": {"text": "Run every day at ${hourToRun}:${minutesToRun}"}
                        }
                    ]
                }""");

        projectFacade.publishProject(projectDTO.id(), "Published for test", false);

        ProjectWorkflow publishedProjectWorkflow = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectDTO.id(), 1)
            .getFirst();

        Map<String, Object> inputs = Map.of(
            "serviceProviderEmail", "racuni@example.com",
            "hourToRun", 11,
            "minutesToRun", 50);

        ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, inputs, true, null, null, null, null, null, null, null, 0,
            publishedProjectWorkflow.getWorkflowId(), publishedProjectWorkflow.getUuidAsString());

        ProjectDeploymentDTO projectDeploymentDTO = ProjectDeploymentDTO.builder()
            .projectId(projectDTO.id())
            .name("GH-5404")
            .environment(Environment.DEVELOPMENT)
            .projectVersion(1)
            .projectDeploymentWorkflows(List.of(projectDeploymentWorkflowDTO))
            .build();

        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(projectDeploymentDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);
        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            projectDeploymentId, publishedProjectWorkflow.getWorkflowId(), true);

        ProjectDeploymentDTO createdProjectDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentId);

        assertThat(createdProjectDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(projectDeploymentWorkflow -> {
                assertThat(projectDeploymentWorkflow.enabled()).isTrue();

                Map<String, ?> savedInputs = projectDeploymentWorkflow.inputs();

                assertThat(savedInputs.get("hourToRun")).isEqualTo(11);
                assertThat(savedInputs.get("minutesToRun")).isEqualTo(50);
                assertThat(savedInputs.get("serviceProviderEmail")).isEqualTo("racuni@example.com");
            });
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

    @Test
    public void testDeleteProjectDeploymentRemovesAllPrincipalJobsBeforeAnyJob() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());
        ProjectDeploymentDTO projectDeploymentDTO =
            projectDeploymentFacadeHelper.createProjectDeployment(workspace.getId(), projectDTO);

        long deploymentId = projectDeploymentDTO.id();
        long parentJobId = 501L;
        long childJobId = 502L;

        when(principalJobService.getJobIds(deploymentId, PlatformType.AUTOMATION))
            .thenReturn(List.of(parentJobId, childJobId));

        projectDeploymentFacade.deleteProjectDeployment(deploymentId);

        InOrder inOrder = inOrder(principalJobService, jobFacade);

        inOrder.verify(principalJobService)
            .deletePrincipalJobs(parentJobId, PlatformType.AUTOMATION);
        inOrder.verify(principalJobService)
            .deletePrincipalJobs(childJobId, PlatformType.AUTOMATION);
        inOrder.verify(jobFacade, calls(1))
            .deleteJob(anyLong());
        inOrder.verify(jobFacade, calls(1))
            .deleteJob(anyLong());

        verify(jobFacade).deleteJob(parentJobId);
        verify(jobFacade).deleteJob(childJobId);
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowEnabledToEnabledShouldDisableAndReEnable() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

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
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, false);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

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
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        long deploymentId = projectDeploymentDTO.id();

        projectDeploymentFacade.enableProjectDeployment(deploymentId, true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(deploymentId, workflowId, true);

        List<Long> runningJobIds = List.of(101L, 202L);

        when(
            principalJobService.getJobIds(
                eq(Job.Status.STARTED), eq(null), eq(null), eq(List.of(deploymentId)), eq(PlatformType.AUTOMATION),
                eq(List.of(workflowId)), eq(false), eq(0)))
                    .thenReturn(new PageImpl<>(runningJobIds, PageRequest.of(0, 20), runningJobIds.size()));

        projectDeploymentFacade.enableProjectDeploymentWorkflow(deploymentId, workflowId, false);

        verify(jobFacade).stopJob(101L);
        verify(jobFacade).stopJob(202L);
    }

    @Test
    public void testEnableRolledBackByTheSurroundingTransactionDisarmsTheTriggersItArmed() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeploymentWithTriggers(
            workspace.getId(), projectDTO);

        long projectDeploymentId = projectDeploymentDTO.id();
        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        reset(triggerLifecycleFacade);

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        IllegalStateException callerFailure = new IllegalStateException("a later write of the caller failed");

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(transactionStatus -> {
            projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

            verify(triggerLifecycleFacade, never()).executeTriggerDisable(any(), any(), any(), any(), any());

            throw callerFailure;
        })).isSameAs(callerFailure);

        ProjectDeploymentDTO rolledBackProjectDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentId);

        assertThat(rolledBackProjectDeployment.enabled()).isFalse();

        ArgumentCaptor<WorkflowExecutionId> armedWorkflowExecutionIdCaptor = ArgumentCaptor.captor();
        InOrder inOrder = inOrder(triggerLifecycleFacade);

        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                eq(workflowId), armedWorkflowExecutionIdCaptor.capture(), any(), any(), any(), any(), anyLong());
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerDisable(eq(workflowId), eq(armedWorkflowExecutionIdCaptor.getValue()), any(), any(), any());
        verifyNoMoreInteractions(triggerLifecycleFacade);
        verify(jobFacade, never()).stopJob(anyLong());
    }

    @Test
    public void testEnableRolledBackByTheSurroundingTransactionUndoesTheArmedWebhookEnableInANewTransaction() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeploymentWithTriggers(
            workspace.getId(), projectDTO);

        long projectDeploymentId = projectDeploymentDTO.id();
        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        reset(triggerLifecycleFacade);

        WebhookEnableOutput armedWebhookEnableOutput = new WebhookEnableOutput(
            Map.of("id", "subscription-armed-in-the-rolled-back-transaction"), null);

        when(triggerStateService.fetchValue(any())).thenReturn(Optional.empty());
        when(triggerLifecycleFacade.executeTriggerEnable(any(), any(), any(), any(), any(), any(), anyLong()))
            .thenReturn(armedWebhookEnableOutput);

        List<Boolean> undoSynchronizationActive = new ArrayList<>();

        when(triggerLifecycleFacade.executeTriggerEnableUndo(any(), any(), any(), any(), any(), any(), any()))
            .thenAnswer(invocation -> {
                undoSynchronizationActive.add(TransactionSynchronizationManager.isSynchronizationActive());

                return true;
            });

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        IllegalStateException callerFailure = new IllegalStateException("a later write of the caller failed");

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(transactionStatus -> {
            projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

            verify(triggerLifecycleFacade, never()).executeTriggerEnableUndo(
                any(), any(), any(), any(), any(), any(), any());

            throw callerFailure;
        })).isSameAs(callerFailure);

        ArgumentCaptor<WorkflowExecutionId> armedWorkflowExecutionIdCaptor = ArgumentCaptor.captor();
        InOrder inOrder = inOrder(triggerLifecycleFacade);

        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnable(
                eq(workflowId), armedWorkflowExecutionIdCaptor.capture(), any(), any(), any(), any(), anyLong());
        inOrder.verify(triggerLifecycleFacade)
            .executeTriggerEnableUndo(
                eq(workflowId), eq(armedWorkflowExecutionIdCaptor.getValue()), any(), any(), any(),
                eq(armedWebhookEnableOutput), isNull());
        verifyNoMoreInteractions(triggerLifecycleFacade);
        verify(triggerStateService, never()).save(any(), any());

        assertThat(undoSynchronizationActive).containsExactly(true);
    }

    @Test
    public void testDisableWorkflowCommittedByTheSurroundingTransactionStopsItsJobsAfterCommitAndUndoesNothing() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeploymentWithTriggers(
            workspace.getId(), projectDTO);

        long projectDeploymentId = projectDeploymentDTO.id();
        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);

        when(
            principalJobService.getJobIds(
                eq(Job.Status.STARTED), eq(null), eq(null), eq(List.of(projectDeploymentId)),
                eq(PlatformType.AUTOMATION), eq(List.of(workflowId)), eq(false), eq(0)))
                    .thenReturn(new PageImpl<>(List.of(101L), PageRequest.of(0, 20), 1));

        reset(triggerLifecycleFacade);

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, false);

            verify(jobFacade, never()).stopJob(anyLong());
        });

        verify(jobFacade).stopJob(101L);
        verify(triggerLifecycleFacade).executeTriggerDisable(eq(workflowId), any(), any(), any(), any());
        verifyNoMoreInteractions(triggerLifecycleFacade);

        ProjectDeploymentDTO committedProjectDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentId);

        assertThat(committedProjectDeployment.projectDeploymentWorkflows())
            .singleElement()
            .satisfies(projectDeploymentWorkflow -> assertThat(projectDeploymentWorkflow.enabled()).isFalse());
    }

    @Test
    public void testUpdateProjectDeploymentWorkflowEnabledToDisabledShouldDisable() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, false);

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
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), false);

        String workflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), workflowId, true);

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

    @Test
    public void testWorkflowLastExecutionDateFiltersJobsByDeploymentId() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        long deploymentId = projectDeploymentDTO.id();

        long jobIdForThisDeployment = 12345L;
        Instant executionTime = Instant.parse("2024-06-15T10:30:00Z");

        Job mockJob = new Job();

        mockJob.setEndDate(executionTime);

        when(principalJobService.fetchLastWorkflowJobId(eq(deploymentId), anyList(), eq(PlatformType.AUTOMATION)))
            .thenReturn(Optional.of(jobIdForThisDeployment));
        when(jobService.getJob(jobIdForThisDeployment))
            .thenReturn(mockJob);

        ProjectDeploymentDTO result = projectDeploymentFacade.getProjectDeployment(deploymentId);

        verify(principalJobService, atLeastOnce()).fetchLastWorkflowJobId(
            eq(deploymentId), anyList(), eq(PlatformType.AUTOMATION));

        assertThat(result.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> assertThat(workflow.lastExecutionDate()).isEqualTo(executionTime));
    }

    @Test
    public void testWorkflowLastExecutionDateIsolatedBetweenDeployments() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO deployment1 = projectDeploymentFacadeHelper.createProjectDeployment(
            workspace.getId(), projectDTO);

        ProjectDeploymentDTO deployment2 = projectDeploymentFacadeHelper.createProjectDeploymentForEnvironment(
            workspace.getId(), projectDTO, Environment.STAGING);

        long deployment1Id = deployment1.id();
        long deployment2Id = deployment2.id();

        long jobIdForDeployment1 = 1001L;
        long jobIdForDeployment2 = 2002L;
        Instant executionTime1 = Instant.parse("2024-06-01T10:00:00Z");
        Instant executionTime2 = Instant.parse("2024-06-15T15:00:00Z");

        Job mockJob1 = new Job();

        mockJob1.setEndDate(executionTime1);

        Job mockJob2 = new Job();

        mockJob2.setEndDate(executionTime2);

        reset(principalJobService);

        when(principalJobService.fetchLastWorkflowJobId(eq(deployment1Id), anyList(), eq(PlatformType.AUTOMATION)))
            .thenReturn(Optional.of(jobIdForDeployment1));
        when(principalJobService.fetchLastWorkflowJobId(eq(deployment2Id), anyList(), eq(PlatformType.AUTOMATION)))
            .thenReturn(Optional.of(jobIdForDeployment2));

        when(jobService.getJob(jobIdForDeployment1))
            .thenReturn(mockJob1);
        when(jobService.getJob(jobIdForDeployment2))
            .thenReturn(mockJob2);

        when(principalJobService.getJobIds(any(), any(), any(), any(), any(), any(), anyBoolean(), anyInt()))
            .thenReturn(Page.empty());

        ProjectDeploymentDTO result1 = projectDeploymentFacade.getProjectDeployment(deployment1Id);
        ProjectDeploymentDTO result2 = projectDeploymentFacade.getProjectDeployment(deployment2Id);

        assertThat(result1.projectDeploymentWorkflows())
            .first()
            .satisfies(workflow -> assertThat(workflow.lastExecutionDate()).isEqualTo(executionTime1));

        assertThat(result2.projectDeploymentWorkflows())
            .first()
            .satisfies(workflow -> assertThat(workflow.lastExecutionDate()).isEqualTo(executionTime2));

        verify(principalJobService, atLeastOnce()).fetchLastWorkflowJobId(
            eq(deployment1Id), anyList(), eq(PlatformType.AUTOMATION));
        verify(principalJobService, atLeastOnce()).fetchLastWorkflowJobId(
            eq(deployment2Id), anyList(), eq(PlatformType.AUTOMATION));
    }

    @Test
    public void testUpdateProjectVersionWithWorkflowDisabledShouldDisableTriggers() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeploymentWithTriggers(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String v1WorkflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), v1WorkflowId, true);

        projectFacade.publishProject(projectDTO.id(), "Published v2 for test", false);

        List<ProjectWorkflow> v2ProjectWorkflows = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectDTO.id(), 2);

        ProjectWorkflow v2ProjectWorkflow = v2ProjectWorkflows.getFirst();

        String v2WorkflowId = v2ProjectWorkflow.getWorkflowId();
        String workflowUuid = v2ProjectWorkflow.getUuidAsString();

        reset(triggerLifecycleFacade);

        ProjectDeploymentWorkflowDTO disabledWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, Map.of(), false, null, null, null, null, null, null, null, 0, v2WorkflowId,
            workflowUuid);

        ProjectDeploymentDTO currentDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        ProjectDeploymentDTO updateDTO = ProjectDeploymentDTO.builder()
            .id(projectDeploymentDTO.id())
            .projectId(projectDTO.id())
            .name(projectDeploymentDTO.name())
            .enabled(true)
            .environment(projectDeploymentDTO.environment())
            .projectVersion(2)
            .projectDeploymentWorkflows(List.of(disabledWorkflowDTO))
            .version(currentDeployment.version())
            .build();

        projectDeploymentFacade.updateProjectDeployment(updateDTO);

        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> assertThat(workflow.enabled()).isFalse());

        verify(triggerLifecycleFacade, atLeastOnce()).executeTriggerDisable(any(), any(), any(), any(), any());
    }

    @Test
    public void testUpdateProjectVersionWithWorkflowStillEnabledShouldReenableTriggers() {
        ProjectDTO projectDTO = projectDeploymentFacadeHelper.createProject(workspace.getId());

        ProjectDeploymentDTO projectDeploymentDTO = projectDeploymentFacadeHelper.createProjectDeploymentWithTriggers(
            workspace.getId(), projectDTO);

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentDTO.id(), true);

        String v1WorkflowId = projectDeploymentDTO.projectDeploymentWorkflows()
            .getFirst()
            .workflowId();

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentDTO.id(), v1WorkflowId, true);

        projectFacade.publishProject(projectDTO.id(), "Published v2 for test", false);

        List<ProjectWorkflow> v2ProjectWorkflows = projectWorkflowRepository.findAllByProjectIdAndProjectVersion(
            projectDTO.id(), 2);

        ProjectWorkflow v2ProjectWorkflow = v2ProjectWorkflows.getFirst();

        String v2WorkflowId = v2ProjectWorkflow.getWorkflowId();
        String workflowUuid = v2ProjectWorkflow.getUuidAsString();

        reset(triggerLifecycleFacade);

        ProjectDeploymentWorkflowDTO enabledWorkflowDTO = new ProjectDeploymentWorkflowDTO(
            List.of(), null, null, Map.of(), true, null, null, null, null, null, null, null, 0, v2WorkflowId,
            workflowUuid);

        ProjectDeploymentDTO currentDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        ProjectDeploymentDTO updateDTO = ProjectDeploymentDTO.builder()
            .id(projectDeploymentDTO.id())
            .projectId(projectDTO.id())
            .name(projectDeploymentDTO.name())
            .enabled(true)
            .environment(projectDeploymentDTO.environment())
            .projectVersion(2)
            .projectDeploymentWorkflows(List.of(enabledWorkflowDTO))
            .version(currentDeployment.version())
            .build();

        projectDeploymentFacade.updateProjectDeployment(updateDTO);

        ProjectDeploymentDTO updatedDeployment = projectDeploymentFacade.getProjectDeployment(
            projectDeploymentDTO.id());

        assertThat(updatedDeployment.projectDeploymentWorkflows())
            .hasSize(1)
            .first()
            .satisfies(workflow -> {
                assertThat(workflow.enabled()).isTrue();
                assertThat(workflow.workflowId()).isEqualTo(v2WorkflowId);
            });

        verify(triggerLifecycleFacade, atLeastOnce()).executeTriggerDisable(any(), any(), any(), any(), any());
        verify(triggerLifecycleFacade, atLeastOnce()).executeTriggerEnable(
            any(), any(), any(), any(), any(), any(), anyLong());
    }
}
