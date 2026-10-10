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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.ai.agent.utils.cluster.workspace.AgentWorkspaceResolver;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springaicommunity.agent.common.workspace.Workspace;
import org.springaicommunity.agent.tools.GrepTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
public class AiAgentUtilsGrepTool {

    public static final String MAX_DEPTH = "maxDepth";
    public static final String MAX_OUTPUT_LENGTH = "maxOutputLength";

    private static final String CLUSTER_ELEMENT_NAME = "grepTool";

    private final AgentWorkspaceResolver agentWorkspaceResolver;

    public final ClusterElementDefinition<ToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsGrepTool(AgentWorkspaceResolver agentWorkspaceResolver) {
        this.agentWorkspaceResolver = agentWorkspaceResolver;

        this.clusterElementDefinition = ComponentDsl.<ToolCallbackProviderFunction>clusterElement(CLUSTER_ELEMENT_NAME)
            .title("Grep Tool")
            .description(
                "Pure Java grep implementation for code search with regex, glob filtering, and multiple output " +
                    "modes, within the agent's own workspace.")
            .type(TOOLS)
            .properties(
                AgentWorkspaceResolver.workspaceNameProperty(),
                integer(MAX_DEPTH)
                    .label("Max Depth")
                    .description("Maximum directory depth to search. Leave empty for the default of 100.")
                    .minValue(1)
                    .advancedOption(true),
                integer(MAX_OUTPUT_LENGTH)
                    .label("Max Output Length")
                    .description("Maximum number of characters returned. Leave empty for the default of 100000.")
                    .minValue(1)
                    .advancedOption(true))
            .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Workspace workspace = agentWorkspaceResolver.resolve(
            context, CLUSTER_ELEMENT_NAME, inputParameters.getString(WORKSPACE_NAME));

        GrepTool.Builder builder = GrepTool.builder()
            .workspace(workspace);

        Integer maxDepth = inputParameters.getInteger(MAX_DEPTH);

        if (maxDepth != null) {
            builder.maxDepth(maxDepth);
        }

        Integer maxOutputLength = inputParameters.getInteger(MAX_OUTPUT_LENGTH);

        if (maxOutputLength != null) {
            builder.maxOutputLength(maxOutputLength);
        }

        return ToolCallbackProvider.from(ToolCallbacks.from(builder.build()));
    }
}
