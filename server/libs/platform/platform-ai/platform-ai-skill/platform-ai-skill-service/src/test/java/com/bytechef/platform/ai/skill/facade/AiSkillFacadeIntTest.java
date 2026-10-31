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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.ai.skill.config.AiSkillIntTestConfiguration;
import com.bytechef.platform.ai.skill.domain.AiSkill;
import com.bytechef.platform.ai.skill.file.storage.AiSkillFileStorage;
import com.bytechef.platform.ai.skill.repository.AiSkillRepository;
import com.bytechef.platform.ai.skill.service.AiSkillService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiSkillIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AiSkillFacadeIntTest {

    @Autowired
    private AiSkillFacade aiSkillFacade;

    @Autowired
    private AiSkillFileStorage aiSkillFileStorage;

    @Autowired
    private AiSkillRepository aiSkillRepository;

    @MockitoSpyBean
    private AiSkillService aiSkillService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void afterEach() {
        aiSkillRepository.deleteAll();
    }

    @Test
    void testCreateAiSkill() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        AiSkill result = aiSkillFacade.createAiSkill("test-skill", "A test skill", "test.skill", zipBytes);

        AiSkill persistedAiSkill = findPersisted(result);

        assertThat(persistedAiSkill.getName()).isEqualTo("test-skill");
        assertThat(persistedAiSkill.getDescription()).isEqualTo("A test skill");

        FileEntry skillFile = persistedAiSkill.getSkillFile();

        assertThat(skillFile.getName()).isEqualTo("test.skill");
        assertThat(aiSkillFileStorage.readAiSkillFileBytes(skillFile)).isEqualTo(zipBytes);
    }

    @Test
    void testCreateAiSkillWithFrontmatterOverride() {
        String skillMdContent =
            "---\nname: frontmatter-name\ndescription: Frontmatter Description\n---\n\nInstructions";

        AiSkill result = aiSkillFacade.createAiSkill(
            "original-name", "Original Description", "test.skill", createZipBytes("SKILL.md", skillMdContent));

        AiSkill persistedAiSkill = findPersisted(result);

        assertThat(persistedAiSkill.getName()).isEqualTo("frontmatter-name");
        assertThat(persistedAiSkill.getDescription()).isEqualTo("Frontmatter Description");
    }

    @Test
    void testCreateAiSkillFromInstructions() {
        AiSkill result = aiSkillFacade.createAiSkillFromInstructions(
            "my-skill", "My description", "Do something useful");

        AiSkill persistedAiSkill = findPersisted(result);

        assertThat(persistedAiSkill.getName()).isEqualTo("my-skill");

        FileEntry skillFile = persistedAiSkill.getSkillFile();

        assertThat(skillFile.getName()).isEqualTo("my-skill.skill");

        Map<String, String> entries = readStoredZipEntries(persistedAiSkill);

        assertThat(entries).containsOnlyKeys("SKILL.md");

        String content = entries.get("SKILL.md");

        assertThat(content).contains("---", "name: \"my-skill\"", "description: \"My description\"",
            "Do something useful");
    }

    @Test
    void testCreateAiSkillFromInstructionsAdoptsFrontmatter() {
        String instructions = """
            ---
            name: customer-refunds
            description: Rules for issuing refunds. Use when a customer asks for one.
            ---

            # Refund Policy

            Escalate anything over 500.
            """;

        AiSkill result = aiSkillFacade.createAiSkillFromInstructions("refund-policy", null, instructions);

        AiSkill persistedAiSkill = findPersisted(result);

        assertThat(persistedAiSkill.getName()).isEqualTo("customer-refunds");
        assertThat(persistedAiSkill.getDescription())
            .isEqualTo("Rules for issuing refunds. Use when a customer asks for one.");

        FileEntry skillFile = persistedAiSkill.getSkillFile();

        assertThat(skillFile.getName()).isEqualTo("customer-refunds.skill");

        String content = readStoredZipEntries(persistedAiSkill).get("SKILL.md");

        assertThat(content)
            .contains(
                "name: \"customer-refunds\"",
                "description: \"Rules for issuing refunds. Use when a customer asks for one.\"",
                "# Refund Policy")
            .doesNotContain("name: customer-refunds");
    }

    @Test
    void testCreateAiSkillWithSpacesAndUppercaseInNameSucceeds() {
        AiSkill result = aiSkillFacade.createAiSkill(
            "Test Skill", null, "test.skill", createZipBytes("SKILL.md", "content"));

        assertThat(findPersisted(result).getName()).isEqualTo("Test Skill");
    }

    @Test
    void testCreateAiSkillRejectsConsecutiveSpaces() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        assertThatThrownBy(() -> aiSkillFacade.createAiSkill("Test  Skill", null, "test.skill", zipBytes))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(aiSkillRepository.count()).isZero();
    }

    @Test
    void testCreateAiSkillRejectsLeadingSpace() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        assertThatThrownBy(() -> aiSkillFacade.createAiSkill(" skill", null, "test.skill", zipBytes))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(aiSkillRepository.count()).isZero();
    }

    @Test
    void testCreateAiSkillRejectsSpecialCharacters() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        assertThatThrownBy(() -> aiSkillFacade.createAiSkill("skill@name", null, "test.skill", zipBytes))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(aiSkillRepository.count()).isZero();
    }

    @Test
    void testCreateAiSkillWithBlankNameThrows() {
        assertThatThrownBy(() -> aiSkillFacade.createAiSkill("", "desc", "test.skill", new byte[0]))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateAiSkillFromInstructionsWithBlankNameThrows() {
        assertThatThrownBy(() -> aiSkillFacade.createAiSkillFromInstructions("", "desc", "instructions"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateAiSkillFromInstructionsWithBlankInstructionsThrows() {
        assertThatThrownBy(() -> aiSkillFacade.createAiSkillFromInstructions("name", "desc", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateAiSkillFromInstructionsWithAdditionalFiles() {
        Map<String, String> additionalFiles = Map.of(
            "scripts/extract.py", "print('hello')",
            "references/REFERENCE.md", "# Reference");

        AiSkill result = aiSkillFacade.createAiSkillFromInstructions(
            "my-skill", "My description", "Do something useful", additionalFiles);

        Map<String, String> entries = readStoredZipEntries(findPersisted(result));

        assertThat(entries).containsKey("SKILL.md");
        assertThat(entries).containsEntry("scripts/extract.py", "print('hello')");
        assertThat(entries).containsEntry("references/REFERENCE.md", "# Reference");
    }

    @Test
    void testCreateAiSkillFromInstructionsRejectsPathTraversalInAdditionalFiles() {
        assertThatThrownBy(() -> aiSkillFacade.createAiSkillFromInstructions(
            "my-skill", null, "instructions", Map.of("../etc/passwd", "bad content")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(aiSkillRepository.count()).isZero();
    }

    @Test
    void testCreateAiSkillFromInstructionsRejectsAbsolutePathInAdditionalFiles() {
        assertThatThrownBy(() -> aiSkillFacade.createAiSkillFromInstructions(
            "my-skill", null, "instructions", Map.of("/absolute/path.txt", "bad content")))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(aiSkillRepository.count()).isZero();
    }

    @Test
    void testCreateAdditionalFilesInSkill() {
        AiSkill aiSkill = saveSkill(
            "my-skill", createZipBytes("SKILL.md", "---\nname: \"my-skill\"\ndescription: \"\"\n---\n\nInstructions"));

        FileEntry oldFileEntry = aiSkill.getSkillFile();

        Map<String, String> additionalFiles = Map.of(
            "scripts/run.py", "print('running')",
            "references/REFERENCE.md", "# Ref");

        AiSkill result = aiSkillFacade.createAdditionalFilesInSkill(aiSkill.getId(), additionalFiles);

        AiSkill persistedAiSkill = findPersisted(result);

        FileEntry newFileEntry = persistedAiSkill.getSkillFile();

        assertThat(newFileEntry).isNotEqualTo(oldFileEntry);
        assertThat(newFileEntry.getName()).isEqualTo("my-skill.skill");

        Map<String, String> entries = readStoredZipEntries(persistedAiSkill);

        assertThat(entries).containsKey("SKILL.md");
        assertThat(entries).containsEntry("scripts/run.py", "print('running')");
        assertThat(entries).containsEntry("references/REFERENCE.md", "# Ref");
    }

    @Test
    void testCreateAdditionalFilesInSkillRejectsPathTraversal() {
        assertThatThrownBy(() -> aiSkillFacade.createAdditionalFilesInSkill(1L, Map.of("../etc/passwd", "bad")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateAdditionalFilesInSkillRejectsAbsolutePath() {
        assertThatThrownBy(() -> aiSkillFacade.createAdditionalFilesInSkill(1L, Map.of("/etc/passwd", "bad")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateAdditionalFilesInSkillRejectsEmptyMap() {
        assertThatThrownBy(() -> aiSkillFacade.createAdditionalFilesInSkill(1L, Map.of()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCreateAiSkillGeneratesUniqueNameOnConflict() {
        saveSkill("duplicate", createZipBytes("SKILL.md", "content"));

        AiSkill result = aiSkillFacade.createAiSkill(
            "duplicate", null, "test.skill", createZipBytes("SKILL.md", "content"));

        assertThat(findPersisted(result).getName()).isEqualTo("duplicate-2");
    }

    @Test
    void testDeleteAiSkillDeletesDbThenFileAfterCommit() {
        AiSkill aiSkill = saveSkill("test-skill", createZipBytes("SKILL.md", "content"));

        FileEntry fileEntry = aiSkill.getSkillFile();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            aiSkillFacade.deleteAiSkill(aiSkill.getId());

            transactionStatus.setRollbackOnly();
        });

        assertThat(aiSkillRepository.findById(aiSkill.getId())).isPresent();
        assertThat(aiSkillFileStorage.readAiSkillFileBytes(fileEntry)).isNotEmpty();

        aiSkillFacade.deleteAiSkill(aiSkill.getId());

        assertThat(aiSkillRepository.findById(aiSkill.getId())).isEmpty();
        assertThatThrownBy(() -> aiSkillFileStorage.readAiSkillFileBytes(fileEntry))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testGetAiSkillFileContent() {
        AiSkill aiSkill = saveSkill("test-skill", createZipBytes("docs/README.md", "Hello from the skill!"));

        String content = aiSkillFacade.getAiSkillFileContent(aiSkill.getId(), "docs/README.md");

        assertThat(content).isEqualTo("Hello from the skill!");
    }

    @Test
    void testGetAiSkillFileContentWithPathTraversal() {
        assertThatThrownBy(() -> aiSkillFacade.getAiSkillFileContent(1L, "../etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path traversal");
    }

    @Test
    void testGetAiSkillFilePaths() {
        AiSkill aiSkill = saveSkill(
            "test-skill",
            createZipBytesWithEntries(
                new String[] {
                    "SKILL.md", "src/", "src/main.py", "../sneaky.txt", "/absolute.txt", "docs/guide.md"
                }));

        List<String> paths = aiSkillFacade.getAiSkillFilePaths(aiSkill.getId());

        assertThat(paths).isEqualTo(List.of("SKILL.md", "src/main.py", "docs/guide.md"));
    }

    @Test
    void testRemoveFileInSkill() {
        AiSkill aiSkill = saveSkill(
            "my-skill", createZipBytesWithEntries(new String[] {
                "SKILL.md", "scripts/run.py"
            }));

        AiSkill result = aiSkillFacade.removeFileInSkill(aiSkill.getId(), "scripts/run.py");

        AiSkill persistedAiSkill = findPersisted(result);

        FileEntry skillFile = persistedAiSkill.getSkillFile();

        assertThat(skillFile.getName()).isEqualTo("my-skill.skill");

        Map<String, String> entries = readStoredZipEntries(persistedAiSkill);

        assertThat(entries).containsKey("SKILL.md");
        assertThat(entries).doesNotContainKey("scripts/run.py");
    }

    @Test
    void testRemoveFileInSkillRejectsPathTraversal() {
        assertThatThrownBy(() -> aiSkillFacade.removeFileInSkill(1L, "../etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRemoveFileInSkillRejectsAbsolutePath() {
        assertThatThrownBy(() -> aiSkillFacade.removeFileInSkill(1L, "/etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRemoveFileInSkillRejectsBlankPath() {
        assertThatThrownBy(() -> aiSkillFacade.removeFileInSkill(1L, " "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRemoveFileInSkillRejectsSkillMd() {
        assertThatThrownBy(() -> aiSkillFacade.removeFileInSkill(1L, "SKILL.md"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRemoveFileInSkillFileNotFoundThrows() {
        AiSkill aiSkill = saveSkill("my-skill", createZipBytes("SKILL.md", "content"));

        assertThatThrownBy(() -> aiSkillFacade.removeFileInSkill(aiSkill.getId(), "nonexistent.txt"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File not found");

        AiSkill persistedAiSkill = findPersisted(aiSkill);

        assertThat(persistedAiSkill.getSkillFile()).isEqualTo(aiSkill.getSkillFile());
    }

    @Test
    void testUpdateAiSkill() {
        AiSkill aiSkill = saveSkill("original-name", createZipBytes("SKILL.md", "content"));

        AiSkill result = aiSkillFacade.updateAiSkill(aiSkill.getId(), "updated-name", "Updated Description");

        assertThat(result.getName()).isEqualTo("updated-name");

        AiSkill persistedAiSkill = findPersisted(aiSkill);

        assertThat(persistedAiSkill.getName()).isEqualTo("updated-name");
        assertThat(persistedAiSkill.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void testUpdateAiSkillWithBlankNameThrows() {
        assertThatThrownBy(() -> aiSkillFacade.updateAiSkill(1L, "", "description"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testToSkillFilenameConvertsZipToSkill() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        AiSkill result = aiSkillFacade.createAiSkill("test-skill", null, "uploaded.zip", zipBytes);

        FileEntry skillFile = findPersisted(result).getSkillFile();

        assertThat(skillFile.getName()).isEqualTo("uploaded.skill");
        assertThat(aiSkillFileStorage.readAiSkillFileBytes(skillFile)).isEqualTo(zipBytes);
    }

    @Test
    void testCreateAiSkillRejectsOversizedFile() {
        byte[] oversizedBytes = new byte[10 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> aiSkillFacade.createAiSkill("Test", null, "test.zip", oversizedBytes))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("maximum allowed size");
    }

    @Test
    void testCreateAiSkillRejectsInvalidZip() {
        byte[] notAZip = "this is not a zip file".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> aiSkillFacade.createAiSkill("Test", null, "test.zip", notAZip))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a valid zip archive");
    }

    @Test
    void testCreateAiSkillFromInstructionsEscapesYamlSpecialCharactersInDescription() {
        AiSkill result = aiSkillFacade.createAiSkillFromInstructions(
            "evil-skill", "desc with \"quotes\"\n---\ninjected: true", "instructions");

        String content = readStoredZipEntries(findPersisted(result)).get("SKILL.md");

        assertThat(content)
            .contains("description: \"desc with \\\"quotes\\\"\\n---\\ninjected: true\"")
            .doesNotContain("\ninjected: true");
    }

    @Test
    void testUpdateAiSkillDuplicateNameThrows() {
        saveSkill("existing-name", createZipBytes("SKILL.md", "content"));

        AiSkill aiSkill = saveSkill("other-name", createZipBytes("SKILL.md", "content"));

        assertThatThrownBy(() -> aiSkillFacade.updateAiSkill(aiSkill.getId(), "existing-name", "description"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");

        assertThat(findPersisted(aiSkill).getName()).isEqualTo("other-name");
    }

    @Test
    void testCreateAiSkillCleansUpFileOnRollback() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        AiSkill rolledBackAiSkill = transactionTemplate.execute(transactionStatus -> {
            AiSkill aiSkill = aiSkillFacade.createAiSkill("test-skill", null, "test.skill", zipBytes);

            assertThat(aiSkillFileStorage.readAiSkillFileBytes(aiSkill.getSkillFile())).isEqualTo(zipBytes);

            transactionStatus.setRollbackOnly();

            return aiSkill;
        });

        assertThat(rolledBackAiSkill).isNotNull();
        assertThat(aiSkillRepository.count()).isZero();
        assertThatThrownBy(() -> aiSkillFileStorage.readAiSkillFileBytes(rolledBackAiSkill.getSkillFile()))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testCreateAiSkillRetriesOnDataIntegrityViolation() {
        saveSkill("test-skill", createZipBytes("SKILL.md", "content"));

        // Simulates a concurrent insert of the same name between the existence check and the INSERT: the first
        // existence check misses the row, so the INSERT hits the real uk_agent_skill_name constraint. The facade
        // recognises the unique violation and retries, but Postgres has already aborted the transaction, so the
        // retry's existence check fails with SQLState 25P02 and nothing is persisted.
        doReturn(false)
            .doCallRealMethod()
            .when(aiSkillService)
            .existsByName("test-skill");

        assertThatThrownBy(() -> aiSkillFacade.createAiSkill(
            "test-skill", null, "test.skill", createZipBytes("SKILL.md", "content")))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("current transaction is aborted");

        verify(aiSkillService, times(2)).existsByName("test-skill");

        assertThat(aiSkillService.existsByName("test-skill-2")).isFalse();
    }

    @Test
    void testUpdateAiSkillNonNameConstraintViolationPropagates() {
        AiSkill aiSkill = saveSkill("valid-name", createZipBytes("SKILL.md", "content"));

        doThrow(new DataIntegrityViolationException(
            "foreign key constraint violated", new SQLException("referential integrity violation", "23503")))
                .when(aiSkillService)
                .updateAiSkill(aiSkill.getId(), "valid-name", "description");

        assertThatThrownBy(() -> aiSkillFacade.updateAiSkill(aiSkill.getId(), "valid-name", "description"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testCreateAiSkillWithNoFrontmatterUsesProvidedName() {
        AiSkill result = aiSkillFacade.createAiSkill(
            "provided-name", "Provided Desc", "test.skill",
            createZipBytes("SKILL.md", "Just some instructions without frontmatter"));

        AiSkill persistedAiSkill = findPersisted(result);

        assertThat(persistedAiSkill.getName()).isEqualTo("provided-name");
        assertThat(persistedAiSkill.getDescription()).isEqualTo("Provided Desc");
    }

    @Test
    void testCreateAiSkillWithUnclosedFrontmatterUsesProvidedName() {
        AiSkill result = aiSkillFacade.createAiSkill(
            "provided-name", "Provided Desc", "test.skill",
            createZipBytes("SKILL.md", "---\nname: Unclosed\nInstructions without closing delimiter"));

        AiSkill persistedAiSkill = findPersisted(result);

        assertThat(persistedAiSkill.getName()).isEqualTo("provided-name");
        assertThat(persistedAiSkill.getDescription()).isEqualTo("Provided Desc");
    }

    @Test
    void testGenerateUniqueNameTimestampFallback() {
        byte[] zipBytes = createZipBytes("SKILL.md", "content");

        saveSkill("popular", zipBytes);

        for (int suffix = 2; suffix <= 100; suffix++) {
            saveSkill("popular-" + suffix, zipBytes);
        }

        AiSkill result = aiSkillFacade.createAiSkill("popular", null, "test.skill", zipBytes);

        assertThat(findPersisted(result).getName()).matches("popular-\\d{13,}");
    }

    @Test
    void testGetAiSkillFileContentWithAbsolutePathThrows() {
        assertThatThrownBy(() -> aiSkillFacade.getAiSkillFileContent(1L, "/etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path traversal");
    }

    @Test
    void testGetAiSkillFileContentFileNotFound() {
        AiSkill aiSkill = saveSkill("test-skill", createZipBytes("SKILL.md", "content"));

        assertThatThrownBy(() -> aiSkillFacade.getAiSkillFileContent(aiSkill.getId(), "nonexistent.txt"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("File not found");
    }

    private AiSkill findPersisted(AiSkill aiSkill) {
        return aiSkillRepository.findById(aiSkill.getId())
            .orElseThrow();
    }

    private Map<String, String> readStoredZipEntries(AiSkill aiSkill) {
        return readZipEntries(aiSkillFileStorage.readAiSkillFileBytes(aiSkill.getSkillFile()));
    }

    private AiSkill saveSkill(String name, byte[] zipBytes) {
        AiSkill aiSkill = new AiSkill();

        aiSkill.setName(name);
        aiSkill.setSkillFile(aiSkillFileStorage.storeAiSkillFile(name + ".skill", zipBytes));

        return aiSkillRepository.save(aiSkill);
    }

    private static byte[] createZipBytes(String entryName, String content) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to create test zip", ioException);
        }
    }

    private static byte[] createZipBytesWithEntries(String[] entryNames) {
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
            throw new UncheckedIOException("Failed to create test zip", ioException);
        }
    }

    private static Map<String, String> readZipEntries(byte[] zipBytes) {
        Map<String, String> entries = new HashMap<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    entries.put(
                        zipEntry.getName(), new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (IOException ioException) {
            throw new UncheckedIOException("Failed to read test zip", ioException);
        }

        return entries;
    }
}
