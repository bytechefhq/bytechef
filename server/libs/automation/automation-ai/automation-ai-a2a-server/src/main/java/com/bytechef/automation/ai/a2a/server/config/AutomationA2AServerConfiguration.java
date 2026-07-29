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

package com.bytechef.automation.ai.a2a.server.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.a2a.server.facade.AutomationA2AServerFacade;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.platform.ai.a2a.A2AAgentCardFactory;
import com.bytechef.platform.ai.a2a.A2AProtocolHandler;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the A2A (Agent2Agent) server surface: the card factory, the workflow-execution bridge facade, and the JSON-RPC
 * protocol handler that dispatches {@code message/send} onto the facade.
 *
 * @author Ivica Cardic
 */
@Configuration
public class AutomationA2AServerConfiguration {

    @Bean
    A2AAgentCardFactory a2aAgentCardFactory() {
        return new A2AAgentCardFactory();
    }

    @Bean
    AutomationA2AServerFacade automationA2AServerFacade(
        A2aProjectService a2aProjectService, A2aProjectWorkflowService a2aProjectWorkflowService,
        A2aServerService a2aServerService, JobCompletionAwaiter jobCompletionAwaiter,
        PrincipalJobFacade principalJobFacade, ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        TaskExecutionService taskExecutionService, TaskFileStorage durableTaskFileStorage,
        WorkflowService workflowService) {

        return new AutomationA2AServerFacade(
            a2aProjectService, a2aProjectWorkflowService, a2aServerService, jobCompletionAwaiter, principalJobFacade,
            projectDeploymentWorkflowService, taskExecutionService, durableTaskFileStorage, workflowService);
    }

    @Bean
    A2AProtocolHandler a2aProtocolHandler(AutomationA2AServerFacade automationA2AServerFacade) {
        return new A2AProtocolHandler(automationA2AServerFacade);
    }
}
