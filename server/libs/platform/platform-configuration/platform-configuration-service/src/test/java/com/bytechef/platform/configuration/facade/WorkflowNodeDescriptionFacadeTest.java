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

package com.bytechef.platform.configuration.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW') on both methods is environment-agnostic, so the
 * caller-supplied environmentId is never checked by the gate. These tests pin the execution side: for a confined
 * (api-key) principal, the environment reaching getEvaluationInputs -- the single place `vars` is merged for editor
 * previews -- must be the principal's own, not the request argument.
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class WorkflowNodeDescriptionFacadeTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private static final Workflow WORKFLOW_WITH_TASK = new Workflow(
        "workflow-1",
        """
            {
                "tasks": [
                    {
                        "name": "node-1",
                        "type": "javascript/v1",
                        "parameters": {}
                    }
                ]
            }
            """,
        Format.JSON);

    @Mock
    private ActionDefinitionService actionDefinitionService;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private Evaluator evaluator;

    @Mock
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Mock
    private TriggerDefinitionService triggerDefinitionService;

    @Mock
    private WorkflowEvaluationInputsFacade workflowEvaluationInputsFacade;

    @Mock
    private WorkflowService workflowService;

    private WorkflowNodeDescriptionFacadeImpl workflowNodeDescriptionFacade;

    @BeforeEach
    void setUp() {
        workflowNodeDescriptionFacade = new WorkflowNodeDescriptionFacadeImpl(
            actionDefinitionService, clusterElementDefinitionService, evaluator, taskDispatcherDefinitionService,
            triggerDefinitionService, workflowEvaluationInputsFacade, workflowService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetWorkflowNodeDescriptionUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_WITH_TASK);
        when(workflowEvaluationInputsFacade.getEvaluationInputs(anyString(), anyLong()))
            .thenReturn(Map.of());
        when(triggerDefinitionService.executeWorkflowNodeDescription("manual", 1, "manual", Map.of()))
            .thenReturn("description");

        workflowNodeDescriptionFacade.getWorkflowNodeDescription("workflow-1", "manual", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testGetWorkflowNodeDescriptionHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_WITH_TASK);
        when(workflowEvaluationInputsFacade.getEvaluationInputs(anyString(), anyLong()))
            .thenReturn(Map.of());
        when(triggerDefinitionService.executeWorkflowNodeDescription("manual", 1, "manual", Map.of()))
            .thenReturn("description");

        workflowNodeDescriptionFacade.getWorkflowNodeDescription("workflow-1", "manual", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    /**
     * getClusterElementWorkflowNodeDescription's environmentId is a boxed {@code Long} (nullable in the method
     * signature, unlike its sibling above), so this also exercises the required-parameter guard rather than the
     * primitive overload.
     */
    @Test
    void testGetClusterElementWorkflowNodeDescriptionUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_WITH_TASK);
        when(workflowEvaluationInputsFacade.getEvaluationInputs(anyString(), anyLong()))
            .thenReturn(Map.of());

        workflowNodeDescriptionFacade.getClusterElementWorkflowNodeDescription(
            "workflow-1", "node-1", "clusterElement-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testGetClusterElementWorkflowNodeDescriptionHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_WITH_TASK);
        when(workflowEvaluationInputsFacade.getEvaluationInputs(anyString(), anyLong()))
            .thenReturn(Map.of());

        workflowNodeDescriptionFacade.getClusterElementWorkflowNodeDescription(
            "workflow-1", "node-1", "clusterElement-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static User user() {
        return new User("connected-user-1", "", List.of());
    }

    private static final class TestApiKeyAuthenticationToken extends AbstractApiKeyAuthenticationToken {

        private TestApiKeyAuthenticationToken(long environmentId, User user) {
            super(environmentId, user);
        }
    }
}
