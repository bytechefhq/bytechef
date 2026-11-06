/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.repository;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetRepository extends ListCrudRepository<AiEvalDataset, Long> {

    List<AiEvalDataset> findAllByWorkspaceId(Long workspaceId);

    Optional<AiEvalDataset> findByWorkspaceIdAndName(Long workspaceId, String name);
}
