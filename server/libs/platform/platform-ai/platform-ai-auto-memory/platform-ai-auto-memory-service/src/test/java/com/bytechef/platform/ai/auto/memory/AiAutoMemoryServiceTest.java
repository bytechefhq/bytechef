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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.auto.memory.repository.AiAutoMemoryRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiAutoMemoryServiceTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long PRINCIPAL_ID = 10L;
    private static final long MEMORY_ID = 11L;
    private static final Environment ENVIRONMENT = Environment.DEVELOPMENT;

    private static final AiAutoMemoryOwner OWNER = new AiAutoMemoryOwner(
        WORKSPACE_ID, AiAutoMemoryPrincipalType.USER, PRINCIPAL_ID, ENVIRONMENT);

    @Mock
    private AiAutoMemoryRepository aiMemoryRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-04-24T10:00:00Z"), ZoneId.of("UTC"));

    @Test
    void testCreatePersistsWithTimestamps() {
        AiAutoMemoryServiceImpl realService = newService();

        stubFindByName(OWNER, "user_profile", List.of());
        when(aiMemoryRepository.save(any(AiAutoMemory.class)))
            .thenAnswer(invocation -> {
                AiAutoMemory memory = invocation.getArgument(0);

                return AiAutoMemory.restore(
                    memory.getOwner(), 42L, memory.getName(), memory.getTitle(), memory.getDescription(),
                    memory.getMemoryType(), memory.getContent(), memory.getCreatedAt(), memory.getUpdatedAt(), 0L);
            });

        AiAutoMemory created = realService.create(
            OWNER, "user_profile", "User Profile", "User preferences", AiAutoMemoryType.USER,
            "Alice prefers concise replies");

        assertThat(created.getId()).isEqualTo(42L);
        assertThat(created.getName()).isEqualTo("user_profile");
        assertThat(created.getMemoryType()).isEqualTo(AiAutoMemoryType.USER);
        assertThat(created.getCreatedAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(created.getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
        assertThat(created.getOwner()).isEqualTo(OWNER);
    }

    @Test
    void testCreateRejectsDuplicateName() {
        AiAutoMemoryServiceImpl realService = newService();

        stubFindByName(OWNER, "dup", List.of(buildMemory(OWNER, "dup")));

        assertThatThrownBy(() -> realService.create(OWNER, "dup", "title", null, AiAutoMemoryType.USER, "content"))
            .isInstanceOf(DuplicateAiAutoMemoryNameException.class);

        verify(aiMemoryRepository, never()).save(any());
    }

    @Test
    void testCreateTranslatesConcurrentDuplicateInsert() {
        AiAutoMemoryServiceImpl realService = newService();

        DuplicateKeyException duplicateKeyException = new DuplicateKeyException("uk_ai_auto_memory_owner_env_name");

        stubFindByName(OWNER, "dup", List.of());
        when(aiMemoryRepository.save(any(AiAutoMemory.class))).thenThrow(duplicateKeyException);

        assertThatThrownBy(() -> realService.create(OWNER, "dup", "title", null, AiAutoMemoryType.USER, "content"))
            .isInstanceOf(DuplicateAiAutoMemoryNameException.class)
            .hasCause(duplicateKeyException);
    }

    @Test
    void testCreateRequiresMemoryType() {
        AiAutoMemoryServiceImpl realService = newService();

        assertThatThrownBy(() -> realService.create(OWNER, "name", "title", null, null, "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("memoryType");
    }

    @Test
    void testCreateRejectsNamesTheEntityWouldRefuse() {
        AiAutoMemoryServiceImpl realService = newService();

        String tooLongName = "a".repeat(65);

        assertThatThrownBy(
            () -> realService.create(OWNER, tooLongName, "title", null, AiAutoMemoryType.USER, "content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1-64 characters");
        assertThatThrownBy(
            () -> realService.create(OWNER, "notes/today", "title", null, AiAutoMemoryType.USER, "content"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(aiMemoryRepository, never()).save(any());
    }

    @Test
    void testUserAndProjectDeploymentMemoriesDoNotCollide() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemoryOwner deploymentOwner = new AiAutoMemoryOwner(
            WORKSPACE_ID, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, PRINCIPAL_ID, ENVIRONMENT);

        stubFindByName(OWNER, "notes", List.of());
        stubFindByName(deploymentOwner, "notes", List.of());
        when(aiMemoryRepository.save(any(AiAutoMemory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiAutoMemory userMemory = realService.create(OWNER, "notes", "t", null, AiAutoMemoryType.USER, "user-body");
        AiAutoMemory deploymentMemory = realService.create(
            deploymentOwner, "notes", "t", null, AiAutoMemoryType.USER, "deployment-body");

        assertThat(userMemory.getOwner()).isEqualTo(OWNER);
        assertThat(deploymentMemory.getOwner()).isEqualTo(deploymentOwner);

        verify(aiMemoryRepository).findAllByOwnerAndName(OWNER, "notes");
        verify(aiMemoryRepository).findAllByOwnerAndName(deploymentOwner, "notes");
    }

    @Test
    void testUpdateRejectsAnEditBasedOnAnOlderVersion() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory existing = buildMemory(OWNER, "user_profile", 3L);

        stubFindByName(OWNER, "user_profile", List.of(existing));

        assertThatThrownBy(
            () -> realService.update(OWNER, "user_profile", 2L, new AiAutoMemoryPatch(null, null, null, "stale edit")))
                .isInstanceOf(AiAutoMemoryConcurrentModificationException.class);

        verify(aiMemoryRepository, never()).save(any(AiAutoMemory.class));
    }

    @Test
    void testUpdateAppliesAnEditBasedOnTheCurrentVersion() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory existing = buildMemory(OWNER, "user_profile", 3L);

        stubFindByName(OWNER, "user_profile", List.of(existing));
        when(aiMemoryRepository.save(any(AiAutoMemory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiAutoMemory updated =
            realService.update(OWNER, "user_profile", 3L, new AiAutoMemoryPatch(null, null, null, "current edit"));

        assertThat(updated.getContent()).isEqualTo("current edit");
    }

    @Test
    void testUpdateTranslatesAnOptimisticLockFailure() {
        AiAutoMemoryServiceImpl realService = newService();

        OptimisticLockingFailureException optimisticLockingFailureException =
            new OptimisticLockingFailureException("stale");

        stubFindByName(OWNER, "user_profile", List.of(buildMemory(OWNER, "user_profile")));
        when(aiMemoryRepository.save(any(AiAutoMemory.class))).thenThrow(optimisticLockingFailureException);

        assertThatThrownBy(
            () -> realService.update(OWNER, "user_profile", 0L, new AiAutoMemoryPatch(null, null, null, "edit")))
                .isInstanceOf(AiAutoMemoryConcurrentModificationException.class)
                .hasCause(optimisticLockingFailureException);
    }

    @Test
    void testDeleteTranslatesAnOptimisticLockFailure() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory memory = buildMemory(OWNER, "user_profile");

        stubFindByName(OWNER, "user_profile", List.of(memory));
        doThrow(new OptimisticLockingFailureException("gone")).when(aiMemoryRepository)
            .delete(memory);

        assertThatThrownBy(() -> realService.delete(OWNER, "user_profile"))
            .isInstanceOf(AiAutoMemoryConcurrentModificationException.class);
    }

    @Test
    void testUpdateAppliesPartialPatch() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory existing = buildMemory(OWNER, "user_profile");

        stubFindByName(OWNER, "user_profile", List.of(existing));
        when(aiMemoryRepository.save(any(AiAutoMemory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiAutoMemory updated =
            realService.update(OWNER, "user_profile", 0L, new AiAutoMemoryPatch(null, null, null, "new content"));

        assertThat(updated.getContent()).isEqualTo("new content");
        assertThat(updated.getTitle()).isEqualTo("Title: user_profile");
        assertThat(updated.getDescription()).isEqualTo("Description: user_profile");
        assertThat(updated.getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void testUpdateWithBlankDescriptionClearsIt() {
        AiAutoMemoryServiceImpl realService = newService();

        stubFindByName(OWNER, "user_profile", List.of(buildMemory(OWNER, "user_profile")));
        when(aiMemoryRepository.save(any(AiAutoMemory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiAutoMemory updated =
            realService.update(OWNER, "user_profile", 0L, new AiAutoMemoryPatch(null, "", null, null));

        assertThat(updated.getDescription()).isNull();
    }

    @Test
    void testUpdateReportsAMissingMemoryWithoutInternalIds() {
        AiAutoMemoryServiceImpl realService = newService();

        stubFindByName(OWNER, "missing", List.of());

        assertThatThrownBy(
            () -> realService.update(OWNER, "missing", 0L, new AiAutoMemoryPatch("title", null, null, null)))
                .isInstanceOf(AiAutoMemoryNotFoundException.class)
                .hasMessage("Memory 'missing' not found");
    }

    @Test
    void testRenameRejectsExistingTarget() {
        AiAutoMemoryServiceImpl realService = newService();

        stubFindByName(OWNER, "target", List.of(buildMemory(OWNER, "target")));

        assertThatThrownBy(() -> realService.rename(OWNER, "source", "target"))
            .isInstanceOf(DuplicateAiAutoMemoryNameException.class);
    }

    @Test
    void testRenameTranslatesAnOptimisticLockFailure() {
        AiAutoMemoryServiceImpl realService = newService();

        OptimisticLockingFailureException optimisticLockingFailureException =
            new OptimisticLockingFailureException("stale");

        stubFindByName(OWNER, "target", List.of());
        stubFindByName(OWNER, "source", List.of(buildMemory(OWNER, "source")));
        when(aiMemoryRepository.save(any(AiAutoMemory.class))).thenThrow(optimisticLockingFailureException);

        assertThatThrownBy(() -> realService.rename(OWNER, "source", "target"))
            .isInstanceOf(AiAutoMemoryConcurrentModificationException.class)
            .hasCause(optimisticLockingFailureException);
    }

    @Test
    void testRenameTranslatesConcurrentDuplicateUpdate() {
        AiAutoMemoryServiceImpl realService = newService();

        DuplicateKeyException duplicateKeyException = new DuplicateKeyException("uk_ai_auto_memory_owner_env_name");

        stubFindByName(OWNER, "target", List.of());
        stubFindByName(OWNER, "source", List.of(buildMemory(OWNER, "source")));
        when(aiMemoryRepository.save(any(AiAutoMemory.class))).thenThrow(duplicateKeyException);

        assertThatThrownBy(() -> realService.rename(OWNER, "source", "target"))
            .isInstanceOf(DuplicateAiAutoMemoryNameException.class)
            .hasCause(duplicateKeyException);
    }

    @Test
    void testRenameChangesTheNameAndTouchesUpdatedAt() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory source = buildMemory(OWNER, "source");

        source.setUpdatedAt(LocalDateTime.now(clock)
            .minusDays(1));

        stubFindByName(OWNER, "target", List.of());
        stubFindByName(OWNER, "source", List.of(source));
        when(aiMemoryRepository.save(any(AiAutoMemory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiAutoMemory renamed = realService.rename(OWNER, "source", "target");

        assertThat(renamed.getName()).isEqualTo("target");
        assertThat(renamed.getUpdatedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void testRenameToTheSameNameSavesNothing() {
        AiAutoMemoryServiceImpl realService = newService();

        stubFindByName(OWNER, "same", List.of(buildMemory(OWNER, "same")));

        AiAutoMemory unchanged = realService.rename(OWNER, "same", "same");

        assertThat(unchanged.getName()).isEqualTo("same");

        verify(aiMemoryRepository, never()).save(any(AiAutoMemory.class));
    }

    @Test
    void testRenameRejectsAnInvalidNameBeforeAnyLookup() {
        AiAutoMemoryServiceImpl realService = newService();

        assertThatThrownBy(() -> realService.rename(OWNER, "source", "Not A Slug"))
            .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(aiMemoryRepository);
    }

    @Test
    void testListFiltersByType() {
        AiAutoMemoryServiceImpl realService = newService();

        when(aiMemoryRepository.findByOwner(OWNER, AiAutoMemoryType.FEEDBACK))
            .thenReturn(List.of(buildMemory(OWNER, "a")));

        List<AiAutoMemory> result = realService.list(OWNER, AiAutoMemoryType.FEEDBACK);

        assertThat(result).hasSize(1);
    }

    @Test
    void testFindByIdReturnsTheRowForItsOwner() {
        AiAutoMemoryServiceImpl realService = newService();

        when(aiMemoryRepository.findOwnedById(OWNER, MEMORY_ID)).thenReturn(Optional.of(buildMemory(OWNER, "mine")));

        assertThat(realService.findById(OWNER, MEMORY_ID)).isPresent();
    }

    @Test
    void testUpdateByIdUpdatesTheRowForItsOwner() {
        AiAutoMemoryServiceImpl realService = newService();

        when(aiMemoryRepository.findOwnedById(OWNER, MEMORY_ID)).thenReturn(Optional.of(buildMemory(OWNER, "mine")));
        when(aiMemoryRepository.save(any(AiAutoMemory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiAutoMemory updated =
            realService.updateById(OWNER, MEMORY_ID, 0L, new AiAutoMemoryPatch("New title", null, null, null));

        assertThat(updated.getTitle()).isEqualTo("New title");
    }

    @Test
    void testUpdateByIdRejectsAnEditBasedOnAnOlderVersion() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory existing = buildMemory(OWNER, "mine", 4L);

        when(aiMemoryRepository.findOwnedById(OWNER, MEMORY_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(
            () -> realService.updateById(OWNER, MEMORY_ID, 3L, new AiAutoMemoryPatch(null, null, null, "stale edit")))
                .isInstanceOf(AiAutoMemoryConcurrentModificationException.class)
                .hasMessageContaining("'mine'");

        verify(aiMemoryRepository, never()).save(any(AiAutoMemory.class));
    }

    @Test
    void testUpdateByIdAppliesAnEditBasedOnTheCurrentVersion() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory existing = buildMemory(OWNER, "mine", 4L);

        when(aiMemoryRepository.findOwnedById(OWNER, MEMORY_ID)).thenReturn(Optional.of(existing));
        when(aiMemoryRepository.save(any(AiAutoMemory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiAutoMemory updated =
            realService.updateById(OWNER, MEMORY_ID, 4L, new AiAutoMemoryPatch(null, null, null, "current edit"));

        assertThat(updated.getContent()).isEqualTo("current edit");
    }

    @Test
    void testUpdateByIdThrowsNotFoundWhenTheCallerOwnsNoSuchRow() {
        AiAutoMemoryServiceImpl realService = newService();

        when(aiMemoryRepository.findOwnedById(OWNER, MEMORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> realService.updateById(OWNER, MEMORY_ID, 0L, new AiAutoMemoryPatch("New title", null, null, null)))
                .isInstanceOf(AiAutoMemoryNotFoundException.class)
                .hasMessage("Memory not found");

        verify(aiMemoryRepository, never()).save(any(AiAutoMemory.class));
    }

    @Test
    void testDeleteByIdDeletesTheRowForItsOwner() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory memory = buildMemory(OWNER, "mine");

        when(aiMemoryRepository.findOwnedById(OWNER, MEMORY_ID)).thenReturn(Optional.of(memory));

        realService.deleteById(OWNER, MEMORY_ID);

        verify(aiMemoryRepository).delete(memory);
    }

    @Test
    void testDeleteByIdThrowsNotFoundWhenTheCallerOwnsNoSuchRow() {
        AiAutoMemoryServiceImpl realService = newService();

        when(aiMemoryRepository.findOwnedById(OWNER, MEMORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> realService.deleteById(OWNER, MEMORY_ID))
            .isInstanceOf(AiAutoMemoryNotFoundException.class)
            .hasMessage("Memory not found");

        verify(aiMemoryRepository, never()).delete(any(AiAutoMemory.class));
    }

    @Test
    void testListAllOwnersPassesEnvironmentAndTypeThrough() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory memory = buildMemory(OWNER, "mine");

        when(aiMemoryRepository.findByWorkspace(WORKSPACE_ID, Environment.STAGING, AiAutoMemoryType.FEEDBACK))
            .thenReturn(List.of(memory));

        assertThat(realService.listAllOwners(WORKSPACE_ID, Environment.STAGING, AiAutoMemoryType.FEEDBACK))
            .containsExactly(memory);
    }

    @Test
    void testListAllOwnersWithoutATypeListsEveryType() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory memory = buildMemory(OWNER, "mine");

        when(aiMemoryRepository.findByWorkspace(WORKSPACE_ID, Environment.PRODUCTION, null))
            .thenReturn(List.of(memory));

        assertThat(realService.listAllOwners(WORKSPACE_ID, Environment.PRODUCTION, null)).containsExactly(memory);
    }

    @Test
    void testListPrincipalsPassesTheEnvironmentThrough() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemoryPrincipalCount principalCount = new AiAutoMemoryPrincipalCount(
            AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 5L, 2);

        when(aiMemoryRepository.listPrincipals(WORKSPACE_ID, Environment.PRODUCTION))
            .thenReturn(List.of(principalCount));

        assertThat(realService.listPrincipals(WORKSPACE_ID, Environment.PRODUCTION)).containsExactly(principalCount);
    }

    @Test
    void testCreateRejectsABlankTitleOrContentBeforeAnyLookup() {
        AiAutoMemoryServiceImpl realService = newService();

        assertThatThrownBy(() -> realService.create(OWNER, "note", " ", null, AiAutoMemoryType.USER, "content"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title");
        assertThatThrownBy(() -> realService.create(OWNER, "note", "Title", null, AiAutoMemoryType.USER, "\n"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("content");

        verifyNoInteractions(aiMemoryRepository);
    }

    @Test
    void testUpdateRejectsABlankTitleOrContentAndSavesNothing() {
        AiAutoMemoryServiceImpl realService = newService();

        stubFindByName(OWNER, "user_profile", List.of(buildMemory(OWNER, "user_profile")));

        AiAutoMemoryPatch blankTitlePatch = new AiAutoMemoryPatch(" ", null, null, null);
        AiAutoMemoryPatch blankContentPatch = new AiAutoMemoryPatch(null, null, null, "");

        assertThatThrownBy(() -> realService.update(OWNER, "user_profile", 0L, blankTitlePatch))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("title");
        assertThatThrownBy(() -> realService.update(OWNER, "user_profile", 0L, blankContentPatch))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("content");

        verify(aiMemoryRepository, never()).save(any(AiAutoMemory.class));
    }

    @Test
    void testRenameOfAMissingMemoryThrowsNotFound() {
        AiAutoMemoryServiceImpl realService = newService();

        stubFindByName(OWNER, "new_name", List.of());
        stubFindByName(OWNER, "missing", List.of());

        assertThatThrownBy(() -> realService.rename(OWNER, "missing", "new_name"))
            .isInstanceOf(AiAutoMemoryNotFoundException.class);

        verify(aiMemoryRepository, never()).save(any(AiAutoMemory.class));
    }

    @Test
    void testReadReturnsTheFirstOfSeveralMatches() {
        AiAutoMemoryServiceImpl realService = newService();

        AiAutoMemory first = buildMemory(OWNER, "dup");
        AiAutoMemory second = AiAutoMemory.restore(
            OWNER, MEMORY_ID + 1, "dup", "Other", null, AiAutoMemoryType.USER, "other", null, null, 0L);

        stubFindByName(OWNER, "dup", List.of(first, second));

        assertThat(realService.read(OWNER, "dup")).containsSame(first);
    }

    private AiAutoMemoryServiceImpl newService() {
        return new AiAutoMemoryServiceImpl(aiMemoryRepository, clock);
    }

    private void stubFindByName(AiAutoMemoryOwner owner, String name, List<AiAutoMemory> matches) {
        when(aiMemoryRepository.findAllByOwnerAndName(owner, name)).thenReturn(matches);
    }

    private AiAutoMemory buildMemory(AiAutoMemoryOwner owner, String name) {
        return buildMemory(owner, name, 0L);
    }

    private AiAutoMemory buildMemory(AiAutoMemoryOwner owner, String name, long version) {
        return AiAutoMemory.restore(
            owner, MEMORY_ID, name, "Title: " + name, "Description: " + name, AiAutoMemoryType.USER, "body: " + name,
            LocalDateTime.now(clock), LocalDateTime.now(clock), version);
    }
}
