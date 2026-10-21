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

package com.bytechef.plugintools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that the committed management MCP server instructions resource matches what
 * {@link McpInstructionFragmentGenerator} produces from the plugin skills as they stand today. The generator is
 * deliberately manual and not wired into {@code compileJava}, so nothing else catches a skill edit that was not
 * followed by a regeneration of the committed resource.
 *
 * @author Ivica Cardic
 */
class McpInstructionsStalenessTest {

    private static final String SKILLS_DIRECTORY = "claude-code-plugin/bytechef-dev/skills";

    private static final String COMMITTED_RESOURCE =
        "server/libs/ai/ai-mcp/ai-mcp-server/src/main/resources/bytechef/mcp-instructions.md";

    private static final String REGENERATE_COMMAND =
        "./gradlew :claude-code-plugin:plugin-tools:generateMcpInstructions";

    @TempDir
    Path tempDir;

    @Test
    void testGeneratedMcpInstructionsMatchTheCommittedResource() throws IOException {
        Path repositoryRoot = repositoryRoot();
        Path outputFile = tempDir.resolve("mcp-instructions.md");

        McpInstructionFragmentGenerator.generate(repositoryRoot.resolve(SKILLS_DIRECTORY), outputFile);

        String generatedContent = Files.readString(outputFile);
        String committedContent = Files.readString(repositoryRoot.resolve(COMMITTED_RESOURCE));

        assertEquals(generatedContent, committedContent,
            "The committed " + COMMITTED_RESOURCE + " is stale relative to the plugin skills. Regenerate it with "
                + REGENERATE_COMMAND);
    }

    private static Path repositoryRoot() {
        Path directory = Path.of("")
            .toAbsolutePath();

        while (directory != null && !Files.exists(directory.resolve("settings.gradle.kts"))) {
            directory = directory.getParent();
        }

        if (directory == null) {
            throw new IllegalStateException("Could not locate the repository root from " + Path.of("")
                .toAbsolutePath());
        }

        return directory;
    }
}
