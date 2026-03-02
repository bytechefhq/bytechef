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

package com.bytechef.automation.configuration.subflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.subflow.domain.SubflowEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class SubflowDataSourceTest {

    private static final String WORKFLOW_UUID = "test-workflow-uuid";
    private static final String WORKFLOW_ID = "workflow-id-123";

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    private SubflowDataSourceImpl subflowDataSource;

    @BeforeEach
    void setUp() {
        subflowDataSource = new SubflowDataSourceImpl(projectService, projectWorkflowService, workflowService);
    }

    @Test
    void testGetSubWorkflowInputSchemaReturnsNullWhenNoCallableTrigger() {
        when(projectWorkflowService.getLastWorkflowId(WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of());

            OutputResponse result = subflowDataSource.getSubWorkflowInputSchema(WORKFLOW_UUID);

            assertNull(result);
        }
    }

    @Test
    void testGetSubWorkflowInputSchemaReturnsNullWhenNoInputSchema() {
        when(projectWorkflowService.getLastWorkflowId(WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        WorkflowTrigger callableTrigger = mock(WorkflowTrigger.class);

        when(callableTrigger.getType()).thenReturn("workflow/v1/newWorkflowCall");
        when(callableTrigger.getParameters()).thenReturn(Map.of());

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class);
            MockedStatic<MapUtils> mockedMapUtils = mockStatic(MapUtils.class)) {

            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of(callableTrigger));
            mockedMapUtils
                .when(() -> MapUtils.getString(callableTrigger.getParameters(), WorkflowConstants.INPUT_SCHEMA))
                .thenReturn(null);

            OutputResponse result = subflowDataSource.getSubWorkflowInputSchema(WORKFLOW_UUID);

            assertNull(result);
        }
    }

    @Test
    void testGetSubWorkflowOutputSchemaReturnsNullWhenNoCallableResponseTask() {
        when(projectWorkflowService.getLastWorkflowId(WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getTasks(true)).thenReturn(List.of());

        OutputResponse result = subflowDataSource.getSubWorkflowOutputSchema(WORKFLOW_UUID);

        assertNull(result);
    }

    @Test
    void testGetSubWorkflowOutputSchemaReturnsNullWhenNoOutputSchema() {
        when(projectWorkflowService.getLastWorkflowId(WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        WorkflowTask callableResponseTask = mock(WorkflowTask.class);

        when(callableResponseTask.getType()).thenReturn("workflow/v1/responseToWorkflowCall");
        when(callableResponseTask.getParameters()).thenReturn(Map.of());
        when(workflow.getTasks(true)).thenReturn(List.of(callableResponseTask));

        try (MockedStatic<MapUtils> mockedMapUtils = mockStatic(MapUtils.class)) {
            mockedMapUtils
                .when(() -> MapUtils.getString(callableResponseTask.getParameters(), WorkflowConstants.OUTPUT_SCHEMA))
                .thenReturn(null);

            OutputResponse result = subflowDataSource.getSubWorkflowOutputSchema(WORKFLOW_UUID);

            assertNull(result);
        }
    }

    @Test
    void testGetSubWorkflowsReturnsEmptyWhenNoCallableWorkflows() {
        when(projectWorkflowService.getLatestProjectWorkflows()).thenReturn(List.of());

        List<SubflowEntry> result = subflowDataSource.getSubWorkflows(PlatformType.AUTOMATION, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSubWorkflowsReturnsCallableWorkflows() {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(projectWorkflow.getProjectId()).thenReturn(1L);
        when(projectWorkflow.getUuidAsString()).thenReturn(WORKFLOW_UUID);

        when(projectWorkflowService.getLatestProjectWorkflows()).thenReturn(List.of(projectWorkflow));

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getLabel()).thenReturn("My Workflow");

        WorkflowTrigger callableTrigger = mock(WorkflowTrigger.class);

        when(callableTrigger.getType()).thenReturn("workflow/v1/newWorkflowCall");

        Project project = mock(Project.class);

        when(project.getName()).thenReturn("My Project");
        when(projectService.getProject(1L)).thenReturn(project);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of(callableTrigger));

            List<SubflowEntry> result = subflowDataSource.getSubWorkflows(PlatformType.AUTOMATION, null);

            assertEquals(1, result.size());
            assertEquals(WORKFLOW_UUID, result.getFirst()
                .workflowUuid());
            assertEquals("My Project > My Workflow", result.getFirst()
                .name());
        }
    }

    @Test
    void testGetSubWorkflowsFiltersNonCallableWorkflows() {
        ProjectWorkflow callableProjectWorkflow = mock(ProjectWorkflow.class);

        when(callableProjectWorkflow.getWorkflowId()).thenReturn("callable-workflow-id");
        when(callableProjectWorkflow.getProjectId()).thenReturn(1L);
        when(callableProjectWorkflow.getUuidAsString()).thenReturn("callable-uuid");

        ProjectWorkflow nonCallableProjectWorkflow = mock(ProjectWorkflow.class);

        when(nonCallableProjectWorkflow.getWorkflowId()).thenReturn("non-callable-workflow-id");

        when(projectWorkflowService.getLatestProjectWorkflows())
            .thenReturn(List.of(callableProjectWorkflow, nonCallableProjectWorkflow));

        Workflow callableWorkflow = mock(Workflow.class);

        when(callableWorkflow.getLabel()).thenReturn("Callable");

        Workflow nonCallableWorkflow = mock(Workflow.class);

        when(workflowService.getWorkflow("callable-workflow-id")).thenReturn(callableWorkflow);
        when(workflowService.getWorkflow("non-callable-workflow-id")).thenReturn(nonCallableWorkflow);

        WorkflowTrigger callableTrigger = mock(WorkflowTrigger.class);

        when(callableTrigger.getType()).thenReturn("workflow/v1/newWorkflowCall");

        WorkflowTrigger nonCallableTrigger = mock(WorkflowTrigger.class);

        when(nonCallableTrigger.getType()).thenReturn("github/v1/newIssue");

        Project project = mock(Project.class);

        when(project.getName()).thenReturn("Project");
        when(projectService.getProject(1L)).thenReturn(project);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(callableWorkflow))
                .thenReturn(List.of(callableTrigger));
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(nonCallableWorkflow))
                .thenReturn(List.of(nonCallableTrigger));

            List<SubflowEntry> result = subflowDataSource.getSubWorkflows(PlatformType.AUTOMATION, null);

            assertEquals(1, result.size());
            assertEquals("callable-uuid", result.getFirst()
                .workflowUuid());
        }
    }

    @Test
    void testGetSubWorkflowsFiltersBySearch() {
        ProjectWorkflow projectWorkflow1 = mock(ProjectWorkflow.class);

        when(projectWorkflow1.getWorkflowId()).thenReturn("wf1");
        when(projectWorkflow1.getProjectId()).thenReturn(1L);
        when(projectWorkflow1.getUuidAsString()).thenReturn("uuid1");

        ProjectWorkflow projectWorkflow2 = mock(ProjectWorkflow.class);

        when(projectWorkflow2.getWorkflowId()).thenReturn("wf2");
        when(projectWorkflow2.getProjectId()).thenReturn(2L);

        when(projectWorkflowService.getLatestProjectWorkflows())
            .thenReturn(List.of(projectWorkflow1, projectWorkflow2));

        Workflow workflow1 = mock(Workflow.class);

        when(workflow1.getLabel()).thenReturn("Invoice Processing");

        Workflow workflow2 = mock(Workflow.class);

        when(workflow2.getLabel()).thenReturn("Order Handling");

        when(workflowService.getWorkflow("wf1")).thenReturn(workflow1);
        when(workflowService.getWorkflow("wf2")).thenReturn(workflow2);

        WorkflowTrigger trigger1 = mock(WorkflowTrigger.class);

        when(trigger1.getType()).thenReturn("workflow/v1/newWorkflowCall");

        WorkflowTrigger trigger2 = mock(WorkflowTrigger.class);

        when(trigger2.getType()).thenReturn("workflow/v1/newWorkflowCall");

        Project project1 = mock(Project.class);

        when(project1.getName()).thenReturn("Finance");
        when(projectService.getProject(1L)).thenReturn(project1);

        Project project2 = mock(Project.class);

        when(project2.getName()).thenReturn("Sales");
        when(projectService.getProject(2L)).thenReturn(project2);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow1))
                .thenReturn(List.of(trigger1));
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow2))
                .thenReturn(List.of(trigger2));

            List<SubflowEntry> result = subflowDataSource.getSubWorkflows(PlatformType.AUTOMATION, "invoice");

            assertEquals(1, result.size());
            assertEquals("Finance > Invoice Processing", result.getFirst()
                .name());
        }
    }

    @Test
    void testGetSubWorkflowsHandlesNullWorkflowLabel() {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(projectWorkflow.getProjectId()).thenReturn(1L);
        when(projectWorkflow.getUuidAsString()).thenReturn(WORKFLOW_UUID);

        when(projectWorkflowService.getLatestProjectWorkflows()).thenReturn(List.of(projectWorkflow));

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getLabel()).thenReturn(null);

        WorkflowTrigger callableTrigger = mock(WorkflowTrigger.class);

        when(callableTrigger.getType()).thenReturn("workflow/v1/newWorkflowCall");

        Project project = mock(Project.class);

        when(project.getName()).thenReturn("My Project");
        when(projectService.getProject(1L)).thenReturn(project);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of(callableTrigger));

            List<SubflowEntry> result = subflowDataSource.getSubWorkflows(PlatformType.AUTOMATION, null);

            assertEquals(1, result.size());
            assertEquals("My Project > Unnamed Workflow", result.getFirst()
                .name());
        }
    }

    @Test
    void testGetSubWorkflowsReturnsAllWithEmptySearch() {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getWorkflowId()).thenReturn(WORKFLOW_ID);
        when(projectWorkflow.getProjectId()).thenReturn(1L);
        when(projectWorkflow.getUuidAsString()).thenReturn(WORKFLOW_UUID);

        when(projectWorkflowService.getLatestProjectWorkflows()).thenReturn(List.of(projectWorkflow));

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflow.getLabel()).thenReturn("Test Workflow");

        WorkflowTrigger callableTrigger = mock(WorkflowTrigger.class);

        when(callableTrigger.getType()).thenReturn("workflow/v1/newWorkflowCall");

        Project project = mock(Project.class);

        when(project.getName()).thenReturn("Project");
        when(projectService.getProject(1L)).thenReturn(project);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of(callableTrigger));

            List<SubflowEntry> result = subflowDataSource.getSubWorkflows(PlatformType.AUTOMATION, "");

            assertEquals(1, result.size());
            assertNotNull(result.getFirst());
        }
    }
}
