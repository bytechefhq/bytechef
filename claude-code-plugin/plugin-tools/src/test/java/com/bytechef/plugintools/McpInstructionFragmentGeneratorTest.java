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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class McpInstructionFragmentGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void testEmitsOneToolsSectionNamingAllOfAFencesTools() throws IOException {
        writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [mcp]
            ---

            <!-- transport: mcp uses: createProject, buildWorkflow -->
            Create the project, then build the workflow.
            <!-- /transport -->
            """);

        Path outputFile = tempDir.resolve("out/mcp-instructions.md");

        McpInstructionFragmentGenerator.generate(tempDir, outputFile);

        String content = Files.readString(outputFile);

        assertTrue(content.contains("## tools: createProject, buildWorkflow"));
    }

    @Test
    void testAFenceWithTwoUsesEntriesProducesExactlyOneSectionWithOneBody() throws IOException {
        writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [mcp]
            ---

            <!-- transport: mcp uses: createProject, buildWorkflow -->
            Create the project, then build the workflow.
            <!-- /transport -->
            """);

        Path outputFile = tempDir.resolve("out/mcp-instructions.md");

        McpInstructionFragmentGenerator.generate(tempDir, outputFile);

        String content = Files.readString(outputFile);

        assertEquals(1, countOccurrences(content, "## tools:"));
        assertEquals(1, countOccurrences(content, "Create the project, then build the workflow."));
    }

    @Test
    void testIgnoresCliAndLocalFences() throws IOException {
        writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [mcp, cli, local]
            ---

            <!-- transport: mcp uses: createProject -->
            Create the project.
            <!-- /transport -->

            <!-- transport: cli uses: component deploy -->
            Deploy the component from the shell. CLI_ONLY_MARKER
            <!-- /transport -->

            <!-- transport: local -->
            Do this directly in the checkout. LOCAL_ONLY_MARKER
            <!-- /transport -->
            """);

        Path outputFile = tempDir.resolve("out/mcp-instructions.md");

        McpInstructionFragmentGenerator.generate(tempDir, outputFile);

        String content = Files.readString(outputFile);

        assertFalse(content.contains("CLI_ONLY_MARKER"));
        assertFalse(content.contains("LOCAL_ONLY_MARKER"));
    }

    @Test
    void testEmitsASeparateSectionPerFenceEvenWhenFencesShareATool() throws IOException {
        writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [mcp]
            ---

            <!-- transport: mcp uses: buildWorkflow -->
            First body marker.
            <!-- /transport -->

            <!-- transport: mcp uses: buildWorkflow -->
            Second body marker.
            <!-- /transport -->
            """);

        Path outputFile = tempDir.resolve("out/mcp-instructions.md");

        McpInstructionFragmentGenerator.generate(tempDir, outputFile);

        String content = Files.readString(outputFile);

        assertEquals(2, countOccurrences(content, "## tools: buildWorkflow"));
        assertEquals(1, countOccurrences(content, "First body marker."));
        assertEquals(1, countOccurrences(content, "Second body marker."));
    }

    @Test
    void testAlwaysSectionIsPresentWithoutAGeneralFence() throws IOException {
        writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [mcp]
            ---

            <!-- transport: mcp uses: createProject -->
            Create the project.
            <!-- /transport -->
            """);

        Path outputFile = tempDir.resolve("out/mcp-instructions.md");

        McpInstructionFragmentGenerator.generate(tempDir, outputFile);

        String content = Files.readString(outputFile);

        assertTrue(content.contains("## always"));
        assertTrue(content.contains("ByteChef management server"));
    }

    @Test
    void testEmitsGeneratedFileBannerAsTheFirstLine() throws IOException {
        writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [mcp]
            ---

            <!-- transport: mcp uses: createProject -->
            Create the project.
            <!-- /transport -->
            """);

        Path outputFile = tempDir.resolve("out/mcp-instructions.md");

        McpInstructionFragmentGenerator.generate(tempDir, outputFile);

        List<String> lines = Files.readAllLines(outputFile);

        assertEquals(McpInstructionFragmentGenerator.GENERATED_FILE_BANNER, lines.getFirst());
    }

    @Test
    void testEmitsGeneralFenceBodyUnderAlwaysWithoutAToolsSection() throws IOException {
        writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [mcp]
            ---

            <!-- transport: mcp uses: general -->
            Special framing text.
            <!-- /transport -->
            """);

        Path outputFile = tempDir.resolve("out/mcp-instructions.md");

        McpInstructionFragmentGenerator.generate(tempDir, outputFile);

        String content = Files.readString(outputFile);

        assertTrue(content.contains("Special framing text."));
        assertFalse(content.contains("## tools: general"));
    }

    private Path writeSkill(String content) throws IOException {
        Path skillDirectory = tempDir.resolve("example-skill");

        Files.createDirectories(skillDirectory);

        Path skillFile = skillDirectory.resolve("SKILL.md");

        Files.writeString(skillFile, content);

        return skillFile;
    }

    private static int countOccurrences(String content, String needle) {
        int count = 0;
        int index = content.indexOf(needle);

        while (index != -1) {
            count++;
            index = content.indexOf(needle, index + needle.length());
        }

        return count;
    }
}
