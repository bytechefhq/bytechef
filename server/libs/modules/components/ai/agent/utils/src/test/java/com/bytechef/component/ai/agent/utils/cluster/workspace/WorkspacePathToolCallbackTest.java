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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkspacePathToolCallbackTest {

    @TempDir
    private Path basedir;

    private final ToolCallback delegate = mock(ToolCallback.class);

    @Test
    void testDescriptionNamesTheSandboxRootInsteadOfTheHostPath() {
        AgentWorkspace workspace = givenWorkspace("Lists files in " + basedir.resolve("default"));

        String description = createToolCallback(workspace).getToolDefinition()
            .description();

        assertThat(description).contains("Lists files in /workspace")
            .contains("The agent workspace is /workspace. " + WorkspacePathToolCallback.FILES_DESCRIPTION)
            .doesNotContain(basedir.toString());
    }

    @Test
    void testCallMapsPathAndPatternArgumentsToTheHostWorkspace() {
        AgentWorkspace workspace = givenWorkspace("Searches files");
        Path root = workspace.root();

        when(delegate.call(anyString())).thenReturn("ok");

        createToolCallback(workspace).call(
            JsonUtils.write(Map.of("path", "/workspace/src", "pattern", "**/*.java", "glob", "/workspace/**/*.md")));

        ArgumentCaptor<String> toolInputArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(delegate).call(toolInputArgumentCaptor.capture());

        Map<String, String> arguments = JsonUtils.readMap(toolInputArgumentCaptor.getValue(), String.class);

        assertThat(arguments).containsEntry("path", root.resolve("src")
            .toString())
            .containsEntry("pattern", "**/*.java")
            .containsEntry("glob", root.resolve("**/*.md")
                .toString());
    }

    @Test
    void testCallShowsResultsWithTheSandboxRoot() {
        AgentWorkspace workspace = givenWorkspace("Reads files");
        Path root = workspace.root();

        when(delegate.call(anyString()))
            .thenReturn("Error: Access denied. Path is outside the allowed directories: " + root + "/../secret.txt");

        String result = createToolCallback(workspace).call(JsonUtils.write(Map.of("path", "/workspace/../secret.txt")));

        assertThat(result).isEqualTo(
            "Error: Access denied. Path is outside the allowed directories: /workspace/../secret.txt");
    }

    @Test
    void testCallPassesInputUnchangedWhenNoPathArgumentsAreConfigured() {
        AgentWorkspace workspace = givenWorkspace("Runs commands");

        when(delegate.call(anyString())).thenReturn("done");

        String toolInput = JsonUtils.write(Map.of("command", "ls /workspace"));

        new WorkspacePathToolCallback(delegate, workspace, "Commands start in it.", Set.of(), Set.of())
            .call(toolInput);

        verify(delegate).call(toolInput);
    }

    private WorkspacePathToolCallback createToolCallback(AgentWorkspace workspace) {
        return new WorkspacePathToolCallback(
            delegate, workspace, WorkspacePathToolCallback.FILES_DESCRIPTION, Set.of("path"), Set.of("glob"));
    }

    private AgentWorkspace givenWorkspace(String delegateDescription) {
        when(delegate.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("Tool")
                .description(delegateDescription)
                .inputSchema("{}")
                .build());

        return AgentWorkspace.sandboxed(basedir.resolve("default"));
    }
}
