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

package com.bytechef.component.claude.code.action;

import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.SCRIPT;
import static com.bytechef.component.claude.code.constant.ClaudeCodeConstants.WORKSPACE_NAME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.sampleOutput;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.ai.workspace.AgentWorkspaceLayout.DEFAULT_WORKSPACE_NAME;
import static com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction.MCP_SERVERS;
import static com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeSkillsFunction.SKILLS;

import com.bytechef.component.claude.code.executor.ClaudeCodeExecutor;
import com.bytechef.component.claude.code.util.ClaudeCodeUtil;
import com.bytechef.component.claude.code.workspace.ClaudeCodeWorkspaceResolver;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 * @author Ivica Cardic
 */
public class ClaudeCodeChat {

    private final ClaudeCodeExecutor claudeCodeExecutor;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver;

    ClaudeCodeChat(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver, ClaudeCodeExecutor claudeCodeExecutor) {

        this.claudeCodeExecutor = claudeCodeExecutor;
        this.claudeCodeWorkspaceResolver = claudeCodeWorkspaceResolver;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    public static ActionDefinition of(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClaudeCodeWorkspaceResolver claudeCodeWorkspaceResolver) {

        ClaudeCodeChat claudeCodeChat = new ClaudeCodeChat(
            clusterElementDefinitionService, claudeCodeWorkspaceResolver, ClaudeCodeUtil::execute);

        return action("chat")
            .title("Chat")
            .description(
                "Sends a message to Claude Code. Claude Code can use the tools of the attached MCP servers and the " +
                    "attached skills.")
            .properties(
                messageProperty(),
                workspaceNameProperty())
            .output(
                outputSchema(
                    string()
                        .description("The final reply of the Claude Code run.")),
                sampleOutput("Sample result"))
            .help("", "https://docs.bytechef.io/reference/components/claude-code_v1#chat")
            .perform((MultipleConnectionsPerformFunction) claudeCodeChat::perform);
    }

    String perform(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context) throws Exception {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        List<ClusterElement> mcpServers = clusterElementMap.getClusterElements(MCP_SERVERS);
        List<ClusterElement> skills = clusterElementMap.getClusterElements(SKILLS);

        Path workspace = claudeCodeWorkspaceResolver.resolve(context, inputParameters.getString(WORKSPACE_NAME));

        return ClaudeCodeUtil.run(
            inputParameters.getRequiredString(SCRIPT), claudeCodeWorkspaceResolver.getQuota(workspace),
            () -> ClaudeCodeUtil.getMcpServerConfigs(
                mcpServers, componentConnections, clusterElementDefinitionService, context),
            skillsDirectory -> ClaudeCodeUtil.writeSkills(
                skills, componentConnections, clusterElementDefinitionService, skillsDirectory, context),
            claudeCodeExecutor);
    }

    private static ModifiableStringProperty messageProperty() {
        return string(SCRIPT)
            .label("Message")
            .description("The message to send to Claude Code.")
            .controlType(ControlType.TEXT_AREA)
            .required(true);
    }

    private static ModifiableStringProperty workspaceNameProperty() {
        return string(WORKSPACE_NAME)
            .label("Workspace Name")
            .description(
                "Name of the working directory, which is kept between runs. Claude Code steps of this workflow with " +
                    "the same workspace name share files; steps with different names do not. Letters, digits, '-' " +
                    "and '_' only.")
            .defaultValue(DEFAULT_WORKSPACE_NAME)
            .advancedOption(true);
    }
}
