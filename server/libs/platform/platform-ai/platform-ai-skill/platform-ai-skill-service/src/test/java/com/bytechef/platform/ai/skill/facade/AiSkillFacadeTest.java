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

package com.bytechef.platform.ai.skill.facade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.file.storage.AiSkillFileStorage;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
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
class AiSkillFacadeTest {

    @Mock
    private AiSkillFileStorage aiSkillFileStorage;

    @Mock
    private AiSkillService aiSkillService;

    @InjectMocks
    private AiSkillFacadeImpl aiSkillFacade;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void testCreateAiSkill() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("test-skill");
        expectedAiSkill.setDescription("A test skill");
        expectedAiSkill.setSkillFile(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiSkillFileStorage.storeAiSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        AiSkill result = aiSkillFacade.createAiSkill(
            "test-skill", "A test skill", "test.skill", zipBytes);

        assertEquals(expectedAiSkill, result);

        verify(aiSkillFileStorage).storeAiSkillFile(eq("test.skill"), eq(zipBytes));
        verify(aiSkillService)
            .createAiSkill(argThat(aiSkill -> "test-skill".equals(aiSkill.getName())
                && "A test skill".equals(aiSkill.getDescription())));
    }

    @Test
    void testCreateAiSkillWithFrontmatterOverride() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("frontmatter-name");
        expectedAiSkill.setDescription("Frontmatter Description");
        expectedAiSkill.setSkillFile(fileEntry);

        String skillMdContent =
            "---\nname: frontmatter-name\ndescription: Frontmatter Description\n---\n\nInstructions";

        byte[] zipBytes = createZipBytes("SKILL.md", skillMdContent);

        when(aiSkillFileStorage.storeAiSkillFile(eq("test.skill"), any(byte[].class))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        aiSkillFacade.createAiSkill("original-name", "Original Description", "test.skill", zipBytes);

        verify(aiSkillService).createAiSkill(
            argThat(aiSkill -> "frontmatter-name".equals(aiSkill.getName())
                && "Frontmatter Description".equals(aiSkill.getDescription())));
    }

    @Test
    void testCreateAiSkillFromInstructions() throws IOException {
        FileEntry fileEntry = new FileEntry("my-skill.skill", "file:///storage/my-skill.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("my-skill");
        expectedAiSkill.setSkillFile(fileEntry);

        when(aiSkillFileStorage.storeAiSkillFile(eq("my-skill.skill"), any(byte[].class)))
            .thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        AiSkill result = aiSkillFacade.createAiSkillFromInstructions(
            "my-skill", "My description", "Do something useful");

        assertEquals(expectedAiSkill, result);

        verify(aiSkillFileStorage).storeAiSkillFile(eq("my-skill.skill"), argThat(zipBytes -> {
            try (ZipInputStream zipInputStream =
                new ZipInputStream(new ByteArrayInputStream(zipBytes))) {

                ZipEntry zipEntry = zipInputStream.getNextEntry();

                if (zipEntry == null || !"SKILL.md".equals(zipEntry.getName())) {
                    return false;
                }

                String content = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);

                return content.contains("---") &&
                    content.contains("name: \"my-skill\"") &&
                    content.contains("description: \"My description\"") &&
                    content.contains("Do something useful");
            } catch (IOException ioException) {
                return false;
            }
        }));
    }

    @Test
    void testCreateAiSkillWithSpacesAndUppercaseInNameSucceeds() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("Test Skill");
        expectedAiSkill.setSkillFile(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiSkillFileStorage.storeAiSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        AiSkill result = aiSkillFacade.createAiSkill("Test Skill", null, "test.skill", zipBytes);

        assertEquals(expectedAiSkill, result);

        verify(aiSkillService).createAiSkill(
            argThat(aiSkill -> "Test Skill".equals(aiSkill.getName())));
    }

    @Test
    void testCreateAiSkillRejectsConsecutiveSpaces() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkill("Test  Skill", null, "test.skill", zipBytes));
    }

    @Test
    void testCreateAiSkillRejectsLeadingSpace() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkill(" skill", null, "test.skill", zipBytes));
    }

    @Test
    void testCreateAiSkillRejectsSpecialCharacters() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkill("skill@name", null, "test.skill", zipBytes));
    }

    @Test
    void testCreateAiSkillWithBlankNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkill("", "desc", "test.skill", new byte[0]));
    }

    @Test
    void testCreateAiSkillFromInstructionsWithBlankNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkillFromInstructions("", "desc", "instructions"));
    }

    @Test
    void testCreateAiSkillFromInstructionsWithBlankInstructionsThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkillFromInstructions("name", "desc", ""));
    }

    @Test
    void testCreateAiSkillFromInstructionsWithAdditionalFiles() throws IOException {
        FileEntry fileEntry = new FileEntry("my-skill.skill", "file:///storage/my-skill.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("my-skill");
        expectedAiSkill.setSkillFile(fileEntry);

        when(aiSkillFileStorage.storeAiSkillFile(eq("my-skill.skill"), any(byte[].class)))
            .thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        Map<String, String> additionalFiles = Map.of(
            "scripts/extract.py", "print('hello')",
            "references/REFERENCE.md", "# Reference");

        AiSkill result = aiSkillFacade.createAiSkillFromInstructions(
            "my-skill", "My description", "Do something useful", additionalFiles);

        assertEquals(expectedAiSkill, result);

        verify(aiSkillFileStorage).storeAiSkillFile(eq("my-skill.skill"), argThat(zipBytes -> {
            Map<String, String> entries = readZipEntries(zipBytes);

            return entries.containsKey("SKILL.md")
                && "print('hello')".equals(entries.get("scripts/extract.py"))
                && "# Reference".equals(entries.get("references/REFERENCE.md"));
        }));
    }

    @Test
    void testCreateAiSkillFromInstructionsRejectsPathTraversalInAdditionalFiles() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkillFromInstructions(
                "my-skill", null, "instructions",
                Map.of("../etc/passwd", "bad content")));
    }

    @Test
    void testCreateAiSkillFromInstructionsRejectsAbsolutePathInAdditionalFiles() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkillFromInstructions(
                "my-skill", null, "instructions",
                Map.of("/absolute/path.txt", "bad content")));
    }

    @Test
    void testCreateAdditionalFilesInSkill() {
        long skillId = 1L;

        byte[] existingZipBytes =
            createZipBytes("SKILL.md", "---\nname: \"my-skill\"\ndescription: \"\"\n---\n\nInstructions");

        FileEntry oldFileEntry = new FileEntry("my-skill.skill", "file:///storage/my-skill.skill");
        FileEntry newFileEntry = new FileEntry("my-skill.skill", "file:///storage/my-skill-new.skill");

        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(skillId);
        aiSkill.setName("my-skill");
        aiSkill.setSkillFile(oldFileEntry);

        AiSkill updatedAiSkill = new AiSkill();

        updatedAiSkill.setId(skillId);
        updatedAiSkill.setName("my-skill");
        updatedAiSkill.setSkillFile(newFileEntry);

        when(aiSkillService.getAiSkill(skillId)).thenReturn(aiSkill);
        when(aiSkillFileStorage.readAiSkillFileBytes(oldFileEntry)).thenReturn(existingZipBytes);
        when(aiSkillFileStorage.storeAiSkillFile(eq("my-skill.skill"), any(byte[].class))).thenReturn(newFileEntry);
        when(aiSkillService.updateAiSkillFile(skillId, newFileEntry)).thenReturn(updatedAiSkill);

        Map<String, String> additionalFiles = Map.of(
            "scripts/run.py", "print('running')",
            "references/REFERENCE.md", "# Ref");

        AiSkill result = aiSkillFacade.createAdditionalFilesInSkill(skillId, additionalFiles);

        assertEquals(updatedAiSkill, result);

        verify(aiSkillFileStorage).storeAiSkillFile(eq("my-skill.skill"), argThat(zipBytes -> {
            Map<String, String> entries = readZipEntries(zipBytes);

            return entries.containsKey("SKILL.md")
                && "print('running')".equals(entries.get("scripts/run.py"))
                && "# Ref".equals(entries.get("references/REFERENCE.md"));
        }));
    }

    @Test
    void testCreateAdditionalFilesInSkillRejectsPathTraversal() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAdditionalFilesInSkill(1L, Map.of("../etc/passwd", "bad")));
    }

    @Test
    void testCreateAdditionalFilesInSkillRejectsAbsolutePath() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAdditionalFilesInSkill(1L, Map.of("/etc/passwd", "bad")));
    }

    @Test
    void testCreateAdditionalFilesInSkillRejectsEmptyMap() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAdditionalFilesInSkill(1L, Map.of()));
    }

    @Test
    void testCreateAiSkillGeneratesUniqueNameOnConflict() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setSkillFile(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiSkillService.existsByName("duplicate")).thenReturn(true);
        when(aiSkillService.existsByName("duplicate-2")).thenReturn(false);
        when(aiSkillFileStorage.storeAiSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        aiSkillFacade.createAiSkill("duplicate", null, "test.skill", zipBytes);

        verify(aiSkillService).createAiSkill(
            argThat(aiSkill -> "duplicate-2".equals(aiSkill.getName())));
    }

    @Test
    void testDeleteAiSkillDeletesDbThenFileAfterCommit() {
        long skillId = 42L;

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(skillId);
        aiSkill.setName("test-skill");
        aiSkill.setSkillFile(fileEntry);

        when(aiSkillService.getAiSkill(skillId)).thenReturn(aiSkill);

        aiSkillFacade.deleteAiSkill(skillId);

        verify(aiSkillService).deleteAiSkill(skillId);

        List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertEquals(1, synchronizations.size());

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        InOrder deleteOrder = inOrder(aiSkillService, aiSkillFileStorage);

        deleteOrder.verify(aiSkillService)
            .deleteAiSkill(skillId);
        deleteOrder.verify(aiSkillFileStorage)
            .deleteAiSkillFile(fileEntry);
    }

    @Test
    void testGetAiSkillFileContent() {
        long skillId = 1L;

        byte[] zipBytes = createZipBytes("docs/README.md", "Hello from the skill!");

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(skillId);
        aiSkill.setName("test-skill");
        aiSkill.setSkillFile(fileEntry);

        when(aiSkillService.getAiSkill(skillId)).thenReturn(aiSkill);
        when(aiSkillFileStorage.readAiSkillFileBytes(fileEntry)).thenReturn(zipBytes);

        String content = aiSkillFacade.getAiSkillFileContent(skillId, "docs/README.md");

        assertEquals("Hello from the skill!", content);
    }

    @Test
    void testGetAiSkillFileContentWithPathTraversal() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.getAiSkillFileContent(1L, "../etc/passwd"));

        assertTrue(exception.getMessage()
            .contains("path traversal"));
    }

    @Test
    void testGetAiSkillFilePaths() {
        long skillId = 1L;

        byte[] zipBytes = createZipBytesWithEntries(
            new String[] {
                "SKILL.md", "src/", "src/main.py", "../sneaky.txt", "/absolute.txt", "docs/guide.md"
            });

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(skillId);
        aiSkill.setName("test-skill");
        aiSkill.setSkillFile(fileEntry);

        when(aiSkillService.getAiSkill(skillId)).thenReturn(aiSkill);
        when(aiSkillFileStorage.readAiSkillFileBytes(fileEntry)).thenReturn(zipBytes);

        List<String> paths = aiSkillFacade.getAiSkillFilePaths(skillId);

        assertEquals(List.of("SKILL.md", "src/main.py", "docs/guide.md"), paths);
    }

    @Test
    void testRemoveFileInSkill() {
        long skillId = 1L;

        byte[] existingZipBytes = createZipBytesWithEntries(
            new String[] {
                "SKILL.md", "scripts/run.py"
            });

        FileEntry oldFileEntry = new FileEntry("my-skill.skill", "file:///storage/my-skill.skill");
        FileEntry newFileEntry = new FileEntry("my-skill.skill", "file:///storage/my-skill-new.skill");

        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(skillId);
        aiSkill.setName("my-skill");
        aiSkill.setSkillFile(oldFileEntry);

        AiSkill updatedAiSkill = new AiSkill();

        updatedAiSkill.setId(skillId);
        updatedAiSkill.setName("my-skill");
        updatedAiSkill.setSkillFile(newFileEntry);

        when(aiSkillService.getAiSkill(skillId)).thenReturn(aiSkill);
        when(aiSkillFileStorage.readAiSkillFileBytes(oldFileEntry)).thenReturn(existingZipBytes);
        when(aiSkillFileStorage.storeAiSkillFile(eq("my-skill.skill"), any(byte[].class))).thenReturn(newFileEntry);
        when(aiSkillService.updateAiSkillFile(skillId, newFileEntry)).thenReturn(updatedAiSkill);

        AiSkill result = aiSkillFacade.removeFileInSkill(skillId, "scripts/run.py");

        assertEquals(updatedAiSkill, result);

        verify(aiSkillFileStorage).storeAiSkillFile(eq("my-skill.skill"), argThat(zipBytes -> {
            Map<String, String> entries = readZipEntries(zipBytes);

            return entries.containsKey("SKILL.md") && !entries.containsKey("scripts/run.py");
        }));
    }

    @Test
    void testRemoveFileInSkillRejectsPathTraversal() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.removeFileInSkill(1L, "../etc/passwd"));
    }

    @Test
    void testRemoveFileInSkillRejectsAbsolutePath() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.removeFileInSkill(1L, "/etc/passwd"));
    }

    @Test
    void testRemoveFileInSkillRejectsBlankPath() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.removeFileInSkill(1L, " "));
    }

    @Test
    void testRemoveFileInSkillRejectsSkillMd() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.removeFileInSkill(1L, "SKILL.md"));
    }

    @Test
    void testRemoveFileInSkillFileNotFoundThrows() {
        long skillId = 1L;

        byte[] existingZipBytes = createZipBytes("SKILL.md", "content");

        FileEntry fileEntry = new FileEntry("my-skill.skill", "file:///storage/my-skill.skill");

        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(skillId);
        aiSkill.setName("my-skill");
        aiSkill.setSkillFile(fileEntry);

        when(aiSkillService.getAiSkill(skillId)).thenReturn(aiSkill);
        when(aiSkillFileStorage.readAiSkillFileBytes(fileEntry)).thenReturn(existingZipBytes);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.removeFileInSkill(skillId, "nonexistent.txt"));

        assertTrue(exception.getMessage()
            .contains("File not found"));
    }

    @Test
    void testUpdateAiSkill() {
        long skillId = 1L;

        AiSkill updatedAiSkill = new AiSkill();

        updatedAiSkill.setId(skillId);
        updatedAiSkill.setName("updated-name");
        updatedAiSkill.setDescription("Updated Description");

        when(aiSkillService.updateAiSkill(skillId, "updated-name", "Updated Description"))
            .thenReturn(updatedAiSkill);

        AiSkill result = aiSkillFacade.updateAiSkill(skillId, "updated-name", "Updated Description");

        assertEquals("updated-name", result.getName());

        verify(aiSkillService).updateAiSkill(skillId, "updated-name", "Updated Description");
    }

    @Test
    void testUpdateAiSkillWithBlankNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.updateAiSkill(1L, "", "description"));
    }

    @Test
    void testToSkillFilenameConvertsZipToSkill() {
        FileEntry fileEntry = new FileEntry("uploaded.skill", "file:///storage/uploaded.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("test-skill");
        expectedAiSkill.setSkillFile(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiSkillFileStorage.storeAiSkillFile(eq("uploaded.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        aiSkillFacade.createAiSkill("test-skill", null, "uploaded.zip", zipBytes);

        verify(aiSkillFileStorage).storeAiSkillFile(eq("uploaded.skill"), eq(zipBytes));
    }

    @Test
    void testCreateAiSkillRejectsOversizedFile() {
        byte[] oversizedBytes = new byte[10 * 1024 * 1024 + 1];

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkill("Test", null, "test.zip", oversizedBytes));

        assertTrue(exception.getMessage()
            .contains("maximum allowed size"));
    }

    @Test
    void testCreateAiSkillRejectsInvalidZip() {
        byte[] notAZip = "this is not a zip file".getBytes(StandardCharsets.UTF_8);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.createAiSkill("Test", null, "test.zip", notAZip));

        assertTrue(exception.getMessage()
            .contains("not a valid zip archive"));
    }

    @Test
    void testCreateAiSkillFromInstructionsEscapesYamlSpecialCharactersInDescription() throws IOException {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setSkillFile(fileEntry);

        when(aiSkillFileStorage.storeAiSkillFile(any(), any(byte[].class))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        aiSkillFacade.createAiSkillFromInstructions(
            "evil-skill", "desc with \"quotes\"\n---\ninjected: true", "instructions");

        verify(aiSkillFileStorage).storeAiSkillFile(any(), argThat(zipBytes -> {
            try (ZipInputStream zipInputStream =
                new ZipInputStream(new ByteArrayInputStream(zipBytes))) {

                ZipEntry zipEntry = zipInputStream.getNextEntry();

                if (zipEntry == null || !"SKILL.md".equals(zipEntry.getName())) {
                    return false;
                }

                String content = new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8);

                return content.contains(
                    "description: \"desc with \\\"quotes\\\"\\n---\\ninjected: true\"") &&
                    !content.contains("\ninjected: true");
            } catch (IOException ioException) {
                return false;
            }
        }));
    }

    @Test
    void testUpdateAiSkillDuplicateNameThrows() {
        when(aiSkillService.updateAiSkill(1L, "existing-name", "description"))
            .thenThrow(new DataIntegrityViolationException(
                "unique constraint violated",
                new SQLException("duplicate key value violates unique constraint", "23505")));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.updateAiSkill(1L, "existing-name", "description"));

        assertTrue(exception.getMessage()
            .contains("already exists"));
    }

    @Test
    void testCreateAiSkillCleansUpFileOnRollback() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("test-skill");
        expectedAiSkill.setSkillFile(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiSkillFileStorage.storeAiSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        aiSkillFacade.createAiSkill("test-skill", null, "test.skill", zipBytes);

        List<TransactionSynchronization> synchronizations =
            TransactionSynchronizationManager.getSynchronizations();

        assertEquals(1, synchronizations.size());

        synchronizations.forEach(
            synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(aiSkillFileStorage).deleteAiSkillFile(fileEntry);
    }

    @Test
    void testCreateAiSkillRetriesOnDataIntegrityViolation() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("test-skill-2");
        expectedAiSkill.setSkillFile(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiSkillFileStorage.storeAiSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiSkillService.existsByName("test-skill"))
            .thenReturn(false)
            .thenReturn(true);
        when(aiSkillService.existsByName("test-skill-2")).thenReturn(false);
        when(aiSkillService.createAiSkill(any(AiSkill.class)))
            .thenThrow(new DataIntegrityViolationException(
                "unique constraint violated",
                new SQLException("duplicate key value violates unique constraint", "23505")))
            .thenReturn(expectedAiSkill);

        AiSkill result = aiSkillFacade.createAiSkill("test-skill", null, "test.skill", zipBytes);

        assertEquals(expectedAiSkill, result);

        verify(aiSkillService).existsByName("test-skill-2");
    }

    @Test
    void testUpdateAiSkillNonNameConstraintViolationPropagates() {
        when(aiSkillService.updateAiSkill(1L, "valid-name", "description"))
            .thenThrow(new DataIntegrityViolationException(
                "foreign key constraint violated",
                new SQLException("referential integrity violation", "23503")));

        assertThrows(
            DataIntegrityViolationException.class,
            () -> aiSkillFacade.updateAiSkill(1L, "valid-name", "description"));
    }

    @Test
    void testCreateAiSkillWithNoFrontmatterUsesProvidedName() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("provided-name");
        expectedAiSkill.setSkillFile(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "Just some instructions without frontmatter");

        when(aiSkillFileStorage.storeAiSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        aiSkillFacade.createAiSkill("provided-name", "Provided Desc", "test.skill", zipBytes);

        verify(aiSkillService).createAiSkill(
            argThat(aiSkill -> "provided-name".equals(aiSkill.getName())
                && "Provided Desc".equals(aiSkill.getDescription())));
    }

    @Test
    void testCreateAiSkillWithUnclosedFrontmatterUsesProvidedName() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setName("provided-name");
        expectedAiSkill.setSkillFile(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "---\nname: Unclosed\nInstructions without closing delimiter");

        when(aiSkillFileStorage.storeAiSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        aiSkillFacade.createAiSkill("provided-name", "Provided Desc", "test.skill", zipBytes);

        verify(aiSkillService).createAiSkill(
            argThat(aiSkill -> "provided-name".equals(aiSkill.getName())
                && "Provided Desc".equals(aiSkill.getDescription())));
    }

    @Test
    void testGenerateUniqueNameTimestampFallback() {
        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill expectedAiSkill = new AiSkill();

        expectedAiSkill.setId(1L);
        expectedAiSkill.setSkillFile(fileEntry);

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        when(aiSkillService.existsByName("popular")).thenReturn(true);

        for (int suffix = 2; suffix <= 100; suffix++) {
            when(aiSkillService.existsByName("popular-" + suffix)).thenReturn(true);
        }

        when(aiSkillFileStorage.storeAiSkillFile(eq("test.skill"), eq(zipBytes))).thenReturn(fileEntry);
        when(aiSkillService.createAiSkill(any(AiSkill.class))).thenReturn(expectedAiSkill);

        aiSkillFacade.createAiSkill("popular", null, "test.skill", zipBytes);

        verify(aiSkillService).createAiSkill(
            argThat(aiSkill -> aiSkill.getName()
                .matches("popular-\\d{13,}")));
    }

    @Test
    void testGetAiSkillFileContentWithAbsolutePathThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.getAiSkillFileContent(1L, "/etc/passwd"));

        assertTrue(exception.getMessage()
            .contains("path traversal"));
    }

    @Test
    void testGetAiSkillFileContentFileNotFound() {
        long skillId = 1L;

        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        FileEntry fileEntry = new FileEntry("test.skill", "file:///storage/test.skill");

        AiSkill aiSkill = new AiSkill();

        aiSkill.setId(skillId);
        aiSkill.setName("test-skill");
        aiSkill.setSkillFile(fileEntry);

        when(aiSkillService.getAiSkill(skillId)).thenReturn(aiSkill);
        when(aiSkillFileStorage.readAiSkillFileBytes(fileEntry)).thenReturn(zipBytes);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> aiSkillFacade.getAiSkillFileContent(skillId, "nonexistent.txt"));

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

    private Map<String, String> readZipEntries(byte[] zipBytes) {
        Map<String, String> entries = new java.util.HashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    entries.put(zipEntry.getName(),
                        new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (IOException ioException) {
            throw new RuntimeException("Failed to read test zip", ioException);
        }

        return entries;
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
