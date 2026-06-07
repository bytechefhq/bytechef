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
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.ai.auto.memory.WorkspaceAiAutoMemory;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.ai.auto.memory.repository.WorkspaceAiAutoMemoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link AiAutoMemoryRepository} via the JDBC binding {@link JdbcAiAutoMemoryRepository}.
 * Workspace dimension comes from {@code workspace_ai_auto_memory} via the JOIN-based queries; the entity itself is
 * workspace-agnostic. Rows are owned by a principal discriminated by {@code principal_type}/{@code principal_id}.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiAutoMemoryRepositoryIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class AiAutoMemoryRepositoryIntTest {

    private static final int DEV = Environment.DEVELOPMENT.ordinal();
    private static final int STAGING = Environment.STAGING.ordinal();
    private static final int USER = AiAutoMemoryPrincipalType.USER.ordinal();

    @Autowired
    private AiAutoMemoryRepository aiMemoryRepository;

    @Autowired
    private WorkspaceAiAutoMemoryRepository workspaceAiMemoryRepository;

    @AfterEach
    public void afterEach() {
        workspaceAiMemoryRepository.deleteAll();
        aiMemoryRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindByName() {
        long memoryId = saveMemoryWithMembership(1L, 10L, "user_profile", AiAutoMemoryType.USER, DEV);

        assertThat(memoryId).isPositive();

        List<AiAutoMemory> found = aiMemoryRepository
            .findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
                1L, USER, 10L, DEV, "user_profile");

        assertThat(found).hasSize(1);
        assertThat(found.get(0)
            .getTitle()).isEqualTo("Title: user_profile");
        assertThat(found.get(0)
            .getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
    }

    @Test
    public void testFindByWorkspaceIdAndPrincipalIdOrderByUpdatedAtDesc() {
        AiAutoMemory older = buildMemory(10L, "older", AiAutoMemoryType.USER);

        older.setUpdatedAt(LocalDateTime.now()
            .minusMinutes(10));

        AiAutoMemory newer = buildMemory(10L, "newer", AiAutoMemoryType.FEEDBACK);

        newer.setUpdatedAt(LocalDateTime.now());

        AiAutoMemory savedOlder = aiMemoryRepository.save(older);
        AiAutoMemory savedNewer = aiMemoryRepository.save(newer);

        workspaceAiMemoryRepository.save(new WorkspaceAiAutoMemory(1L, savedOlder.getId()));
        workspaceAiMemoryRepository.save(new WorkspaceAiAutoMemory(1L, savedNewer.getId()));

        List<AiAutoMemory> all = aiMemoryRepository
            .findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(1L, USER, 10L, DEV);

        assertThat(all).hasSize(2);
        assertThat(all.get(0)
            .getName()).isEqualTo("newer");
        assertThat(all.get(1)
            .getName()).isEqualTo("older");
    }

    @Test
    public void testFindByMemoryTypeFiltersCorrectly() {
        saveMemoryWithMembership(1L, 10L, "a", AiAutoMemoryType.USER, DEV);
        saveMemoryWithMembership(1L, 10L, "b", AiAutoMemoryType.FEEDBACK, DEV);
        saveMemoryWithMembership(1L, 10L, "c", AiAutoMemoryType.USER, DEV);

        List<AiAutoMemory> userTyped = aiMemoryRepository
            .findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
                1L, USER, 10L, DEV, AiAutoMemoryType.USER.ordinal());

        assertThat(userTyped).hasSize(2);
        assertThat(userTyped)
            .extracting(AiAutoMemory::getName)
            .containsExactlyInAnyOrder("a", "c");
    }

    @Test
    public void testRelationUniqueConstraintOnWorkspaceAndMemory() {
        long memoryId = saveMemoryWithMembership(1L, 10L, "shared", AiAutoMemoryType.USER, DEV);

        // Same (workspace_id, ai_auto_memory_id) pair → DB rejects the second insert. The unique constraint on the
        // relation prevents the same workspace from claiming the same memory twice.
        assertThatThrownBy(() -> workspaceAiMemoryRepository.save(new WorkspaceAiAutoMemory(1L, memoryId)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void testSameNameAcrossEnvironmentsIsAllowed() {
        AiAutoMemory dev = buildMemory(10L, "shared", AiAutoMemoryType.USER);
        AiAutoMemory staging = buildMemory(10L, "shared", AiAutoMemoryType.USER);

        staging.setEnvironment(Environment.STAGING);

        AiAutoMemory savedDev = aiMemoryRepository.save(dev);
        AiAutoMemory savedStaging = aiMemoryRepository.save(staging);

        workspaceAiMemoryRepository.save(new WorkspaceAiAutoMemory(1L, savedDev.getId()));
        workspaceAiMemoryRepository.save(new WorkspaceAiAutoMemory(1L, savedStaging.getId()));

        List<AiAutoMemory> devRows = aiMemoryRepository
            .findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(1L, USER, 10L, DEV, "shared");
        List<AiAutoMemory> stagingRows = aiMemoryRepository
            .findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(1L, USER, 10L, STAGING, "shared");

        assertThat(devRows).hasSize(1);
        assertThat(stagingRows).hasSize(1);
        assertThat(devRows.get(0)
            .getId()).isNotEqualTo(stagingRows.get(0)
                .getId());
    }

    @Test
    public void testFindByWorkspaceIdAndPrincipalIdFiltersOtherPrincipals() {
        saveMemoryWithMembership(1L, 10L, "alice_profile", AiAutoMemoryType.USER, DEV);
        saveMemoryWithMembership(1L, 20L, "bob_profile", AiAutoMemoryType.USER, DEV);

        List<AiAutoMemory> aliceMemories = aiMemoryRepository
            .findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(1L, USER, 10L, DEV);

        assertThat(aliceMemories).hasSize(1);
        assertThat(aliceMemories.get(0)
            .getPrincipalId()).isEqualTo(10L);
    }

    @Test
    public void testRelationCascadeDeletesMembershipWhenMemoryRemoved() {
        long memoryId = saveMemoryWithMembership(1L, 10L, "to-delete", AiAutoMemoryType.USER, DEV);

        Optional<WorkspaceAiAutoMemory> beforeDelete = workspaceAiMemoryRepository
            .findByWorkspaceIdAndAiAutoMemoryId(1L, memoryId);

        assertThat(beforeDelete).isPresent();

        aiMemoryRepository.deleteById(memoryId);

        Optional<WorkspaceAiAutoMemory> afterDelete = workspaceAiMemoryRepository
            .findByWorkspaceIdAndAiAutoMemoryId(1L, memoryId);

        assertThat(afterDelete).isEmpty();
    }

    @Test
    public void testPrincipalTypeIsolatesRows() {
        AiAutoMemory userRow = new AiAutoMemory(AiAutoMemoryPrincipalType.USER, 100L);

        userRow.setName("shared-name");
        userRow.setTitle("u");
        userRow.setContent("user-content");
        userRow.setMemoryType(AiAutoMemoryType.USER);
        userRow.setEnvironment(Environment.DEVELOPMENT);
        userRow.setCreatedAt(LocalDateTime.now());
        userRow.setUpdatedAt(LocalDateTime.now());

        userRow = aiMemoryRepository.save(userRow);

        workspaceAiMemoryRepository.save(new WorkspaceAiAutoMemory(1L, userRow.getId()));

        AiAutoMemory deploymentRow = new AiAutoMemory(AiAutoMemoryPrincipalType.DEPLOYMENT, 100L);

        deploymentRow.setName("shared-name");
        deploymentRow.setTitle("d");
        deploymentRow.setContent("deployment-content");
        deploymentRow.setMemoryType(AiAutoMemoryType.USER);
        deploymentRow.setEnvironment(Environment.DEVELOPMENT);
        deploymentRow.setCreatedAt(LocalDateTime.now());
        deploymentRow.setUpdatedAt(LocalDateTime.now());

        deploymentRow = aiMemoryRepository.save(deploymentRow);

        workspaceAiMemoryRepository.save(new WorkspaceAiAutoMemory(1L, deploymentRow.getId()));

        List<AiAutoMemory> userHits = aiMemoryRepository
            .findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
                1L, AiAutoMemoryPrincipalType.USER.ordinal(), 100L, DEV, "shared-name");

        assertThat(userHits).hasSize(1);
        assertThat(userHits.get(0)
            .getContent()).isEqualTo("user-content");
    }

    private long saveMemoryWithMembership(
        long workspaceId, long principalId, String name, AiAutoMemoryType memoryType, int environmentOrdinal) {

        AiAutoMemory memory = buildMemory(principalId, name, memoryType);

        memory.setEnvironment(Environment.values()[environmentOrdinal]);

        AiAutoMemory saved = aiMemoryRepository.save(memory);

        workspaceAiMemoryRepository.save(new WorkspaceAiAutoMemory(workspaceId, saved.getId()));

        return saved.getId();
    }

    private static AiAutoMemory buildMemory(long principalId, String name, AiAutoMemoryType memoryType) {
        AiAutoMemory memory = new AiAutoMemory(AiAutoMemoryPrincipalType.USER, principalId);

        memory.setName(name);
        memory.setTitle("Title: " + name);
        memory.setDescription("Description for " + name);
        memory.setMemoryType(memoryType);
        memory.setContent("Body content for " + name);
        memory.setEnvironment(Environment.DEVELOPMENT);
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
