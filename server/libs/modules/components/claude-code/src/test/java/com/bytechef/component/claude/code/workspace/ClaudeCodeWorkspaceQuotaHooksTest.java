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

package com.bytechef.component.claude.code.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.ai.workspace.AgentWorkspaceQuota;
import io.github.markpollack.claude.agent.sdk.hooks.HookRegistry;
import io.github.markpollack.claude.agent.sdk.types.control.HookEvent;
import io.github.markpollack.claude.agent.sdk.types.control.HookInput.PreToolUseInput;
import io.github.markpollack.claude.agent.sdk.types.control.HookOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class ClaudeCodeWorkspaceQuotaHooksTest {

    @TempDir
    private Path outsideDirectory;

    @TempDir
    private Path workspace;

    @Test
    void testCreateRegistersNoHooksWithoutALimit() {
        HookRegistry hookRegistry = ClaudeCodeWorkspaceQuotaHooks.create(new AgentWorkspaceQuota(workspace, 0));

        assertThat(hookRegistry.hasHooks()).isFalse();
    }

    @Test
    void testCreateRegistersTheFileWriteAndBashHooks() {
        HookRegistry hookRegistry = ClaudeCodeWorkspaceQuotaHooks.create(new AgentWorkspaceQuota(workspace, 100));

        assertThat(hookRegistry.getByEvent(HookEvent.PRE_TOOL_USE)).hasSize(2);
        assertThat(hookRegistry.getByEvent(HookEvent.POST_TOOL_USE)).hasSize(1);
    }

    @Test
    void testCheckFileWriteDeniesAWriteThatGoesOverTheLimit() throws IOException {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(workspace, 100);

        Files.writeString(workspace.resolve("existing.txt"), "x".repeat(60));

        HookOutput denied = ClaudeCodeWorkspaceQuotaHooks.checkFileWrite(
            quota, preToolUse("Write", Map.of("file_path", workspace.resolve("new.txt")
                .toString(), "content", "y".repeat(50))));
        HookOutput allowed = ClaudeCodeWorkspaceQuotaHooks.checkFileWrite(
            quota, preToolUse("Write", Map.of("file_path", workspace.resolve("existing.txt")
                .toString(), "content", "y".repeat(90))));

        assertThat(denied.decision()).isEqualTo("block");
        assertThat(denied.reason()).contains("workspace is full");
        assertThat(allowed.decision()).isNull();
    }

    @Test
    void testCheckFileWriteCountsEveryEditOfAMultiEdit() throws IOException {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(workspace, 100);

        Path file = Files.writeString(workspace.resolve("notes.md"), "a b c " + "x".repeat(80));

        Map<String, Object> singleEdit = Map.of(
            "file_path", file.toString(), "old_string", "a", "new_string", "a".repeat(10));
        Map<String, Object> multiEdit = Map.of(
            "file_path", file.toString(),
            "edits", List.of(
                Map.of("old_string", "a", "new_string", "a".repeat(10)),
                Map.of("old_string", "b", "new_string", "b".repeat(10))));

        assertThat(ClaudeCodeWorkspaceQuotaHooks.checkFileWrite(quota, preToolUse("Edit", singleEdit))
            .decision()).isNull();
        assertThat(ClaudeCodeWorkspaceQuotaHooks.checkFileWrite(quota, preToolUse("MultiEdit", multiEdit))
            .decision()).isEqualTo("block");
    }

    @Test
    void testFileWriteHookMatchesNotebookEdit() {
        assertThat(Pattern.matches(ClaudeCodeWorkspaceQuotaHooks.FILE_WRITE_TOOL_NAMES, "NotebookEdit")).isTrue();
    }

    @Test
    void testCheckFileWriteDeniesANotebookEditThatGoesOverTheLimit() throws IOException {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(workspace, 100);

        Files.writeString(workspace.resolve("existing.txt"), "x".repeat(60));

        String notebookPath = workspace.resolve("analysis.ipynb")
            .toString();

        HookOutput denied = ClaudeCodeWorkspaceQuotaHooks.checkFileWrite(
            quota, preToolUse(
                "NotebookEdit",
                Map.of("notebook_path", notebookPath, "new_source", "y".repeat(50), "edit_mode", "insert")));
        HookOutput deleteCell = ClaudeCodeWorkspaceQuotaHooks.checkFileWrite(
            quota, preToolUse(
                "NotebookEdit",
                Map.of("notebook_path", notebookPath, "new_source", "y".repeat(50), "edit_mode", "delete")));

        assertThat(denied.decision()).isEqualTo("block");
        assertThat(deleteCell.decision()).isNull();
    }

    @Test
    void testCheckFileWriteIgnoresWritesOutsideTheWorkspace() throws IOException {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(workspace, 100);

        Files.writeString(workspace.resolve("existing.txt"), "x".repeat(60));

        HookOutput outside = ClaudeCodeWorkspaceQuotaHooks.checkFileWrite(
            quota, preToolUse("Write", Map.of("file_path", outsideDirectory.resolve("new.txt")
                .toString(), "content", "y".repeat(50))));
        HookOutput escaping = ClaudeCodeWorkspaceQuotaHooks.checkFileWrite(
            quota, preToolUse("Write", Map.of("file_path", "sub/../../escape.txt", "content", "y".repeat(50))));
        HookOutput relativeInside = ClaudeCodeWorkspaceQuotaHooks.checkFileWrite(
            quota, preToolUse("Write", Map.of("file_path", "new.txt", "content", "y".repeat(50))));

        assertThat(outside.decision()).isNull();
        assertThat(escaping.decision()).isNull();
        assertThat(relativeInside.decision()).isEqualTo("block");
    }

    @Test
    void testCheckCommandRunsOnlyCleanupCommandsWhileOverTheLimit() throws IOException {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(workspace, 100);

        HookOutput withinLimit = ClaudeCodeWorkspaceQuotaHooks.checkCommand(
            quota, preToolUse("Bash", Map.of("command", "git clone https://example.com/repo.git")));

        Files.writeString(workspace.resolve("big.bin"), "x".repeat(150));

        HookOutput refused = ClaudeCodeWorkspaceQuotaHooks.checkCommand(
            quota, preToolUse("Bash", Map.of("command", "git clone https://example.com/repo.git")));
        HookOutput cleanup = ClaudeCodeWorkspaceQuotaHooks.checkCommand(
            quota, preToolUse("Bash", Map.of("command", "rm big.bin")));

        assertThat(withinLimit.decision()).isNull();
        assertThat(refused.decision()).isEqualTo("block");
        assertThat(cleanup.decision()).isNull();
    }

    @Test
    void testReportOverLimitAddsANoteOnlyWhenOverTheLimit() throws IOException {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(workspace, 100);

        assertThat(ClaudeCodeWorkspaceQuotaHooks.reportOverLimit(quota)
            .decision()).isNull();

        Files.writeString(workspace.resolve("big.bin"), "x".repeat(150));

        assertThat(ClaudeCodeWorkspaceQuotaHooks.reportOverLimit(quota)
            .reason()).contains("over its limit");
    }

    private static PreToolUseInput preToolUse(String toolName, Map<String, Object> toolInput) {
        return new PreToolUseInput(
            "PreToolUse", "session", "transcript", "/workspace", "bypassPermissions", toolName, "tool-use",
            toolInput);
    }
}
