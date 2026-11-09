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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.claude.code.executor.ClaudeCodeExecutor;
import com.bytechef.component.definition.Context;
import com.bytechef.platform.ai.workspace.AgentWorkspaceQuota;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction.McpServer;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.github.markpollack.claude.agent.sdk.config.PermissionMode;
import io.github.markpollack.claude.agent.sdk.mcp.McpServerConfig;
import io.github.markpollack.claude.agent.sdk.transport.CLIOptions;
import io.github.markpollack.claude.agent.sdk.types.Message;
import io.github.markpollack.claude.agent.sdk.types.ResultMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ClaudeCodeUtilTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final Context context = mock(Context.class);

    @TempDir
    private Path workspace;

    @Test
    void testGetMcpServerConfigsPassesEachElementItsOwnConnectionAndKeepsNamesUnique() throws Exception {
        List<String> tokensSeen = new ArrayList<>();

        ClaudeCodeMcpServerFunction claudeCodeMcpServerFunction = (inputParameters, connectionParameters, context) -> {
            tokensSeen.add(connectionParameters.getRequiredString("token"));

            return new McpServer(
                inputParameters.getRequiredString("serverName"), connectionParameters.getRequiredString("transport"),
                connectionParameters.getRequiredString("url"),
                Map.of("Authorization", "Bearer " + connectionParameters.getRequiredString("token")));
        };

        when(clusterElementDefinitionService.getClusterElement("mcpClient", 1, "mcpServer"))
            .thenReturn(claudeCodeMcpServerFunction);

        Map<String, McpServerConfig> mcpServerConfigs = ClaudeCodeUtil.getMcpServerConfigs(
            List.of(
                clusterElement("mcpServer_1", "mcpClient/v1/mcpServer", Map.of("serverName", "github")),
                clusterElement("mcpServer_2", "mcpClient/v1/mcpServer", Map.of("serverName", "github"))),
            Map.of(
                "mcpServer_1", connection(Map.of("url", "https://one.example.com/mcp", "transport", "http",
                    "token", "one")),
                "mcpServer_2", connection(Map.of("url", "https://two.example.com/sse", "transport", "sse",
                    "token", "two"))),
            clusterElementDefinitionService, context);

        assertThat(mcpServerConfigs).containsExactly(
            Map.entry(
                "github",
                new McpServerConfig.McpHttpServerConfig(
                    "https://one.example.com/mcp", Map.of("Authorization", "Bearer one"))),
            Map.entry(
                "github_2",
                new McpServerConfig.McpSseServerConfig(
                    "https://two.example.com/sse", Map.of("Authorization", "Bearer two"))));
        assertThat(tokensSeen).containsExactly("one", "two");
    }

    @Test
    void testRunKeepsTheWorkspaceFilesAndRebuildsTheSkillsEachRun() throws Exception {
        String firstResult = ClaudeCodeUtil.run(
            "clone", new AgentWorkspaceQuota(workspace, 0), Map::of,
            skillsDirectory -> writeSkill(skillsDirectory, "release-notes"),
            (prompt, workingDirectory, mcpServerConfigs, hookRegistry) -> {
                assertThat(workingDirectory).isEqualTo(workspace);
                assertThat(workingDirectory.resolve(".claude/skills/release-notes/SKILL.md")).hasContent("skill");

                Files.writeString(workingDirectory.resolve("notes.md"), "kept");
                Files.writeString(workingDirectory.resolve(".claude/settings.json"), "{}");

                return "reply to " + prompt;
            });

        String secondResult = ClaudeCodeUtil.run(
            "review", new AgentWorkspaceQuota(workspace, 0), Map::of,
            skillsDirectory -> writeSkill(skillsDirectory, "triage"),
            (prompt, workingDirectory, mcpServerConfigs, hookRegistry) -> {
                assertThat(workingDirectory.resolve("notes.md")).hasContent("kept");
                assertThat(workingDirectory.resolve(".claude/skills/triage/SKILL.md")).hasContent("skill");
                assertThat(workingDirectory.resolve(".claude/skills/release-notes")).doesNotExist();
                assertThat(workingDirectory.resolve(".claude/settings.json")).doesNotExist();

                return "reply to " + prompt;
            });

        assertThat(firstResult).isEqualTo("reply to clone");
        assertThat(secondResult).isEqualTo("reply to review");
        assertThat(workspace.resolve("notes.md")).hasContent("kept");
    }

    @Test
    void testRunKeepsTheWorkspaceWhenClaudeCodeFails() throws Exception {
        assertThatThrownBy(() -> ClaudeCodeUtil.run(
            "hello", new AgentWorkspaceQuota(workspace, 0), Map::of,
            skillsDirectory -> Files.writeString(skillsDirectory.resolve("file"), "x"),
            (prompt, workingDirectory, mcpServerConfigs, hookRegistry) -> {
                Files.writeString(workingDirectory.resolve("partial.txt"), "work");

                throw new IllegalStateException("boom");
            }))
                .hasMessage("boom");

        assertThat(workspace.resolve("partial.txt")).hasContent("work");
        assertThat(ClaudeCodeUtil.WORKSPACE_LOCKS).doesNotContainKey(workspace);
    }

    @Test
    void testRunLetsOnlyOneRunUseAWorkspaceAtATime() throws Exception {
        CountDownLatch firstRunStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstRun = new CountDownLatch(1);
        AtomicInteger activeRuns = new AtomicInteger();
        AtomicInteger maxActiveRuns = new AtomicInteger();

        ClaudeCodeExecutor claudeCodeExecutor = (prompt, workingDirectory, mcpServerConfigs, hookRegistry) -> {
            maxActiveRuns.accumulateAndGet(activeRuns.incrementAndGet(), Math::max);

            if ("first".equals(prompt)) {
                firstRunStarted.countDown();

                assertThat(releaseFirstRun.await(10, TimeUnit.SECONDS)).isTrue();
            }

            activeRuns.decrementAndGet();

            return prompt;
        };

        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            Future<String> firstRun = executorService.submit(
                () -> ClaudeCodeUtil.run("first", new AgentWorkspaceQuota(workspace, 0), Map::of,
                    skillsDirectory -> {}, claudeCodeExecutor));

            assertThat(firstRunStarted.await(10, TimeUnit.SECONDS)).isTrue();

            Future<String> secondRun = executorService.submit(
                () -> ClaudeCodeUtil.run("second", new AgentWorkspaceQuota(workspace, 0), Map::of,
                    skillsDirectory -> {}, claudeCodeExecutor));

            assertThatThrownBy(() -> secondRun.get(200, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);

            releaseFirstRun.countDown();

            assertThat(firstRun.get(10, TimeUnit.SECONDS)).isEqualTo("first");
            assertThat(secondRun.get(10, TimeUnit.SECONDS)).isEqualTo("second");
        }

        assertThat(maxActiveRuns.get()).isEqualTo(1);
        assertThat(ClaudeCodeUtil.WORKSPACE_LOCKS).doesNotContainKey(workspace);
    }

    @Test
    void testRunGivesClaudeCodeTheQuotaHooksOnlyWhenTheWorkspaceIsLimited() throws Exception {
        List<Boolean> hooksSeen = new ArrayList<>();

        ClaudeCodeExecutor claudeCodeExecutor = (prompt, workingDirectory, mcpServerConfigs, hookRegistry) -> {
            hooksSeen.add(hookRegistry.hasHooks());

            return prompt;
        };

        ClaudeCodeUtil.run(
            "limited", new AgentWorkspaceQuota(workspace, 1024), Map::of, skillsDirectory -> {}, claudeCodeExecutor);
        ClaudeCodeUtil.run(
            "unlimited", new AgentWorkspaceQuota(workspace, 0), Map::of, skillsDirectory -> {}, claudeCodeExecutor);

        assertThat(hooksSeen).containsExactly(true, false);
    }

    @Test
    void testRunResolvesTheMcpServersInsideTheWorkspaceLockAfterTheSkillsAreWritten() throws Exception {
        List<String> events = new ArrayList<>();

        ClaudeCodeUtil.run(
            "hello", new AgentWorkspaceQuota(workspace, 0),
            () -> {
                events.add("mcp locked=" + ClaudeCodeUtil.WORKSPACE_LOCKS.containsKey(workspace));

                return Map.of();
            },
            skillsDirectory -> events.add("skills"),
            (prompt, workingDirectory, mcpServerConfigs, hookRegistry) -> {
                events.add("execute");

                return prompt;
            });

        assertThat(events).containsExactly("skills", "mcp locked=true", "execute");
    }

    @Test
    void testIsWorkspaceInUseIsTrueForTheWorkspaceAndItsParentsOnlyWhileARunUsesIt() throws Exception {
        List<Boolean> inUse = new ArrayList<>();

        ClaudeCodeUtil.run(
            "hello", new AgentWorkspaceQuota(workspace, 0), Map::of, skillsDirectory -> {},
            (prompt, workingDirectory, mcpServerConfigs, hookRegistry) -> {
                inUse.add(ClaudeCodeUtil.isWorkspaceInUse(workspace));
                inUse.add(ClaudeCodeUtil.isWorkspaceInUse(workspace.getParent()));
                inUse.add(ClaudeCodeUtil.isWorkspaceInUse(workspace.resolveSibling("other")));

                return prompt;
            });

        inUse.add(ClaudeCodeUtil.isWorkspaceInUse(workspace));

        assertThat(inUse).containsExactly(true, true, false, false);
    }

    @Test
    void testGetRunWorkspaceQuotaDoesNotCountTheSkills() throws Exception {
        Path skillsDirectory = Files.createDirectories(workspace.resolve(".claude/skills/one"));

        Files.write(skillsDirectory.resolve("SKILL.md"), new byte[40]);

        AgentWorkspaceQuota runWorkspaceQuota = ClaudeCodeUtil.getRunWorkspaceQuota(
            new AgentWorkspaceQuota(workspace, 100), workspace.resolve(".claude/skills"));

        AgentWorkspaceQuota unlimitedRunWorkspaceQuota = ClaudeCodeUtil.getRunWorkspaceQuota(
            new AgentWorkspaceQuota(workspace, 0), workspace.resolve(".claude/skills"));

        assertThat(runWorkspaceQuota.maxBytes()).isEqualTo(140);
        assertThat(unlimitedRunWorkspaceQuota.maxBytes()).isZero();
    }

    @Test
    void testCreateCliOptionsLoadsOnlyProjectSettings() {
        Map<String, McpServerConfig> mcpServerConfigs = Map.of(
            "github", new McpServerConfig.McpHttpServerConfig("https://example.com/mcp"));

        CLIOptions cliOptions = ClaudeCodeUtil.createCliOptions(mcpServerConfigs);

        assertThat(cliOptions.getSettingSources()).containsExactly("project");
        assertThat(cliOptions.getPermissionMode()).isEqualTo(PermissionMode.DANGEROUSLY_SKIP_PERMISSIONS);
        assertThat(cliOptions.getMcpServers()).isEqualTo(mcpServerConfigs);
        assertThat(cliOptions.getTimeout()).isEqualTo(ClaudeCodeUtil.TIMEOUT);
    }

    @Test
    void testGetResultReturnsTheResultAndFailsOnAnErrorResult() {
        Message success = ResultMessage.builder()
            .subtype("success")
            .result("done")
            .build();
        Message failure = ResultMessage.builder()
            .subtype("error_max_turns")
            .isError(true)
            .result("too many turns")
            .build();

        assertThat(ClaudeCodeUtil.getResult(List.of(success))).isEqualTo("done");
        assertThatThrownBy(() -> ClaudeCodeUtil.getResult(List.of(failure)))
            .hasMessageContaining("error_max_turns")
            .hasMessageContaining("too many turns");
        assertThatThrownBy(() -> ClaudeCodeUtil.getResult(List.of()))
            .hasMessageContaining("without a result");
    }

    private static ClusterElement clusterElement(String workflowNodeName, String type, Map<String, ?> parameters) {
        return new ClusterElement(null, null, Map.of(), null, type, parameters, workflowNodeName);
    }

    private static ComponentConnection connection(Map<String, ?> parameters) {
        return new ComponentConnection("mcpClient", 1, 1L, parameters, null);
    }

    private static void writeSkill(Path skillsDirectory, String name) throws IOException {
        Path skillDirectory = Files.createDirectories(skillsDirectory.resolve(name));

        Files.writeString(skillDirectory.resolve("SKILL.md"), "skill");
    }
}
