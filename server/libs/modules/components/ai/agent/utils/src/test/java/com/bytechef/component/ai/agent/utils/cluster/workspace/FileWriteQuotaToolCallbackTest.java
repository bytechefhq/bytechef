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
class FileWriteQuotaToolCallbackTest {

    @TempDir
    private Path root;

    @Test
    void testWriteWithinTheLimitIsDelegated() {
        ToolCallback delegate = givenTool("Write");

        String result = call(delegate, 100, Map.of("filePath", "a.txt", "content", "12345"));

        assertThat(result).isEqualTo("done");
    }

    @Test
    void testWriteOverTheLimitIsRefused() throws IOException {
        Files.writeString(root.resolve("existing.txt"), "1234567890");

        ToolCallback delegate = givenTool("Write");

        String result = call(delegate, 12, Map.of("filePath", "a.txt", "content", "12345"));

        assertThat(result).startsWith("Error: the agent workspace is full: 10 bytes of 12 bytes used, this write " +
            "needs 5 bytes.");

        verify(delegate, never()).call(anyString());
    }

    @Test
    void testWriteThatOverwritesAFileCountsOnlyTheGrowth() throws IOException {
        Files.writeString(root.resolve("a.txt"), "1234567890");

        ToolCallback delegate = givenTool("Write");

        String result = call(delegate, 12, Map.of("filePath", "a.txt", "content", "123456789012"));

        assertThat(result).isEqualTo("done");
    }

    @Test
    void testEditThatGrowsTheFileOverTheLimitIsRefused() throws IOException {
        Files.writeString(root.resolve("a.txt"), "1234567890");

        ToolCallback delegate = givenTool("Edit");

        String result = call(
            delegate, 12, Map.of("filePath", "a.txt", "old_string", "1", "new_string", "one-two-three"));

        assertThat(result).contains("the agent workspace is full");

        verify(delegate, never()).call(anyString());
    }

    @Test
    void testEditThatShrinksTheFileIsDelegatedEvenOverTheLimit() throws IOException {
        Files.writeString(root.resolve("a.txt"), "1234567890");

        ToolCallback delegate = givenTool("Edit");

        String result = call(delegate, 5, Map.of("filePath", "a.txt", "old_string", "123456", "new_string", "1"));

        assertThat(result).isEqualTo("done");
    }

    @Test
    void testEditWithReplaceAllCountsEveryOccurrence() throws IOException {
        Files.writeString(root.resolve("a.txt"), "x.x.x.x.x.");

        ToolCallback delegate = givenTool("Edit");

        Map<String, Object> arguments = Map.of(
            "filePath", "a.txt", "old_string", "x", "new_string", "xy", "replace_all", true);

        assertThat(call(delegate, 14, arguments)).contains("the agent workspace is full");
        assertThat(call(delegate, 15, arguments)).isEqualTo("done");
    }

    @Test
    void testWrapLeavesReadAndUnlimitedWorkspacesUnchanged() {
        ToolCallback readTool = givenTool("Read");
        ToolCallback writeTool = givenTool("Write");

        assertThat(FileWriteQuotaToolCallback.wrap(readTool, new AgentWorkspaceQuota(root, 10))).isSameAs(readTool);
        assertThat(FileWriteQuotaToolCallback.wrap(writeTool, new AgentWorkspaceQuota(root, 0))).isSameAs(writeTool);
        assertThat(FileWriteQuotaToolCallback.wrap(writeTool, new AgentWorkspaceQuota(root, 10)))
            .isInstanceOf(FileWriteQuotaToolCallback.class);
    }

    private String call(ToolCallback delegate, long maxBytes, Map<String, Object> arguments) {
        ToolCallback toolCallback = FileWriteQuotaToolCallback.wrap(delegate, new AgentWorkspaceQuota(root, maxBytes));

        return toolCallback.call(JsonUtils.write(arguments));
    }

    private static ToolCallback givenTool(String name) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputSchema("{}")
                .build());
        when(toolCallback.call(anyString())).thenReturn("done");

        return toolCallback;
    }
}
