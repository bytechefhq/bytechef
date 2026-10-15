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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.exception.ProjectErrorType;
import com.bytechef.automation.configuration.exception.ProjectWorkflowErrorType;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ExecutionException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ScopedProjectGuardTest {

    private static final long OWN_PROJECT_ID = 7L;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private PermissionService permissionService;

    @Mock
    private ScopedConnectionGuard scopedConnectionGuard;

    @Test
    void testCheckWorkflowAllowsAWorkflowOfTheScopedProject() {
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1"))
            .thenReturn(new ProjectWorkflow(OWN_PROJECT_ID, 1, "wf-1"));

        assertThatCode(
            () -> newGuard().checkWorkflow("wf-1", ScopedProjectGuard.WORKFLOW_VIEW, scopedToolContext(),
                ProjectWorkflowErrorType.GET_WORKFLOW))
                    .doesNotThrowAnyException();
    }

    @Test
    void testCheckWorkflowRejectsAWorkflowOfAnotherProject() {
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1"))
            .thenReturn(new ProjectWorkflow(9L, 1, "wf-1"));

        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkWorkflow(
                "wf-1", ScopedProjectGuard.WORKFLOW_VIEW, scopedToolContext(), ProjectWorkflowErrorType.GET_WORKFLOW))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage("Workflow wf-1 was not found in this chat's project. Check the workflow id.");
    }

    @Test
    void testCheckWorkflowRejectsAnUnknownWorkflow() {
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1"))
            .thenThrow(new IllegalArgumentException("not found"));

        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkWorkflow(
                "wf-1", ScopedProjectGuard.WORKFLOW_VIEW, scopedToolContext(), ProjectWorkflowErrorType.GET_WORKFLOW))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("was not found in this chat's project");
    }

    @Test
    void testCheckWorkflowRejectsAWorkflowWhoseLookupFails() {
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-1"))
            .thenThrow(new IllegalStateException("database unavailable"));

        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkWorkflow(
                "wf-1", ScopedProjectGuard.WORKFLOW_VIEW, scopedToolContext(), ProjectWorkflowErrorType.GET_WORKFLOW))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(
                        "This chat could not verify that workflow wf-1 belongs to its project. Try again later.")
                    .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void testCheckWorkflowFailsClosedWithoutAProjectWorkflowLookup() {
        ScopedProjectGuard scopedProjectGuard = new ScopedProjectGuard(
            permissionService, (ProjectWorkflowService) null, scopedConnectionGuard);

        assertThatCode(
            () -> scopedProjectGuard.checkWorkflow(
                "wf-1", ScopedProjectGuard.WORKFLOW_VIEW, scopedToolContext(), ProjectWorkflowErrorType.GET_WORKFLOW))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("could not verify that workflow wf-1 belongs to its project");
    }

    @Test
    void testCheckWorkflowAllowsAnUnconfinedRunWhoseCallerHoldsTheScope() {
        when(permissionService.hasWorkflowScope("wf-1", ScopedProjectGuard.WORKFLOW_EDIT)).thenReturn(true);

        assertThatCode(
            () -> newGuard().checkWorkflow(
                "wf-1", ScopedProjectGuard.WORKFLOW_EDIT, new ToolContext(Map.of()),
                ProjectWorkflowErrorType.UPDATE_WORKFLOW))
                    .doesNotThrowAnyException();

        verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
    }

    @Test
    void testCheckWorkflowRejectsAnUnconfinedRunWhoseCallerLacksTheScope() {
        when(permissionService.hasWorkflowScope("wf-1", ScopedProjectGuard.WORKFLOW_EDIT)).thenReturn(false);

        assertThatCode(
            () -> newGuard().checkWorkflow(
                "wf-1", ScopedProjectGuard.WORKFLOW_EDIT, new ToolContext(Map.of()),
                ProjectWorkflowErrorType.UPDATE_WORKFLOW))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage("Workflow wf-1 was not found or you do not have access to it.");
    }

    @Test
    void testCheckProjectAllowsAnUnconfinedRunWhoseCallerHoldsTheScope() {
        when(permissionService.hasWorkspaceScopeForProject(9L, ScopedProjectGuard.WORKFLOW_VIEW)).thenReturn(true);

        assertThatCode(
            () -> newGuard().checkProject(
                9L, ScopedProjectGuard.WORKFLOW_VIEW, new ToolContext(Map.of()), ProjectErrorType.GET_PROJECT))
                    .doesNotThrowAnyException();
    }

    @Test
    void testCheckProjectRejectsAnUnconfinedRunWhoseCallerLacksTheScope() {
        when(permissionService.hasWorkspaceScopeForProject(9L, ScopedProjectGuard.WORKFLOW_VIEW)).thenReturn(false);

        assertThatCode(
            () -> newGuard().checkProject(
                9L, ScopedProjectGuard.WORKFLOW_VIEW, new ToolContext(Map.of()), ProjectErrorType.GET_PROJECT))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage("Project 9 was not found or you do not have access to it.");
    }

    @Test
    void testCheckProjectRejectsAnotherProject() {
        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkProject(9L, ScopedProjectGuard.WORKFLOW_VIEW, scopedToolContext(),
                ProjectErrorType.GET_PROJECT))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
        assertThatCode(
            () -> scopedProjectGuard.checkProject(OWN_PROJECT_ID, ScopedProjectGuard.WORKFLOW_VIEW, scopedToolContext(),
                ProjectErrorType.GET_PROJECT))
                    .doesNotThrowAnyException();
    }

    @Test
    void testCheckProjectChangeAllowedRejectsAConfinedRun() {
        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkProjectChangeAllowed(scopedToolContext(), ProjectErrorType.CREATE_PROJECT))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.PROJECT_CHANGE_MESSAGE);
    }

    @Test
    void testCheckProjectChangeAllowedAllowsAnUnconfinedRun() {
        assertThatCode(
            () -> newGuard().checkProjectChangeAllowed(new ToolContext(Map.of()), ProjectErrorType.CREATE_PROJECT))
                .doesNotThrowAnyException();
    }

    @Test
    void testCheckConnectionRejectsAConnectionTheUserIsNotEntitledTo() {
        ToolContext toolContext = scopedToolContext();

        when(scopedConnectionGuard.fetchDenialMessage(null, 900L, toolContext))
            .thenReturn(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);

        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkConnection(900L, toolContext, ProjectWorkflowErrorType.SAVE_TEST_CONNECTION))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);
    }

    @Test
    void testCheckConnectionReportsAConnectionItCouldNotVerifyAsUnverified() {
        ToolContext toolContext = scopedToolContext();

        when(scopedConnectionGuard.fetchDenialMessage(null, 900L, toolContext))
            .thenReturn(ScopedConnectionGuard.UNVERIFIED_MESSAGE);

        ScopedProjectGuard scopedProjectGuard = newGuard();

        assertThatCode(
            () -> scopedProjectGuard.checkConnection(900L, toolContext, ProjectWorkflowErrorType.SAVE_TEST_CONNECTION))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedConnectionGuard.UNVERIFIED_MESSAGE);
    }

    @Test
    void testCheckConnectionAllowsAConnectionTheUserIsEntitledTo() {
        ToolContext toolContext = scopedToolContext();

        assertThatCode(
            () -> newGuard().checkConnection(42L, toolContext, ProjectWorkflowErrorType.SAVE_TEST_CONNECTION))
                .doesNotThrowAnyException();

        verify(scopedConnectionGuard).fetchDenialMessage(null, 42L, toolContext);
    }

    private ScopedProjectGuard newGuard() {
        return new ScopedProjectGuard(permissionService, projectWorkflowService, scopedConnectionGuard);
    }

    private static ToolContext scopedToolContext() {
        return new ToolContext(Map.of(ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, OWN_PROJECT_ID));
    }
}
