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

package com.bytechef.platform.ai.auto.memory.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiAutoMemoryRepositoryIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class AiAutoMemoryRepositoryIntTest {

    private static final int DEV = Environment.DEVELOPMENT.ordinal();
    private static final int STAGING = Environment.STAGING.ordinal();

    @Autowired
    private AiAutoMemoryRepository aiMemoryRepository;

    @Autowired
    private NamedParameterJdbcOperations namedParameterJdbcOperations;

    @AfterEach
    void afterEach() {
        namedParameterJdbcOperations.getJdbcOperations()
            .update("DELETE FROM ai_auto_memory");
    }

    @Test
    void testSaveAndFindByName() {
        long memoryId = saveMemoryInWorkspace(1L, 10L, "user_profile", AiAutoMemoryType.USER, DEV);

        assertThat(memoryId).isPositive();

        List<AiAutoMemory> found = aiMemoryRepository.findAllByOwnerAndName(
            owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT), "user_profile");

        assertThat(found).hasSize(1);
        assertThat(found.get(0)
            .getTitle()).isEqualTo("Title: user_profile");
        assertThat(found.get(0)
            .getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
    }

    @Test
    void testSaveRejectsDuplicateNameForSameOwner() {
        saveMemoryInWorkspace(1L, 10L, "user_profile", AiAutoMemoryType.USER, DEV);

        assertThatThrownBy(() -> saveMemoryInWorkspace(1L, 10L, "user_profile", AiAutoMemoryType.FEEDBACK, DEV))
            .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void testSaveAllowsSameNameForDifferentOwnerWorkspaceOrEnvironment() {
        saveMemoryInWorkspace(1L, 10L, "user_profile", AiAutoMemoryType.USER, DEV);
        saveMemoryInWorkspace(1L, 11L, "user_profile", AiAutoMemoryType.USER, DEV);
        saveMemoryInWorkspace(2L, 10L, "user_profile", AiAutoMemoryType.USER, DEV);
        saveMemoryInWorkspace(1L, 10L, "user_profile", AiAutoMemoryType.USER, STAGING);

        assertThat(
            aiMemoryRepository.findAllByOwnerAndName(
                owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT), "user_profile")).hasSize(1);
        assertThat(
            aiMemoryRepository.findAllByOwnerAndName(
                owner(1L, AiAutoMemoryPrincipalType.USER, 11L, Environment.DEVELOPMENT), "user_profile")).hasSize(1);
        assertThat(
            aiMemoryRepository.findAllByOwnerAndName(
                owner(2L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT), "user_profile")).hasSize(1);
        assertThat(
            aiMemoryRepository.findAllByOwnerAndName(
                owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.STAGING), "user_profile")).hasSize(1);
    }

    @Test
    void testFindByWorkspaceIdAndPrincipalIdOrderByUpdatedAtDesc() {
        AiAutoMemory older = buildMemory(1L, 10L, "older", AiAutoMemoryType.USER, Environment.DEVELOPMENT);

        older.setUpdatedAt(LocalDateTime.now()
            .minusMinutes(10));

        AiAutoMemory newer = buildMemory(1L, 10L, "newer", AiAutoMemoryType.FEEDBACK, Environment.DEVELOPMENT);

        newer.setUpdatedAt(LocalDateTime.now());

        aiMemoryRepository.save(older);
        aiMemoryRepository.save(newer);

        List<AiAutoMemory> all = aiMemoryRepository
            .findByOwner(owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT), null);

        assertThat(all).hasSize(2);
        assertThat(all.get(0)
            .getName()).isEqualTo("newer");
        assertThat(all.get(1)
            .getName()).isEqualTo("older");
    }

    @Test
    void testFindByMemoryTypeFiltersCorrectly() {
        saveMemoryInWorkspace(1L, 10L, "a", AiAutoMemoryType.USER, DEV);
        saveMemoryInWorkspace(1L, 10L, "b", AiAutoMemoryType.FEEDBACK, DEV);
        saveMemoryInWorkspace(1L, 10L, "c", AiAutoMemoryType.USER, DEV);

        List<AiAutoMemory> userTyped = aiMemoryRepository.findByOwner(
            owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT), AiAutoMemoryType.USER);

        assertThat(userTyped).hasSize(2);
        assertThat(userTyped)
            .extracting(AiAutoMemory::getName)
            .containsExactlyInAnyOrder("a", "c");
    }

    @Test
    void testRawInsertWithoutWorkspaceIsRejected() {
        assertThatThrownBy(() -> insertRaw(null, "no_workspace"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testRawInsertWithAnIdOrOrdinalTheOwnerWouldRejectIsRejected() {
        assertThatThrownBy(() -> insertRaw(0L, 10L, 0, "zero_workspace"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRaw(1L, 0L, 0, "zero_principal"))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRaw(1L, 10L, -1, "negative_principal_type"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testRawInsertWithNameThatIsNotASlugIsRejected() {
        assertThatThrownBy(() -> insertRaw(1L, "Not A Slug"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testSameNameAcrossEnvironmentsIsAllowed() {
        AiAutoMemory dev = buildMemory(1L, 10L, "shared", AiAutoMemoryType.USER, Environment.DEVELOPMENT);
        AiAutoMemory staging = buildMemory(1L, 10L, "shared", AiAutoMemoryType.USER, Environment.STAGING);

        aiMemoryRepository.save(dev);
        aiMemoryRepository.save(staging);

        List<AiAutoMemory> devRows = aiMemoryRepository
            .findAllByOwnerAndName(owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT), "shared");
        List<AiAutoMemory> stagingRows = aiMemoryRepository
            .findAllByOwnerAndName(owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.STAGING), "shared");

        assertThat(devRows).hasSize(1);
        assertThat(stagingRows).hasSize(1);
        assertThat(devRows.get(0)
            .getId()).isNotEqualTo(stagingRows.get(0)
                .getId());
    }

    @Test
    void testFindByWorkspaceIdAndPrincipalIdFiltersOtherPrincipals() {
        saveMemoryInWorkspace(1L, 10L, "alice_profile", AiAutoMemoryType.USER, DEV);
        saveMemoryInWorkspace(1L, 20L, "bob_profile", AiAutoMemoryType.USER, DEV);

        List<AiAutoMemory> aliceMemories = aiMemoryRepository
            .findByOwner(owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT), null);

        assertThat(aliceMemories).hasSize(1);
        assertThat(aliceMemories.get(0)
            .getPrincipalId()).isEqualTo(10L);
    }

    @Test
    void testDeleteRemovesTheRowFromItsWorkspace() {
        long memoryId = saveMemoryInWorkspace(1L, 10L, "to-delete", AiAutoMemoryType.USER, DEV);

        AiAutoMemoryOwner owner = owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT);

        aiMemoryRepository.delete(aiMemoryRepository.findOwnedById(owner, memoryId)
            .orElseThrow());

        assertThat(aiMemoryRepository.findOwnedById(owner, memoryId)).isEmpty();
        assertThat(aiMemoryRepository
            .findByOwner(owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT), null))
                .isEmpty();
    }

    @Test
    void testPrincipalTypeIsolatesRows() {
        AiAutoMemory userRow = new AiAutoMemory(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.USER, 100L, Environment.DEVELOPMENT));

        userRow.setName("shared-name");
        userRow.setTitle("u");
        userRow.setContent("user-content");
        userRow.setMemoryType(AiAutoMemoryType.USER);
        userRow.setCreatedAt(LocalDateTime.now());
        userRow.setUpdatedAt(LocalDateTime.now());

        aiMemoryRepository.save(userRow);

        AiAutoMemory deploymentRow = new AiAutoMemory(
            new AiAutoMemoryOwner(1L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 100L, Environment.DEVELOPMENT));

        deploymentRow.setName("shared-name");
        deploymentRow.setTitle("d");
        deploymentRow.setContent("deployment-content");
        deploymentRow.setMemoryType(AiAutoMemoryType.USER);
        deploymentRow.setCreatedAt(LocalDateTime.now());
        deploymentRow.setUpdatedAt(LocalDateTime.now());

        aiMemoryRepository.save(deploymentRow);

        List<AiAutoMemory> userHits = aiMemoryRepository.findAllByOwnerAndName(
            owner(1L, AiAutoMemoryPrincipalType.USER, 100L, Environment.DEVELOPMENT), "shared-name");

        assertThat(userHits).hasSize(1);
        assertThat(userHits.get(0)
            .getContent()).isEqualTo("user-content");
    }

    @Test
    void testRowsWithAnOrdinalThisBuildDoesNotKnowAreSkipped() {
        AiAutoMemoryOwner owner = owner(1L, AiAutoMemoryPrincipalType.USER, 10L, Environment.DEVELOPMENT);

        long knownMemoryId = saveMemoryInWorkspace(1L, 10L, "known", AiAutoMemoryType.USER, DEV);

        insertRaw(1L, 10L, AiAutoMemoryPrincipalType.USER.ordinal(), "future_type", 99);
        insertRaw(1L, 20L, 99, "future_principal", AiAutoMemoryType.USER.ordinal());

        long futureTypeMemoryId = Objects.requireNonNull(
            namedParameterJdbcOperations.getJdbcOperations()
                .queryForObject("SELECT id FROM ai_auto_memory WHERE name = 'future_type'", Long.class));

        assertThat(aiMemoryRepository.findByOwner(owner, null))
            .extracting(AiAutoMemory::getId)
            .containsExactly(knownMemoryId);
        assertThat(aiMemoryRepository.findAllByOwnerAndName(owner, "future_type")).isEmpty();
        assertThat(aiMemoryRepository.findOwnedById(owner, futureTypeMemoryId)).isEmpty();
        assertThat(aiMemoryRepository.findByWorkspace(1L, Environment.DEVELOPMENT, null))
            .extracting(AiAutoMemory::getId)
            .containsExactly(knownMemoryId);
        assertThat(aiMemoryRepository.findByWorkspace(1L, Environment.DEVELOPMENT, AiAutoMemoryType.USER))
            .extracting(AiAutoMemory::getId)
            .containsExactly(knownMemoryId);
        assertThat(aiMemoryRepository.listPrincipals(1L, Environment.DEVELOPMENT))
            .containsExactly(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.USER, 10L, 1));
    }

    private long saveMemoryInWorkspace(
        long workspaceId, long principalId, String name, AiAutoMemoryType memoryType, int environmentOrdinal) {

        AiAutoMemory memory = buildMemory(
            workspaceId, principalId, name, memoryType, Environment.values()[environmentOrdinal]);

        AiAutoMemory saved = aiMemoryRepository.save(memory);

        return saved.getId();
    }

    private void insertRaw(Long workspaceId, String name) {
        insertRaw(workspaceId, 10L, 0, name);
    }

    private void insertRaw(Long workspaceId, long principalId, int principalType, String name) {
        insertRaw(workspaceId, principalId, principalType, name, AiAutoMemoryType.USER.ordinal());
    }

    private void insertRaw(Long workspaceId, long principalId, int principalType, String name, int memoryType) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        parameters.addValue("workspaceId", workspaceId, Types.BIGINT);
        parameters.addValue("principalId", principalId);
        parameters.addValue("principalType", principalType);
        parameters.addValue("name", name);
        parameters.addValue("memoryType", memoryType);
        parameters.addValue("now", LocalDateTime.now());

        namedParameterJdbcOperations.update(
            "INSERT INTO ai_auto_memory (workspace_id, principal_id, principal_type, name, title, memory_type, "
                + "content, environment, created_at, updated_at) VALUES (:workspaceId, :principalId, :principalType, "
                + ":name, 'Title', :memoryType, 'content', 0, :now, :now)",
            parameters);
    }

    private static AiAutoMemoryOwner owner(
        long workspaceId, AiAutoMemoryPrincipalType principalType, long principalId, Environment environment) {

        return new AiAutoMemoryOwner(workspaceId, principalType, principalId, environment);
    }

    private static AiAutoMemory buildMemory(
        long workspaceId, long principalId, String name, AiAutoMemoryType memoryType, Environment environment) {

        AiAutoMemory memory = new AiAutoMemory(
            new AiAutoMemoryOwner(workspaceId, AiAutoMemoryPrincipalType.USER, principalId, environment));

        memory.setName(name);
        memory.setTitle("Title: " + name);
        memory.setDescription("Description for " + name);
        memory.setMemoryType(memoryType);
        memory.setContent("Body content for " + name);
        memory.setCreatedAt(LocalDateTime.now());
        memory.setUpdatedAt(LocalDateTime.now());

        return memory;
    }

    @EnableAutoConfiguration(exclude = DataJdbcRepositoriesAutoConfiguration.class)
    @Import({
        LiquibaseConfiguration.class,
        AiAutoMemoryRepositoryIntTest.IntTestConfiguration.IntTestJdbcConfiguration.class
    })
    @Configuration
    public static class IntTestConfiguration {

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        @Configuration
        public static class IntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
