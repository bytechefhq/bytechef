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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.springaicommunity.agent.common.workspace.Workspace;
import org.springaicommunity.agent.exec.LocalExecBackend;
import org.springaicommunity.agent.tools.ShellTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Provides shell tools (Bash, BashOutput, KillShell) for the AI agent. Commands start in the agent's workspace with a
 * clean environment, so none of the server's environment variables reach them. Commands still run in the server
 * process's operating system account and are not confined to the workspace, which is why the handler only registers
 * this tool when shell tools are enabled on the server.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsShellTools {

    private static final String CLUSTER_ELEMENT_NAME = "shellTools";
    private static final String DEFAULT_PATH = "/usr/local/bin:/usr/bin:/bin";

    private final AgentWorkspaceResolver agentWorkspaceResolver;

    public final ClusterElementDefinition<ToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsShellTools(AgentWorkspaceResolver agentWorkspaceResolver) {
        this.agentWorkspaceResolver = agentWorkspaceResolver;

        this.clusterElementDefinition = ComponentDsl.<ToolCallbackProviderFunction>clusterElement(CLUSTER_ELEMENT_NAME)
            .title("Shell Tools")
            .description(
                "Execute shell commands in the agent's own workspace with timeout control, background process " +
                    "management, and regex output filtering.")
            .type(TOOLS)
            .properties(AgentWorkspaceResolver.workspaceNameProperty())
            .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Workspace workspace = agentWorkspaceResolver.resolve(
            context, CLUSTER_ELEMENT_NAME, inputParameters.getString(WORKSPACE_NAME));

        Path workspaceRoot = workspace.root();

        LocalExecBackend localExecBackend = LocalExecBackend.builder()
            .workingDirectory(workspaceRoot)
            .cleanEnvironment(true)
            .environment(createEnvironment(workspaceRoot))
            .build();

        ShellTools shellTools = ShellTools.builder()
            .execBackend(localExecBackend)
            .build();

        return ToolCallbackProvider.from(ToolCallbacks.from(shellTools));
    }

    private static Map<String, String> createEnvironment(Path workspaceRoot) {
        Map<String, String> environment = new HashMap<>();

        environment.put("HOME", workspaceRoot.toString());
        environment.put("PATH", DEFAULT_PATH);
        environment.put("TMPDIR", workspaceRoot.toString());

        return environment;
    }
}
