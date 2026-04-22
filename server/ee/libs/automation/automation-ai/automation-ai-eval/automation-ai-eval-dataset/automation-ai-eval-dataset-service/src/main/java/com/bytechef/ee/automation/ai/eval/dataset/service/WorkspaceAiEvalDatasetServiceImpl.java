/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.service;

import com.bytechef.ee.automation.ai.eval.dataset.domain.WorkspaceAiEvalDataset;
import com.bytechef.ee.automation.ai.eval.dataset.repository.WorkspaceAiEvalDatasetRepository;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Workspace-aware operations on {@link AiEvalDataset}. Delegates entity-level CRUD to the platform
 * {@link AiEvalDatasetService}; owns the workspace membership row in {@code workspace_ai_eval_dataset} and
 * workspace-scoped queries that join through it.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class WorkspaceAiEvalDatasetServiceImpl implements WorkspaceAiEvalDatasetService {

    private final AiEvalDatasetService aiEvalDatasetService;
    private final WorkspaceAiEvalDatasetRepository workspaceAiEvalDatasetRepository;

    WorkspaceAiEvalDatasetServiceImpl(
        AiEvalDatasetService aiEvalDatasetService,
        WorkspaceAiEvalDatasetRepository workspaceAiEvalDatasetRepository) {

        this.aiEvalDatasetService = aiEvalDatasetService;
        this.workspaceAiEvalDatasetRepository = workspaceAiEvalDatasetRepository;
    }

    @Override
    public AiEvalDataset createInWorkspace(AiEvalDataset dataset, long workspaceId) {
        Validate.notNull(dataset, "dataset must not be null");
        Validate.isTrue(dataset.getId() == null, "dataset id must be null for creation");

        AiEvalDataset saved = aiEvalDatasetService.create(dataset);

        workspaceAiEvalDatasetRepository.save(new WorkspaceAiEvalDataset(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long datasetId) {
        return workspaceAiEvalDatasetRepository.findByAiEvalDatasetId(datasetId)
            .map(WorkspaceAiEvalDataset::getWorkspaceId)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalDataset> findAllByWorkspace(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        return workspaceAiEvalDatasetRepository.findAllDatasetsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiEvalDataset> findByWorkspaceAndName(Long workspaceId, String name) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");

        return workspaceAiEvalDatasetRepository.findDatasetByWorkspaceIdAndName(workspaceId, name);
    }
}
