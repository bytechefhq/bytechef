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

package com.bytechef.automation.workflow.execution.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Covers the rows of the executions list that are trigger executions without a job.
 *
 * @author Ivica Cardic
 */
class ProjectWorkflowExecutionFacadeTriggerRowsTest {

    private static final long DEPLOYMENT_ID = 9L;
    private static final long PROJECT_ID = 3L;
    private static final long TRIGGER_EXECUTION_ID = 77L;
    private static final UUID WORKFLOW_UUID = UUID.randomUUID();
    private static final String WORKFLOW_ID = "workflow-1";

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final TriggerExecutionService triggerExecutionService = mock(TriggerExecutionService.class);
    private final WorkflowExecutionRowService workflowExecutionRowService = mock(WorkflowExecutionRowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private ProjectWorkflowExecutionFacadeImpl facade;
    private Workflow workflow;
    private TriggerExecution triggerExecution;

    @BeforeEach
    void beforeEach() {
        ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
        ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

        lenient().when(componentDefinition.getTitle())
            .thenReturn("Webhook");
        lenient().when(componentDefinition.getIcon())
            .thenReturn("icon");
        lenient().when(componentDefinitionService.hasComponentDefinition(anyString(), any()))
            .thenReturn(true);
        lenient().when(componentDefinitionService.getComponentDefinition(anyString(), any()))
            .thenReturn(componentDefinition);

        facade = new ProjectWorkflowExecutionFacadeImpl(
            componentDefinitionService, mock(ContextService.class), mock(Evaluator.class),
            mock(EnvironmentService.class), workflowExecutionRowService, mock(JobService.class),
            mock(PrincipalJobService.class), mock(ProjectFacade.class), projectDeploymentService, projectService,
            projectWorkflowService, mock(TaskDispatcherDefinitionService.class), mock(TaskExecutionService.class),
            mock(TaskFileStorage.class), triggerExecutionService, mock(TriggerFileStorage.class), workflowService);

        WorkflowTrigger workflowTrigger = mock(WorkflowTrigger.class);

        lenient().when(workflowTrigger.getName())
            .thenReturn("trigger_1");
        lenient().when(workflowTrigger.getType())
            .thenReturn("webhook/v1/newRequest");
        lenient().when(workflowTrigger.getParameters())
            .thenReturn(Map.of());

        workflow = mock(Workflow.class);

        lenient().when(workflow.getId())
            .thenReturn(WORKFLOW_ID);
        lenient().when(workflow.getExtensions(WorkflowExtConstants.TRIGGERS, WorkflowTrigger.class, List.of()))
            .thenReturn(List.of(workflowTrigger));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(DEPLOYMENT_ID);
        projectDeployment.setProjectId(PROJECT_ID);
        projectDeployment.setProjectVersion(1);

        Project project = mock(Project.class);

        lenient().when(project.getId())
            .thenReturn(PROJECT_ID);

        triggerExecution = TriggerExecution.builder()
            .id(TRIGGER_EXECUTION_ID)
            .status(TriggerExecution.Status.FAILED)
            .startDate(Instant.parse("2026-09-04T10:00:00Z"))
            .workflowExecutionId(
                WorkflowExecutionId.of(PlatformType.AUTOMATION, DEPLOYMENT_ID, WORKFLOW_UUID.toString(), "trigger_1"))
            .workflowTrigger(workflowTrigger)
            .build();

        lenient().when(projectDeploymentService.getProjectDeployment(DEPLOYMENT_ID))
            .thenReturn(projectDeployment);
        lenient().when(projectDeploymentService.getProjectDeployments(List.of(DEPLOYMENT_ID)))
            .thenReturn(List.of(projectDeployment));
        lenient().when(projectService.getProject(PROJECT_ID))
            .thenReturn(project);
        lenient().when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID.toString()))
            .thenReturn(Optional.of(WORKFLOW_ID));
        lenient().when(projectWorkflowService.getProjectWorkflows(List.of(PROJECT_ID)))
            .thenReturn(
                List.of(
                    new ProjectWorkflow(PROJECT_ID, 1, WORKFLOW_ID, WORKFLOW_UUID),
                    new ProjectWorkflow(PROJECT_ID, 2, WORKFLOW_ID, WORKFLOW_UUID)));
        lenient().when(workflowService.getWorkflow(WORKFLOW_ID))
            .thenReturn(workflow);
        lenient().when(workflowService.getWorkflows(List.of(WORKFLOW_ID)))
            .thenReturn(List.of(workflow));
    }

    @Test
    void testAFailedJoblessTriggerBecomesARowWithoutAJob() {
        when(workflowExecutionRowService.getWorkflowExecutionRows(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyList(), anyInt()))
                .thenReturn(new PageImpl<>(
                    List.of(new WorkflowExecutionRowDTO(
                        WorkflowExecutionRowDTO.Kind.TRIGGER_EXECUTION, TRIGGER_EXECUTION_ID)),
                    PageRequest.of(0, 20), 1));
        when(triggerExecutionService.getTriggerExecutions(List.of(TRIGGER_EXECUTION_ID)))
            .thenReturn(List.of(triggerExecution));

        Page<WorkflowExecutionDTO> page = facade.getWorkflowExecutions(
            false, null, null, null, null, null, DEPLOYMENT_ID, WORKFLOW_ID, 1L, 0);

        assertEquals(1, page.getTotalElements());

        WorkflowExecutionDTO row = page.getContent()
            .get(0);

        assertEquals(TRIGGER_EXECUTION_ID, row.id());
        assertTrue(row.isTriggerOnly());
        assertNull(row.job());
        assertEquals(WORKFLOW_ID, row.workflow()
            .getId());
        assertEquals(DEPLOYMENT_ID, row.projectDeployment()
            .getId());
        assertNotNull(row.triggerExecution());
        assertEquals(TriggerExecution.Status.FAILED, row.triggerExecution()
            .status());
    }

    @Test
    void testTheEncodedWorkflowExecutionIdsOfEveryDeployedTriggerReachThePageQuery() {
        when(workflowExecutionRowService.getWorkflowExecutionRows(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyList(), anyInt()))
                .thenReturn(Page.empty());

        facade.getWorkflowExecutions(false, null, null, null, null, null, DEPLOYMENT_ID, WORKFLOW_ID, 1L, 0);

        ArgumentCaptor<List<String>> idsArgumentCaptor = ArgumentCaptor.captor();

        verify(workflowExecutionRowService).getWorkflowExecutionRows(
            any(), any(), any(), eq(List.of(DEPLOYMENT_ID)), eq(PlatformType.AUTOMATION), eq(List.of(WORKFLOW_ID)),
            eq(true), idsArgumentCaptor.capture(), eq(0));

        assertEquals(
            List.of(
                WorkflowExecutionId.of(PlatformType.AUTOMATION, DEPLOYMENT_ID, WORKFLOW_UUID.toString(), "trigger_1")
                    .toString()),
            idsArgumentCaptor.getValue());
    }

    @Test
    void testTriggersStayOutOfThePageWhenTheStatusFilterIsNotFailed() {
        when(workflowExecutionRowService.getWorkflowExecutionRows(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyList(), anyInt()))
                .thenReturn(Page.empty());

        facade.getWorkflowExecutions(
            false, null, Status.COMPLETED, null, null, null, DEPLOYMENT_ID, WORKFLOW_ID, 1L, 0);

        verify(workflowExecutionRowService).getWorkflowExecutionRows(
            eq(Status.COMPLETED), any(), any(), anyList(), any(), anyList(), anyBoolean(), eq(List.of()), anyInt());
    }

    @Test
    void testTheDetailOfATriggerExecutionWhoseWorkflowIsGoneIsNotFound() {
        when(triggerExecutionService.getTriggerExecution(TRIGGER_EXECUTION_ID)).thenReturn(triggerExecution);
        when(projectWorkflowService.fetchProjectWorkflowWorkflowId(DEPLOYMENT_ID, WORKFLOW_UUID.toString()))
            .thenReturn(Optional.empty());

        assertThrows(
            NoSuchElementException.class, () -> facade.getTriggerExecutionWorkflowExecution(TRIGGER_EXECUTION_ID));
    }

    @Test
    void testTheDetailOfATriggerExecutionHasNoJobAndItsTrigger() {
        when(triggerExecutionService.getTriggerExecution(TRIGGER_EXECUTION_ID)).thenReturn(triggerExecution);

        WorkflowExecutionDTO detail = facade.getTriggerExecutionWorkflowExecution(TRIGGER_EXECUTION_ID);

        assertEquals(TRIGGER_EXECUTION_ID, detail.id());
        assertNull(detail.job());
        assertEquals("Webhook", detail.triggerExecution()
            .title());
        assertEquals(WORKFLOW_ID, detail.workflow()
            .getId());
    }
}
