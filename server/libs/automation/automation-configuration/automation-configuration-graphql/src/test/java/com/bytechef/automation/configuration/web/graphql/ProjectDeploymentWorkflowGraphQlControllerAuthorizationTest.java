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

package com.bytechef.automation.configuration.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade.ChatWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Pins that both of this controller's root queries reach their rows through the facade layer, which is where this
 * codebase puts authorization.
 *
 * <p>
 * The controller carries no {@code @PreAuthorize} of its own and is not meant to. That convention has one failure mode,
 * and both queries were instances of it: each was assembled here, out of services directly, past the facade. The
 * listing shipped with neither a membership gate nor a visibility filter and named the project and labelled the
 * workflow of every hosted-chat deployment in any workspace; the by-id read shipped with no gate at all and returned
 * the whole workflow definition of any deployment in the tenant. Asserting that the facade was called is not enough on
 * its own; the assertion that none of the services this controller still holds are touched is what makes a revert to
 * locally assembled rows fail here rather than pass.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentWorkflowGraphQlControllerAuthorizationTest {

    private static final long ENVIRONMENT_ID = 2L;
    private static final long WORKSPACE_ID = 1L;

    private final ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);
    private final ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
    private final TriggerDefinitionService triggerDefinitionService = mock(TriggerDefinitionService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final ProjectDeploymentWorkflowGraphQlController projectDeploymentWorkflowGraphQlController =
        new ProjectDeploymentWorkflowGraphQlController(
            projectDeploymentFacade, projectDeploymentWorkflowService, projectWorkflowService,
            triggerDefinitionService, "http://localhost/webhooks/{id}", workflowService);

    /**
     * The by-id read had the same shape of hole as the listing below, and worse consequences: the row was resolved here
     * out of {@code ProjectWorkflowService} and {@code ProjectDeploymentWorkflowService}, so a root query keyed by the
     * string that is also the path segment of the workflow's public static webhook URL returned any deployment's inputs
     * and connection bindings and, through {@code projectWorkflow.workflow}, its whole workflow definition.
     *
     * <p>
     * The two assertions do different jobs and neither substitutes for the other. That the facade was called says the
     * gate is now on the path; that <em>none</em> of the services this controller still holds were touched is what
     * makes a revert to a locally resolved row fail here instead of passing — the returned object would be the same
     * either way, so an assertion on the result alone would not tell the two paths apart.
     */
    @Test
    void testProjectDeploymentWorkflowReadsThroughTheGuardedFacade() {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 10L, "workflow-uuid", "trigger-1");

        when(projectDeploymentFacade.getProjectDeploymentWorkflow(any(WorkflowExecutionId.class)))
            .thenReturn(projectDeploymentWorkflow);

        assertThat(
            projectDeploymentWorkflowGraphQlController.projectDeploymentWorkflow(workflowExecutionId.toString()))
                .isSameAs(projectDeploymentWorkflow);

        ArgumentCaptor<WorkflowExecutionId> captor = ArgumentCaptor.forClass(WorkflowExecutionId.class);

        verify(projectDeploymentFacade).getProjectDeploymentWorkflow(captor.capture());

        WorkflowExecutionId capturedWorkflowExecutionId = captor.getValue();

        assertThat(capturedWorkflowExecutionId.getJobPrincipalId()).isEqualTo(10L);
        assertThat(capturedWorkflowExecutionId.getWorkflowUuid()).isEqualTo("workflow-uuid");

        verifyNoInteractions(
            projectDeploymentWorkflowService, projectWorkflowService, triggerDefinitionService, workflowService);
    }

    @Test
    void testWorkspaceChatWorkflowsReadsThroughTheGuardedFacade() {
        ChatWorkflow chatWorkflow = new ChatWorkflow(10L, 1L, "Shared", 1000L, "execution-id", "wf-1", "Shared chat");

        when(projectDeploymentFacade.getWorkspaceChatWorkflows(WORKSPACE_ID, ENVIRONMENT_ID))
            .thenReturn(List.of(chatWorkflow));

        List<ChatWorkflow> chatWorkflows =
            projectDeploymentWorkflowGraphQlController.workspaceChatWorkflows(WORKSPACE_ID, ENVIRONMENT_ID);

        assertThat(chatWorkflows).containsExactly(chatWorkflow);

        verify(projectDeploymentFacade).getWorkspaceChatWorkflows(WORKSPACE_ID, ENVIRONMENT_ID);

        verifyNoInteractions(
            projectDeploymentWorkflowService, projectWorkflowService, triggerDefinitionService, workflowService);
    }

    /**
     * The primitive {@code long} arguments are load-bearing, and nothing else pins them.
     *
     * <p>
     * The facade this delegates to gates on {@code hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')}, and
     * {@code #workspaceId} is only a usable gate key while the parameter cannot be null: a boxed {@code null} reaches
     * {@code AutomationPermissionEvaluator} as a null target id. The GraphQL schema declares {@code ID!} today, so null
     * cannot arrive — but that is a second file's promise, and widening these back to {@code Long} would fail as an
     * unboxing NPE at runtime rather than at compile time.
     *
     * <p>
     * {@code VisibilityBearingSurfaceAuditTest} cannot cover this: it keys on methods whose signatures mention a
     * visibility-bearing type, and {@code workspaceChatWorkflows} returns a purpose-built {@code ChatWorkflow}
     * projection. That is precisely why the pin has to live here — the surface is outside the audit's reach, which is
     * the same blind spot that let an ungated root query survive on this controller.
     */
    @Test
    void testWorkspaceChatWorkflowsKeysOnPrimitiveArguments() {
        Method method = findMethod("workspaceChatWorkflows", long.class, long.class);

        assertThat(method.getParameterTypes()).containsExactly(long.class, long.class);
    }

    private static Method findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return ProjectDeploymentWorkflowGraphQlController.class.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found with primitive arguments", exception);
        }
    }
}
