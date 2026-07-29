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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ErrorWorkflowConfigurationValidatorTest {

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private WorkflowService workflowService;

    private ErrorWorkflowConfigurationValidator validator;

    @BeforeEach
    void beforeEach() {
        validator = new ErrorWorkflowConfigurationValidator(projectWorkflowService, workflowService);
    }

    @Test
    void testRejectsTargetInAnotherProject() {
        ProjectWorkflow target = new ProjectWorkflow(999L, 1, "wf-5");

        Mockito.when(projectWorkflowService.getProjectWorkflow(5L))
            .thenReturn(target);

        Assertions.assertThrows(
            IllegalArgumentException.class, () -> validator.validate(1L, 5L, null));
    }

    @Test
    void testRejectsTargetWithoutErrorTrigger() {
        stubTarget(5L, 1L, "wf-5");

        Workflow workflow = Mockito.mock(Workflow.class);

        Mockito.when(workflowService.getWorkflow("wf-5"))
            .thenReturn(workflow);

        WorkflowTrigger callTrigger = Mockito.mock(WorkflowTrigger.class);

        Mockito.when(callTrigger.getType())
            .thenReturn("workflow/v1/newWorkflowCall");

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = Mockito.mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of(callTrigger));

            Assertions.assertThrows(
                IllegalArgumentException.class, () -> validator.validate(1L, 5L, null));
        }
    }

    @Test
    void testRejectsSelfReference() {
        Assertions.assertThrows(
            IllegalArgumentException.class, () -> validator.validate(1L, 5L, 5L));
    }

    @Test
    void testAcceptsValidTarget() {
        stubTarget(5L, 1L, "wf-5");

        Workflow workflow = Mockito.mock(Workflow.class);

        Mockito.when(workflowService.getWorkflow("wf-5"))
            .thenReturn(workflow);

        WorkflowTrigger errorTrigger = Mockito.mock(WorkflowTrigger.class);

        Mockito.when(errorTrigger.getType())
            .thenReturn("workflow/v1/newWorkflowError");

        try (MockedStatic<WorkflowTrigger> mockedWorkflowTrigger = Mockito.mockStatic(WorkflowTrigger.class)) {
            mockedWorkflowTrigger.when(() -> WorkflowTrigger.of(workflow))
                .thenReturn(List.of(errorTrigger));

            Assertions.assertDoesNotThrow(() -> validator.validate(1L, 5L, 9L));
        }
    }

    private void stubTarget(long id, long projectId, String workflowId) {
        ProjectWorkflow target = new ProjectWorkflow(projectId, 1, workflowId);

        Mockito.when(projectWorkflowService.getProjectWorkflow(id))
            .thenReturn(target);
    }
}
