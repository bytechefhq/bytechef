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

import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builds the project-deployment tool-callback lists shared by the Copilot deployment panel agents
 * ({@code deployment_ask}/{@code deployment_build} in {@code DeploymentAgentConfiguration},
 * {@code project_deployment_ask}/{@code project_deployment_build} in {@code ProjectDeploymentAgentConfiguration}), the
 * AI Hub ASK/BUILD agents, and the management MCP server. Read list feeds ASK; write list feeds BUILD (and the MCP
 * surface, which has no ASK/BUILD split of its own).
 *
 * <p>
 * Formerly also backed the {@code project_deployment_agent} delegate ({@code ProjectDeploymentSubAgentConfiguration},
 * which built its own tool list from this same factory) — dissolved ticket 732, CRUD-delegate unwind Task 3, with its
 * seven tools registered flat directly from {@link #readToolCallbacks()}/{@link #writeToolCallbacks()} on
 * {@code AiHubConfiguration} and a new MCP contributor in {@code ToolCallbackContributorConfiguration}
 * (ai-copilot-service). Keeping every surface on one factory is what stops a tool added for one from silently missing
 * on another.
 * </p>
 *
 * <p>
 * A sibling factory, {@code ProjectDeploymentToolCallbacksFactory}, used to build the SAME seven tools independently
 * for the {@code project_deployment_ask}/{@code project_deployment_build} Copilot panel agents alone — a pre-existing
 * phase-3 duplication. Folded into this factory (ticket 732, CRUD-delegate-unwind plan, Task 9): the twin is deleted
 * and {@code ProjectDeploymentAgentConfiguration} now consumes this bean directly, so every surface (both panel pairs,
 * AI Hub, and the management MCP server) shares one factory.
 * </p>
 *
 * @author Ivica Cardic
 */
public class DeploymentToolCallbacksFactory {

    private final ProjectDeploymentFacade projectDeploymentFacade;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DeploymentToolCallbacksFactory(ProjectDeploymentFacade projectDeploymentFacade) {
        this.projectDeploymentFacade = projectDeploymentFacade;
    }

    public List<ToolCallback> readToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        toolCallbacks.add(new ListProjectDeploymentsToolCallback(projectDeploymentFacade));

        return toolCallbacks;
    }

    public List<ToolCallback> writeToolCallbacks() {
        List<ToolCallback> toolCallbacks = new ArrayList<>(readToolCallbacks());

        toolCallbacks.add(new CreateProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new UpdateProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new DeleteProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new RollbackProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new ToggleProjectDeploymentToolCallback(projectDeploymentFacade));
        toolCallbacks.add(new PromoteWorkflowToolCallback(projectDeploymentFacade));

        return toolCallbacks;
    }
}
