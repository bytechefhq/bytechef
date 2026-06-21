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

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Provides file system tools (Read, Write, Edit) for the AI agent.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsFileSystemTools {

    public static final ClusterElementDefinition<ToolCallbackProviderFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ToolCallbackProviderFunction>clusterElement("fileSystemTools")
            .title("File System Tools")
            .description("Read, write, and edit files with precise control.")
            .type(TOOLS)
            .object(() -> AiAgentUtilsFileSystemTools::apply);

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private static ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        // Jail the agent's file read/write/edit to a per-run sandbox directory. Without an allowed directory the tool
        // operates on the raw filesystem, letting the agent (or a prompt-injection of it) read or overwrite any file
        // the server process can reach. FileSystemTools canonicalizes paths (toRealPath) and enforces the allowlist,
        // so '..'/symlink escapes are rejected.
        Path sandboxDirectory;

        try {
            sandboxDirectory = Files.createTempDirectory("bytechef-agent-fs-");
        } catch (IOException ioException) {
            throw new UncheckedIOException(
                "Failed to create a sandbox directory for the file system tools", ioException);
        }

        FileSystemTools fileSystemTools = FileSystemTools.builder()
            .allowedDirectory(sandboxDirectory)
            .build();

        return ToolCallbackProvider.from(ToolCallbacks.from(fileSystemTools));
    }
}
