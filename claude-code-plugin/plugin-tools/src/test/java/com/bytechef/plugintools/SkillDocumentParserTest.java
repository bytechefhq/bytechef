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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class SkillDocumentParserTest {

    @TempDir
    Path tempDir;

    @Test
    void testParsesFrontmatterTransports() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [local]
              optional: [cli]
            ---

            Body.
            """);

        SkillDocument skillDocument = SkillDocumentParser.parse(skillFile);

        assertEquals("Example", skillDocument.name());
        assertEquals(Set.of(Transport.LOCAL), skillDocument.requiredTransports());
        assertEquals(Set.of(Transport.CLI), skillDocument.optionalTransports());
    }

    @Test
    void testParsesFenceWithUsesAndDefaultEdition() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [mcp]
            ---

            <!-- transport: mcp uses: createProjectWorkflow, getWorkflow -->
            Call createProjectWorkflow first.
            <!-- /transport -->
            """);

        SkillDocument skillDocument = SkillDocumentParser.parse(skillFile);
        TransportFence fence = skillDocument.fences()
            .getFirst();

        assertEquals(Transport.MCP, fence.transport());
        assertEquals(Edition.CE, fence.edition());
        assertEquals(List.of("createProjectWorkflow", "getWorkflow"), fence.uses());
        assertTrue(fence.body()
            .contains("Call createProjectWorkflow first."));
    }

    @Test
    void testParsesEeEdition() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [cli]
            ---

            <!-- transport: cli uses: bytechef-project-init edition: ee -->
            Run the CLI command.
            <!-- /transport -->
            """);

        SkillDocument skillDocument = SkillDocumentParser.parse(skillFile);
        TransportFence fence = skillDocument.fences()
            .getFirst();

        assertEquals(Edition.EE, fence.edition());
    }

    @Test
    void testLocalFenceMayOmitUses() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [local]
            ---

            <!-- transport: local -->
            Do this directly in the checkout.
            <!-- /transport -->
            """);

        SkillDocument skillDocument = SkillDocumentParser.parse(skillFile);
        TransportFence fence = skillDocument.fences()
            .getFirst();

        assertEquals(Transport.LOCAL, fence.transport());
        assertTrue(fence.uses()
            .isEmpty());
    }

    @Test
    void testMcpFenceWithoutUsesIsRejected() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [mcp]
            ---

            <!-- transport: mcp -->
            Call something.
            <!-- /transport -->
            """);

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> SkillDocumentParser.parse(skillFile));

        assertTrue(exception.getMessage()
            .contains("uses"));
    }

    @Test
    void testUnknownTransportIsRejected() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [local]
            ---

            <!-- transport: http -->
            Body.
            <!-- /transport -->
            """);

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> SkillDocumentParser.parse(skillFile));

        assertTrue(exception.getMessage()
            .contains("http"));
    }

    @Test
    void testUnclosedFenceIsRejected() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [local]
            ---

            <!-- transport: local -->
            Body with no closing fence.
            """);

        assertThrows(IllegalArgumentException.class, () -> SkillDocumentParser.parse(skillFile));
    }

    @Test
    void testUnclosedFrontmatterIsRejected() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [local]

            Body with no closing frontmatter delimiter.
            """);

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> SkillDocumentParser.parse(skillFile));

        assertTrue(exception.getMessage()
            .contains("line 1"));
    }

    @Test
    void testNestedFenceIsRejected() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [local]
            ---

            <!-- transport: local -->
            Outer body.
            <!-- transport: local -->
            Inner body.
            <!-- /transport -->
            <!-- /transport -->
            """);

        assertThrows(IllegalArgumentException.class, () -> SkillDocumentParser.parse(skillFile));
    }

    @Test
    void testFenceSpanningAHeadingIsRejected() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [local]
            ---

            <!-- transport: local -->
            Body before the heading.
            # A heading
            Body after the heading.
            <!-- /transport -->
            """);

        assertThrows(IllegalArgumentException.class, () -> SkillDocumentParser.parse(skillFile));
    }

    @Test
    void testFenceWithHashCommentInsideCodeBlockIsAccepted() throws IOException {
        Path skillFile = writeSkill("""
            ---
            name: Example
            description: An example.
            transports:
              required: [cli]
            ---

            <!-- transport: cli uses: automation project deploy -->
            ```bash
            # Automation project — via the CLI (workspaceId optional; defaults server-side)
            bytechef automation project deploy --project-file my-code-project.js --workspace-id 1049
            ```
            <!-- /transport -->
            """);

        SkillDocument skillDocument = SkillDocumentParser.parse(skillFile);
        TransportFence fence = skillDocument.fences()
            .getFirst();

        assertTrue(fence.body()
            .contains("# Automation project — via the CLI (workspaceId optional; defaults server-side)"));
    }

    @Test
    void testReferenceFileWithoutFrontmatterIsValid() throws IOException {
        Path referenceFile = tempDir.resolve("detail.md");

        Files.writeString(referenceFile, """
            # Reference

            Some explanatory prose with no frontmatter block.

            <!-- transport: local -->
            Do this directly in the checkout.
            <!-- /transport -->
            """);

        SkillDocument skillDocument = SkillDocumentParser.parse(referenceFile);

        assertEquals("", skillDocument.name());
        assertEquals("", skillDocument.description());
        assertTrue(skillDocument.requiredTransports()
            .isEmpty());
        assertTrue(skillDocument.optionalTransports()
            .isEmpty());

        TransportFence fence = skillDocument.fences()
            .getFirst();

        assertEquals(Transport.LOCAL, fence.transport());
    }

    @Test
    void testParseAllFindsSkillAndReferenceFiles() throws IOException {
        Path skillDirectory = tempDir.resolve("one");

        Files.createDirectories(skillDirectory);
        Files.writeString(skillDirectory.resolve("SKILL.md"), """
            ---
            name: One
            description: A skill.
            transports:
              required: [local]
            ---

            Body.
            """);

        Path referencesDirectory = skillDirectory.resolve("references");

        Files.createDirectories(referencesDirectory);
        Files.writeString(referencesDirectory.resolve("detail.md"), """
            ---
            name: Detail
            description: A reference.
            transports:
              required: [local]
            ---

            Reference body.
            """);

        List<SkillDocument> skillDocuments = SkillDocumentParser.parseAll(tempDir);

        assertEquals(2, skillDocuments.size());
    }

    @Test
    void testParseAllOnEmptyDirectoryReturnsEmptyList() throws IOException {
        List<SkillDocument> skillDocuments = SkillDocumentParser.parseAll(tempDir);

        assertTrue(skillDocuments.isEmpty());
    }

    private Path writeSkill(String content) throws IOException {
        Path skillFile = tempDir.resolve("SKILL.md");

        Files.writeString(skillFile, content);

        return skillFile;
    }
}
