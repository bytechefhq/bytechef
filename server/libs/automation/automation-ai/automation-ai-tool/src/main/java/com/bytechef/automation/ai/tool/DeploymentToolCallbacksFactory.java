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
 * Builds the project-deployment tool-callback lists shared by the Copilot deployment panel agents and the AI Hub
 * {@code deployment_manager} subagent. Read list feeds ASK; write list feeds BUILD.
 *
 * <p>
 * The write list is the deployment_manager's tool set verbatim — the manager has no ASK/BUILD split of its own, so it
 * consumes {@link #writeToolCallbacks()} directly. Keeping both surfaces on one factory is what stops a tool added for
 * the hub from silently missing on the panel.
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
