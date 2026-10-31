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

package com.bytechef.platform.ai.auto.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import com.bytechef.platform.ai.auto.memory.config.AiAutoMemoryIntTestConfiguration;
import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AiAutoMemoryIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AiAutoMemoryServiceIntTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long PRINCIPAL_ID = 10L;
    private static final Environment ENVIRONMENT = Environment.DEVELOPMENT;

    private static final AiAutoMemoryOwner OWNER = new AiAutoMemoryOwner(
        WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, ENVIRONMENT);

    private static final AiAutoMemoryOwner OTHER_OWNER = new AiAutoMemoryOwner(
        WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID + 1, ENVIRONMENT);

    private static final LocalDateTime NOW = LocalDateTime.now(AiAutoMemoryIntTestConfiguration.CLOCK);

    @Autowired
    private AiAutoMemoryService aiAutoMemoryService;

    @MockitoSpyBean
    private AiAutoMemoryRepository aiAutoMemoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM ai_auto_memory");
    }

    @Test
    void testCreatePersistsWithTimestamps() {
        AiAutoMemory created = aiAutoMemoryService.create(
            OWNER, "user_profile", "User Profile", "User preferences", AiAutoMemoryType.USER,
            "Alice prefers concise replies");

        assertThat(created.getId()).isNotNull();

        AiAutoMemory persisted = findPersisted(OWNER, created.getId());

        assertThat(persisted.getName()).isEqualTo("user_profile");
        assertThat(persisted.getTitle()).isEqualTo("User Profile");
        assertThat(persisted.getDescription()).isEqualTo("User preferences");
        assertThat(persisted.getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(persisted.getContent()).isEqualTo("Alice prefers concise replies");
        assertThat(persisted.getCreatedAt()).isEqualTo(NOW);
        assertThat(persisted.getUpdatedAt()).isEqualTo(NOW);
        assertThat(persisted.getOwner()).isEqualTo(OWNER);
    }

    @Test
    void testCreateRejectsDuplicateName() {
        AiAutoMemory existing = saveMemory(OWNER, "dup", AiAutoMemoryType.USER, NOW);

        assertThatThrownBy(
            () -> aiAutoMemoryService.create(OWNER, "dup", "title", null, AiAutoMemoryType.USER, "content"))
                .isInstanceOf(DuplicateAiAutoMemoryNameException.class);

        assertThat(aiAutoMemoryRepository.findAllByOwnerAndName(OWNER, "dup"))
            .singleElement()
            .extracting(AiAutoMemory::getContent)
            .isEqualTo(existing.getContent());
    }

    @Test
    void testCreateTranslatesConcurrentDuplicateInsert() throws InterruptedException {
        Throwable failure = failureOfCallBlockedByConcurrentTransaction(
            () -> saveMemory(OWNER, "dup", AiAutoMemoryType.USER, NOW),
            () -> aiAutoMemoryService.create(OWNER, "dup", "title", null, AiAutoMemoryType.USER, "content"));

        assertThat(failure)
            .isInstanceOf(DuplicateAiAutoMemoryNameException.class)
            .hasCauseInstanceOf(DuplicateKeyException.class);
        assertThat(aiAutoMemoryRepository.findAllByOwnerAndName(OWNER, "dup")).hasSize(1);
    }

    @Test
    void testCreateRequiresMemoryType() {
        assertThatThrownBy(() -> aiAutoMemoryService.create(OWNER, "name", "title", null, null, "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("memoryType");

        assertThat(countRows()).isZero();
    }

    @Test
    void testCreateRejectsNamesTheEntityWouldRefuse() {
        String tooLongName = "a".repeat(65);

        assertThatThrownBy(
            () -> aiAutoMemoryService.create(OWNER, tooLongName, "title", null, AiAutoMemoryType.USER, "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1-64 characters");
        assertThatThrownBy(
            () -> aiAutoMemoryService.create(OWNER, "notes/today", "title", null, AiAutoMemoryType.USER, "content"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(countRows()).isZero();
    }

    @Test
    void testUserAndProjectDeploymentMemoriesDoNotCollide() {
        AiAutoMemoryOwner deploymentOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID, ENVIRONMENT);

        AiAutoMemory userMemory = aiAutoMemoryService.create(
            OWNER, "notes", "t", null, AiAutoMemoryType.USER, "user-body");
        AiAutoMemory deploymentMemory = aiAutoMemoryService.create(
            deploymentOwner, "notes", "t", null, AiAutoMemoryType.USER, "deployment-body");

        assertThat(userMemory.getOwner()).isEqualTo(OWNER);
        assertThat(deploymentMemory.getOwner()).isEqualTo(deploymentOwner);

        assertThat(aiAutoMemoryService.read(OWNER, "notes"))
            .map(AiAutoMemory::getContent)
            .hasValue("user-body");
        assertThat(aiAutoMemoryService.read(deploymentOwner, "notes"))
            .map(AiAutoMemory::getContent)
            .hasValue("deployment-body");
    }

    @Test
    void testUpdateRejectsAnEditBasedOnAnOlderVersion() {
        AiAutoMemory existing = saveMemory(OWNER, "user_profile", AiAutoMemoryType.USER, NOW);

        long staleVersion = existing.getVersion();

        aiAutoMemoryService.update(
            OWNER, "user_profile", staleVersion, new AiAutoMemoryPatch(null, null, null, "first edit"));

        assertThatThrownBy(
            () -> aiAutoMemoryService.update(
                OWNER, "user_profile", staleVersion, new AiAutoMemoryPatch(null, null, null, "stale edit")))
                    .isInstanceOf(AiAutoMemoryConcurrentModificationException.class);

        assertThat(findPersisted(OWNER, existing.getId()).getContent()).isEqualTo("first edit");
    }

    @Test
    void testUpdateAppliesAnEditBasedOnTheCurrentVersion() {
        AiAutoMemory existing = saveMemory(OWNER, "user_profile", AiAutoMemoryType.USER, NOW);

        AiAutoMemory updated = aiAutoMemoryService.update(
            OWNER, "user_profile", existing.getVersion(), new AiAutoMemoryPatch(null, null, null, "current edit"));

        assertThat(updated.getContent()).isEqualTo("current edit");
        assertThat(findPersisted(OWNER, existing.getId()).getContent()).isEqualTo("current edit");
    }

    @Test
    void testUpdateTranslatesAnOptimisticLockFailure() throws InterruptedException {
        AiAutoMemory existing = saveMemory(OWNER, "user_profile", AiAutoMemoryType.USER, NOW);

        Throwable failure = failureOfCallBlockedByConcurrentTransaction(
            () -> bumpVersion(existing.getId(), "concurrent edit"),
            () -> aiAutoMemoryService.update(
                OWNER, "user_profile", existing.getVersion(), new AiAutoMemoryPatch(null, null, null, "edit")));

        assertThat(failure)
            .isInstanceOf(AiAutoMemoryConcurrentModificationException.class)
            .hasCauseInstanceOf(OptimisticLockingFailureException.class);
        assertThat(findPersisted(OWNER, existing.getId()).getContent()).isEqualTo("concurrent edit");
    }

    @Test
    void testDeleteTranslatesAnOptimisticLockFailure() throws InterruptedException {
        AiAutoMemory existing = saveMemory(OWNER, "user_profile", AiAutoMemoryType.USER, NOW);

        Throwable failure = failureOfCallBlockedByConcurrentTransaction(
            () -> bumpVersion(existing.getId(), "concurrent edit"),
            () -> aiAutoMemoryService.delete(OWNER, "user_profile"));

        assertThat(failure).isInstanceOf(AiAutoMemoryConcurrentModificationException.class);
        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, existing.getId())).isPresent();
    }

    @Test
    void testUpdateAppliesPartialPatch() {
        AiAutoMemory existing = saveMemory(OWNER, "user_profile", AiAutoMemoryType.USER, NOW.minusDays(1));

        AiAutoMemory updated = aiAutoMemoryService.update(
            OWNER, "user_profile", existing.getVersion(), new AiAutoMemoryPatch(null, null, null, "new content"));

        assertThat(updated.getContent()).isEqualTo("new content");

        AiAutoMemory persisted = findPersisted(OWNER, existing.getId());

        assertThat(persisted.getContent()).isEqualTo("new content");
        assertThat(persisted.getTitle()).isEqualTo("Title: user_profile");
        assertThat(persisted.getDescription()).isEqualTo("Description: user_profile");
        assertThat(persisted.getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(persisted.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void testUpdateWithBlankDescriptionClearsIt() {
        AiAutoMemory existing = saveMemory(OWNER, "user_profile", AiAutoMemoryType.USER, NOW);

        AiAutoMemory updated = aiAutoMemoryService.update(
            OWNER, "user_profile", existing.getVersion(), new AiAutoMemoryPatch(null, "", null, null));

        assertThat(updated.getDescription()).isNull();
        assertThat(findPersisted(OWNER, existing.getId()).getDescription()).isNull();
    }

    @Test
    void testUpdateReportsAMissingMemoryWithoutInternalIds() {
        assertThatThrownBy(
            () -> aiAutoMemoryService.update(OWNER, "missing", 0L, new AiAutoMemoryPatch("title", null, null, null)))
                .isInstanceOf(AiAutoMemoryNotFoundException.class)
                .hasMessage("Memory 'missing' not found");
    }

    @Test
    void testRenameRejectsExistingTarget() {
        AiAutoMemory source = saveMemory(OWNER, "source", AiAutoMemoryType.USER, NOW);
        AiAutoMemory target = saveMemory(OWNER, "target", AiAutoMemoryType.USER, NOW);

        assertThatThrownBy(() -> aiAutoMemoryService.rename(OWNER, "source", "target"))
            .isInstanceOf(DuplicateAiAutoMemoryNameException.class);

        assertThat(findPersisted(OWNER, source.getId()).getName()).isEqualTo("source");
        assertThat(findPersisted(OWNER, target.getId()).getName()).isEqualTo("target");
    }

    @Test
    void testRenameTranslatesAnOptimisticLockFailure() throws InterruptedException {
        AiAutoMemory source = saveMemory(OWNER, "source", AiAutoMemoryType.USER, NOW);

        Throwable failure = failureOfCallBlockedByConcurrentTransaction(
            () -> bumpVersion(source.getId(), "concurrent edit"),
            () -> aiAutoMemoryService.rename(OWNER, "source", "target"));

        assertThat(failure)
            .isInstanceOf(AiAutoMemoryConcurrentModificationException.class)
            .hasCauseInstanceOf(OptimisticLockingFailureException.class);
        assertThat(findPersisted(OWNER, source.getId()).getName()).isEqualTo("source");
    }

    @Test
    void testRenameTranslatesConcurrentDuplicateUpdate() throws InterruptedException {
        AiAutoMemory source = saveMemory(OWNER, "source", AiAutoMemoryType.USER, NOW);

        Throwable failure = failureOfCallBlockedByConcurrentTransaction(
            () -> saveMemory(OWNER, "target", AiAutoMemoryType.USER, NOW),
            () -> aiAutoMemoryService.rename(OWNER, "source", "target"));

        assertThat(failure)
            .isInstanceOf(DuplicateAiAutoMemoryNameException.class)
            .hasCauseInstanceOf(DuplicateKeyException.class);
        assertThat(findPersisted(OWNER, source.getId()).getName()).isEqualTo("source");
    }

    @Test
    void testRenameChangesTheNameAndTouchesUpdatedAt() {
        AiAutoMemory source = saveMemory(OWNER, "source", AiAutoMemoryType.USER, NOW.minusDays(1));

        AiAutoMemory renamed = aiAutoMemoryService.rename(OWNER, "source", "target");

        assertThat(renamed.getName()).isEqualTo("target");

        AiAutoMemory persisted = findPersisted(OWNER, source.getId());

        assertThat(persisted.getName()).isEqualTo("target");
        assertThat(persisted.getUpdatedAt()).isEqualTo(NOW);
        assertThat(aiAutoMemoryService.read(OWNER, "source")).isEmpty();
    }

    @Test
    void testRenameToTheSameNameSavesNothing() {
        AiAutoMemory existing = saveMemory(OWNER, "same", AiAutoMemoryType.USER, NOW.minusDays(1));

        AiAutoMemory unchanged = aiAutoMemoryService.rename(OWNER, "same", "same");

        assertThat(unchanged.getName()).isEqualTo("same");

        AiAutoMemory persisted = findPersisted(OWNER, existing.getId());

        assertThat(persisted.getVersion()).isEqualTo(existing.getVersion());
        assertThat(persisted.getUpdatedAt()).isEqualTo(NOW.minusDays(1));
    }

    @Test
    void testRenameRejectsAnInvalidNameBeforeAnyLookup() {
        // The source does not exist: a lookup before validation would surface AiAutoMemoryNotFoundException.
        assertThatThrownBy(() -> aiAutoMemoryService.rename(OWNER, "source", "Not A Slug"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testListFiltersByType() {
        saveMemory(OWNER, "a", AiAutoMemoryType.FEEDBACK, NOW);
        saveMemory(OWNER, "b", AiAutoMemoryType.USER, NOW);
        saveMemory(OTHER_OWNER, "c", AiAutoMemoryType.FEEDBACK, NOW);

        List<AiAutoMemory> result = aiAutoMemoryService.list(OWNER, AiAutoMemoryType.FEEDBACK);

        assertThat(result).extracting(AiAutoMemory::getName)
            .containsExactly("a");
    }

    @Test
    void testFindByIdReturnsTheRowForItsOwner() {
        AiAutoMemory memory = saveMemory(OWNER, "mine", AiAutoMemoryType.USER, NOW);

        assertThat(aiAutoMemoryService.findById(OWNER, memory.getId())).isPresent();
        assertThat(aiAutoMemoryService.findById(OTHER_OWNER, memory.getId())).isEmpty();
    }

    @Test
    void testUpdateByIdUpdatesTheRowForItsOwner() {
        AiAutoMemory memory = saveMemory(OWNER, "mine", AiAutoMemoryType.USER, NOW);

        AiAutoMemory updated = aiAutoMemoryService.updateById(
            OWNER, memory.getId(), memory.getVersion(), new AiAutoMemoryPatch("New title", null, null, null));

        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(findPersisted(OWNER, memory.getId()).getTitle()).isEqualTo("New title");
    }

    @Test
    void testUpdateByIdRejectsAnEditBasedOnAnOlderVersion() {
        AiAutoMemory memory = saveMemory(OWNER, "mine", AiAutoMemoryType.USER, NOW);

        long staleVersion = memory.getVersion();

        aiAutoMemoryService.updateById(
            OWNER, memory.getId(), staleVersion, new AiAutoMemoryPatch(null, null, null, "first edit"));

        assertThatThrownBy(
            () -> aiAutoMemoryService.updateById(
                OWNER, memory.getId(), staleVersion, new AiAutoMemoryPatch(null, null, null, "stale edit")))
                    .isInstanceOf(AiAutoMemoryConcurrentModificationException.class)
                    .hasMessageContaining("'mine'");

        assertThat(findPersisted(OWNER, memory.getId()).getContent()).isEqualTo("first edit");
    }

    @Test
    void testUpdateByIdAppliesAnEditBasedOnTheCurrentVersion() {
        AiAutoMemory memory = saveMemory(OWNER, "mine", AiAutoMemoryType.USER, NOW);

        AiAutoMemory updated = aiAutoMemoryService.updateById(
            OWNER, memory.getId(), memory.getVersion(), new AiAutoMemoryPatch(null, null, null, "current edit"));

        assertThat(updated.getContent()).isEqualTo("current edit");
        assertThat(findPersisted(OWNER, memory.getId()).getContent()).isEqualTo("current edit");
    }

    @Test
    void testUpdateByIdThrowsNotFoundWhenTheCallerOwnsNoSuchRow() {
        AiAutoMemory foreignMemory = saveMemory(OTHER_OWNER, "theirs", AiAutoMemoryType.USER, NOW);

        assertThatThrownBy(
            () -> aiAutoMemoryService.updateById(
                OWNER, foreignMemory.getId(), foreignMemory.getVersion(),
                new AiAutoMemoryPatch("New title", null, null, null)))
                    .isInstanceOf(AiAutoMemoryNotFoundException.class)
                    .hasMessage("Memory not found");

        assertThat(findPersisted(OTHER_OWNER, foreignMemory.getId()).getTitle()).isEqualTo("Title: theirs");
    }

    @Test
    void testDeleteByIdDeletesTheRowForItsOwner() {
        AiAutoMemory memory = saveMemory(OWNER, "mine", AiAutoMemoryType.USER, NOW);

        aiAutoMemoryService.deleteById(OWNER, memory.getId());

        assertThat(aiAutoMemoryRepository.findOwnedById(OWNER, memory.getId())).isEmpty();
    }

    @Test
    void testDeleteByIdThrowsNotFoundWhenTheCallerOwnsNoSuchRow() {
        AiAutoMemory foreignMemory = saveMemory(OTHER_OWNER, "theirs", AiAutoMemoryType.USER, NOW);

        assertThatThrownBy(() -> aiAutoMemoryService.deleteById(OWNER, foreignMemory.getId()))
            .isInstanceOf(AiAutoMemoryNotFoundException.class)
            .hasMessage("Memory not found");

        assertThat(aiAutoMemoryRepository.findOwnedById(OTHER_OWNER, foreignMemory.getId())).isPresent();
    }

    @Test
    void testListAllOwnersPassesEnvironmentAndTypeThrough() {
        AiAutoMemoryOwner stagingOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, Environment.STAGING);
        AiAutoMemoryOwner otherStagingOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.STAGING);

        saveMemory(stagingOwner, "mine", AiAutoMemoryType.FEEDBACK, NOW);
        saveMemory(otherStagingOwner, "theirs", AiAutoMemoryType.FEEDBACK, NOW);
        saveMemory(stagingOwner, "wrong_type", AiAutoMemoryType.USER, NOW);
        saveMemory(OWNER, "wrong_environment", AiAutoMemoryType.FEEDBACK, NOW);

        assertThat(aiAutoMemoryService.listAllOwners(WORKSPACE_ID, Environment.STAGING, AiAutoMemoryType.FEEDBACK))
            .extracting(AiAutoMemory::getName)
            .containsExactlyInAnyOrder("mine", "theirs");
    }

    @Test
    void testListAllOwnersWithoutATypeListsEveryType() {
        AiAutoMemoryOwner productionOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, Environment.PRODUCTION);

        saveMemory(productionOwner, "feedback", AiAutoMemoryType.FEEDBACK, NOW);
        saveMemory(productionOwner, "user", AiAutoMemoryType.USER, NOW);
        saveMemory(OWNER, "wrong_environment", AiAutoMemoryType.USER, NOW);

        assertThat(aiAutoMemoryService.listAllOwners(WORKSPACE_ID, Environment.PRODUCTION, null))
            .extracting(AiAutoMemory::getName)
            .containsExactlyInAnyOrder("feedback", "user");
    }

    @Test
    void testListPrincipalsPassesTheEnvironmentThrough() {
        AiAutoMemoryOwner deploymentOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.PRODUCTION);
        AiAutoMemoryOwner developmentDeploymentOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, Environment.DEVELOPMENT);

        saveMemory(deploymentOwner, "first", AiAutoMemoryType.USER, NOW);
        saveMemory(deploymentOwner, "second", AiAutoMemoryType.FEEDBACK, NOW);
        saveMemory(developmentDeploymentOwner, "elsewhere", AiAutoMemoryType.USER, NOW);

        assertThat(aiAutoMemoryService.listPrincipals(WORKSPACE_ID, Environment.PRODUCTION))
            .containsExactly(new AiAutoMemoryPrincipalCount(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 2));
    }

    @Test
    void testCreateRejectsABlankTitleOrContentBeforeAnyLookup() {
        saveMemory(OWNER, "note", AiAutoMemoryType.USER, NOW);

        // A name that already exists: a lookup before validation would surface DuplicateAiAutoMemoryNameException.
        assertThatThrownBy(() -> aiAutoMemoryService.create(OWNER, "note", " ", null, AiAutoMemoryType.USER, "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title");
        assertThatThrownBy(() -> aiAutoMemoryService.create(OWNER, "note", "Title", null, AiAutoMemoryType.USER, "\n"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("content");

        assertThat(countRows()).isEqualTo(1);
    }

    @Test
    void testUpdateRejectsABlankTitleOrContentAndSavesNothing() {
        AiAutoMemory existing = saveMemory(OWNER, "user_profile", AiAutoMemoryType.USER, NOW);

        AiAutoMemoryPatch blankTitlePatch = new AiAutoMemoryPatch(" ", null, null, null);
        AiAutoMemoryPatch blankContentPatch = new AiAutoMemoryPatch(null, null, null, "");

        assertThatThrownBy(
            () -> aiAutoMemoryService.update(OWNER, "user_profile", existing.getVersion(), blankTitlePatch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
        assertThatThrownBy(
            () -> aiAutoMemoryService.update(OWNER, "user_profile", existing.getVersion(), blankContentPatch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content");

        AiAutoMemory persisted = findPersisted(OWNER, existing.getId());

        assertThat(persisted.getVersion()).isEqualTo(existing.getVersion());
        assertThat(persisted.getTitle()).isEqualTo("Title: user_profile");
        assertThat(persisted.getContent()).isEqualTo("body: user_profile");
    }

    @Test
    void testRenameOfAMissingMemoryThrowsNotFound() {
        assertThatThrownBy(() -> aiAutoMemoryService.rename(OWNER, "missing", "new_name"))
            .isInstanceOf(AiAutoMemoryNotFoundException.class);

        assertThat(countRows()).isZero();
    }

    @Test
    void testReadReturnsTheFirstOfSeveralMatches() {
        AiAutoMemory first = saveMemory(OWNER, "dup", AiAutoMemoryType.USER, NOW);
        AiAutoMemory second = saveMemory(OWNER, "dup_other", AiAutoMemoryType.USER, NOW);

        // The JDBC binding's unique constraint makes two same-named rows impossible in Postgres (only other
        // bindings can return several matches), so the spy returns two persisted rows for the one name.
        doReturn(List.of(first, second)).when(aiAutoMemoryRepository)
            .findAllByOwnerAndName(OWNER, "dup");

        assertThat(aiAutoMemoryService.read(OWNER, "dup")).containsSame(first);
    }

    private void bumpVersion(long memoryId, String content) {
        jdbcTemplate.update(
            "UPDATE ai_auto_memory SET content = ?, version = version + 1 WHERE id = ?", content, memoryId);
    }

    private long countRows() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_auto_memory", Long.class);

        return count == null ? 0 : count;
    }

    /**
     * Runs {@code lockingStatement} in a transaction that stays open until {@code serviceCall}, started on another
     * thread, is blocked by Postgres on the row or unique-index lock that statement took. The transaction then
     * commits, so the service call proceeds against the concurrently committed state, the way a real race does.
     */
    private Throwable failureOfCallBlockedByConcurrentTransaction(Runnable lockingStatement, Runnable serviceCall)
        throws InterruptedException {

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AtomicReference<Future<?>> futureReference = new AtomicReference<>();

        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(transactionStatus -> {
                lockingStatement.run();

                futureReference.set(executorService.submit(serviceCall));

                awaitBlockedOnLock();
            });

            Future<?> future = futureReference.get();

            try {
                future.get(30, TimeUnit.SECONDS);
            } catch (ExecutionException executionException) {
                return executionException.getCause();
            } catch (TimeoutException timeoutException) {
                throw new IllegalStateException("The concurrent service call did not finish", timeoutException);
            }

            throw new AssertionError("The concurrent service call was expected to fail");
        } finally {
            executorService.shutdownNow();
        }
    }

    private void awaitBlockedOnLock() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);

        while (System.nanoTime() < deadline) {
            Integer waitingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock'", Integer.class);

            if (waitingCount != null && waitingCount > 0) {
                return;
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                throw new IllegalStateException(interruptedException);
            }
        }

        throw new IllegalStateException("The concurrent service call never blocked on a database lock");
    }

    private AiAutoMemory findPersisted(AiAutoMemoryOwner owner, long memoryId) {
        return aiAutoMemoryRepository.findOwnedById(owner, memoryId)
            .orElseThrow();
    }

    private AiAutoMemory saveMemory(
        AiAutoMemoryOwner owner, String name, AiAutoMemoryType memoryType, LocalDateTime updatedAt) {

        AiAutoMemory memory = new AiAutoMemory(owner);

        memory.setName(name);
        memory.setTitle("Title: " + name);
        memory.setDescription("Description: " + name);
        memory.setMemoryType(memoryType);
        memory.setContent("body: " + name);
        memory.setCreatedAt(updatedAt);
        memory.setUpdatedAt(updatedAt);

        return aiAutoMemoryRepository.save(memory);
    }
}
