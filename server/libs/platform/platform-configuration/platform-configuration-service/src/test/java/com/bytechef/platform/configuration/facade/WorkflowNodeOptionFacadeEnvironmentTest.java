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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * The {@code @PreAuthorize} gate both methods now carry is environment-agnostic, so the environment reaching
 * getEvaluationInputs, the {@code @Cacheable} getPreviousWorkflowNodeSampleOutputs, and the test-configuration
 * connection lookups must still be resolved to the caller's own; executeOptions ultimately makes a live outbound call
 * using whatever connectionId those lookups resolve. This test pins that execution side; the gate itself is pinned by
 * {@link WorkflowNodeOptionFacadeAuthorizationTest}. The facade is constructed directly here, so no proxy intercepts
 * and the gate does not run.
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class WorkflowNodeOptionFacadeEnvironmentTest {

    private static final long DEVELOPMENT_ORDINAL = 0L;
    private static final long PRODUCTION_ORDINAL = 2L;

    private static final Workflow WORKFLOW_WITH_TRIGGER = new Workflow(
        "workflow-1",
        """
            {
                "triggers": [
                    {
                        "label": "Manual",
                        "name": "trigger-1",
                        "type": "manual/v1/manual"
                    }
                ]
            }
            """,
        Format.JSON);

    private static final Workflow WORKFLOW_WITH_CLUSTER_ELEMENT = new Workflow(
        "workflow-1",
        """
            {
                "tasks": [
                    {
                        "name": "node-1",
                        "type": "aiAgent/v1/chat",
                        "parameters": {},
                        "clusterElements": {
                            "model": {
                                "name": "openAi_1",
                                "type": "openAi/v1/model",
                                "parameters": {}
                            }
                        }
                    }
                ]
            }
            """,
        Format.JSON);

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    @Mock
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @Mock
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @Mock
    private Evaluator evaluator;

    @Mock
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @Mock
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @Mock
    private WorkflowEvaluationInputsFacade workflowEvaluationInputsFacade;

    @Mock
    private WorkflowNodeOutputFacade workflowNodeOutputFacade;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private WorkflowNodeOptionFacadeImpl workflowNodeOptionFacade;

    @BeforeEach
    void setUp() {
        workflowNodeOptionFacade = new WorkflowNodeOptionFacadeImpl(
            evaluator, actionDefinitionFacade, clusterElementDefinitionFacade, clusterElementDefinitionService,
            taskDispatcherDefinitionService, triggerDefinitionFacade, workflowService,
            workflowEvaluationInputsFacade, workflowNodeOutputFacade, workflowTestConfigurationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetWorkflowNodeOptionsUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        stubWorkflowAndConnectionLookup();

        workflowNodeOptionFacade.getWorkflowNodeOptions(
            "workflow-1", "trigger-1", "property", List.of(), "search", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testGetWorkflowNodeOptionsHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        stubWorkflowAndConnectionLookup();

        workflowNodeOptionFacade.getWorkflowNodeOptions(
            "workflow-1", "trigger-1", "property", List.of(), "search", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testGetClusterElementNodeOptionsUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        stubClusterElementLookup();

        workflowNodeOptionFacade.getClusterElementNodeOptions(
            "workflow-1", "node-1", "model", "openAi_1", "property", List.of(), "search", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testGetClusterElementNodeOptionsHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        stubClusterElementLookup();

        workflowNodeOptionFacade.getClusterElementNodeOptions(
            "workflow-1", "node-1", "model", "openAi_1", "property", List.of(), "search", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowEvaluationInputsFacade).getEvaluationInputs(eq("workflow-1"), environmentIdCaptor.capture());

        assertEquals(DEVELOPMENT_ORDINAL, environmentIdCaptor.getValue());
    }

    private void stubClusterElementLookup() {
        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_WITH_CLUSTER_ELEMENT);
        when(workflowEvaluationInputsFacade.getEvaluationInputs(anyString(), anyLong())).thenReturn(Map.of());
        when(workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(anyString(), anyString(), anyLong()))
            .thenReturn(Map.of());
        when(workflowTestConfigurationService.fetchWorkflowTestConfiguration(anyString(), anyLong()))
            .thenReturn(Optional.empty());
        when(clusterElementDefinitionService.getClusterElementType(anyString(), anyInt(), anyString()))
            .thenReturn(new ClusterElementType("model", "model", "Model"));
        when(evaluator.evaluate(anyMap(), anyMap())).thenReturn(Map.of());
        when(clusterElementDefinitionFacade.executeOptions(
            anyString(), anyInt(), anyString(), anyString(), anyMap(), anyMap(), anyList(), anyString(), any(),
            anyMap(), anyMap())).thenReturn(List.of());
    }

    private void stubWorkflowAndConnectionLookup() {
        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_WITH_TRIGGER);
        when(workflowEvaluationInputsFacade.getEvaluationInputs(anyString(), anyLong())).thenReturn(Map.of());
        when(workflowTestConfigurationService.fetchWorkflowTestConfigurationConnectionId(
            anyString(), anyString(), anyLong())).thenReturn(Optional.empty());
        when(evaluator.evaluate(anyMap(), anyMap(), anyBoolean()))
            .thenReturn(Map.of("name", "trigger-1", "type", "manual/v1/manual"));
        when(triggerDefinitionFacade.executeOptions(
            anyString(), anyInt(), anyString(), anyString(), anyMap(), anyList(), anyString(), any()))
                .thenReturn(List.of());
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
