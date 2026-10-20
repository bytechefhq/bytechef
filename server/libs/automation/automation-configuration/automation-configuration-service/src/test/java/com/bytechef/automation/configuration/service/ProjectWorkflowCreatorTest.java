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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class ProjectWorkflowCreatorTest {

    private static final String DEFINITION = "{\"tasks\":[]}";

    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkflowValidatorFacade workflowValidatorFacade = mock(WorkflowValidatorFacade.class);
    private final ProjectWorkflowCreator projectWorkflowCreator = new ProjectWorkflowCreatorImpl(
        projectService, projectWorkflowService, workflowService, workflowValidatorFacade);

    @Test
    void testAddWorkflowCreatesTheWorkflowInTheLastProjectVersion() {
        Project project = mock(Project.class);
        Workflow workflow = mock(Workflow.class);
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(project.getLastProjectVersion()).thenReturn(3);
        when(projectService.getProject(7L)).thenReturn(project);
        when(workflow.getId()).thenReturn("workflow-1");
        when(workflowService.create(DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC))
            .thenReturn(workflow);
        when(projectWorkflowService.addWorkflow(7L, 3, "workflow-1")).thenReturn(projectWorkflow);

        assertThat(projectWorkflowCreator.addWorkflow(7L, DEFINITION)).isSameAs(projectWorkflow);

        verify(workflowValidatorFacade).validateNoDuplicateNodeNames(DEFINITION);
    }

    @Test
    void testAddWorkflowCreatesNothingWhenTheDefinitionIsInvalid() {
        Mockito.doThrow(new IllegalArgumentException("duplicate"))
            .when(workflowValidatorFacade)
            .validateNoDuplicateNodeNames(DEFINITION);

        assertThatThrownBy(() -> projectWorkflowCreator.addWorkflow(7L, DEFINITION))
            .isInstanceOf(IllegalArgumentException.class);

        verify(workflowService, never()).create(anyString(), Mockito.any(), Mockito.any());
        verify(projectWorkflowService, never()).addWorkflow(anyLong(), anyInt(), anyString());
    }
}
