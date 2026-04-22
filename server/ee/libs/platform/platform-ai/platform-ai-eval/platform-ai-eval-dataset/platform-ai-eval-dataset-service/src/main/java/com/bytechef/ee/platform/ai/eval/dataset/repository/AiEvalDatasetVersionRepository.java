/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.repository;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetVersionRepository extends ListCrudRepository<AiEvalDatasetVersion, Long> {

    List<AiEvalDatasetVersion> findAllByDatasetId(Long datasetId);

    Optional<AiEvalDatasetVersion> findByDatasetIdAndVersionNumber(Long datasetId, int versionNumber);

    Optional<AiEvalDatasetVersion> findByDatasetIdAndLabel(Long datasetId, String label);

    Optional<AiEvalDatasetVersion> findFirstByDatasetIdAndFrozenFalseOrderByVersionNumberDesc(Long datasetId);
}
