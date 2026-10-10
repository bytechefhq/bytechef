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

package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.ai.agent.utils.cluster.workspace.AgentWorkspaceResolver.WORKSPACE_NAME;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.ai.agent.utils.cluster.workspace.AgentWorkspaceResolver;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springaicommunity.agent.common.workspace.Workspace;
import org.springaicommunity.agent.tools.ListDirectoryTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
public class AiAgentUtilsListDirectoryTool {

    private static final String CLUSTER_ELEMENT_NAME = "listDirectoryTool";

    private final AgentWorkspaceResolver agentWorkspaceResolver;

    public final ClusterElementDefinition<ToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsListDirectoryTool(AgentWorkspaceResolver agentWorkspaceResolver) {
        this.agentWorkspaceResolver = agentWorkspaceResolver;

        this.clusterElementDefinition = ComponentDsl.<ToolCallbackProviderFunction>clusterElement(CLUSTER_ELEMENT_NAME)
            .title("List Directory Tool")
            .description("List files and directories within the agent's own workspace.")
            .type(TOOLS)
            .properties(AgentWorkspaceResolver.workspaceNameProperty())
            .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Workspace workspace = agentWorkspaceResolver.resolve(
            context, CLUSTER_ELEMENT_NAME, inputParameters.getString(WORKSPACE_NAME));

        ListDirectoryTool listDirectoryTool = ListDirectoryTool.builder()
            .workspace(workspace)
            .build();

        return ToolCallbackProvider.from(ToolCallbacks.from(listDirectoryTool));
    }
}
