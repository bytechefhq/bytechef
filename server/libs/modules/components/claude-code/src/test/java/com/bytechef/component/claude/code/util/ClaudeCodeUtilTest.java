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

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeMcpServerFunction.McpServer;
import com.bytechef.platform.component.definition.ai.claudecode.ClaudeCodeSkillsFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.github.markpollack.claude.agent.sdk.config.PermissionMode;
import io.github.markpollack.claude.agent.sdk.mcp.McpServerConfig;
import io.github.markpollack.claude.agent.sdk.transport.CLIOptions;
import io.github.markpollack.claude.agent.sdk.types.Message;
import io.github.markpollack.claude.agent.sdk.types.ResultMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ClaudeCodeUtilTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private final Context context = mock(Context.class);

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
    void testRunGivesClaudeCodeTheSkillsInAWorkspaceItDeletesAfterwards() throws Exception {
        ClaudeCodeSkillsFunction claudeCodeSkillsFunction = (
            inputParameters, connectionParameters, skillsDirectory, context) -> Files.writeString(
                Files.createDirectories(skillsDirectory.resolve(inputParameters.getRequiredString("name")))
                    .resolve("SKILL.md"),
                "skill");

        when(clusterElementDefinitionService.getClusterElement("aiAgentUtils", 1, "claudeCodeSkills"))
            .thenReturn(claudeCodeSkillsFunction);

        List<ClusterElement> skills = List.of(
            clusterElement("skills_1", "aiAgentUtils/v1/claudeCodeSkills", Map.of("name", "release-notes")));
        AtomicReference<Path> workspaceReference = new AtomicReference<>();

        String result = ClaudeCodeUtil.run(
            "hello", Map.of(),
            skillsDirectory -> ClaudeCodeUtil.writeSkills(
                skills, Map.of(), clusterElementDefinitionService, skillsDirectory, context),
            (prompt, workingDirectory, mcpServerConfigs) -> {
                workspaceReference.set(workingDirectory);

                assertThat(workingDirectory.resolve(".claude/skills/release-notes/SKILL.md")).hasContent("skill");

                return "reply to " + prompt;
            });

        assertThat(result).isEqualTo("reply to hello");
        assertThat(workspaceReference.get()).doesNotExist();
    }

    @Test
    void testRunDeletesTheWorkspaceWhenClaudeCodeFails() {
        AtomicReference<Path> workspaceReference = new AtomicReference<>();

        assertThatThrownBy(() -> ClaudeCodeUtil.run(
            "hello", Map.of(), skillsDirectory -> Files.writeString(skillsDirectory.resolve("file"), "x"),
            (prompt, workingDirectory, mcpServerConfigs) -> {
                workspaceReference.set(workingDirectory);

                throw new IllegalStateException("boom");
            }))
                .hasMessage("boom");

        assertThat(workspaceReference.get()).doesNotExist();
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
}
