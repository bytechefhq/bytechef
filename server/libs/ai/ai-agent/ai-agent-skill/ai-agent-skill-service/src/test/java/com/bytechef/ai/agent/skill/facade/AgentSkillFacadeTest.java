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

package com.bytechef.ai.agent.skill.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.skill.domain.AgentSkill;
import com.bytechef.ai.agent.skill.file.storage.AgentSkillFileStorage;
import com.bytechef.ai.agent.skill.service.AgentSkillService;
import com.bytechef.file.storage.domain.FileEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AgentSkillFacadeTest {

    @Mock
    private AgentSkillFileStorage agentSkillFileStorage;

    @Mock
    private AgentSkillService agentSkillService;

    @InjectMocks
    private AgentSkillFacadeImpl agentSkillFacade;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void testCreateAgentSkill() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setName("Test Skill");
        expectedAgentSkill.setDescription("A test skill");
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(agentSkillFileStorage.storeAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        AgentSkill result = agentSkillFacade.createAgentSkill(
            "Test Skill", "A test skill", "test.skill", zipBytes);

        assertEquals(expectedAgentSkill, result);

        verify(agentSkillFileStorage).storeAgentSkillFile(eq("test.skill"), eq(zipBytes));
        verify(agentSkillService).createAgentSkill(argThat(agentSkill -> "Test Skill".equals(agentSkill.getName())
            && "A test skill".equals(agentSkill.getDescription())));
    }

    @Test
    void testCreateAgentSkillWithFrontmatterOverride() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setName("Frontmatter Name");
        expectedAgentSkill.setDescription("Frontmatter Description");
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        String skillMdContent =
            "---\nname: Frontmatter Name\ndescription: Frontmatter Description\n---\n\nInstructions";

        byte[] zipBytes = createZipBytes("SKILL.md", skillMdContent);

        when(agentSkillFileStorage.storeAgentSkillFile(eq("test.skill"), any(byte[].class))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        agentSkillFacade.createAgentSkill("Original Name", "Original Description", "test.skill", zipBytes);

        verify(agentSkillService).createAgentSkill(
            argThat(agentSkill -> "Frontmatter Name".equals(agentSkill.getName())
                && "Frontmatter Description".equals(agentSkill.getDescription())));
    }

    @Test
    void testCreateAgentSkillFromInstructions() throws IOException {
        FileEntry fileEntry = new FileEntry("mySkill.skill", "file:///storage/mySkill.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setName("mySkill");
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        when(agentSkillFileStorage.storeAgentSkillFile(eq("mySkill.skill"), any(byte[].class))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        AgentSkill result = agentSkillFacade.createAgentSkillFromInstructions(
            "mySkill", "My description", "Do something useful");

        assertEquals(expectedAgentSkill, result);

        verify(agentSkillFileStorage).storeAgentSkillFile(eq("mySkill.skill"), argThat(zipBytes -> {
            try (ZipInputStream zipInputStream =
                new ZipInputStream(new ByteArrayInputStream(zipBytes))) {

                ZipEntry zipEntry = zipInputStream.getNextEntry();

                if (zipEntry == null || !"SKILL.md".equals(zipEntry.getName())) {
                    return false;
                }

                String content = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);

                return content.contains("---") &&
                    content.contains("name: \"mySkill\"") &&
                    content.contains("description: \"My description\"") &&
                    content.contains("Do something useful");
            } catch (IOException ioException) {
                return false;
            }
        }));
    }

    @Test
    void testCreateAgentSkillWithBlankNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.createAgentSkill("", "desc", "test.skill", new byte[0]));
    }

    @Test
    void testCreateAgentSkillFromInstructionsWithBlankNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.createAgentSkillFromInstructions("", "desc", "instructions"));
    }

    @Test
    void testCreateAgentSkillFromInstructionsWithBlankInstructionsThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.createAgentSkillFromInstructions("name", "desc", ""));
    }

    @Test
    void testCreateAgentSkillGeneratesUniqueNameOnConflict() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(agentSkillService.existsByName("Duplicate")).thenReturn(true);
        when(agentSkillService.existsByName("Duplicate (2)")).thenReturn(false);
        when(agentSkillFileStorage.storeAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        agentSkillFacade.createAgentSkill("Duplicate", null, "test.skill", zipBytes);

        verify(agentSkillService).createAgentSkill(
            argThat(agentSkill -> "Duplicate (2)".equals(agentSkill.getName())));
    }

    @Test
    void testDeleteAgentSkillDeletesDbThenFileAfterCommit() {
        long skillId = 42L;

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill agentSkill = new AgentSkill();

        agentSkill.setId(skillId);
        agentSkill.setName("Test Skill");
        agentSkill.setSkillFileEntry(fileEntry);

        when(agentSkillService.getAgentSkill(skillId)).thenReturn(agentSkill);

        agentSkillFacade.deleteAgentSkill(skillId);

        verify(agentSkillService).deleteAgentSkill(skillId);

        List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertEquals(1, synchronizations.size());

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        InOrder deleteOrder = inOrder(agentSkillService, agentSkillFileStorage);

        deleteOrder.verify(agentSkillService)
            .deleteAgentSkill(skillId);
        deleteOrder.verify(agentSkillFileStorage)
            .deleteAgentSkillFile(fileEntry);
    }

    @Test
    void testGetAgentSkillFileContent() {
        long skillId = 1L;

        byte[] zipBytes = createZipBytes("docs/README.md", "Hello from the skill!");

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill agentSkill = new AgentSkill();

        agentSkill.setId(skillId);
        agentSkill.setName("Test Skill");
        agentSkill.setSkillFileEntry(fileEntry);

        when(agentSkillService.getAgentSkill(skillId)).thenReturn(agentSkill);
        when(agentSkillFileStorage.readAgentSkillFileBytes(fileEntry)).thenReturn(zipBytes);

        String content = agentSkillFacade.getAgentSkillFileContent(skillId, "docs/README.md");

        assertEquals("Hello from the skill!", content);
    }

    @Test
    void testGetAgentSkillFileContentWithPathTraversal() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.getAgentSkillFileContent(1L, "../etc/passwd"));

        assertTrue(exception.getMessage()
            .contains("path traversal"));
    }

    @Test
    void testGetAgentSkillFilePaths() {
        long skillId = 1L;

        byte[] zipBytes = createZipBytesWithEntries(
            new String[] {
                "SKILL.md", "src/", "src/main.py", "../sneaky.txt", "/absolute.txt", "docs/guide.md"
            });

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill agentSkill = new AgentSkill();

        agentSkill.setId(skillId);
        agentSkill.setName("Test Skill");
        agentSkill.setSkillFileEntry(fileEntry);

        when(agentSkillService.getAgentSkill(skillId)).thenReturn(agentSkill);
        when(agentSkillFileStorage.readAgentSkillFileBytes(fileEntry)).thenReturn(zipBytes);

        List<String> paths = agentSkillFacade.getAgentSkillFilePaths(skillId);

        assertEquals(List.of("SKILL.md", "src/main.py", "docs/guide.md"), paths);
    }

    @Test
    void testUpdateAgentSkill() {
        long skillId = 1L;

        AgentSkill updatedAgentSkill = new AgentSkill();

        updatedAgentSkill.setId(skillId);
        updatedAgentSkill.setName("Updated Name");
        updatedAgentSkill.setDescription("Updated Description");

        when(agentSkillService.updateAgentSkill(skillId, "Updated Name", "Updated Description"))
            .thenReturn(updatedAgentSkill);

        AgentSkill result = agentSkillFacade.updateAgentSkill(skillId, "Updated Name", "Updated Description");

        assertEquals("Updated Name", result.getName());

        verify(agentSkillService).updateAgentSkill(skillId, "Updated Name", "Updated Description");
    }

    @Test
    void testUpdateAgentSkillWithBlankNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.updateAgentSkill(1L, "", "description"));
    }

    @Test
    void testToSkillFilenameConvertsZipToSkill() {
        FileEntry fileEntry = new FileEntry("uploaded.skill", "file:///storage/uploaded.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setName("Test Skill");
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(agentSkillFileStorage.storeAgentSkillFile(eq("uploaded.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        agentSkillFacade.createAgentSkill("Test Skill", null, "uploaded.zip", zipBytes);

        verify(agentSkillFileStorage).storeAgentSkillFile(eq("uploaded.skill"), eq(zipBytes));
    }

    @Test
    void testCreateAgentSkillRejectsOversizedFile() {
        byte[] oversizedBytes = new byte[10 * 1024 * 1024 + 1];

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.createAgentSkill("Test", null, "test.zip", oversizedBytes));

        assertTrue(exception.getMessage()
            .contains("maximum allowed size"));
    }

    @Test
    void testCreateAgentSkillRejectsInvalidZip() {
        byte[] notAZip = "this is not a zip file".getBytes(StandardCharsets.UTF_8);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.createAgentSkill("Test", null, "test.zip", notAZip));

        assertTrue(exception.getMessage()
            .contains("not a valid zip archive"));
    }

    @Test
    void testCreateAgentSkillFromInstructionsEscapesYamlSpecialCharacters() throws IOException {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        when(agentSkillFileStorage.storeAgentSkillFile(any(), any(byte[].class))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        agentSkillFacade.createAgentSkillFromInstructions(
            "evil\n---\ninjected: true", "desc with \"quotes\"", "instructions");

        verify(agentSkillFileStorage).storeAgentSkillFile(any(), argThat(zipBytes -> {
            try (ZipInputStream zipInputStream =
                new ZipInputStream(new ByteArrayInputStream(zipBytes))) {

                ZipEntry zipEntry = zipInputStream.getNextEntry();

                if (zipEntry == null || !"SKILL.md".equals(zipEntry.getName())) {
                    return false;
                }

                String content = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);

                // The newlines in the name should be escaped to literal \n inside quotes
                // so "injected: true" does NOT appear as a standalone YAML key-value pair
                return content.contains("name: \"evil\\n---\\ninjected: true\"") &&
                    !content.contains("\ninjected: true");
            } catch (IOException ioException) {
                return false;
            }
        }));
    }

    @Test
    void testUpdateAgentSkillDuplicateNameThrows() {
        when(agentSkillService.updateAgentSkill(1L, "Existing Name", "description"))
            .thenThrow(new DataIntegrityViolationException(
                "unique constraint violated",
                new SQLException("duplicate key value violates unique constraint", "23505")));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.updateAgentSkill(1L, "Existing Name", "description"));

        assertTrue(exception.getMessage()
            .contains("already exists"));
    }

    @Test
    void testCreateAgentSkillCleansUpFileOnRollback() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setName("Test Skill");
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(agentSkillFileStorage.storeAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        agentSkillFacade.createAgentSkill("Test Skill", null, "test.skill", zipBytes);

        List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertEquals(1, synchronizations.size());

        synchronizations.forEach(
            synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(agentSkillFileStorage).deleteAgentSkillFile(fileEntry);
    }

    @Test
    void testCreateAgentSkillRetriesOnDataIntegrityViolation() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setName("Test Skill (2)");
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(agentSkillFileStorage.storeAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(agentSkillService.existsByName("Test Skill"))
            .thenReturn(false)
            .thenReturn(true);
        when(agentSkillService.existsByName("Test Skill (2)")).thenReturn(false);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class)))
            .thenThrow(new DataIntegrityViolationException(
                "unique constraint violated",
                new SQLException("duplicate key value violates unique constraint", "23505")))
            .thenReturn(expectedAgentSkill);

        AgentSkill result = agentSkillFacade.createAgentSkill("Test Skill", null, "test.skill", zipBytes);

        assertEquals(expectedAgentSkill, result);

        verify(agentSkillService).existsByName("Test Skill (2)");
    }

    @Test
    void testUpdateAgentSkillNonNameConstraintViolationPropagates() {
        when(agentSkillService.updateAgentSkill(1L, "Valid Name", "description"))
            .thenThrow(new DataIntegrityViolationException(
                "foreign key constraint violated",
                new SQLException("referential integrity violation", "23503")));

        assertThrows(
            DataIntegrityViolationException.class,
            () -> agentSkillFacade.updateAgentSkill(1L, "Valid Name", "description"));
    }

    @Test
    void testCreateAgentSkillWithNoFrontmatterUsesProvidedName() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setName("Provided Name");
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "Just some instructions without frontmatter");

        when(agentSkillFileStorage.storeAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        agentSkillFacade.createAgentSkill("Provided Name", "Provided Desc", "test.skill", zipBytes);

        verify(agentSkillService).createAgentSkill(
            argThat(agentSkill -> "Provided Name".equals(agentSkill.getName())
                && "Provided Desc".equals(agentSkill.getDescription())));
    }

    @Test
    void testCreateAgentSkillWithUnclosedFrontmatterUsesProvidedName() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setName("Provided Name");
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "---\nname: Unclosed\nInstructions without closing delimiter");

        when(agentSkillFileStorage.storeAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        agentSkillFacade.createAgentSkill("Provided Name", "Provided Desc", "test.skill", zipBytes);

        verify(agentSkillService).createAgentSkill(
            argThat(agentSkill -> "Provided Name".equals(agentSkill.getName())
                && "Provided Desc".equals(agentSkill.getDescription())));
    }

    @Test
    void testGenerateUniqueNameTimestampFallback() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill expectedAgentSkill = new AgentSkill();

        expectedAgentSkill.setId(1L);
        expectedAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(agentSkillService.existsByName("Popular")).thenReturn(true);

        for (int suffix = 2; suffix <= 100; suffix++) {
            when(agentSkillService.existsByName("Popular (" + suffix + ")")).thenReturn(true);
        }

        when(agentSkillFileStorage.storeAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(agentSkillService.createAgentSkill(any(AgentSkill.class))).thenReturn(expectedAgentSkill);

        agentSkillFacade.createAgentSkill("Popular", null, "test.skill", zipBytes);

        verify(agentSkillService).createAgentSkill(
            argThat(agentSkill -> agentSkill.getName()
                .matches("Popular \\(\\d{13,}\\)")));
    }

    @Test
    void testGetAgentSkillFileContentWithAbsolutePathThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.getAgentSkillFileContent(1L, "/etc/passwd"));

        assertTrue(exception.getMessage()
            .contains("path traversal"));
    }

    @Test
    void testGetAgentSkillFileContentFileNotFound() {
        long skillId = 1L;

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AgentSkill agentSkill = new AgentSkill();

        agentSkill.setId(skillId);
        agentSkill.setName("Test Skill");
        agentSkill.setSkillFileEntry(fileEntry);

        when(agentSkillService.getAgentSkill(skillId)).thenReturn(agentSkill);
        when(agentSkillFileStorage.readAgentSkillFileBytes(fileEntry)).thenReturn(zipBytes);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> agentSkillFacade.getAgentSkillFileContent(skillId, "nonexistent.txt"));

        assertTrue(exception.getMessage()
            .contains("File not found"));
    }

    private byte[] createZipBytes(String entryName, String content) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to create test zip", ioException);
        }
    }

    private byte[] createZipBytesWithEntries(String[] entryNames) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            for (String entryName : entryNames) {
                zipOutputStream.putNextEntry(new ZipEntry(entryName));

                if (!entryName.endsWith("/")) {
                    zipOutputStream.write(("content of " + entryName).getBytes(StandardCharsets.UTF_8));
                }

                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to create test zip", ioException);
        }
    }
}
