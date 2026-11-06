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

    private static final String VERSION_NUMBER_DEDUP_INDEX = "uk_ai_eval_dataset_version_number";

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
