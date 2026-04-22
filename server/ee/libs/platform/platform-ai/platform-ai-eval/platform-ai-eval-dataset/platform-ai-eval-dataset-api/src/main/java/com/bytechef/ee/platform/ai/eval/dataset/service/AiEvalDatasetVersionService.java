/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import java.util.List;
import java.util.Optional;

/**
 * Version lifecycle for {@link AiEvalDatasetVersion}. Versions follow a copy-on-freeze discipline: once a version is
 * frozen it is immutable, and any new item insertion targets a freshly-created unfrozen version rather than mutating
 * the frozen snapshot. This preserves the exact dataset state any experiment was pinned to.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetVersionService {

    /**
     * Creates a new version for the dataset. {@code versionNumber} auto-increments (max existing + 1, or 1 if this is
     * the first version). If {@code frozen} is {@code true}, the version is immutable and cannot accept new items.
     */
    AiEvalDatasetVersion createVersion(long datasetId, String label, boolean frozen);

    /**
     * Returns the latest unfrozen version for the dataset, or creates a new unfrozen version (version_number = 1 if no
     * versions exist, else previous_max + 1) when none exists. This is the entry point the item service uses to enforce
     * the copy-on-freeze rule.
     */
    AiEvalDatasetVersion getOrCreateUnfrozenVersion(long datasetId);

    AiEvalDatasetVersion getVersion(long versionId);

    List<AiEvalDatasetVersion> findAllByDataset(Long datasetId);

    Optional<AiEvalDatasetVersion> findByDatasetAndLabel(Long datasetId, String label);

    /**
     * Marks a version as frozen. Subsequent attempts to add items to this version must instead create a new unfrozen
     * version (enforced by {@link #getOrCreateUnfrozenVersion(long)}). Throws {@link IllegalStateException} if the
     * version is already frozen.
     */
    AiEvalDatasetVersion freeze(long versionId);
}
