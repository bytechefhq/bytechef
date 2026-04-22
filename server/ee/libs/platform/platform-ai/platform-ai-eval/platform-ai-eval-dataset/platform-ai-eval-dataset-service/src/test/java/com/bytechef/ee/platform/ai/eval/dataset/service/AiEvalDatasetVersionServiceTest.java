/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.repository.AiEvalDatasetVersionRepository;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;

/**
 * Unit tests for {@link AiEvalDatasetVersionServiceImpl}. Covers the copy-on-freeze invariant: freeze is one-way, and
 * {@code getOrCreateUnfrozenVersion} must prefer an existing unfrozen version and only auto-create when every version
 * is frozen (or no versions exist).
 *
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(MockitoExtension.class)
class AiEvalDatasetVersionServiceTest {

    @Mock
    private AiEvalDatasetVersionRepository aiEvalDatasetVersionRepository;

    private AiEvalDatasetVersionServiceImpl aiEvalDatasetVersionService;

    @BeforeEach
    void setUp() {
        // selfProvider returns the same instance so getOrCreateUnfrozenVersion's proxy-bypass
        // (selfProvider.getObject().createVersion(...)) lands back in the test instance. Production wiring
        // gives a real Spring proxy here so the inner createVersion runs in its own @Transactional boundary;
        // unit tests don't exercise the proxy and therefore can't observe the tx-isolation property.
        SelfReferenceProvider selfProvider = new SelfReferenceProvider();

        aiEvalDatasetVersionService = new AiEvalDatasetVersionServiceImpl(aiEvalDatasetVersionRepository, selfProvider);

        selfProvider.setTarget(aiEvalDatasetVersionService);
    }

    @Test
    void testCreateFirstVersionIncrementsToOne() {
        long datasetId = 42L;

        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalDatasetVersion created = aiEvalDatasetVersionService.createVersion(datasetId, "seed", false);

        assertThat(created.getVersionNumber()).isEqualTo(1);
        assertThat(created.getLabel()).isEqualTo("seed");
        assertThat(created.isFrozen()).isFalse();
        assertThat(created.getDatasetId()).isEqualTo(datasetId);
    }

    @Test
    void testCreateSubsequentVersionIncrements() {
        long datasetId = 42L;

        AiEvalDatasetVersion existingV1 = new AiEvalDatasetVersion(datasetId, 1);
        AiEvalDatasetVersion existingV3 = new AiEvalDatasetVersion(datasetId, 3);
        AiEvalDatasetVersion existingV2 = new AiEvalDatasetVersion(datasetId, 2);

        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId))
            .thenReturn(List.of(existingV1, existingV3, existingV2));
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalDatasetVersion created = aiEvalDatasetVersionService.createVersion(datasetId, null, true);

        // max(1, 3, 2) + 1 = 4 — order-independent, so the unit test exercises the stream.max path, not list order.
        assertThat(created.getVersionNumber()).isEqualTo(4);
        assertThat(created.isFrozen()).isTrue();
    }

    @Test
    void testFreezeRejectsAlreadyFrozen() {
        AiEvalDatasetVersion frozenVersion = seedVersion(99L, 42L, 1);

        frozenVersion.freeze();

        when(aiEvalDatasetVersionRepository.findById(99L)).thenReturn(Optional.of(frozenVersion));

        assertThatThrownBy(() -> aiEvalDatasetVersionService.freeze(99L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already frozen");

        verify(aiEvalDatasetVersionRepository, never()).save(any());
    }

    @Test
    void testGetOrCreateUnfrozenReturnsExistingWhenPresent() {
        long datasetId = 42L;

        AiEvalDatasetVersion existingUnfrozen = seedVersion(77L, datasetId, 5);

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.of(existingUnfrozen));

        AiEvalDatasetVersion result = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(result).isSameAs(existingUnfrozen);

        // Must not fall through to createVersion — that would produce a spurious empty version per add.
        verify(aiEvalDatasetVersionRepository, never()).save(any());
        verify(aiEvalDatasetVersionRepository, never()).findAllByDatasetId(any());
    }

    @Test
    void testGetOrCreateUnfrozenCreatesWhenAllFrozen() {
        long datasetId = 42L;

        AiEvalDatasetVersion frozenV1 = new AiEvalDatasetVersion(datasetId, 1);
        AiEvalDatasetVersion frozenV2 = new AiEvalDatasetVersion(datasetId, 2);

        frozenV1.freeze();
        frozenV2.freeze();

        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of(frozenV1, frozenV2));
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiEvalDatasetVersion created = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(created.isFrozen()).isFalse();
        assertThat(created.getVersionNumber()).isEqualTo(3);
        assertThat(created.getDatasetId()).isEqualTo(datasetId);
        assertThat(created.getLabel()).isNull();
    }

    @Test
    void testGetOrCreateUnfrozenRefetchesWinnerOnDuplicateKey() {
        long datasetId = 42L;

        AiEvalDatasetVersion winnerVersion = seedVersion(101L, datasetId, 1);

        // First lookup: race-loser sees no unfrozen version (winner's insert hasn't committed yet from this
        // observer's perspective). save() then trips the partial unique index. Second lookup, after the winner
        // committed, returns the winner. The service must catch the DuplicateKeyException and re-fetch rather
        // than propagating a 500 to the caller — same contract as resolveOrCreateTrace in the OTLP path.
        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty(), Optional.of(winnerVersion));
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_ai_eval_dataset_version_unfrozen_per_dataset"));

        AiEvalDatasetVersion result = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(result).isSameAs(winnerVersion);
    }

    @Test
    void testGetOrCreateUnfrozenRefetchesWinnerOnVersionNumberIndexRace() {
        long datasetId = 42L;

        AiEvalDatasetVersion winnerVersion = seedVersion(101L, datasetId, 1);

        // Two concurrent unfrozen-version creators both compute nextVersionNumber = max + 1 and race on TWO unique
        // indexes. PostgreSQL is free to surface uk_ai_eval_dataset_version_number (the regular (dataset_id,
        // version_number) constraint) before the partial ux_ai_eval_dataset_version_unfrozen_per_dataset. Without
        // matching this name, the race-loser path leaks a 500 to the caller — both indexes signal "another thread
        // won" and both must recover by re-fetching the winner.
        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty(), Optional.of(winnerVersion));
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("uk_ai_eval_dataset_version_number"));

        AiEvalDatasetVersion result = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(result).isSameAs(winnerVersion);
    }

    @Test
    void testGetOrCreateUnfrozenPropagatesUnknownDuplicateKey() {
        long datasetId = 42L;

        // A DuplicateKeyException whose constraint matches NEITHER known dedup index must NOT be silently swallowed
        // and re-fetched as if the race won — the service has no business deciding that an unrecognised constraint
        // violation indicates a benign concurrent-creator outcome. Propagating preserves the schema-corruption
        // signal for ops dashboards.
        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_some_future_unique_index_not_yet_known"));

        assertThatThrownBy(() -> aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId))
            .isInstanceOf(DuplicateKeyException.class)
            .hasMessageContaining("ux_some_future_unique_index_not_yet_known");
    }

    @Test
    void testGetOrCreateUnfrozenSurfacesIllegalStateWhenRefetchEmpty() {
        long datasetId = 42L;

        // Pathological: insert rejected by the unique index, recovery re-fetch returns empty, retry create ALSO
        // hits the unique index, second re-fetch still empty. The service exhausts its bounded retry and surfaces
        // an IllegalStateException with the retry-side DuplicateKeyException as cause. This covers two real
        // scenarios: (a) genuine schema corruption where the index fired without a committed winner row, and
        // (b) repeated freeze/create racing where every observation lands in the gap. Either way we surface the
        // exhaustion to the caller rather than retrying unboundedly or returning null.
        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_ai_eval_dataset_version_unfrozen_per_dataset"));

        assertThatThrownBy(() -> aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId))
            .isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(DuplicateKeyException.class)
            .hasMessageContaining(String.valueOf(datasetId));
    }

    @Test
    void testGetOrCreateUnfrozenRetriesCreateAfterFreezeRacedTheRecoveryWindow() {
        long datasetId = 42L;

        AiEvalDatasetVersion retryWinnerVersion = seedVersion(202L, datasetId, 2);

        // Freeze raced the recovery: first save() loses the unique-index race, recovery re-fetch finds NO
        // unfrozen winner (because a concurrent freeze() flipped the winner to frozen), and the retry create
        // succeeds because the freeze freed the partial-index slot. Without the bounded retry, this scenario
        // surfaced a misleading 500 to the caller — system state was internally consistent but the recovery
        // path could not see through the interleave.
        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty(), Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());

        // First save() trips the dedup; second save() (the retry inside recoverUnfrozenVersion) succeeds and
        // returns the freshly-claimed winner.
        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_ai_eval_dataset_version_unfrozen_per_dataset"))
            .thenReturn(retryWinnerVersion);

        AiEvalDatasetVersion result = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        assertThat(result).isSameAs(retryWinnerVersion);
    }

    @Test
    void testGetOrCreateUnfrozenPropagatesUnknownDuplicateKeyOnRetry() {
        long datasetId = 42L;

        // The retry path inside recoverUnfrozenVersion mirrors the first-attempt discipline: a DuplicateKey on
        // an unrecognised constraint (e.g., a future NOT-NULL or FK addition that fires AFTER our freeze raced
        // the recovery) must propagate as a real schema violation rather than be silently absorbed under
        // "exhausted retry." Without this guard, a real bug introduced by a migration would surface as the
        // generic IllegalStateException instead of the specific DuplicateKeyException with constraint name.
        when(aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId))
            .thenReturn(Optional.empty(), Optional.empty());
        when(aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)).thenReturn(List.of());

        when(aiEvalDatasetVersionRepository.save(any(AiEvalDatasetVersion.class)))
            .thenThrow(new DuplicateKeyException("ux_ai_eval_dataset_version_unfrozen_per_dataset"))
            .thenThrow(new DuplicateKeyException("ux_some_future_constraint_added_by_migration"));

        assertThatThrownBy(() -> aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId))
            .isInstanceOf(DuplicateKeyException.class)
            .hasMessageContaining("ux_some_future_constraint_added_by_migration");
    }

    /**
     * Minimal {@link ObjectProvider} that returns a single target instance — sufficient for unit tests that exercise
     * the self-injection escape hatch in {@code getOrCreateUnfrozenVersion} without pulling in a Spring context.
     */
    private static final class SelfReferenceProvider implements ObjectProvider<AiEvalDatasetVersionService> {

        private AiEvalDatasetVersionService target;

        void setTarget(AiEvalDatasetVersionService target) {
            this.target = target;
        }

        @Override
        public AiEvalDatasetVersionService getObject() throws BeansException {
            return target;
        }

        @Override
        public AiEvalDatasetVersionService getObject(Object... args) throws BeansException {
            return target;
        }

        @Override
        public AiEvalDatasetVersionService getIfAvailable() throws BeansException {
            return target;
        }

        @Override
        public AiEvalDatasetVersionService getIfUnique() throws BeansException {
            return target;
        }

        @Override
        public Iterator<AiEvalDatasetVersionService> iterator() {
            return target == null ? List.<AiEvalDatasetVersionService>of()
                .iterator()
                : List.of(target)
                    .iterator();
        }

        @Override
        public void forEach(Consumer<? super AiEvalDatasetVersionService> action) {
            if (target != null) {
                action.accept(target);
            }
        }
    }

    private static AiEvalDatasetVersion seedVersion(long id, long datasetId, int versionNumber) {
        AiEvalDatasetVersion version = new AiEvalDatasetVersion(datasetId, versionNumber);

        try {
            Field idField = AiEvalDatasetVersion.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(version, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id", reflectiveOperationException);
        }

        return version;
    }
}
