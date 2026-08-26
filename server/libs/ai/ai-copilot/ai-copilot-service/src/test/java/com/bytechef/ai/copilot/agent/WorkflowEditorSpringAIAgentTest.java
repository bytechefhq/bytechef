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

package com.bytechef.ai.copilot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
class WorkflowEditorSpringAIAgentTest {

    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade = mock(WorkflowNodeOutputFacade.class);
    private final PermissionService permissionService = mock(PermissionService.class);
    private final SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);

    @Test
    void testCreateSystemMessageDeniesInaccessibleWorkflow() throws Exception {
        runSupplierInline();

        when(permissionService.hasWorkflowScope("wf-1", "WORKFLOW_VIEW")).thenReturn(false);

        WorkflowEditorSpringAIAgent agent = newAgent();

        State state = newState("wf-1", 7L);

        assertThatThrownBy(() -> agent.createSystemMessage(state, new ArrayList<>()))
            .isInstanceOf(AccessDeniedException.class);

        // The unauthorized workflow must never be read into the system message.
        verify(workflowService, never()).getWorkflow(anyString());
    }

    @Test
    void testCreateSystemMessageAllowsAccessibleWorkflow() throws Exception {
        runSupplierInline();

        when(permissionService.hasWorkflowScope("wf-1", "WORKFLOW_VIEW")).thenReturn(true);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getDefinition()).thenReturn("{}");
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs("wf-1", null, 0)).thenReturn(List.of());

        WorkflowEditorSpringAIAgent agent = newAgent();

        State state = newState("wf-1", 7L);

        assertThatCode(() -> agent.createSystemMessage(state, new ArrayList<>()))
            .doesNotThrowAnyException();

        verify(workflowService).getWorkflow("wf-1");
    }

    @Test
    void testPreviousWorkflowNodeOutputsReadInsideSecurityContext() throws Exception {
        AtomicBoolean insideSecurityContext = new AtomicBoolean(false);

        when(securityContextRehydrator.withUserSecurityContext(eq(7L), any()))
            .thenAnswer(invocation -> {
                insideSecurityContext.set(true);

                try {
                    return ((Supplier<?>) invocation.getArgument(1)).get();
                } finally {
                    insideSecurityContext.set(false);
                }
            });

        when(permissionService.hasWorkflowScope("wf-1", "WORKFLOW_VIEW")).thenReturn(true);

        Workflow workflow = mock(Workflow.class);

        when(workflow.getDefinition()).thenReturn("{}");
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);

        // The @PreAuthorize-guarded node-output read runs on an agent worker thread that does not inherit the request
        // SecurityContext, so it must run inside the rehydrated context or it fails closed with AccessDenied. A plain
        // facade mock has no @PreAuthorize proxy, so this flag is what reproduces the production bug pattern: on the
        // pre-fix code the read ran after the rehydrated scope had already closed.
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs("wf-1", null, 0))
            .thenAnswer(invocation -> {
                assertThat(insideSecurityContext.get())
                    .withFailMessage("getPreviousWorkflowNodeOutputs must run inside the rehydrated SecurityContext")
                    .isTrue();

                return List.of();
            });

        WorkflowEditorSpringAIAgent agent = newAgent();

        State state = newState("wf-1", 7L);

        assertThatCode(() -> agent.createSystemMessage(state, new ArrayList<>()))
            .doesNotThrowAnyException();

        verify(workflowNodeOutputFacade).getPreviousWorkflowNodeOutputs("wf-1", null, 0);
    }

    @Test
    void testCreateSystemMessageSkipsNothingForEmbeddedRun() throws Exception {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getDefinition()).thenReturn("{}");
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);

        // Ticket 1051 Stage 4. This branch used to arm a resource-scoped skip mode around the delegated read,
        // because TenantContext did not reach the agent worker and ConnectedUserResourceMembershipResolver could not
        // recognise the connected user. CopilotSpringAIAgent.run now binds the tenant for the whole run, so the
        // resolver governs and the WORKFLOW_VIEW gate on getPreviousWorkflowNodeOutputs is answered from the
        // connected user's own membership. Probing from inside the read is what pins that nothing is armed.
        AtomicBoolean skipInside = new AtomicBoolean(true);

        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs("wf-1", null, 0))
            .thenAnswer(invocation -> {
                skipInside.set(AutomationAuthorizationContext.isSkipChecks());

                return List.of();
            });

        WorkflowEditorSpringAIAgent agent = newAgent();

        // Embedded run: no platform user id, but the full Authentication is carried in STATE_AUTHENTICATION. The
        // embedded request layer already resolved this workflow against the connected user's own
        // ConnectedUserProjectWorkflow row, so the agent's own hasWorkflowScope gate does not re-derive it.
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", "wf-1");
        stateMap.put(CopilotConstants.STATE_AUTHENTICATION, mock(Authentication.class));

        State state = new State(stateMap);

        assertThatCode(() -> agent.createSystemMessage(state, new ArrayList<>()))
            .doesNotThrowAnyException();

        verify(permissionService, never()).hasWorkflowScope(anyString(), anyString());
        verify(workflowService).getWorkflow("wf-1");

        assertThat(skipInside).isFalse();
    }

    @Test
    void testCreateSystemMessageDeniesWhenNoUserIdAndNoAuthentication() throws Exception {
        WorkflowEditorSpringAIAgent agent = newAgent();

        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", "wf-1");

        State state = new State(stateMap);

        assertThatThrownBy(() -> agent.createSystemMessage(state, new ArrayList<>()))
            .isInstanceOf(AccessDeniedException.class);

        verify(workflowService, never()).getWorkflow(anyString());
    }

    private void runSupplierInline() {
        when(securityContextRehydrator.withUserSecurityContext(eq(7L), any()))
            .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());
    }

    private WorkflowEditorSpringAIAgent newAgent() throws Exception {
        return WorkflowEditorSpringAIAgent.builder()
            .agentId("test")
            .chatModel(mock(ChatModel.class))
            .systemMessage("system")
            .state(new State(new HashMap<>()))
            .workflowService(workflowService)
            .workflowNodeOutputFacade(workflowNodeOutputFacade)
            .permissionService(permissionService)
            .securityContextRehydrator(securityContextRehydrator)
            .build();
    }

    private static State newState(String workflowId, long userId) {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", workflowId);
        stateMap.put(CopilotConstants.STATE_AUTHENTICATED_USER_ID, userId);

        return new State(stateMap);
    }
}
