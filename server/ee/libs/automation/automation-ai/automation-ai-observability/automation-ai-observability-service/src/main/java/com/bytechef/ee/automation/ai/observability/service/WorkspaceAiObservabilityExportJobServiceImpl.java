/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityExportJobRepository;
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

    private final AiObservabilityExportJobRepository aiObservabilityExportJobRepository;
    private final AiObservabilityExportJobService aiObservabilityExportJobService;

    WorkspaceAiObservabilityExportJobServiceImpl(
        AiObservabilityExportJobRepository aiObservabilityExportJobRepository,
        AiObservabilityExportJobService aiObservabilityExportJobService) {

        this.aiObservabilityExportJobRepository = aiObservabilityExportJobRepository;
        this.aiObservabilityExportJobService = aiObservabilityExportJobService;
    }

    @Override
    public AiObservabilityExportJob createInWorkspace(AiObservabilityExportJob exportJob, long workspaceId) {
        exportJob.setWorkspaceId(workspaceId);

        return aiObservabilityExportJobService.create(exportJob);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityExportJob> getExportJobsByWorkspace(Long workspaceId) {
        return aiObservabilityExportJobRepository.findAllByWorkspaceIdOrderByCreatedDateDesc(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long exportJobId) {
        // findById rather than the service's getExportJob: an unknown id must still yield null (the pre-collapse
        // "no membership row" answer) because callers use this as an authorization probe, not as a fetch.
        return aiObservabilityExportJobRepository.findById(exportJobId)
            .map(AiObservabilityExportJob::getWorkspaceId)
            .orElse(null);
    }
}
