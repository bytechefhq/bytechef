/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.repository.AiEvalDatasetRepository;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class WorkspaceAiEvalDatasetServiceImpl implements WorkspaceAiEvalDatasetService {

    private final AiEvalDatasetRepository aiEvalDatasetRepository;
    private final AiEvalDatasetService aiEvalDatasetService;

    WorkspaceAiEvalDatasetServiceImpl(
        AiEvalDatasetRepository aiEvalDatasetRepository, AiEvalDatasetService aiEvalDatasetService) {

        this.aiEvalDatasetRepository = aiEvalDatasetRepository;
        this.aiEvalDatasetService = aiEvalDatasetService;
    }

    @Override
    public AiEvalDataset createInWorkspace(AiEvalDataset dataset, long workspaceId) {
        Validate.notNull(dataset, "dataset must not be null");
        Validate.isTrue(dataset.getId() == null, "dataset id must be null for creation");

        dataset.setWorkspaceId(workspaceId);

        return aiEvalDatasetService.create(dataset);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long datasetId) {
        return aiEvalDatasetRepository.findById(datasetId)
            .map(AiEvalDataset::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalDataset> findAllByWorkspace(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return aiEvalDatasetRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiEvalDataset> findByWorkspaceAndName(Long workspaceId, String name) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");

        return aiEvalDatasetRepository.findByWorkspaceIdAndName(workspaceId, name);
    }
}
