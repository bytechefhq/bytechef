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

package com.bytechef.platform.ai.auto.memory.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Behavior every {@link AiAutoMemoryRepository} binding must exhibit, regardless of backend.
 *
 * <p>
 * The repository interface documents a filter shape and an ordering that consumers depend on, but each backend
 * implements them differently — the JDBC binding filters on a column while file backends read and sort stored
 * documents. Running one suite against every binding is what keeps them from drifting apart; a backend that cannot
 * satisfy a case here is a real divergence, not a reason to weaken the case.
 * </p>
 *
 * <p>
 * Subclasses supply only the binding under test.
 * </p>
 *
 * @author Ivica Cardic
 */
public abstract class AiAutoMemoryRepositoryContractTests {

    protected static final long WORKSPACE_ID = 1;

    /**
     * Shares {@link #WORKSPACE_ID}'s leading digit, so a binding that lists a workspace by name prefix would also pick
     * up this one's memories.
     */
    protected static final long OTHER_WORKSPACE_ID = 10;
    protected static final long PRINCIPAL_ID = 10;
    protected static final int ENVIRONMENT = 1;

    private static final AiAutoMemoryOwner OWNER = new AiAutoMemoryOwner(
        WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, Environment.values()[ENVIRONMENT]);

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 12, 0);

    /**
     * The binding under test.
     */
    protected abstract AiAutoMemoryRepository getAiAutoMemoryRepository();

    /**
     * Persists a copy of the memory placed in the given workspace. A memory's owner is fixed at construction, so the
     * copy is the only way to move a built memory into another workspace.
     */
    protected AiAutoMemory saveInWorkspace(AiAutoMemory aiAutoMemory, long workspaceId) {
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

        return getAiAutoMemoryRepository().save(placedMemory);
    }

    @Test
    void testFindByOwnerExcludesTheSamePrincipalInOtherWorkspaces() {
        saveInWorkspace(buildMemory("mine", AiAutoMemoryType.USER), WORKSPACE_ID);
        saveInWorkspace(buildMemory("theirs", AiAutoMemoryType.USER), OTHER_WORKSPACE_ID);

        assertThat(findOwnMemoriesInWorkspace(WORKSPACE_ID))
            .extracting(AiAutoMemory::getName)
            .containsExactly("mine");
    }

    @Test
    void testFindByOwnerOrdersByUpdatedAtDescending() {
        AiAutoMemory older = buildMemory("older", AiAutoMemoryType.USER);
        AiAutoMemory newer = buildMemory("newer", AiAutoMemoryType.USER);

        // Insert the newer-updated memory FIRST so a backend that returns insertion or storage order fails here.
        newer.setUpdatedAt(BASE_TIME.plusMinutes(10));
        older.setUpdatedAt(BASE_TIME);

        AiAutoMemory savedNewer = saveInWorkspace(newer, WORKSPACE_ID);
        AiAutoMemory savedOlder = saveInWorkspace(older, WORKSPACE_ID);

        assertThat(findOwnMemoriesInWorkspace(WORKSPACE_ID))
            .extracting(AiAutoMemory::getId)
            .containsExactly(savedNewer.getId(), savedOlder.getId());
    }

    @Test
    void testFindByOwnerNarrowsByMemoryType() {
        saveInWorkspace(buildMemory("user_scoped", AiAutoMemoryType.USER), WORKSPACE_ID);
        saveInWorkspace(buildMemory("feedback_scoped", AiAutoMemoryType.FEEDBACK), WORKSPACE_ID);

        assertThat(getAiAutoMemoryRepository().findByOwner(OWNER, AiAutoMemoryType.FEEDBACK))
            .extracting(AiAutoMemory::getName)
            .containsExactly("feedback_scoped");
    }

    @Test
    void testFindAllByNameIsScopedToOwnerWorkspaceAndEnvironment() {
        AiAutoMemory mine = saveInWorkspace(buildMemory("shared_name", AiAutoMemoryType.USER), WORKSPACE_ID);

        saveInWorkspace(buildMemory("shared_name", AiAutoMemoryType.USER), OTHER_WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("shared_name", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID + 1,
                ENVIRONMENT),
            WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("shared_name", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID,
                ENVIRONMENT + 1),
            WORKSPACE_ID);

        assertThat(getAiAutoMemoryRepository().findAllByOwnerAndName(OWNER, "shared_name"))
            .extracting(AiAutoMemory::getId)
            .containsExactly(mine.getId());
    }

    /**
     * The owner-typed finders convert the owner to the stored ordinals. The owner here has a principal type and an
     * environment with different ordinals, so a conversion that swapped the two would find nothing.
     */
    @Test
    void testOwnerTypedFindersMatchTheOwnersRowsOnly() {
        AiAutoMemoryOwner deploymentOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID, Environment.PRODUCTION);

        AiAutoMemory feedback = saveInWorkspace(
            buildMemory(
                "feedback_note", AiAutoMemoryType.FEEDBACK, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT,
                PRINCIPAL_ID, Environment.PRODUCTION.ordinal()),
            WORKSPACE_ID);
        AiAutoMemory user = saveInWorkspace(
            buildMemory(
                "user_note", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID,
                Environment.PRODUCTION.ordinal()),
            WORKSPACE_ID);

        saveInWorkspace(buildMemory("feedback_note", AiAutoMemoryType.FEEDBACK), WORKSPACE_ID);

        assertThat(getAiAutoMemoryRepository().findByOwner(deploymentOwner, null))
            .extracting(AiAutoMemory::getId)
            .containsExactlyInAnyOrder(feedback.getId(), user.getId());
        assertThat(getAiAutoMemoryRepository().findByOwner(deploymentOwner, AiAutoMemoryType.FEEDBACK))
            .extracting(AiAutoMemory::getId)
            .containsExactly(feedback.getId());
        assertThat(getAiAutoMemoryRepository().findAllByOwnerAndName(deploymentOwner, "feedback_note"))
            .extracting(AiAutoMemory::getId)
            .containsExactly(feedback.getId());
    }

    @Test
    void testFindOwnedByIdRoundTripsTheStoredMemory() {
        AiAutoMemory memory = buildMemory("round_trip", AiAutoMemoryType.FEEDBACK);

        memory.setDescription("Why it was stored");

        // Distinct from createdAt, so a binding that maps one onto the other fails here.
        memory.setUpdatedAt(BASE_TIME.plusMinutes(5));

        AiAutoMemory saved = saveInWorkspace(memory, WORKSPACE_ID);

        AiAutoMemory found = findOwned(saved);

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getOwner()).isEqualTo(
            new AiAutoMemoryOwner(
                WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, Environment.values()[ENVIRONMENT]));
        assertThat(found.getName()).isEqualTo("round_trip");
        assertThat(found.getTitle()).isEqualTo("Title");
        assertThat(found.getDescription()).isEqualTo("Why it was stored");
        assertThat(found.getContent()).isEqualTo("content");
        assertThat(found.getMemoryType()).isEqualTo(AiAutoMemoryType.FEEDBACK);
        assertThat(found.getCreatedAt()).isEqualTo(BASE_TIME);
        assertThat(found.getUpdatedAt()).isEqualTo(BASE_TIME.plusMinutes(5));
        assertThat(found.getVersion()).isEqualTo(saved.getVersion());
    }

    @Test
    void testFindOwnedByIdHidesAnotherOwnersMemory() {
        AiAutoMemory saved = saveInWorkspace(buildMemory("mine", AiAutoMemoryType.USER), WORKSPACE_ID);

        AiAutoMemoryOwner otherPrincipal = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID + 1, Environment.values()[ENVIRONMENT]);
        AiAutoMemoryOwner otherWorkspace = new AiAutoMemoryOwner(
            OTHER_WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, Environment.values()[ENVIRONMENT]);
        AiAutoMemoryOwner otherEnvironment = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, Environment.values()[ENVIRONMENT + 1]);

        // The same id under another principal type is a different owner: user 10 is not deployment 10.
        AiAutoMemoryOwner otherPrincipalType = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID,
            Environment.values()[ENVIRONMENT]);

        assertThat(getAiAutoMemoryRepository().findOwnedById(otherPrincipal, saved.getId())).isEmpty();
        assertThat(getAiAutoMemoryRepository().findOwnedById(otherWorkspace, saved.getId())).isEmpty();
        assertThat(getAiAutoMemoryRepository().findOwnedById(otherEnvironment, saved.getId())).isEmpty();
        assertThat(getAiAutoMemoryRepository().findOwnedById(otherPrincipalType, saved.getId())).isEmpty();
    }

    @Test
    void testSaveOfALoadedMemoryUpdatesItInPlace() {
        AiAutoMemory saved = saveInWorkspace(buildMemory("before", AiAutoMemoryType.USER), WORKSPACE_ID);

        AiAutoMemory loaded = findOwned(saved);
        Long loadedVersion = loaded.getVersion();

        loaded.setName("after");
        loaded.setContent("changed");
        loaded.setUpdatedAt(BASE_TIME.plusMinutes(5));

        AiAutoMemory returned = getAiAutoMemoryRepository().save(loaded);

        AiAutoMemory reloaded = findOwned(saved);

        assertThat(returned.getVersion()).isEqualTo(reloaded.getVersion());
        assertThat(reloaded.getName()).isEqualTo("after");
        assertThat(reloaded.getContent()).isEqualTo("changed");
        assertThat(reloaded.getUpdatedAt()).isEqualTo(BASE_TIME.plusMinutes(5));
        assertThat(reloaded.getVersion()).isGreaterThan(loadedVersion);
        assertThat(findOwnMemoriesInWorkspace(WORKSPACE_ID)).extracting(AiAutoMemory::getId)
            .containsExactly(saved.getId());
        assertThat(findAllByName("before")).isEmpty();
        assertThat(findAllByName("after")).extracting(AiAutoMemory::getId)
            .containsExactly(saved.getId());
    }

    @Test
    void testSaveOfAStaleCopyIsRejected() {
        AiAutoMemory saved = saveInWorkspace(buildMemory("contended", AiAutoMemoryType.USER), WORKSPACE_ID);

        AiAutoMemory firstCopy = findOwned(saved);
        AiAutoMemory secondCopy = findOwned(saved);

        firstCopy.setContent("first writer");

        getAiAutoMemoryRepository().save(firstCopy);

        secondCopy.setContent("second writer");

        assertThatThrownBy(() -> getAiAutoMemoryRepository().save(secondCopy))
            .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(findOwned(saved).getContent()).isEqualTo("first writer");
    }

    @Test
    void testSaveOfACopyWhoseMemoryWasDeletedIsRejected() {
        AiAutoMemory saved = saveInWorkspace(buildMemory("deleted", AiAutoMemoryType.USER), WORKSPACE_ID);

        AiAutoMemory deletedCopy = findOwned(saved);
        AiAutoMemory editedCopy = findOwned(saved);

        getAiAutoMemoryRepository().delete(deletedCopy);

        editedCopy.setContent("edited after the delete");

        assertThatThrownBy(() -> getAiAutoMemoryRepository().save(editedCopy))
            .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(getAiAutoMemoryRepository().findOwnedById(saved.getOwner(), saved.getId())).isEmpty();
        assertThat(getAiAutoMemoryRepository().findByOwner(OWNER, null)).isEmpty();
    }

    /**
     * A memory carrying another owner's id and current version must not reach that owner's row — neither overwrite it,
     * owner columns included, nor delete it.
     */
    @Test
    void testSaveUnderAnotherOwnerIsRejected() {
        AiAutoMemory victim = saveInWorkspace(buildMemory("victim", AiAutoMemoryType.USER), OTHER_WORKSPACE_ID);

        AiAutoMemory forged = restoreForOwner(OWNER, findOwned(victim), "forged");

        assertThatThrownBy(() -> getAiAutoMemoryRepository().save(forged))
            .isInstanceOf(OptimisticLockingFailureException.class);

        AiAutoMemory stored = findOwned(victim);

        assertThat(stored.getOwner()).isEqualTo(victim.getOwner());
        assertThat(stored.getContent()).isEqualTo("content");
        assertThat(getAiAutoMemoryRepository().findByOwner(OWNER, null)).isEmpty();
    }

    @Test
    void testDeleteUnderAnotherOwnerIsRejected() {
        AiAutoMemory victim = saveInWorkspace(buildMemory("victim", AiAutoMemoryType.USER), OTHER_WORKSPACE_ID);

        AiAutoMemory forged = restoreForOwner(OWNER, findOwned(victim), "content");

        assertThatThrownBy(() -> getAiAutoMemoryRepository().delete(forged))
            .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(getAiAutoMemoryRepository().findOwnedById(victim.getOwner(), victim.getId())).isPresent();
    }

    @Test
    void testDeleteRemovesOnlyTheTargetedMemory() {
        AiAutoMemory kept = saveInWorkspace(buildMemory("kept", AiAutoMemoryType.USER), WORKSPACE_ID);
        AiAutoMemory removed = saveInWorkspace(buildMemory("removed", AiAutoMemoryType.USER), WORKSPACE_ID);

        getAiAutoMemoryRepository().delete(findOwned(removed));

        assertThat(getAiAutoMemoryRepository().findOwnedById(removed.getOwner(), removed.getId())).isEmpty();
        assertThat(getAiAutoMemoryRepository().findOwnedById(kept.getOwner(), kept.getId())).isPresent();
    }

    @Test
    void testDeleteOfAMemoryThatIsAlreadyGoneIsRejected() {
        AiAutoMemory saved = saveInWorkspace(buildMemory("gone", AiAutoMemoryType.USER), WORKSPACE_ID);

        AiAutoMemory firstCopy = findOwned(saved);
        AiAutoMemory secondCopy = findOwned(saved);

        getAiAutoMemoryRepository().delete(firstCopy);

        assertThatThrownBy(() -> getAiAutoMemoryRepository().delete(secondCopy))
            .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void testDeleteOfAStaleCopyIsRejected() {
        AiAutoMemory saved = saveInWorkspace(buildMemory("edited", AiAutoMemoryType.USER), WORKSPACE_ID);

        AiAutoMemory staleCopy = findOwned(saved);
        AiAutoMemory editedCopy = findOwned(saved);

        editedCopy.setContent("edited meanwhile");

        getAiAutoMemoryRepository().save(editedCopy);

        assertThatThrownBy(() -> getAiAutoMemoryRepository().delete(staleCopy))
            .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(findOwned(saved).getContent()).isEqualTo("edited meanwhile");
    }

    @Test
    void testUnknownPrincipalYieldsNoMemories() {
        saveInWorkspace(buildMemory("present", AiAutoMemoryType.USER), WORKSPACE_ID);

        AiAutoMemoryOwner unknownOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, 9999, Environment.values()[ENVIRONMENT]);

        assertThat(getAiAutoMemoryRepository().findByOwner(unknownOwner, null)).isEmpty();
    }

    @Test
    void testListPrincipalsReturnsDistinctOwnersWithCounts() {
        saveInWorkspace(
            buildMemory("a", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT), WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("b", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT), WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("c", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5, ENVIRONMENT),
            WORKSPACE_ID);

        List<AiAutoMemoryPrincipalCount> principals = getAiAutoMemoryRepository()
            .listPrincipals(WORKSPACE_ID, Environment.values()[ENVIRONMENT]);

        assertThat(principals)
            .extracting(
                AiAutoMemoryPrincipalCount::principalType, AiAutoMemoryPrincipalCount::principalId,
                AiAutoMemoryPrincipalCount::memoryCount)
            .containsExactly(
                tuple(AiAutoMemoryPrincipalType.USER, 1L, 2),
                tuple(AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 1));
    }

    @Test
    void testListPrincipalsExcludesOtherWorkspacesAndEnvironments() {
        saveInWorkspace(
            buildMemory("a", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT), WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("b", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 2, ENVIRONMENT),
            OTHER_WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("c", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 3, ENVIRONMENT + 1), WORKSPACE_ID);

        List<AiAutoMemoryPrincipalCount> principals = getAiAutoMemoryRepository()
            .listPrincipals(WORKSPACE_ID, Environment.values()[ENVIRONMENT]);

        assertThat(principals)
            .extracting(AiAutoMemoryPrincipalCount::principalId)
            .containsExactly(1L);
    }

    @Test
    void testFindByWorkspaceAndEnvironmentSpansEveryOwner() {
        saveInWorkspace(
            buildMemory("a", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT), WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("b", AiAutoMemoryType.PROJECT, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5, ENVIRONMENT),
            WORKSPACE_ID);

        assertThat(findAllOwners(null))
            .extracting(AiAutoMemory::getName)
            .containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void testFindByWorkspaceAndEnvironmentOrdersByUpdatedAtDescendingAcrossOwners() {
        AiAutoMemory newest = buildMemory(
            "newest", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5, ENVIRONMENT);
        AiAutoMemory middle = buildMemory(
            "middle", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT);
        AiAutoMemory oldest = buildMemory(
            "oldest", AiAutoMemoryType.PROJECT, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5, ENVIRONMENT);

        newest.setUpdatedAt(BASE_TIME.plusMinutes(20));
        middle.setUpdatedAt(BASE_TIME.plusMinutes(10));
        oldest.setUpdatedAt(BASE_TIME);

        // Saved oldest-last and interleaved across owners, so neither storage order nor per-owner grouping passes.
        saveInWorkspace(middle, WORKSPACE_ID);
        saveInWorkspace(newest, WORKSPACE_ID);
        saveInWorkspace(oldest, WORKSPACE_ID);

        assertThat(findAllOwners(null))
            .extracting(AiAutoMemory::getName)
            .containsExactly("newest", "middle", "oldest");
        assertThat(findAllOwners(AiAutoMemoryType.USER))
            .extracting(AiAutoMemory::getName)
            .containsExactly("newest", "middle");
    }

    @Test
    void testFindByWorkspaceAndEnvironmentExcludesOtherWorkspacesAndEnvironments() {
        saveInWorkspace(
            buildMemory("a", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT), WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("b", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 2, ENVIRONMENT),
            OTHER_WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("c", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 3, ENVIRONMENT + 1), WORKSPACE_ID);

        assertThat(findAllOwners(null))
            .extracting(AiAutoMemory::getName)
            .containsExactly("a");
    }

    @Test
    void testFindByWorkspaceAndEnvironmentNarrowsByMemoryType() {
        saveInWorkspace(
            buildMemory("a", AiAutoMemoryType.USER, AiAutoMemoryPrincipalType.USER, 1, ENVIRONMENT), WORKSPACE_ID);
        saveInWorkspace(
            buildMemory("b", AiAutoMemoryType.PROJECT, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5, ENVIRONMENT),
            WORKSPACE_ID);

        assertThat(findAllOwners(AiAutoMemoryType.PROJECT))
            .extracting(AiAutoMemory::getName)
            .containsExactly("b");
    }

    private AiAutoMemory findOwned(AiAutoMemory memory) {
        return getAiAutoMemoryRepository().findOwnedById(memory.getOwner(), memory.getId())
            .orElseThrow();
    }

    private List<AiAutoMemory> findAllByName(String name) {
        return getAiAutoMemoryRepository().findAllByOwnerAndName(OWNER, name);
    }

    private List<AiAutoMemory> findAllOwners(AiAutoMemoryType memoryType) {
        return getAiAutoMemoryRepository().findByWorkspace(
            WORKSPACE_ID, Environment.values()[ENVIRONMENT], memoryType);
    }

    private List<AiAutoMemory> findOwnMemoriesInWorkspace(long workspaceId) {
        return getAiAutoMemoryRepository().findByOwner(
            new AiAutoMemoryOwner(
                workspaceId, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, Environment.values()[ENVIRONMENT]),
            null);
    }

    /**
     * A copy of {@code memory} — same id and version — that claims to belong to {@code owner}, as a caller holding a
     * foreign id could build one.
     */
    private static AiAutoMemory restoreForOwner(AiAutoMemoryOwner owner, AiAutoMemory memory, String content) {
        return AiAutoMemory.restore(
            owner, memory.getId(), memory.getName(), memory.getTitle(), memory.getDescription(),
            memory.getMemoryType(), content, memory.getCreatedAt(), memory.getUpdatedAt(), memory.getVersion());
    }

    protected AiAutoMemory buildMemory(String name, AiAutoMemoryType memoryType) {
        return buildMemory(name, memoryType, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, ENVIRONMENT);
    }

    /**
     * Builds a memory owned by an explicit principal in an explicit environment, in {@link #WORKSPACE_ID} until
     * {@link #saveInWorkspace} places it.
     */
    protected AiAutoMemory buildMemory(
        String name, AiAutoMemoryType memoryType, AiAutoMemoryPrincipalType principalType, long principalId,
        int environment) {

        AiAutoMemory aiAutoMemory = new AiAutoMemory(
            new AiAutoMemoryOwner(WORKSPACE_ID, principalType, principalId, Environment.values()[environment]));

        aiAutoMemory.setName(name);
        aiAutoMemory.setTitle("Title");
        aiAutoMemory.setContent("content");
        aiAutoMemory.setMemoryType(memoryType);

        // The service layer owns timestamps for every backend, so the contract supplies them rather than expecting
        // a repository to invent them.
        aiAutoMemory.setCreatedAt(BASE_TIME);
        aiAutoMemory.setUpdatedAt(BASE_TIME);

        return aiAutoMemory;
    }
}
