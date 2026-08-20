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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;

/**
 * An executions query narrowed by an explicit project id gets by-id semantics: a project the caller may not see is
 * denied outright rather than answered with an empty page.
 *
 * @author Ivica Cardic
 */
class ProjectWorkflowExecutionFacadeVisibilityTest {

    private static final long PROJECT_ID = 7L;
    private static final long WORKSPACE_ID = 1L;

    private final PermissionService permissionService = mock(PermissionService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);

    private ProjectWorkflowExecutionFacadeImpl projectWorkflowExecutionFacade;

    @BeforeEach
    void setUp() {
        projectWorkflowExecutionFacade = new ProjectWorkflowExecutionFacadeImpl(
            mock(ComponentDefinitionService.class), mock(ContextService.class), mock(Evaluator.class),
            mock(EnvironmentService.class), mock(JobService.class), permissionService, mock(PrincipalJobService.class),
            mock(ProjectFacade.class), mock(ProjectDeploymentService.class), mock(ProjectService.class),
            projectWorkflowService, mock(TaskDispatcherDefinitionService.class), mock(TaskExecutionService.class),
            mock(TaskFileStorage.class), mock(TriggerExecutionService.class), mock(TriggerFileStorage.class),
            mock(WorkflowService.class));
    }

    @Test
    void testExecutionsOfAHiddenProjectAreDeniedRatherThanEmptied() {
        when(permissionService.hasResourceScope(PROJECT_ID, "Project", "EXECUTION_VIEW")).thenReturn(false);

        assertThatExceptionOfType(AccessDeniedException.class)
            .isThrownBy(
                () -> projectWorkflowExecutionFacade.getWorkflowExecutions(
                    null, null, null, null, null, PROJECT_ID, null, null, WORKSPACE_ID, 0))
            .withMessageContaining("Project id=7");
    }

    @Test
    void testExecutionsOfAVisibleProjectAreServed() {
        when(permissionService.hasResourceScope(PROJECT_ID, "Project", "EXECUTION_VIEW")).thenReturn(true);
        when(projectWorkflowService.getProjectWorkflowIds(PROJECT_ID)).thenReturn(List.of());

        Page<?> workflowExecutionPage = projectWorkflowExecutionFacade.getWorkflowExecutions(
            null, null, null, null, null, PROJECT_ID, null, null, WORKSPACE_ID, 0);

        assertThat(workflowExecutionPage).isEmpty();
    }
}
