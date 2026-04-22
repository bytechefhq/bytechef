/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.repository.AiEvalDatasetRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-agnostic CRUD on {@link AiEvalDataset}. The workspace membership row in {@code workspace_ai_eval_dataset}
 * and any workspace-scoped query is owned by
 * {@code com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetServiceImpl}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class AiEvalDatasetServiceImpl implements AiEvalDatasetService {

    private final AiEvalDatasetRepository aiEvalDatasetRepository;

    AiEvalDatasetServiceImpl(AiEvalDatasetRepository aiEvalDatasetRepository) {
        this.aiEvalDatasetRepository = aiEvalDatasetRepository;
    }

    @Override
    public AiEvalDataset create(AiEvalDataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        Validate.isTrue(dataset.getId() == null, "dataset id must be null for creation");

        return aiEvalDatasetRepository.save(dataset);
    }

    @Override
    public void archive(long id) {
        AiEvalDataset dataset = getDataset(id);

        dataset.archive();

        aiEvalDatasetRepository.save(dataset);
    }

    @Override
    @Transactional(readOnly = true)
    public AiEvalDataset getDataset(long id) {
        return aiEvalDatasetRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("AiEvalDataset not found with id: " + id));
    }

    @Override
    public AiEvalDataset update(AiEvalDataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        Validate.notNull(dataset.getId(), "dataset id must not be null for update");

        // Reject updates against deleted rows — cheaper than letting optimistic-locking blow up at save time.
        if (!aiEvalDatasetRepository.existsById(dataset.getId())) {
            throw new IllegalArgumentException("AiEvalDataset not found with id: " + dataset.getId());
        }

        return aiEvalDatasetRepository.save(dataset);
    }
}
