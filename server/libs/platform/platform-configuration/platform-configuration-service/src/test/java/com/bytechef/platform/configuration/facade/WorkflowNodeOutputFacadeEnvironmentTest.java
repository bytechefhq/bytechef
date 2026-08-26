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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.Workflow.Format;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.security.web.authentication.AbstractApiKeyAuthenticationToken;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
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
 * hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW') on checkWorkflowCache is environment-agnostic, so the
 * caller-supplied environmentId is never checked by the gate. Pins the execution side: for a confined (api-key)
 * principal, the environment the cache is evicted for must be the principal's own, not the request argument.
 * (getClusterElementOutput/getWorkflowNodeOutput share the identical resolve-at-entry mechanism; the interesting,
 * separately-tested case is getPreviousWorkflowNodeOutputs, which is @Cacheable and therefore deliberately does NOT
 * resolve internally -- see WorkflowNodeOutputApiControllerTest.)
 *
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class WorkflowNodeOutputFacadeEnvironmentTest {

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

    @Mock
    private ActionDefinitionFacade actionDefinitionFacade;

    @Mock
    private ActionDefinitionService actionDefinitionService;

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
    private TriggerDefinitionService triggerDefinitionService;

    @Mock
    private WorkflowCacheManager workflowCacheManager;

    @Mock
    private WorkflowEvaluationInputsFacade workflowEvaluationInputsFacade;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @Mock
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    private WorkflowNodeOutputFacadeImpl workflowNodeOutputFacade;

    @BeforeEach
    void setUp() {
        workflowNodeOutputFacade = new WorkflowNodeOutputFacadeImpl(
            actionDefinitionFacade, actionDefinitionService, clusterElementDefinitionFacade,
            clusterElementDefinitionService, evaluator, taskDispatcherDefinitionService, triggerDefinitionFacade,
            triggerDefinitionService, workflowCacheManager, workflowEvaluationInputsFacade, workflowService,
            workflowNodeTestOutputService, workflowTestConfigurationService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCheckWorkflowCacheUsesConfinedPrincipalEnvironmentAtExecution() {
        authenticate(new TestApiKeyAuthenticationToken(PRODUCTION_ORDINAL, user()));

        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_WITH_TRIGGER);
        when(triggerDefinitionService.isDynamicOutputDefined("manual", 1, "manual")).thenReturn(true);

        workflowNodeOutputFacade.checkWorkflowCache("workflow-1", "trigger-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowCacheManager, atLeastOnce()).clearCacheForWorkflow(
            eq("workflow-1"), anyString(), environmentIdCaptor.capture());

        assertEquals(PRODUCTION_ORDINAL, environmentIdCaptor.getValue());
    }

    @Test
    void testCheckWorkflowCacheHonoursSessionPrincipalRequestedEnvironment() {
        authenticate(new UsernamePasswordAuthenticationToken("admin@localhost.com", "n/a", List.of()));

        when(workflowService.getWorkflow("workflow-1")).thenReturn(WORKFLOW_WITH_TRIGGER);
        when(triggerDefinitionService.isDynamicOutputDefined("manual", 1, "manual")).thenReturn(true);

        workflowNodeOutputFacade.checkWorkflowCache("workflow-1", "trigger-1", DEVELOPMENT_ORDINAL);

        ArgumentCaptor<Long> environmentIdCaptor = ArgumentCaptor.forClass(Long.class);

        verify(workflowCacheManager, atLeastOnce()).clearCacheForWorkflow(
            eq("workflow-1"), anyString(), environmentIdCaptor.capture());

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
