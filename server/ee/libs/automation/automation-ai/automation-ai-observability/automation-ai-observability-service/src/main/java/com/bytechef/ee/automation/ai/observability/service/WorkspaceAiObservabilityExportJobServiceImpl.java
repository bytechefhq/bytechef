/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityExportJob;
import com.bytechef.ee.automation.ai.observability.repository.WorkspaceAiObservabilityExportJobRepository;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityExportJobService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class WorkspaceAiObservabilityExportJobServiceImpl implements WorkspaceAiObservabilityExportJobService {

    private final AiObservabilityExportJobService aiObservabilityExportJobService;
    private final WorkspaceAiObservabilityExportJobRepository workspaceAiObservabilityExportJobRepository;

    WorkspaceAiObservabilityExportJobServiceImpl(
        AiObservabilityExportJobService aiObservabilityExportJobService,
        WorkspaceAiObservabilityExportJobRepository workspaceAiObservabilityExportJobRepository) {

        this.aiObservabilityExportJobService = aiObservabilityExportJobService;
        this.workspaceAiObservabilityExportJobRepository = workspaceAiObservabilityExportJobRepository;
    }

    @Override
    public AiObservabilityExportJob createInWorkspace(AiObservabilityExportJob exportJob, long workspaceId) {
        AiObservabilityExportJob saved = aiObservabilityExportJobService.create(exportJob);

        workspaceAiObservabilityExportJobRepository.save(
            new WorkspaceAiObservabilityExportJob(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityExportJob> getExportJobsByWorkspace(Long workspaceId) {
        return workspaceAiObservabilityExportJobRepository.findAllExportJobsByWorkspaceIdOrderByCreatedDateDesc(
            workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long exportJobId) {
        return workspaceAiObservabilityExportJobRepository.findByAiObservabilityExportJobId(exportJobId)
            .map(WorkspaceAiObservabilityExportJob::getWorkspaceId)
            .orElse(null);
    }
}
