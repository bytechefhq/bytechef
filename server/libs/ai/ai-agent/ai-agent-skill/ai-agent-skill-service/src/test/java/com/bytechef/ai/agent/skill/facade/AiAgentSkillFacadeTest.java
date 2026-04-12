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

import com.bytechef.ai.agent.skill.domain.AiAgentSkill;
import com.bytechef.ai.agent.skill.file.storage.AiAgentSkillFileStorage;
import com.bytechef.ai.agent.skill.service.AiAgentSkillService;
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
class AiAgentSkillFacadeTest {

    @Mock
    private AiAgentSkillFileStorage aiAgentSkillFileStorage;

    @Mock
    private AiAgentSkillService aiAgentSkillService;

    @InjectMocks
    private AiAgentSkillFacadeImpl aiAgentSkillFacade;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void testCreateAiAgentSkill() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setName("Test Skill");
        expectedAiAgentSkill.setDescription("A test skill");
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        AiAgentSkill result = aiAgentSkillFacade.createAiAgentSkill(
            "Test Skill", "A test skill", "test.skill", zipBytes);

        assertEquals(expectedAiAgentSkill, result);

        verify(aiAgentSkillFileStorage).storeAiAgentSkillFile(eq("test.skill"), eq(zipBytes));
        verify(aiAgentSkillService)
            .createAiAgentSkill(argThat(aiAgentSkill -> "Test Skill".equals(aiAgentSkill.getName())
                && "A test skill".equals(aiAgentSkill.getDescription())));
    }

    @Test
    void testCreateAiAgentSkillWithFrontmatterOverride() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setName("Frontmatter Name");
        expectedAiAgentSkill.setDescription("Frontmatter Description");
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        String skillMdContent =
            "---\nname: Frontmatter Name\ndescription: Frontmatter Description\n---\n\nInstructions";

        byte[] zipBytes = createZipBytes("SKILL.md", skillMdContent);

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("test.skill"), any(byte[].class))).thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        aiAgentSkillFacade.createAiAgentSkill("Original Name", "Original Description", "test.skill", zipBytes);

        verify(aiAgentSkillService).createAiAgentSkill(
            argThat(aiAgentSkill -> "Frontmatter Name".equals(aiAgentSkill.getName())
                && "Frontmatter Description".equals(aiAgentSkill.getDescription())));
    }

    @Test
    void testCreateAiAgentSkillFromInstructions() throws IOException {
        FileEntry fileEntry = new FileEntry("mySkill.skill", "file:///storage/mySkill.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setName("mySkill");
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("mySkill.skill"), any(byte[].class)))
            .thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        AiAgentSkill result = aiAgentSkillFacade.createAiAgentSkillFromInstructions(
            "mySkill", "My description", "Do something useful");

        assertEquals(expectedAiAgentSkill, result);

        verify(aiAgentSkillFileStorage).storeAiAgentSkillFile(eq("mySkill.skill"), argThat(zipBytes -> {
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
    void testCreateAiAgentSkillWithBlankNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.createAiAgentSkill("", "desc", "test.skill", new byte[0]));
    }

    @Test
    void testCreateAiAgentSkillFromInstructionsWithBlankNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.createAiAgentSkillFromInstructions("", "desc", "instructions"));
    }

    @Test
    void testCreateAiAgentSkillFromInstructionsWithBlankInstructionsThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.createAiAgentSkillFromInstructions("name", "desc", ""));
    }

    @Test
    void testCreateAiAgentSkillGeneratesUniqueNameOnConflict() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiAgentSkillService.existsByName("Duplicate")).thenReturn(true);
        when(aiAgentSkillService.existsByName("Duplicate (2)")).thenReturn(false);
        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        aiAgentSkillFacade.createAiAgentSkill("Duplicate", null, "test.skill", zipBytes);

        verify(aiAgentSkillService).createAiAgentSkill(
            argThat(aiAgentSkill -> "Duplicate (2)".equals(aiAgentSkill.getName())));
    }

    @Test
    void testDeleteAiAgentSkillDeletesDbThenFileAfterCommit() {
        long skillId = 42L;

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill aiAgentSkill = new AiAgentSkill();

        aiAgentSkill.setId(skillId);
        aiAgentSkill.setName("Test Skill");
        aiAgentSkill.setSkillFileEntry(fileEntry);

        when(aiAgentSkillService.getAiAgentSkill(skillId)).thenReturn(aiAgentSkill);

        aiAgentSkillFacade.deleteAiAgentSkill(skillId);

        verify(aiAgentSkillService).deleteAiAgentSkill(skillId);

        List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertEquals(1, synchronizations.size());

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        InOrder deleteOrder = inOrder(aiAgentSkillService, aiAgentSkillFileStorage);

        deleteOrder.verify(aiAgentSkillService)
            .deleteAiAgentSkill(skillId);
        deleteOrder.verify(aiAgentSkillFileStorage)
            .deleteAiAgentSkillFile(fileEntry);
    }

    @Test
    void testGetAiAgentSkillFileContent() {
        long skillId = 1L;

        byte[] zipBytes = createZipBytes("docs/README.md", "Hello from the skill!");

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill aiAgentSkill = new AiAgentSkill();

        aiAgentSkill.setId(skillId);
        aiAgentSkill.setName("Test Skill");
        aiAgentSkill.setSkillFileEntry(fileEntry);

        when(aiAgentSkillService.getAiAgentSkill(skillId)).thenReturn(aiAgentSkill);
        when(aiAgentSkillFileStorage.readAiAgentSkillFileBytes(fileEntry)).thenReturn(zipBytes);

        String content = aiAgentSkillFacade.getAiAgentSkillFileContent(skillId, "docs/README.md");

        assertEquals("Hello from the skill!", content);
    }

    @Test
    void testGetAiAgentSkillFileContentWithPathTraversal() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.getAiAgentSkillFileContent(1L, "../etc/passwd"));

        assertTrue(exception.getMessage()
            .contains("path traversal"));
    }

    @Test
    void testGetAiAgentSkillFilePaths() {
        long skillId = 1L;

        byte[] zipBytes = createZipBytesWithEntries(
            new String[] {
                "SKILL.md", "src/", "src/main.py", "../sneaky.txt", "/absolute.txt", "docs/guide.md"
            });

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill aiAgentSkill = new AiAgentSkill();

        aiAgentSkill.setId(skillId);
        aiAgentSkill.setName("Test Skill");
        aiAgentSkill.setSkillFileEntry(fileEntry);

        when(aiAgentSkillService.getAiAgentSkill(skillId)).thenReturn(aiAgentSkill);
        when(aiAgentSkillFileStorage.readAiAgentSkillFileBytes(fileEntry)).thenReturn(zipBytes);

        List<String> paths = aiAgentSkillFacade.getAiAgentSkillFilePaths(skillId);

        assertEquals(List.of("SKILL.md", "src/main.py", "docs/guide.md"), paths);
    }

    @Test
    void testUpdateAiAgentSkill() {
        long skillId = 1L;

        AiAgentSkill updatedAiAgentSkill = new AiAgentSkill();

        updatedAiAgentSkill.setId(skillId);
        updatedAiAgentSkill.setName("Updated Name");
        updatedAiAgentSkill.setDescription("Updated Description");

        when(aiAgentSkillService.updateAiAgentSkill(skillId, "Updated Name", "Updated Description"))
            .thenReturn(updatedAiAgentSkill);

        AiAgentSkill result = aiAgentSkillFacade.updateAiAgentSkill(skillId, "Updated Name", "Updated Description");

        assertEquals("Updated Name", result.getName());

        verify(aiAgentSkillService).updateAiAgentSkill(skillId, "Updated Name", "Updated Description");
    }

    @Test
    void testUpdateAiAgentSkillWithBlankNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.updateAiAgentSkill(1L, "", "description"));
    }

    @Test
    void testToSkillFilenameConvertsZipToSkill() {
        FileEntry fileEntry = new FileEntry("uploaded.skill", "file:///storage/uploaded.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setName("Test Skill");
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("uploaded.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        aiAgentSkillFacade.createAiAgentSkill("Test Skill", null, "uploaded.zip", zipBytes);

        verify(aiAgentSkillFileStorage).storeAiAgentSkillFile(eq("uploaded.skill"), eq(zipBytes));
    }

    @Test
    void testCreateAiAgentSkillRejectsOversizedFile() {
        byte[] oversizedBytes = new byte[10 * 1024 * 1024 + 1];

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.createAiAgentSkill("Test", null, "test.zip", oversizedBytes));

        assertTrue(exception.getMessage()
            .contains("maximum allowed size"));
    }

    @Test
    void testCreateAiAgentSkillRejectsInvalidZip() {
        byte[] notAZip = "this is not a zip file".getBytes(StandardCharsets.UTF_8);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.createAiAgentSkill("Test", null, "test.zip", notAZip));

        assertTrue(exception.getMessage()
            .contains("not a valid zip archive"));
    }

    @Test
    void testCreateAiAgentSkillFromInstructionsEscapesYamlSpecialCharacters() throws IOException {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(any(), any(byte[].class))).thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        aiAgentSkillFacade.createAiAgentSkillFromInstructions(
            "evil\n---\ninjected: true", "desc with \"quotes\"", "instructions");

        verify(aiAgentSkillFileStorage).storeAiAgentSkillFile(any(), argThat(zipBytes -> {
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
    void testUpdateAiAgentSkillDuplicateNameThrows() {
        when(aiAgentSkillService.updateAiAgentSkill(1L, "Existing Name", "description"))
            .thenThrow(new DataIntegrityViolationException(
                "unique constraint violated",
                new SQLException("duplicate key value violates unique constraint", "23505")));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.updateAiAgentSkill(1L, "Existing Name", "description"));

        assertTrue(exception.getMessage()
            .contains("already exists"));
    }

    @Test
    void testCreateAiAgentSkillCleansUpFileOnRollback() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setName("Test Skill");
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        aiAgentSkillFacade.createAiAgentSkill("Test Skill", null, "test.skill", zipBytes);

        List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertEquals(1, synchronizations.size());

        synchronizations.forEach(
            synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(aiAgentSkillFileStorage).deleteAiAgentSkillFile(fileEntry);
    }

    @Test
    void testCreateAiAgentSkillRetriesOnDataIntegrityViolation() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setName("Test Skill (2)");
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiAgentSkillService.existsByName("Test Skill"))
            .thenReturn(false)
            .thenReturn(true);
        when(aiAgentSkillService.existsByName("Test Skill (2)")).thenReturn(false);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class)))
            .thenThrow(new DataIntegrityViolationException(
                "unique constraint violated",
                new SQLException("duplicate key value violates unique constraint", "23505")))
            .thenReturn(expectedAiAgentSkill);

        AiAgentSkill result = aiAgentSkillFacade.createAiAgentSkill("Test Skill", null, "test.skill", zipBytes);

        assertEquals(expectedAiAgentSkill, result);

        verify(aiAgentSkillService).existsByName("Test Skill (2)");
    }

    @Test
    void testUpdateAiAgentSkillNonNameConstraintViolationPropagates() {
        when(aiAgentSkillService.updateAiAgentSkill(1L, "Valid Name", "description"))
            .thenThrow(new DataIntegrityViolationException(
                "foreign key constraint violated",
                new SQLException("referential integrity violation", "23503")));

        assertThrows(
            DataIntegrityViolationException.class,
            () -> aiAgentSkillFacade.updateAiAgentSkill(1L, "Valid Name", "description"));
    }

    @Test
    void testCreateAiAgentSkillWithNoFrontmatterUsesProvidedName() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setName("Provided Name");
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "Just some instructions without frontmatter");

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        aiAgentSkillFacade.createAiAgentSkill("Provided Name", "Provided Desc", "test.skill", zipBytes);

        verify(aiAgentSkillService).createAiAgentSkill(
            argThat(aiAgentSkill -> "Provided Name".equals(aiAgentSkill.getName())
                && "Provided Desc".equals(aiAgentSkill.getDescription())));
    }

    @Test
    void testCreateAiAgentSkillWithUnclosedFrontmatterUsesProvidedName() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setName("Provided Name");
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "---\nname: Unclosed\nInstructions without closing delimiter");

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        aiAgentSkillFacade.createAiAgentSkill("Provided Name", "Provided Desc", "test.skill", zipBytes);

        verify(aiAgentSkillService).createAiAgentSkill(
            argThat(aiAgentSkill -> "Provided Name".equals(aiAgentSkill.getName())
                && "Provided Desc".equals(aiAgentSkill.getDescription())));
    }

    @Test
    void testGenerateUniqueNameTimestampFallback() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill expectedAiAgentSkill = new AiAgentSkill();

        expectedAiAgentSkill.setId(1L);
        expectedAiAgentSkill.setSkillFileEntry(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiAgentSkillService.existsByName("Popular")).thenReturn(true);

        for (int suffix = 2; suffix <= 100; suffix++) {
            when(aiAgentSkillService.existsByName("Popular (" + suffix + ")")).thenReturn(true);
        }

        when(aiAgentSkillFileStorage.storeAiAgentSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiAgentSkillService.createAiAgentSkill(any(AiAgentSkill.class))).thenReturn(expectedAiAgentSkill);

        aiAgentSkillFacade.createAiAgentSkill("Popular", null, "test.skill", zipBytes);

        verify(aiAgentSkillService).createAiAgentSkill(
            argThat(aiAgentSkill -> aiAgentSkill.getName()
                .matches("Popular \\(\\d{13,}\\)")));
    }

    @Test
    void testGetAiAgentSkillFileContentWithAbsolutePathThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.getAiAgentSkillFileContent(1L, "/etc/passwd"));

        assertTrue(exception.getMessage()
            .contains("path traversal"));
    }

    @Test
    void testGetAiAgentSkillFileContentFileNotFound() {
        long skillId = 1L;

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiAgentSkill aiAgentSkill = new AiAgentSkill();

        aiAgentSkill.setId(skillId);
        aiAgentSkill.setName("Test Skill");
        aiAgentSkill.setSkillFileEntry(fileEntry);

        when(aiAgentSkillService.getAiAgentSkill(skillId)).thenReturn(aiAgentSkill);
        when(aiAgentSkillFileStorage.readAiAgentSkillFileBytes(fileEntry)).thenReturn(zipBytes);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiAgentSkillFacade.getAiAgentSkillFileContent(skillId, "nonexistent.txt"));

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
