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

package com.bytechef.component.claude.code.util;

import com.bytechef.component.claude.code.executor.ClaudeCodeExecutor;
import com.bytechef.component.claude.code.workspace.ClaudeCodeWorkspaceQuotaHooks;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.workspace.AgentWorkspaceQuota;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction.McpServer;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeSkillsFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import io.github.markpollack.claude.agent.sdk.ClaudeClient;
import io.github.markpollack.claude.agent.sdk.ClaudeSyncClient;
import io.github.markpollack.claude.agent.sdk.config.ClaudeCliDiscovery;
import io.github.markpollack.claude.agent.sdk.config.ClaudeCliDiscovery.ClaudeCliNotFoundException;
import io.github.markpollack.claude.agent.sdk.config.PermissionMode;
import io.github.markpollack.claude.agent.sdk.hooks.HookRegistry;
import io.github.markpollack.claude.agent.sdk.mcp.McpServerConfig;
import io.github.markpollack.claude.agent.sdk.transport.CLIOptions;
import io.github.markpollack.claude.agent.sdk.types.Message;
import io.github.markpollack.claude.agent.sdk.types.ResultMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * @author Marko Kriskovic
 * @author Ivica Cardic
 */
public final class ClaudeCodeUtil {

    static final List<String> SETTING_SOURCES = List.of("project");
    static final Duration TIMEOUT = Duration.ofMinutes(10);

    private static final String CLAUDE_DIRECTORY = ".claude";
    private static final String SKILLS_DIRECTORY = "skills";

    static final Map<Path, WorkspaceLock> WORKSPACE_LOCKS = new ConcurrentHashMap<>();

    private ClaudeCodeUtil() {
    }

    public static String execute(
        String prompt, Path workingDirectory, Map<String, McpServerConfig> mcpServerConfigs,
        HookRegistry hookRegistry) {

        CLIOptions cliOptions = createCliOptions(mcpServerConfigs);

        try (ClaudeSyncClient claudeSyncClient = ClaudeClient.sync(cliOptions)
            .claudePath(getClaudePath())
            .workingDirectory(workingDirectory)
            .timeout(TIMEOUT)
            .hookRegistry(hookRegistry)
            .build()) {

            return getResult(claudeSyncClient.connectAndReceive(prompt));
        }
    }

    public static Map<String, McpServerConfig> getMcpServerConfigs(
        List<ClusterElement> clusterElements, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService, Context context) throws Exception {

        Map<String, McpServerConfig> mcpServerConfigs = new LinkedHashMap<>();

        for (ClusterElement clusterElement : clusterElements) {
            ClaudeCodeMcpServerFunction claudeCodeMcpServerFunction =
                clusterElementDefinitionService.getClusterElement(
                    clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                    clusterElement.getClusterElementName());

            McpServer mcpServer = claudeCodeMcpServerFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(clusterElement, componentConnections), context);

            mcpServerConfigs.put(getUniqueName(mcpServer.name(), mcpServerConfigs), toMcpServerConfig(mcpServer));
        }

        return mcpServerConfigs;
    }

    public static boolean isWorkspaceInUse(Path directory) {
        Set<Path> workspaces = WORKSPACE_LOCKS.keySet();

        return workspaces.stream()
            .anyMatch(workspace -> workspace.startsWith(directory));
    }

    public static String run(
        String prompt, AgentWorkspaceQuota workspaceQuota, McpServerConfigsSupplier mcpServerConfigsSupplier,
        SkillsWriter skillsWriter, ClaudeCodeExecutor claudeCodeExecutor) throws Exception {

        Path workspace = workspaceQuota.root();

        WorkspaceLock workspaceLock = WORKSPACE_LOCKS.compute(
            workspace, (key, existingWorkspaceLock) -> WorkspaceLock.acquire(existingWorkspaceLock));

        try {
            workspaceLock.lock.lockInterruptibly();

            try {
                Path claudeDirectory = workspace.resolve(CLAUDE_DIRECTORY);

                if (Files.exists(claudeDirectory, LinkOption.NOFOLLOW_LINKS)) {
                    deleteRecursively(claudeDirectory);
                }

                Path skillsDirectory = Files.createDirectories(claudeDirectory.resolve(SKILLS_DIRECTORY));

                skillsWriter.write(skillsDirectory);

                AgentWorkspaceQuota runWorkspaceQuota = getRunWorkspaceQuota(workspaceQuota, skillsDirectory);

                return claudeCodeExecutor.execute(
                    prompt, workspace, mcpServerConfigsSupplier.get(),
                    ClaudeCodeWorkspaceQuotaHooks.create(runWorkspaceQuota));
            } finally {
                workspaceLock.lock.unlock();
            }
        } finally {
            WORKSPACE_LOCKS.computeIfPresent(workspace,
                (key, existingWorkspaceLock) -> existingWorkspaceLock.release());
        }
    }

    public static void writeSkills(
        List<ClusterElement> clusterElements, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService, Path skillsDirectory, Context context)
        throws Exception {

        for (ClusterElement clusterElement : clusterElements) {
            ClaudeCodeSkillsFunction claudeCodeSkillsFunction = clusterElementDefinitionService.getClusterElement(
                clusterElement.getComponentName(), clusterElement.getComponentVersion(),
                clusterElement.getClusterElementName());

            claudeCodeSkillsFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                getConnectionParameters(clusterElement, componentConnections), skillsDirectory, context);
        }
    }

    static String getClaudePath() {
        try {
            return ClaudeCliDiscovery.discoverClaudePath();
        } catch (ClaudeCliNotFoundException claudeCliNotFoundException) {
            throw new IllegalStateException(
                "The Claude Code CLI is not installed on the ByteChef server. Install it for the user that runs " +
                    "ByteChef with 'curl -fsSL https://claude.ai/install.sh | bash', put it on that user's PATH and " +
                    "restart ByteChef.",
                claudeCliNotFoundException);
        }
    }

    static CLIOptions createCliOptions(Map<String, McpServerConfig> mcpServerConfigs) {
        CLIOptions.Builder builder = CLIOptions.builder()
            .permissionMode(PermissionMode.DANGEROUSLY_SKIP_PERMISSIONS)
            .settingSources(SETTING_SOURCES)
            .timeout(TIMEOUT);

        if (!mcpServerConfigs.isEmpty()) {
            builder.mcpServers(mcpServerConfigs);
        }

        return builder.build();
    }

    static String getResult(Iterable<Message> messages) {
        for (Message message : messages) {
            if (message instanceof ResultMessage resultMessage) {
                if (resultMessage.isError()) {
                    throw new IllegalStateException(
                        "Claude Code run failed (%s): %s".formatted(resultMessage.subtype(), resultMessage.result()));
                }

                return resultMessage.result();
            }
        }

        throw new IllegalStateException("Claude Code ended without a result");
    }

    static String getUniqueName(String name, Map<String, ?> existingNames) {
        String uniqueName = name;

        for (int index = 2; existingNames.containsKey(uniqueName); index++) {
            uniqueName = name + "_" + index;
        }

        return uniqueName;
    }

    static McpServerConfig toMcpServerConfig(McpServer mcpServer) {
        if (McpServer.SSE.equals(mcpServer.transportType())) {
            return new McpServerConfig.McpSseServerConfig(mcpServer.url(), mcpServer.headers());
        }

        return new McpServerConfig.McpHttpServerConfig(mcpServer.url(), mcpServer.headers());
    }

    private static void deleteRecursively(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> sortedPaths = paths.sorted(Comparator.reverseOrder())
                .toList();

            for (Path path : sortedPaths) {
                Files.deleteIfExists(path);
            }
        }
    }

    static AgentWorkspaceQuota getRunWorkspaceQuota(
        AgentWorkspaceQuota workspaceQuota, Path skillsDirectory) {

        if (!workspaceQuota.isLimited()) {
            return workspaceQuota;
        }

        return new AgentWorkspaceQuota(
            workspaceQuota.root(),
            workspaceQuota.maxBytes() + AgentWorkspaceQuota.getDirectorySize(skillsDirectory));
    }

    private static Parameters getConnectionParameters(
        ClusterElement clusterElement, Map<String, ComponentConnection> componentConnections) {

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        if (componentConnection == null) {
            return ParametersFactory.create(Map.of());
        }

        return ParametersFactory.create(componentConnection.getParameters());
    }

    static final class WorkspaceLock {

        private final ReentrantLock lock = new ReentrantLock();
        private int users;

        private static WorkspaceLock acquire(@Nullable WorkspaceLock existingWorkspaceLock) {
            WorkspaceLock workspaceLock = existingWorkspaceLock == null ? new WorkspaceLock() : existingWorkspaceLock;

            workspaceLock.users++;

            return workspaceLock;
        }

        private @Nullable WorkspaceLock release() {
            users--;

            return users == 0 ? null : this;
        }
    }

    @FunctionalInterface
    public interface McpServerConfigsSupplier {

        Map<String, McpServerConfig> get() throws Exception;
    }

    @FunctionalInterface
    public interface SkillsWriter {

        void write(Path skillsDirectory) throws Exception;
    }
}
