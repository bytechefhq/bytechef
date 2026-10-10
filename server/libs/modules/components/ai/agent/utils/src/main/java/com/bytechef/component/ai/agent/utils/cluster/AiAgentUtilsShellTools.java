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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.ai.agent.utils.cluster.exec.DockerExecBackendRegistry;
import com.bytechef.component.ai.agent.utils.cluster.workspace.AgentWorkspaceResolver;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springaicommunity.agent.common.exec.ExecBackend;
import org.springaicommunity.agent.common.workspace.Workspace;
import org.springaicommunity.agent.exec.LocalExecBackend;
import org.springaicommunity.agent.tools.ShellTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Provides shell tools (Bash, BashOutput, KillShell) for the AI agent, running in the agent's workspace. Commands run
 * in a Docker container with only the workspace mounted. Running them directly on the server is offered only when the
 * server allows it, because local commands are not confined to the workspace; they get a clean environment, so none of
 * the server's environment variables reach them.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsShellTools {

    public static final String EXEC_BACKEND = "execBackend";
    public static final String DOCKER = "DOCKER";
    public static final String LOCAL = "LOCAL";

    private static final String CLUSTER_ELEMENT_NAME = "shellTools";
    private static final String DEFAULT_PATH = "/usr/local/bin:/usr/bin:/bin";

    private final boolean allowLocal;
    private final AgentWorkspaceResolver agentWorkspaceResolver;
    private final DockerExecBackendRegistry dockerExecBackendRegistry;

    public final ClusterElementDefinition<ToolCallbackProviderFunction> clusterElementDefinition;

    @SuppressFBWarnings("EI")
    public AiAgentUtilsShellTools(
        AgentWorkspaceResolver agentWorkspaceResolver, DockerExecBackendRegistry dockerExecBackendRegistry,
        boolean allowLocal) {

        this.agentWorkspaceResolver = agentWorkspaceResolver;
        this.allowLocal = allowLocal;
        this.dockerExecBackendRegistry = dockerExecBackendRegistry;

        this.clusterElementDefinition = ComponentDsl.<ToolCallbackProviderFunction>clusterElement(CLUSTER_ELEMENT_NAME)
            .title("Shell Tools")
            .description(
                "Execute shell commands in the agent's own workspace with timeout control, background process " +
                    "management, and regex output filtering.")
            .type(TOOLS)
            .properties(
                AgentWorkspaceResolver.workspaceNameProperty(),
                string(EXEC_BACKEND)
                    .label("Execution Backend")
                    .description(
                        "Where commands run. Docker runs them in a container that only sees the agent's workspace. " +
                            "Local runs them on the server and is only available when the server allows it.")
                    .options(getExecBackendOptions(allowLocal))
                    .defaultValue(DOCKER)
                    .advancedOption(true))
            .object(() -> this::apply);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Workspace workspace = agentWorkspaceResolver.resolve(
            context, CLUSTER_ELEMENT_NAME, inputParameters.getString(WORKSPACE_NAME));

        ShellTools shellTools = ShellTools.builder()
            .execBackend(createExecBackend(inputParameters.getString(EXEC_BACKEND, DOCKER), workspace.root()))
            .build();

        return ToolCallbackProvider.from(ToolCallbacks.from(shellTools));
    }

    private ExecBackend createExecBackend(String execBackend, Path workspaceRoot) {
        if (DOCKER.equals(execBackend)) {
            return dockerExecBackendRegistry.getExecBackend(workspaceRoot);
        }

        if (!LOCAL.equals(execBackend)) {
            throw new IllegalArgumentException("Unknown shell execution backend: " + execBackend);
        }

        if (!allowLocal) {
            throw new IllegalStateException(
                "Running shell commands locally is not allowed on this server; use the Docker execution backend.");
        }

        return LocalExecBackend.builder()
            .workingDirectory(workspaceRoot)
            .cleanEnvironment(true)
            .environment(createLocalEnvironment(workspaceRoot))
            .build();
    }

    private static Map<String, String> createLocalEnvironment(Path workspaceRoot) {
        Map<String, String> environment = new HashMap<>();

        environment.put("HOME", workspaceRoot.toString());
        environment.put("PATH", DEFAULT_PATH);
        environment.put("TMPDIR", workspaceRoot.toString());

        return environment;
    }

    private static List<Option<String>> getExecBackendOptions(boolean allowLocal) {
        List<Option<String>> options = new ArrayList<>();

        options.add(option("Docker", DOCKER));

        if (allowLocal) {
            options.add(option("Local", LOCAL));
        }

        return options;
    }
}
