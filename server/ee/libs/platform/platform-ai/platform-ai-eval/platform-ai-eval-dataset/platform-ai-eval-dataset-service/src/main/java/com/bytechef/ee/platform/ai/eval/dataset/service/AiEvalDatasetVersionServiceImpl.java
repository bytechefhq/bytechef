/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.repository.AiEvalDatasetVersionRepository;
import com.bytechef.ee.platform.ai.eval.dataset.util.AiEvalDatasetConstraintMatchers;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiEvalDatasetVersionServiceImpl implements AiEvalDatasetVersionService {

    private static final Logger log = LoggerFactory.getLogger(AiEvalDatasetVersionServiceImpl.class);

    private static final String UNFROZEN_VERSION_DEDUP_INDEX = "ux_ai_eval_dataset_version_unfrozen_per_dataset";

    // The regular (dataset_id, version_number) unique constraint. Two concurrent unfrozen-version creators both
    // compute nextVersionNumber = max+1 and race on TWO indexes: the partial unfrozen index above AND this one.
    // PostgreSQL is free to surface either violation first; both signal "another thread won the race" and both
    // recover by re-fetching the unfrozen winner. Without matching this name, ~half of races leak a 500.
    private static final String VERSION_NUMBER_DEDUP_INDEX = "uk_ai_eval_dataset_version_number";

    // Word-boundary patterns for the substring fallback. Without anchors, a future second unique constraint named
    // with one of these as a prefix (e.g. "ux_ai_eval_dataset_version_unfrozen_per_dataset_v2") would silently match
    // and
    // be mis-classified as the dedup hit. Matches the hardening applied in AiObservabilityOtlpIngestFacadeImpl.
    private static final Pattern UNFROZEN_VERSION_DEDUP_INDEX_PATTERN =
        AiEvalDatasetConstraintMatchers.wordBoundaryPattern(UNFROZEN_VERSION_DEDUP_INDEX);

    private static final Pattern VERSION_NUMBER_DEDUP_INDEX_PATTERN =
        AiEvalDatasetConstraintMatchers.wordBoundaryPattern(VERSION_NUMBER_DEDUP_INDEX);

    private final AiEvalDatasetVersionRepository aiEvalDatasetVersionRepository;
    private final ObjectProvider<AiEvalDatasetVersionService> selfProvider;

    AiEvalDatasetVersionServiceImpl(
        AiEvalDatasetVersionRepository aiEvalDatasetVersionRepository,
        ObjectProvider<AiEvalDatasetVersionService> selfProvider) {

        this.aiEvalDatasetVersionRepository = aiEvalDatasetVersionRepository;
        this.selfProvider = selfProvider;
    }

    @Override
    @Transactional
    public AiEvalDatasetVersion createVersion(long datasetId, String label, boolean frozen) {
        int nextVersionNumber = aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId)
            .stream()
            .mapToInt(AiEvalDatasetVersion::getVersionNumber)
            .max()
            .orElse(0) + 1;

        AiEvalDatasetVersion datasetVersion = new AiEvalDatasetVersion(datasetId, nextVersionNumber);

        datasetVersion.setLabel(label);

        if (frozen) {
            datasetVersion.freeze();
        }

        return aiEvalDatasetVersionRepository.save(datasetVersion);
    }

    /**
     * Returns the dataset's existing unfrozen version, or creates one. The check-then-create is racy at the application
     * layer — two concurrent {@code addItem}/{@code addItems} callers can both observe no unfrozen version and both try
     * to insert. The partial unique index {@code ux_ai_eval_dataset_version_unfrozen_per_dataset} (on
     * {@code dataset_id WHERE frozen = false}) pins the invariant in the database; the race-loser sees a
     * {@link DuplicateKeyException} and re-fetches the winner.
     *
     * <p>
     * This method intentionally has no {@code @Transactional} so the inner {@link #createVersion} call runs in its own
     * transaction (entered through the Spring proxy via {@code selfProvider}). If the create call's transaction aborts
     * on the unique-index violation, the rollback is contained to that inner transaction; the outer scope is then free
     * to re-fetch on a fresh connection. A class-level {@code @Transactional} would put everything in one tx — once
     * PostgreSQL marks the connection aborted on 23505, the recovery {@code findFirst...} would throw
     * {@code "current transaction is aborted, commands ignored"}.
     *
     * <p>
     * The {@link DuplicateKeyException} catch is narrowed to two known dedup indexes by name. Both
     * {@code ux_ai_eval_dataset_version_unfrozen_per_dataset} (partial: at-most-one unfrozen per dataset) AND
     * {@code uk_ai_eval_dataset_version_number} (regular: unique (dataset_id, version_number)) signal "another
     * concurrent creator won." Two concurrent {@code addItem} callers both compute {@code nextVersionNumber = max + 1},
     * so PostgreSQL may surface EITHER violation first depending on insert order; matching only the partial index used
     * to leak ~half of races as 500s. Any future third unique constraint on {@code ai_eval_dataset_version} still
     * propagates so it does not get silently re-fetched under the wrong invariant.
     */
    @Override
    public AiEvalDatasetVersion getOrCreateUnfrozenVersion(long datasetId) {
        Optional<AiEvalDatasetVersion> unfrozenVersion =
            aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId);

        if (unfrozenVersion.isPresent()) {
            return unfrozenVersion.get();
        }

        try {
            return selfProvider.getObject()
                .createVersion(datasetId, null, false);
        } catch (DuplicateKeyException duplicateKeyException) {
            if (!isKnownDedupRace(duplicateKeyException)) {
                throw duplicateKeyException;
            }

            String matchedIndex = AiEvalDatasetConstraintMatchers.matchesConstraint(
                duplicateKeyException, UNFROZEN_VERSION_DEDUP_INDEX, UNFROZEN_VERSION_DEDUP_INDEX_PATTERN)
                    ? UNFROZEN_VERSION_DEDUP_INDEX
                    : VERSION_NUMBER_DEDUP_INDEX;

            log.debug(
                "Concurrent unfrozen-version creation lost the race for dataset {} on index {} — re-fetching " +
                    "the winner",
                datasetId, matchedIndex);

            return recoverUnfrozenVersion(datasetId, matchedIndex, duplicateKeyException);
        }
    }

    /**
     * Resolves the race-recovery re-fetch. The straight-line case is "find the winning unfrozen row that the dedup
     * violation pointed at and return it." The subtle case is the freeze-during-recovery interleave: a concurrent
     * {@link #freeze} on the winning row commits AFTER our duplicate-key violation but BEFORE our re-fetch. The
     * unfrozen slot is now empty again — neither the winner (now frozen) nor the loser (us) holds it — so we retry
     * {@link #createVersion} ONCE. If a freeze freed the slot, the retry succeeds; if a third concurrent creator wins
     * the second race, the post-retry re-fetch returns it.
     *
     * <p>
     * If the retry surfaces a {@link DuplicateKeyException} on a constraint OTHER than the two known dedup indexes,
     * that is a real schema violation (e.g., a future NOT-NULL or FK addition) and propagates unchanged — same
     * discipline as the first-attempt path.
     *
     * <p>
     * We do not retry beyond one attempt: if races keep flipping the slot, the dataset is hot-spotting and forcing the
     * caller to retry surfaces that load pattern instead of hiding it inside an unbounded loop.
     */
    private AiEvalDatasetVersion recoverUnfrozenVersion(
        long datasetId, String matchedIndex, DuplicateKeyException originalDuplicate) {

        Optional<AiEvalDatasetVersion> winner =
            aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId);

        if (winner.isPresent()) {
            return winner.get();
        }

        log.debug(
            "Unfrozen slot for dataset {} was empty on recovery re-fetch (likely freeze raced) — retrying createVersion",
            datasetId);

        try {
            return selfProvider.getObject()
                .createVersion(datasetId, null, false);
        } catch (DuplicateKeyException retryDuplicate) {
            if (!isKnownDedupRace(retryDuplicate)) {
                throw retryDuplicate;
            }

            return aiEvalDatasetVersionRepository.findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(datasetId)
                .orElseThrow(() -> new IllegalStateException(
                    "Unfrozen-version recovery exhausted for dataset " + datasetId + " (first violation on "
                        + matchedIndex + ", retry also raced) — repeated freeze/create races prevented stable "
                        + "resolution; caller should retry",
                    initCauseChain(retryDuplicate, originalDuplicate)));
        }
    }

    private static boolean isKnownDedupRace(DuplicateKeyException exception) {
        return AiEvalDatasetConstraintMatchers.matchesConstraint(
            exception, UNFROZEN_VERSION_DEDUP_INDEX, UNFROZEN_VERSION_DEDUP_INDEX_PATTERN)
            || AiEvalDatasetConstraintMatchers.matchesConstraint(
                exception, VERSION_NUMBER_DEDUP_INDEX, VERSION_NUMBER_DEDUP_INDEX_PATTERN);
    }

    /**
     * Suppresses the original duplicate so both race-loss exceptions are visible to a debugger / log appender. We pick
     * the second exception as the primary cause because it is the one that closed the recovery window; the first lives
     * on as a suppressed throwable for forensic context.
     *
     * <p>
     * Skips the {@code addSuppressed} call when the same instance is rethrown — JDBC drivers and certain mocking
     * frameworks legitimately reuse the same exception across retries, and Java rejects self-suppression with an IAE
     * that would obscure the real failure.
     */
    private static DuplicateKeyException initCauseChain(
        DuplicateKeyException retry, DuplicateKeyException original) {

        if (retry != original) {
            retry.addSuppressed(original);
        }

        return retry;
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvalDatasetVersion getVersion(long versionId) {
        return aiEvalDatasetVersionRepository.findById(versionId)
            .orElseThrow(() -> new IllegalArgumentException("AiEvalDatasetVersion not found with id: " + versionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalDatasetVersion> findAllByDataset(Long datasetId) {
        return aiEvalDatasetVersionRepository.findAllByDatasetId(datasetId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiEvalDatasetVersion> findByDatasetAndLabel(Long datasetId, String label) {
        return aiEvalDatasetVersionRepository.findByDatasetIdAndLabel(datasetId, label);
    }

    @Override
    @Transactional
    public AiEvalDatasetVersion freeze(long versionId) {
        AiEvalDatasetVersion datasetVersion = aiEvalDatasetVersionRepository.findById(versionId)
            .orElseThrow(() -> new IllegalArgumentException("AiEvalDatasetVersion not found with id: " + versionId));

        if (datasetVersion.isFrozen()) {
            throw new IllegalStateException("AiEvalDatasetVersion " + versionId + " is already frozen");
        }

        datasetVersion.freeze();

        return aiEvalDatasetVersionRepository.save(datasetVersion);
    }
}
