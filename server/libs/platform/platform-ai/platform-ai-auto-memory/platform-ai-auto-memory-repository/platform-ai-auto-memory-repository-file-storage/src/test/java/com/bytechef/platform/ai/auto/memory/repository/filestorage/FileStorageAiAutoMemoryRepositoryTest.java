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

package com.bytechef.platform.ai.auto.memory.repository.filestorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.exception.FileStorageException;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class FileStorageAiAutoMemoryRepositoryTest {

    private static final long WORKSPACE_ID = 7;
    private static final long OTHER_WORKSPACE_ID = 8;
    private static final long PRINCIPAL_ID = 42;
    private static final int PRINCIPAL_TYPE = 0;
    private static final int ENVIRONMENT = 1;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final AiAutoMemoryOwner OWNER = new AiAutoMemoryOwner(
        WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, Environment.values()[ENVIRONMENT]);
    private static final String DIRECTORY = FileStorageAiAutoMemoryRepository.ROOT_DIRECTORY + "/" + WORKSPACE_ID + "/"
        + PRINCIPAL_TYPE + "_" + PRINCIPAL_ID + "/" + ENVIRONMENT;

    @TempDir
    private Path tempDir;

    private FilesystemFileStorageService fileStorageService;
    private FileStorageAiAutoMemoryRepository aiAutoMemoryRepository;

    @BeforeEach
    void beforeEach() {
        fileStorageService = new FilesystemFileStorageService(tempDir.toString());

        aiAutoMemoryRepository = new FileStorageAiAutoMemoryRepository(fileStorageService);
    }

    @Test
    void testSaveAndFindByIdRoundTripsEveryField() {
        AiAutoMemory memory = buildMemory("preferred_tone", "Preferred tone", "How the user likes replies", "concise");

        memory.setUpdatedAt(BASE_TIME.plusMinutes(5));

        AiAutoMemory saved = save(memory, WORKSPACE_ID);

        AiAutoMemory found = aiAutoMemoryRepository.findOwnedById(OWNER, saved.getId())
            .orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getName()).isEqualTo("preferred_tone");
        assertThat(found.getTitle()).isEqualTo("Preferred tone");
        assertThat(found.getDescription()).isEqualTo("How the user likes replies");
        assertThat(found.getContent()).isEqualTo("concise");
        assertThat(found.getPrincipalId()).isEqualTo(PRINCIPAL_ID);
        assertThat(found.getPrincipalType()).isEqualTo(AiAutoMemoryPrincipalType.USER);
        assertThat(found.getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(found.getEnvironment()).isEqualTo(Environment.values()[ENVIRONMENT]);
        assertThat(found.getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(found.getUpdatedAt()).isEqualTo(BASE_TIME.plusMinutes(5));
        assertThat(found.getVersion()).isZero();
    }

    @Test
    void testSaveReturnsTheStoredCopyAndLeavesTheArgumentUntouched() {
        AiAutoMemory unsaved = buildMemory("fresh", "Fresh", null, "1");

        AiAutoMemory saved = aiAutoMemoryRepository.save(unsaved);

        assertThat(unsaved.getId()).isNull();
        assertThat(unsaved.getVersion()).isNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isZero();

        saved.setContent("2");

        AiAutoMemory updated = aiAutoMemoryRepository.save(saved);

        assertThat(saved.getVersion()).isZero();
        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    void testAnIncompleteMemoryIsRejectedWithoutWritingADocument() {
        AiAutoMemory incomplete = new AiAutoMemory(OWNER);

        incomplete.setMemoryType(AiAutoMemoryType.USER);

        assertThatThrownBy(() -> aiAutoMemoryRepository.save(incomplete)).isInstanceOf(RuntimeException.class);
        assertThat(fileStorageService.getFileEntries(DIRECTORY)).isEmpty();
    }

    /**
     * The directory a document sits in is not trusted on its own: a document naming another owner — copied there by
     * hand, or listed by a storage backend whose prefix match strays — is not returned as this owner's.
     */
    @Test
    void testADocumentOfAnotherOwnerInThisOwnersDirectoryIsNotReturned() {
        fileStorageService.storeFileContent(
            DIRECTORY, "9.json", JsonUtils.write(
                new AiAutoMemoryDocument(
                    9, OTHER_WORKSPACE_ID, PRINCIPAL_ID, PRINCIPAL_TYPE, "stray", "Stray", null, 0, ENVIRONMENT,
                    "body", null, null, 0L)),
            false);

        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, 9)).isEmpty();
        assertThat(aiAutoMemoryRepository.findByOwner(OWNER, null)).isEmpty();
        assertThat(aiAutoMemoryRepository.findByWorkspace(WORKSPACE_ID, Environment.values()[ENVIRONMENT], null))
            .isEmpty();
        assertThat(aiAutoMemoryRepository.listPrincipals(WORKSPACE_ID, Environment.values()[ENVIRONMENT])).isEmpty();
    }

    @Test
    void testADocumentWhoseIdDoesNotMatchItsFilenameIsSkipped() {
        fileStorageService.storeFileContent(
            DIRECTORY, "9.json", JsonUtils.write(
                new AiAutoMemoryDocument(
                    10, WORKSPACE_ID, PRINCIPAL_ID, PRINCIPAL_TYPE, "copied", "Copied", null, 0, ENVIRONMENT, "body",
                    null, null, 0L)),
            false);

        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, 9)).isEmpty();
        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, 10)).isEmpty();
        assertThat(aiAutoMemoryRepository.findByOwner(OWNER, null)).isEmpty();
    }

    @Test
    void testFindByOwnerReturnsNewestUpdatedFirst() {
        AiAutoMemory first = buildMemory("first", "First", null, "1");
        AiAutoMemory second = buildMemory("second", "Second", null, "2");

        // Distinct timestamps, inserted oldest-last, so a backend returning storage order would fail.
        first.setUpdatedAt(BASE_TIME.plusMinutes(10));
        second.setUpdatedAt(BASE_TIME);

        AiAutoMemory savedSecond = save(second, WORKSPACE_ID);
        AiAutoMemory savedFirst = save(first, WORKSPACE_ID);

        List<AiAutoMemory> memories =
            aiAutoMemoryRepository.findByOwner(OWNER, null);

        assertThat(memories).extracting(AiAutoMemory::getId)
            .containsExactly(savedFirst.getId(), savedSecond.getId());
    }

    @Test
    void testFindByOwnerExcludesOtherWorkspaces() {
        save(buildMemory("mine", "Mine", null, "1"), WORKSPACE_ID);
        save(buildMemory("theirs", "Theirs", null, "2"), OTHER_WORKSPACE_ID);

        assertThat(
            aiAutoMemoryRepository.findByOwner(OWNER, null))
                .extracting(AiAutoMemory::getName)
                .containsExactly("mine");
    }

    @Test
    void testFindByOwnerNarrowsByMemoryType() {
        save(buildMemory("user_memory", "User", null, "1"), WORKSPACE_ID);

        AiAutoMemory feedback = buildMemory("feedback_memory", "Feedback", null, "2");

        feedback.setMemoryType(AiAutoMemoryType.FEEDBACK);

        save(feedback, WORKSPACE_ID);

        assertThat(
            aiAutoMemoryRepository.findByOwner(OWNER, AiAutoMemoryType.FEEDBACK))
                .extracting(AiAutoMemory::getName)
                .containsExactly("feedback_memory");
    }

    @Test
    void testFindAllByNameReturnsEveryMatch() {
        save(buildMemory("duplicate", "One", null, "1"), WORKSPACE_ID);
        save(buildMemory("duplicate", "Two", null, "2"), WORKSPACE_ID);

        assertThat(
            aiAutoMemoryRepository.findAllByOwnerAndName(OWNER, "duplicate"))
                .hasSize(2);
    }

    @Test
    void testDeleteRemovesOnlyTheTarget() {
        AiAutoMemory kept = save(buildMemory("kept", "Kept", null, "1"), WORKSPACE_ID);
        AiAutoMemory removed = save(buildMemory("removed", "Removed", null, "2"), WORKSPACE_ID);

        aiAutoMemoryRepository.delete(removed);

        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, removed.getId())).isEmpty();
        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, kept.getId())).isPresent();
    }

    @Test
    void testDeleteFailsWhenTheDocumentIsAlreadyGone() {
        AiAutoMemory saved = save(buildMemory("vanishing", "Vanishing", null, "1"), WORKSPACE_ID);

        fileStorageService.deleteFile(DIRECTORY, fileStorageService.getFileEntry(DIRECTORY, saved.getId() + ".json"));

        assertThatThrownBy(() -> aiAutoMemoryRepository.delete(saved))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void testDocumentWrittenBeforeVersioningReadsAsVersionZeroAndCanBeUpdated() {
        fileStorageService.storeFileContent(
            DIRECTORY, "5.json",
            "{\"id\":5,\"workspaceId\":" + WORKSPACE_ID + ",\"principalId\":" + PRINCIPAL_ID + ",\"principalType\":"
                + PRINCIPAL_TYPE + ",\"name\":\"legacy\",\"title\":\"Legacy\",\"memoryType\":0,\"environment\":"
                + ENVIRONMENT + ",\"content\":\"old\"}",
            false);

        AiAutoMemory legacy = aiAutoMemoryRepository.findOwnedById(OWNER, 5)
            .orElseThrow();

        assertThat(legacy.getVersion()).isZero();

        legacy.setContent("new");

        aiAutoMemoryRepository.save(legacy);

        AiAutoMemory reloaded = aiAutoMemoryRepository.findOwnedById(OWNER, 5)
            .orElseThrow();

        assertThat(reloaded.getContent()).isEqualTo("new");
        assertThat(reloaded.getVersion()).isEqualTo(1L);
    }

    @Test
    void testStorageReadFailureIsNotReportedAsAMissingMemory() {
        AiAutoMemory saved = save(buildMemory("unreadable", "Unreadable", null, "1"), WORKSPACE_ID);

        FileStorageAiAutoMemoryRepository failingRepository = new FileStorageAiAutoMemoryRepository(
            new FilesystemFileStorageService(tempDir.toString()) {

                @Override
                public String readFileToString(String directory, FileEntry fileEntry) {
                    throw new FileStorageException("Storage is unavailable");
                }
            });

        assertThatThrownBy(() -> failingRepository.findOwnedById(OWNER, saved.getId()))
            .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(
            () -> failingRepository.findAllByOwnerAndName(OWNER, "unreadable"))
                .isInstanceOf(FileStorageException.class);
    }

    /**
     * On S3 the lookup of an object deleted after the existence check fails with a {@link FileStorageException}. That
     * is the memory being gone, not a storage failure: a read finds nothing and a delete reports the concurrent change.
     */
    @Test
    void testAFileDeletedBetweenTheExistenceCheckAndTheLookupIsGone() {
        AiAutoMemory saved = save(buildMemory("racing", "Racing", null, "1"), WORKSPACE_ID);
        AiAutoMemory other = save(buildMemory("other", "Other", null, "1"), WORKSPACE_ID);

        FileStorageAiAutoMemoryRepository racingRepository = new FileStorageAiAutoMemoryRepository(
            new FilesystemFileStorageService(tempDir.toString()) {

                @Override
                public FileEntry getFileEntry(String directory, String filename) {
                    deleteFile(directory, super.getFileEntry(directory, filename));

                    throw new FileStorageException("File " + filename + " doesn't exist");
                }
            });

        assertThat(racingRepository.findOwnedById(OWNER, saved.getId())).isEmpty();
        assertThatThrownBy(() -> racingRepository.delete(other))
            .isInstanceOf(OptimisticLockingFailureException.class)
            .hasMessageContaining("no longer exists");
    }

    @Test
    void testADocumentDeletedBetweenListingAndReadingIsSkipped() {
        save(buildMemory("kept", "Kept", null, "1"), WORKSPACE_ID);

        AtomicReference<String> racingFilename = new AtomicReference<>();

        FileStorageAiAutoMemoryRepository racingRepository = new FileStorageAiAutoMemoryRepository(
            new FilesystemFileStorageService(tempDir.toString()) {

                @Override
                public String readFileToString(String directory, FileEntry fileEntry) {
                    if (fileEntry.getName()
                        .equals(racingFilename.get())) {

                        deleteFile(directory, fileEntry);

                        throw new FileStorageException("File " + fileEntry.getName() + " doesn't exist");
                    }

                    return super.readFileToString(directory, fileEntry);
                }
            });

        Environment environment = Environment.values()[ENVIRONMENT];

        racingFilename.set(save(buildMemory("racing", "Racing", null, "1"), WORKSPACE_ID).getId() + ".json");

        assertThat(racingRepository.findByOwner(OWNER, null)).extracting(AiAutoMemory::getName)
            .containsExactly("kept");

        racingFilename.set(save(buildMemory("racing", "Racing", null, "1"), WORKSPACE_ID).getId() + ".json");

        assertThat(racingRepository.findByWorkspace(WORKSPACE_ID, environment, null))
            .extracting(AiAutoMemory::getName)
            .containsExactly("kept");

        racingFilename.set(save(buildMemory("racing", "Racing", null, "1"), WORKSPACE_ID).getId() + ".json");

        assertThat(racingRepository.listPrincipals(WORKSPACE_ID, environment))
            .extracting(AiAutoMemoryPrincipalCount::memoryCount)
            .containsExactly(1);
    }

    @Test
    void testALookupFailureForAFileThatStillExistsPropagates() {
        AiAutoMemory saved = save(buildMemory("present", "Present", null, "1"), WORKSPACE_ID);

        FileStorageAiAutoMemoryRepository failingRepository = new FileStorageAiAutoMemoryRepository(
            new FilesystemFileStorageService(tempDir.toString()) {

                @Override
                public FileEntry getFileEntry(String directory, String filename) {
                    throw new FileStorageException("Storage is unavailable");
                }
            });

        assertThatThrownBy(() -> failingRepository.findOwnedById(OWNER, saved.getId()))
            .isInstanceOf(FileStorageException.class);
        assertThatThrownBy(() -> failingRepository.delete(saved))
            .isInstanceOf(FileStorageException.class);
    }

    @Test
    void testLookupOfAPrincipalWithoutMemoriesCreatesNoDirectory() throws IOException {
        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, 1)).isEmpty();
        assertThat(
            aiAutoMemoryRepository.findByOwner(OWNER, null))
                .isEmpty();

        try (Stream<Path> paths = Files.walk(tempDir)) {
            assertThat(paths.anyMatch(path -> path.toString()
                .contains(FileStorageAiAutoMemoryRepository.ROOT_DIRECTORY))).isFalse();
        }
    }

    @Test
    void testQueryForPrincipalWithoutMemoriesIsEmpty() {
        assertThat(
            aiAutoMemoryRepository.findByOwner(
                new AiAutoMemoryOwner(
                    WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, 999, Environment.values()[ENVIRONMENT]),
                null))
                    .isEmpty();
    }

    @Test
    void testUnreadableDocumentIsSkippedInsteadOfFailingTheWholeListing() {
        AiAutoMemory readable = save(buildMemory("readable", "Readable", null, "1"), WORKSPACE_ID);

        fileStorageService.storeFileContent(DIRECTORY, "1.json", "{\"id\": 1, \"name\":", false);

        // Stored under its own id, so only the out-of-range memory type ordinal makes it unreadable.
        fileStorageService.storeFileContent(
            DIRECTORY, "2.json", JsonUtils.write(
                new AiAutoMemoryDocument(
                    2, WORKSPACE_ID, PRINCIPAL_ID, PRINCIPAL_TYPE, "bad_ordinal", "Bad", null, 99, ENVIRONMENT, "x",
                    null, null, 0L)),
            false);

        assertThat(
            aiAutoMemoryRepository.findByOwner(OWNER, null))
                .extracting(AiAutoMemory::getName)
                .containsExactly("readable");
        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, readable.getId())).isPresent();
        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, 1)).isEmpty();
        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, 2)).isEmpty();
        assertThat(aiAutoMemoryRepository.listPrincipals(WORKSPACE_ID, Environment.values()[ENVIRONMENT])).hasSize(1);
    }

    /**
     * A memory stored before a format rule was tightened is still a memory: the JDBC binding loads such a row, and
     * skipping it here would hide it from the agent and let a create reuse its name.
     */
    @Test
    void testDocumentBreakingAFormatRuleStillLoads() {
        fileStorageService.storeFileContent(
            DIRECTORY, "3.json", JsonUtils.write(
                new AiAutoMemoryDocument(
                    3, WORKSPACE_ID, PRINCIPAL_ID, PRINCIPAL_TYPE, "Legacy Name", "Two\nlines", null, 0, ENVIRONMENT,
                    "body", null, null, 2L)),
            false);

        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, 3))
            .hasValueSatisfying(memory -> {
                assertThat(memory.getName()).isEqualTo("Legacy Name");
                assertThat(memory.getTitle()).isEqualTo("Two\nlines");
                assertThat(memory.getVersion()).isEqualTo(2L);
            });
        assertThat(
            aiAutoMemoryRepository.findAllByOwnerAndName(OWNER, "Legacy Name"))
                .hasSize(1);
    }

    @Test
    void testGeneratedIdsStayWithinTheJavaScriptSafeIntegerRange() {
        for (int index = 0; index < 1_000; index++) {
            assertThat(FileStorageAiAutoMemoryRepository.generateId())
                .isBetween(1L, FileStorageAiAutoMemoryRepository.MAX_SAFE_ID);
        }

        AiAutoMemory saved = save(buildMemory("safe_id", "Safe", null, "1"), WORKSPACE_ID);

        assertThat(saved.getId()).isBetween(1L, (1L << 53) - 1);
    }

    private AiAutoMemory save(AiAutoMemory aiAutoMemory, long workspaceId) {
        AiAutoMemory placedMemory = new AiAutoMemory(
            new AiAutoMemoryOwner(
                workspaceId, aiAutoMemory.getPrincipalType(), aiAutoMemory.getPrincipalId(),
                aiAutoMemory.getEnvironment()));

        placedMemory.setName(aiAutoMemory.getName());
        placedMemory.setTitle(aiAutoMemory.getTitle());
        placedMemory.setDescription(aiAutoMemory.getDescription());
        placedMemory.setContent(aiAutoMemory.getContent());
        placedMemory.setMemoryType(aiAutoMemory.getMemoryType());
        placedMemory.setCreatedAt(aiAutoMemory.getCreatedAt());
        placedMemory.setUpdatedAt(aiAutoMemory.getUpdatedAt());

        return aiAutoMemoryRepository.save(placedMemory);
    }

    private static AiAutoMemory buildMemory(String name, String title, String description, String content) {
        AiAutoMemory aiAutoMemory = new AiAutoMemory(
            new AiAutoMemoryOwner(
                WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, Environment.values()[ENVIRONMENT]));

        aiAutoMemory.setName(name);
        aiAutoMemory.setTitle(title);
        aiAutoMemory.setDescription(description);
        aiAutoMemory.setContent(content);
        aiAutoMemory.setMemoryType(AiAutoMemoryType.USER);
        aiAutoMemory.setCreatedAt(BASE_TIME);
        aiAutoMemory.setUpdatedAt(BASE_TIME);

        return aiAutoMemory;
    }
}
