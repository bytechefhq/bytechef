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

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import java.util.List;
import org.junit.jupiter.api.Test;

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
 * Subclasses supply only the binding under test. Placing a memory in a workspace is no longer backend-specific — it is
 * a field on the memory for every backend — so this class does it.
 * </p>
 *
 * @author Ivica Cardic
 */
public abstract class AiAutoMemoryRepositoryContractTests {

    protected static final long WORKSPACE_ID = 1;
    protected static final long OTHER_WORKSPACE_ID = 2;
    protected static final long PRINCIPAL_ID = 10;
    protected static final int PRINCIPAL_TYPE = 0;
    protected static final int ENVIRONMENT = 1;

    private static final java.time.LocalDateTime BASE_TIME =
        java.time.LocalDateTime.of(2026, 1, 1, 12, 0);

    /**
     * The binding under test.
     */
    protected abstract AiAutoMemoryRepository getAiAutoMemoryRepository();

    /**
     * Persists the memory into the given workspace.
     */
    protected AiAutoMemory saveInWorkspace(AiAutoMemory aiAutoMemory, long workspaceId) {
        aiAutoMemory.setWorkspaceId(workspaceId);

        return getAiAutoMemoryRepository().save(aiAutoMemory);
    }

    @Test
    void testFindByWorkspaceReturnsOnlyThatWorkspacesMemories() {
        saveInWorkspace(buildMemory("mine", AiAutoMemoryType.USER), WORKSPACE_ID);
        saveInWorkspace(buildMemory("theirs", AiAutoMemoryType.USER), OTHER_WORKSPACE_ID);

        assertThat(findByWorkspace(WORKSPACE_ID))
            .extracting(AiAutoMemory::getName)
            .containsExactly("mine");
    }

    @Test
    void testFindByWorkspaceOrdersByUpdatedAtDescending() {
        AiAutoMemory older = buildMemory("older", AiAutoMemoryType.USER);
        AiAutoMemory newer = buildMemory("newer", AiAutoMemoryType.USER);

        // Insert the newer-updated memory FIRST so a backend that returns insertion or storage order fails here.
        newer.setUpdatedAt(BASE_TIME.plusMinutes(10));
        older.setUpdatedAt(BASE_TIME);

        AiAutoMemory savedNewer = saveInWorkspace(newer, WORKSPACE_ID);
        AiAutoMemory savedOlder = saveInWorkspace(older, WORKSPACE_ID);

        assertThat(findByWorkspace(WORKSPACE_ID))
            .extracting(AiAutoMemory::getId)
            .containsExactly(savedNewer.getId(), savedOlder.getId());
    }

    @Test
    void testFindByWorkspaceNarrowsByMemoryType() {
        saveInWorkspace(buildMemory("user_scoped", AiAutoMemoryType.USER), WORKSPACE_ID);
        saveInWorkspace(buildMemory("feedback_scoped", AiAutoMemoryType.FEEDBACK), WORKSPACE_ID);

        assertThat(
            getAiAutoMemoryRepository()
                .findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndMemoryTypeOrderByUpdatedAtDesc(
                    WORKSPACE_ID, PRINCIPAL_TYPE, PRINCIPAL_ID, ENVIRONMENT, AiAutoMemoryType.FEEDBACK.ordinal()))
                        .extracting(AiAutoMemory::getName)
                        .containsExactly("feedback_scoped");
    }

    @Test
    void testFindAllByNameReturnsEveryMatch() {
        saveInWorkspace(buildMemory("duplicate", AiAutoMemoryType.USER), WORKSPACE_ID);
        saveInWorkspace(buildMemory("duplicate", AiAutoMemoryType.USER), WORKSPACE_ID);

        assertThat(
            getAiAutoMemoryRepository().findAllByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentAndName(
                WORKSPACE_ID, PRINCIPAL_TYPE, PRINCIPAL_ID, ENVIRONMENT, "duplicate"))
                    .hasSize(2);
    }

    @Test
    void testFindByIdRoundTripsTheStoredMemory() {
        AiAutoMemory saved = saveInWorkspace(buildMemory("round_trip", AiAutoMemoryType.USER), WORKSPACE_ID);

        AiAutoMemory found = getAiAutoMemoryRepository().findById(saved.getId())
            .orElseThrow();

        assertThat(found.getName()).isEqualTo("round_trip");
        assertThat(found.getContent()).isEqualTo("content");
        assertThat(found.getPrincipalId()).isEqualTo(PRINCIPAL_ID);
        assertThat(found.getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
    }

    @Test
    void testDeleteRemovesOnlyTheTargetedMemory() {
        AiAutoMemory kept = saveInWorkspace(buildMemory("kept", AiAutoMemoryType.USER), WORKSPACE_ID);
        AiAutoMemory removed = saveInWorkspace(buildMemory("removed", AiAutoMemoryType.USER), WORKSPACE_ID);

        getAiAutoMemoryRepository().deleteById(removed.getId());

        assertThat(getAiAutoMemoryRepository().findById(removed.getId())).isEmpty();
        assertThat(getAiAutoMemoryRepository().findById(kept.getId())).isPresent();
    }

    @Test
    void testUnknownPrincipalYieldsNoMemories() {
        saveInWorkspace(buildMemory("present", AiAutoMemoryType.USER), WORKSPACE_ID);

        assertThat(
            getAiAutoMemoryRepository()
                .findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
                    WORKSPACE_ID, PRINCIPAL_TYPE, 9999, ENVIRONMENT))
                        .isEmpty();
    }

    private List<AiAutoMemory> findByWorkspace(long workspaceId) {
        return getAiAutoMemoryRepository()
            .findByWorkspaceIdAndPrincipalTypeAndPrincipalIdAndEnvironmentOrderByUpdatedAtDesc(
                workspaceId, PRINCIPAL_TYPE, PRINCIPAL_ID, ENVIRONMENT);
    }

    protected AiAutoMemory buildMemory(String name, AiAutoMemoryType memoryType) {
        AiAutoMemory aiAutoMemory = new AiAutoMemory(AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID);

        aiAutoMemory.setName(name);
        aiAutoMemory.setTitle("Title");
        aiAutoMemory.setContent("content");
        aiAutoMemory.setMemoryType(memoryType);
        aiAutoMemory.setEnvironment(resolveEnvironment());

        // The service layer owns timestamps for every backend, so the contract supplies them rather than expecting
        // a repository to invent them.
        aiAutoMemory.setCreatedAt(BASE_TIME);
        aiAutoMemory.setUpdatedAt(BASE_TIME);

        return aiAutoMemory;
    }

    private static com.bytechef.platform.configuration.domain.Environment resolveEnvironment() {
        return com.bytechef.platform.configuration.domain.Environment.values()[ENVIRONMENT];
    }
}
