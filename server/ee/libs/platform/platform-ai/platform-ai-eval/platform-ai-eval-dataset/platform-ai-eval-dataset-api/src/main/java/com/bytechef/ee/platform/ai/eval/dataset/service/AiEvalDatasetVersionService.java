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
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetVersionService {

    AiEvalDatasetVersion createVersion(long datasetId, String label, boolean frozen);

    AiEvalDatasetVersion getOrCreateUnfrozenVersion(long datasetId);

    AiEvalDatasetVersion getVersion(long versionId);

    List<AiEvalDatasetVersion> findAllByDataset(Long datasetId);

    Optional<AiEvalDatasetVersion> findByDatasetAndLabel(Long datasetId, String label);

    AiEvalDatasetVersion freeze(long versionId);
}
