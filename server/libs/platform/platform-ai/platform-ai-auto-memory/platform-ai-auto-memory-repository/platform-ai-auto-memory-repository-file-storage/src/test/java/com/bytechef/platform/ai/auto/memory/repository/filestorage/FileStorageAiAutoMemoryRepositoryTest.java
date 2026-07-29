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

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.file.storage.filesystem.service.FilesystemFileStorageService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
class FileStorageAiAutoMemoryRepositoryTest {

    private static final long WORKSPACE_ID = 7;
    private static final long OTHER_WORKSPACE_ID = 8;
    private static final long PRINCIPAL_ID = 42;
    private static final int PRINCIPAL_TYPE = 0;
    private static final int ENVIRONMENT = 1;
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 12, 0);

    @TempDir
    private static Path tempDir;

    private FileStorageAiAutoMemoryRepository aiAutoMemoryRepository;

    @BeforeAll
    static void beforeAll() {
        JsonUtils.setObjectMapper(JsonMapper.builder()
            .build());
    }

    @BeforeEach
    void beforeEach() {
        aiAutoMemoryRepository = new FileStorageAiAutoMemoryRepository(
            new FilesystemFileStorageService(tempDir.toString()));

        aiAutoMemoryRepository.deleteAll();
    }

    @Test
    void testSaveAndFindByIdRoundTripsEveryField() {
        AiAutoMemory saved = aiAutoMemoryRepository.save(
            buildMemory("preferred_tone", "Preferred tone", "How the user likes replies", "concise"), WORKSPACE_ID);

        AiAutoMemory found = aiAutoMemoryRepository.findById(saved.getId())
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
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void testWorkspaceQueryReturnsNewestUpdatedFirst() {
        AiAutoMemory first = buildMemory("first", "First", null, "1");
        AiAutoMemory second = buildMemory("second", "Second", null, "2");

        // Distinct timestamps, inserted oldest-last, so a backend returning storage order would fail.
        first.setUpdatedAt(BASE_TIME.plusMinutes(10));
        second.setUpdatedAt(BASE_TIME);

        aiAutoMemoryRepository.save(second, WORKSPACE_ID);
        aiAutoMemoryRepository.save(first, WORKSPACE_ID);

        List<AiAutoMemory> memories =
            aiAutoMemoryRepository.findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
                WORKSPACE_ID, PRINCIPAL_TYPE, PRINCIPAL_ID, ENVIRONMENT);

        assertThat(memories).extracting(AiAutoMemory::getId)
            .containsExactly(first.getId(), second.getId());
    }

    @Test
    void testWorkspaceQueryExcludesOtherWorkspaces() {
        aiAutoMemoryRepository.save(buildMemory("mine", "Mine", null, "1"), WORKSPACE_ID);
        aiAutoMemoryRepository.save(buildMemory("theirs", "Theirs", null, "2"), OTHER_WORKSPACE_ID);

        assertThat(
            aiAutoMemoryRepository.findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
                WORKSPACE_ID, PRINCIPAL_TYPE, PRINCIPAL_ID, ENVIRONMENT))
                    .extracting(AiAutoMemory::getName)
                    .containsExactly("mine");
    }

    @Test
    void testWorkspaceQueryNarrowsByMemoryType() {
        aiAutoMemoryRepository.save(buildMemory("user_memory", "User", null, "1"), WORKSPACE_ID);

        AiAutoMemory feedback = buildMemory("feedback_memory", "Feedback", null, "2");

        feedback.setMemoryType(AiAutoMemoryType.FEEDBACK);

        aiAutoMemoryRepository.save(feedback, WORKSPACE_ID);

        assertThat(
            aiAutoMemoryRepository
                .findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
                    WORKSPACE_ID, PRINCIPAL_TYPE, PRINCIPAL_ID, ENVIRONMENT, AiAutoMemoryType.FEEDBACK.ordinal()))
                        .extracting(AiAutoMemory::getName)
                        .containsExactly("feedback_memory");
    }

    @Test
    void testFindAllByNameReturnsEveryMatch() {
        aiAutoMemoryRepository.save(buildMemory("duplicate", "One", null, "1"), WORKSPACE_ID);
        aiAutoMemoryRepository.save(buildMemory("duplicate", "Two", null, "2"), WORKSPACE_ID);

        assertThat(
            aiAutoMemoryRepository.findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
                WORKSPACE_ID, PRINCIPAL_TYPE, PRINCIPAL_ID, ENVIRONMENT, "duplicate"))
                    .hasSize(2);
    }

    @Test
    void testDeleteRemovesOnlyTheTarget() {
        AiAutoMemory kept = aiAutoMemoryRepository.save(buildMemory("kept", "Kept", null, "1"), WORKSPACE_ID);
        AiAutoMemory removed = aiAutoMemoryRepository.save(buildMemory("removed", "Removed", null, "2"), WORKSPACE_ID);

        aiAutoMemoryRepository.delete(removed);

        assertThat(aiAutoMemoryRepository.findById(removed.getId())).isEmpty();
        assertThat(aiAutoMemoryRepository.findById(kept.getId())).isPresent();
    }

    @Test
    void testQueryForPrincipalWithoutMemoriesIsEmpty() {
        assertThat(
            aiAutoMemoryRepository.findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
                WORKSPACE_ID, PRINCIPAL_TYPE, 999, ENVIRONMENT))
                    .isEmpty();
    }

    private static AiAutoMemory buildMemory(String name, String title, String description, String content) {
        AiAutoMemory aiAutoMemory = new AiAutoMemory(AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID);

        aiAutoMemory.setName(name);
        aiAutoMemory.setTitle(title);
        aiAutoMemory.setDescription(description);
        aiAutoMemory.setContent(content);
        aiAutoMemory.setMemoryType(AiAutoMemoryType.USER);
        aiAutoMemory.setEnvironment(Environment.values()[ENVIRONMENT]);
        aiAutoMemory.setCreatedAt(BASE_TIME);
        aiAutoMemory.setUpdatedAt(BASE_TIME);

        return aiAutoMemory;
    }
}
