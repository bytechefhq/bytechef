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

package com.bytechef.component.ai.agent.utils.cluster.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.ai.workspace.AgentWorkspaceQuota;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ShellQuotaToolCallbackTest {

    @TempDir
    private Path root;

    @Test
    void testCommandWithinTheLimitRunsUnchanged() {
        ToolCallback delegate = givenTool("Bash", "output");

        assertThat(callBash(delegate, 100, "ls")).isEqualTo("output");
    }

    @Test
    void testCommandThatGoesOverTheLimitGetsANote() {
        ToolCallback delegate = givenTool("Bash", "output");

        when(delegate.call(anyString())).thenAnswer(invocation -> {
            Files.writeString(root.resolve("big.bin"), "x".repeat(200));

            return "output";
        });

        assertThat(callBash(delegate, 100, "make-big-file")).startsWith("output\n\n")
            .contains("The agent workspace is over its limit (200 bytes of 100 bytes)");
    }

    @Test
    void testCommandIsRefusedWhileOverTheLimit() throws IOException {
        Files.writeString(root.resolve("big.bin"), "x".repeat(200));

        ToolCallback delegate = givenTool("Bash", "output");

        assertThat(callBash(delegate, 100, "echo hello")).startsWith(
            "Error: the agent workspace is over its limit (200 bytes of 100 bytes), so the command was not run.");

        verify(delegate, never()).call(anyString());
    }

    @Test
    void testCleanupCommandsRunWhileOverTheLimit() throws IOException {
        Files.writeString(root.resolve("big.bin"), "x".repeat(200));

        ToolCallback delegate = givenTool("Bash", "removed");

        assertThat(callBash(delegate, 100, "rm big.bin")).startsWith("removed");
        assertThat(callBash(delegate, 100, "  rmdir old")).startsWith("removed");
        assertThat(callBash(delegate, 100, "truncate -s 0 big.bin")).startsWith("removed");
        assertThat(callBash(delegate, 100, "find . -name '*.log' -delete")).startsWith("removed");
        assertThat(callBash(delegate, 100, "find . -name '*.log'")).contains("the command was not run");
        assertThat(callBash(delegate, 100, "rmx big.bin")).contains("the command was not run");
    }

    @Test
    void testWrapLeavesOtherShellToolsAndUnlimitedWorkspacesUnchanged() {
        ToolCallback bashOutputTool = givenTool("BashOutput", "output");
        ToolCallback bashTool = givenTool("Bash", "output");

        assertThat(ShellQuotaToolCallback.wrap(bashOutputTool, new AgentWorkspaceQuota(root, 10)))
            .isSameAs(bashOutputTool);
        assertThat(ShellQuotaToolCallback.wrap(bashTool, new AgentWorkspaceQuota(root, 0))).isSameAs(bashTool);
        assertThat(ShellQuotaToolCallback.wrap(bashTool, new AgentWorkspaceQuota(root, 10)))
            .isInstanceOf(ShellQuotaToolCallback.class);
    }

    private String callBash(ToolCallback delegate, long maxBytes, String command) {
        ToolCallback toolCallback = ShellQuotaToolCallback.wrap(delegate, new AgentWorkspaceQuota(root, maxBytes));

        return toolCallback.call(JsonUtils.write(Map.of("command", command)));
    }

    private static ToolCallback givenTool(String name, String output) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{}")
                .build());
        when(toolCallback.call(anyString())).thenReturn(output);

        return toolCallback;
    }
}
