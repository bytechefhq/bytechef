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

package com.bytechef.automation.ai.a2a.server.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.ai.a2a.A2AAgentRequest;
import com.bytechef.platform.ai.a2a.A2AAgentResult;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class AutomationA2AServerFacadeTest {

    private final A2aProjectService a2aProjectService = mock(A2aProjectService.class);
    private final A2aProjectWorkflowService a2aProjectWorkflowService = mock(A2aProjectWorkflowService.class);
    private final A2aServerService a2aServerService = mock(A2aServerService.class);
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService =
        mock(ProjectDeploymentWorkflowService.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider =
        (ObjectProvider<PlanLimitsProvider>) mock(ObjectProvider.class);

    private final AutomationA2AServerFacade facade = new AutomationA2AServerFacade(
        a2aProjectService, a2aProjectWorkflowService, a2aServerService, mock(JobCompletionAwaiter.class),
        planLimitsProviderObjectProvider, mock(PrincipalJobFacade.class), projectDeploymentWorkflowService,
        mock(TaskExecutionService.class), mock(TaskFileStorage.class), mock(WorkflowService.class));

    @Test
    void testExecuteReturnsErrorWhenServerDisabled() {
        A2aServer a2aServer = new A2aServer();

        a2aServer.setEnabled(false);

        when(a2aServerService.getA2aServer("secret")).thenReturn(a2aServer);

        A2AAgentResult result = facade.execute(new A2AAgentRequest("secret", "hi", null, null));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("disabled");
    }

    @Test
    void testExecuteReturnsErrorWhenNoWorkflowExposed() {
        A2aServer a2aServer = new A2aServer();

        a2aServer.setEnabled(true);
        a2aServer.setId(1L);

        when(a2aServerService.getA2aServer("secret")).thenReturn(a2aServer);
        when(a2aProjectService.getA2aServerA2aProjects(1L)).thenReturn(List.of());

        A2AAgentResult result = facade.execute(new A2AAgentRequest("secret", "hi", null, null));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("No agent-backed workflow");
    }
}
