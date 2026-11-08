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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.ai.agent.utils.cluster.workspace.AgentWorkspaceResolver;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
class AiAgentUtilsFileSystemToolsTest {

    private static final Path AGENT_WORKSPACE = Path.of("public/automation/11/staging/21/default");

    @TempDir
    private Path basedir;

    @TempDir
    private Path outsideDirectory;

    private final Parameters connectionParameters = mock(Parameters.class);
    private final Parameters inputParameters = mock(Parameters.class);

    private long workspaceMaxBytes;

    @Test
    void testApplyExposesReadWriteAndEdit() throws Exception {
        assertThat(toolNames(applyTools())).containsExactlyInAnyOrder("Read", "Write", "Edit");
    }

    @Test
    void testApplyWithReadOnlyExposesOnlyRead() throws Exception {
        when(inputParameters.getBoolean(AiAgentUtilsFileSystemTools.READ_ONLY, false)).thenReturn(true);

        assertThat(toolNames(applyTools())).containsExactly("Read");
    }

    @Test
    void testReadInsideTheWorkspaceSucceeds() throws Exception {
        ToolCallback[] toolCallbacks = applyTools();

        Path file = Files.writeString(
            Files.createDirectories(basedir.resolve(AGENT_WORKSPACE))
                .resolve("notes.txt"),
            "workspace content");

        assertThat(callTool(toolCallbacks, "Read", Map.of("filePath", file.toString())))
            .contains("workspace content");
    }

    @Test
    void testReadOutsideTheWorkspaceIsDenied() throws Exception {
        Path secret = Files.writeString(outsideDirectory.resolve("secret.txt"), "server secret");

        String result = callTool(applyTools(), "Read", Map.of("filePath", secret.toString()));

        assertThat(result).contains("Access denied")
            .doesNotContain("server secret");
    }

    @Test
    void testReadOfAnotherTenantsWorkspaceIsDenied() throws Exception {
        Path otherTenantFile = Files.writeString(
            Files.createDirectories(basedir.resolve("other_tenant/automation/11/staging/21/default"))
                .resolve("notes.txt"),
            "other tenant content");

        String result = callTool(applyTools(), "Read", Map.of("filePath", otherTenantFile.toString()));

        assertThat(result).contains("Access denied")
            .doesNotContain("other tenant content");
    }

    @Test
    void testWriteWithParentTraversalIsDenied() throws Exception {
        Path workspace = basedir.resolve(AGENT_WORKSPACE);

        ToolCallback[] toolCallbacks = applyTools();

        String result = callTool(
            toolCallbacks, "Write",
            Map.of("filePath", workspace.resolve("../../escaped.txt")
                .toString(), "content", "x"));

        assertThat(result).contains("Access denied");
        assertThat(workspace.resolve("../../escaped.txt")
            .normalize()).doesNotExist();
    }

    @Test
    void testWriteWithARelativePathWritesInsideTheWorkspace() throws Exception {
        Path workspace = basedir.resolve(AGENT_WORKSPACE);

        String result = callTool(applyTools(), "Write", Map.of("filePath", "notes/hello.txt", "content", "hello"));

        assertThat(result).doesNotContain("Access denied");
        assertThat(workspace.resolve("notes/hello.txt")).hasContent("hello");
    }

    @Test
    void testWriteWithARelativeParentTraversalIsDenied() throws Exception {
        Path workspace = basedir.resolve(AGENT_WORKSPACE);

        String result = callTool(applyTools(), "Write", Map.of("filePath", "../../escaped.txt", "content", "x"));

        assertThat(result).contains("Access denied");
        assertThat(workspace.resolve("../../escaped.txt")
            .normalize()).doesNotExist();
    }

    @Test
    void testToolDescriptionsNameTheSandboxWorkspaceInsteadOfTheHostPath() throws Exception {
        assertThat(applyTools()).allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
            .description()).contains("The agent workspace is /workspace.")
                .doesNotContain(basedir.toString()));
    }

    @Test
    void testReadWithASandboxPathReadsFromTheWorkspace() throws Exception {
        ToolCallback[] toolCallbacks = applyTools();

        Files.writeString(
            Files.createDirectories(basedir.resolve(AGENT_WORKSPACE))
                .resolve("notes.txt"),
            "workspace content");

        String result = callTool(toolCallbacks, "Read", Map.of("filePath", "/workspace/notes.txt"));

        assertThat(result).contains("workspace content")
            .contains("/workspace/notes.txt")
            .doesNotContain(basedir.toString());
    }

    @Test
    void testWriteWithASandboxParentTraversalIsDeniedWithoutShowingTheHostPath() throws Exception {
        Path workspace = basedir.resolve(AGENT_WORKSPACE);

        String result = callTool(
            applyTools(), "Write", Map.of("filePath", "/workspace/../escaped.txt", "content", "x"));

        assertThat(result).contains("Access denied")
            .doesNotContain(basedir.toString());
        assertThat(workspace.resolve("../escaped.txt")
            .normalize()).doesNotExist();
    }

    @Test
    void testWriteOverTheWorkspaceSizeLimitIsRefused() throws Exception {
        Path workspace = basedir.resolve(AGENT_WORKSPACE);

        workspaceMaxBytes = 10;

        String result = callTool(
            applyTools(), "Write", Map.of("filePath", "/workspace/big.txt", "content", "more than ten bytes"));

        assertThat(result).contains("the agent workspace is full");
        assertThat(workspace.resolve("big.txt")).doesNotExist();
    }

    @Test
    void testWriteWithinTheWorkspaceSizeLimitSucceeds() throws Exception {
        Path workspace = basedir.resolve(AGENT_WORKSPACE);

        workspaceMaxBytes = 1024;

        String result = callTool(applyTools(), "Write", Map.of("filePath", "/workspace/small.txt", "content", "small"));

        assertThat(result).doesNotContain("the agent workspace is full");
        assertThat(workspace.resolve("small.txt")).hasContent("small");
    }

    private ToolCallback[] applyTools() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);
        when(actionContext.getJobPrincipalWorkflowId()).thenReturn(21L);

        ToolCallbackProviderFunction toolCallbackProviderFunction = new AiAgentUtilsFileSystemTools(
            new AgentWorkspaceResolver(basedir, workspaceMaxBytes)).clusterElementDefinition.getElement();

        return toolCallbackProviderFunction.apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();
    }

    private static String callTool(ToolCallback[] toolCallbacks, String toolName, Map<String, Object> arguments) {
        ToolCallback toolCallback = Arrays.stream(toolCallbacks)
            .filter(candidate -> toolName.equals(candidate.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();

        return toolCallback.call(JsonUtils.write(arguments));
    }

    private static String[] toolNames(ToolCallback[] toolCallbacks) {
        return Arrays.stream(toolCallbacks)
            .map(ToolCallback::getToolDefinition)
            .map(ToolDefinition::name)
            .toArray(String[]::new);
    }
}
