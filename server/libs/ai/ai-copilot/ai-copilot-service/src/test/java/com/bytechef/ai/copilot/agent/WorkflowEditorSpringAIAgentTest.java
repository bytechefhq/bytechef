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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.agui.core.state.State;
import com.bytechef.ai.copilot.constant.CopilotConstants;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
class WorkflowEditorSpringAIAgentTest {

    private static final String OTHER_USERS_WORKFLOW_ID = "workflow-of-bob";
    private static final String OWN_WORKFLOW_ID = "workflow-of-alice";

    private final PermissionService permissionService = mock(PermissionService.class);
    private final SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade = mock(WorkflowNodeOutputFacade.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testConnectedUsersRunOnAnotherUsersWorkflowIsDenied() throws Exception {
        Authentication alice = TestConnectedUserAuthentication.of("alice");
        AtomicReference<Authentication> checkedAs = new AtomicReference<>();

        when(permissionService.hasWorkflowScope(OTHER_USERS_WORKFLOW_ID, "WORKFLOW_VIEW")).thenAnswer(invocation -> {
            checkedAs.set(getCurrentAuthentication());

            return false;
        });

        WorkflowEditorSpringAIAgent agent = newAgent();

        State state = newState(OTHER_USERS_WORKFLOW_ID, alice);

        assertThatThrownBy(() -> agent.createSystemMessage(state, new ArrayList<>()))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(checkedAs.get()).isSameAs(alice);

        verify(workflowService, never()).getWorkflow(anyString());
    }

    @Test
    void testConnectedUsersRunOnOwnWorkflowReadsOutputsAsTheUserWithoutSkippingChecks() throws Exception {
        Authentication alice = TestConnectedUserAuthentication.of("alice");
        AtomicReference<Authentication> readAs = new AtomicReference<>();
        AtomicBoolean skipChecksDuringRead = new AtomicBoolean(true);
        Workflow workflow = mock(Workflow.class);

        when(permissionService.hasWorkflowScope(OWN_WORKFLOW_ID, "WORKFLOW_VIEW")).thenReturn(true);
        when(workflow.getDefinition()).thenReturn("{}");
        when(workflowService.getWorkflow(OWN_WORKFLOW_ID)).thenReturn(workflow);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(OWN_WORKFLOW_ID, null, 0))
            .thenAnswer(invocation -> {
                readAs.set(getCurrentAuthentication());
                skipChecksDuringRead.set(AutomationAuthorizationContext.isSkipChecks());

                return List.of();
            });

        WorkflowEditorSpringAIAgent agent = newAgent();

        agent.createSystemMessage(newState(OWN_WORKFLOW_ID, alice), new ArrayList<>());

        assertThat(readAs.get()).isSameAs(alice);
        assertThat(skipChecksDuringRead.get()).isFalse();
    }

    @Test
    void testCarriedAuthenticationWinsOverAClientSuppliedUserId() throws Exception {
        Authentication alice = TestConnectedUserAuthentication.of("alice");
        AtomicReference<Authentication> checkedAs = new AtomicReference<>();
        AtomicReference<Authentication> readAs = new AtomicReference<>();
        Workflow workflow = mock(Workflow.class);

        when(permissionService.hasWorkflowScope(OWN_WORKFLOW_ID, "WORKFLOW_VIEW")).thenAnswer(invocation -> {
            checkedAs.set(getCurrentAuthentication());

            return true;
        });
        when(workflow.getDefinition()).thenReturn("{}");
        when(workflowService.getWorkflow(OWN_WORKFLOW_ID)).thenReturn(workflow);
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeOutputs(OWN_WORKFLOW_ID, null, 0))
            .thenAnswer(invocation -> {
                readAs.set(getCurrentAuthentication());

                return List.of();
            });

        State state = newState(OWN_WORKFLOW_ID, alice);

        state.set(CopilotConstants.STATE_AUTHENTICATED_USER_ID, 1L);

        newAgent().createSystemMessage(state, new ArrayList<>());

        assertThat(checkedAs.get()).isSameAs(alice);
        assertThat(readAs.get()).isSameAs(alice);

        verifyNoInteractions(securityContextRehydrator);
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

    private static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext()
            .getAuthentication();
    }

    private static State newState(String workflowId, Authentication authentication) {
        Map<String, Object> stateMap = new HashMap<>();

        stateMap.put("workflowId", workflowId);
        stateMap.put(CopilotConstants.STATE_AUTHENTICATION, authentication);

        return new State(stateMap);
    }
}
