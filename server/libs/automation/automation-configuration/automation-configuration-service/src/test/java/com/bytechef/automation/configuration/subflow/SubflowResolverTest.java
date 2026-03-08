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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver.Subflow;
import java.util.List;
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
class SubflowResolverTest {

    private static final String DRAFT_WORKFLOW_ID = "draft-workflow-id-456";
    private static final String PUBLISHED_WORKFLOW_ID = "published-workflow-id-123";
    private static final String TRIGGER_NAME = "trigger_1";
    private static final String WORKFLOW_UUID = "test-workflow-uuid";

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    private SubflowResolverImpl subflowResolver;

    @BeforeEach
    void setUp() {
        subflowResolver = new SubflowResolverImpl(projectWorkflowService, workflowService);
    }

    @Test
    void testResolveSubflowEditorEnvironmentReturnsDraftWorkflow() {
        when(projectWorkflowService.getLastWorkflowId(WORKFLOW_UUID))
            .thenReturn(DRAFT_WORKFLOW_ID);

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(DRAFT_WORKFLOW_ID)).thenReturn(workflow);

        WorkflowTrigger callableTrigger = mock(WorkflowTrigger.class);

        when(callableTrigger.getType()).thenReturn("workflow/v1/newWorkflowCall");
        when(callableTrigger.getName()).thenReturn(TRIGGER_NAME);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of(callableTrigger));

            Subflow result = subflowResolver.resolveSubflow(WORKFLOW_UUID, WorkflowConstants.NEW_WORKFLOW_CALL, true);

            assertEquals(DRAFT_WORKFLOW_ID, result.workflowId());
            assertEquals(TRIGGER_NAME, result.inputsName());
        }

        verify(projectWorkflowService).getLastWorkflowId(WORKFLOW_UUID);
        verify(projectWorkflowService, never()).getLastPublishedWorkflowId(WORKFLOW_UUID);
    }

    @Test
    void testResolveSubflowProductionReturnsPublishedWorkflow() {
        when(projectWorkflowService.getLastPublishedWorkflowId(WORKFLOW_UUID))
            .thenReturn(PUBLISHED_WORKFLOW_ID);

        Workflow workflow = mock(Workflow.class);

        when(workflowService.getWorkflow(PUBLISHED_WORKFLOW_ID)).thenReturn(workflow);

        WorkflowTrigger callableTrigger = mock(WorkflowTrigger.class);

        when(callableTrigger.getType()).thenReturn("workflow/v1/newWorkflowCall");
        when(callableTrigger.getName()).thenReturn(TRIGGER_NAME);

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of(callableTrigger));

            Subflow result = subflowResolver.resolveSubflow(WORKFLOW_UUID, WorkflowConstants.NEW_WORKFLOW_CALL, false);

            assertEquals(PUBLISHED_WORKFLOW_ID, result.workflowId());
            assertEquals(TRIGGER_NAME, result.inputsName());
        }

        verify(projectWorkflowService).getLastPublishedWorkflowId(WORKFLOW_UUID);
        verify(projectWorkflowService, never()).getLastWorkflowId(WORKFLOW_UUID);
    }

    @Test
    void testResolveSubflowProductionThrowsWhenNotPublished() {
        when(projectWorkflowService.getLastPublishedWorkflowId(WORKFLOW_UUID))
            .thenThrow(new IllegalArgumentException("No published workflow found"));

        assertThrows(
            IllegalArgumentException.class,
            () -> subflowResolver.resolveSubflow(WORKFLOW_UUID, WorkflowConstants.NEW_WORKFLOW_CALL, false));

        verify(projectWorkflowService, never()).getLastWorkflowId(WORKFLOW_UUID);
    }
}
