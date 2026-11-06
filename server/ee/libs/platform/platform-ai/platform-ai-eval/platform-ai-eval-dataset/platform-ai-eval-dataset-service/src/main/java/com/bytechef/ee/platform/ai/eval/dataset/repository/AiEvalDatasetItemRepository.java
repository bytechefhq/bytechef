/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.repository;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetItemRepository extends ListCrudRepository<AiEvalDatasetItem, Long> {

    List<AiEvalDatasetItem> findAllByDatasetVersionId(Long datasetVersionId);

    long countByDatasetVersionId(Long datasetVersionId);
}
