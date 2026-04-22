/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJob;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJobStatus;
import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityExportJobType;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityExportJobRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.scheduler.ExportScheduler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
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
class AiObservabilityExportJobServiceImpl implements AiObservabilityExportJobService {

    private final AiObservabilityExportJobRepository aiObservabilityExportJobRepository;
    private final ExportScheduler exportScheduler;

    public AiObservabilityExportJobServiceImpl(
        AiObservabilityExportJobRepository aiObservabilityExportJobRepository,
        ExportScheduler exportScheduler) {

        this.aiObservabilityExportJobRepository = aiObservabilityExportJobRepository;
        this.exportScheduler = exportScheduler;
    }

    @Override
    public AiObservabilityExportJob create(AiObservabilityExportJob exportJob) {
        Validate.notNull(exportJob, "exportJob must not be null");
        Validate.isTrue(exportJob.getId() == null, "exportJob id must be null for creation");

        AiObservabilityExportJob saved = aiObservabilityExportJobRepository.save(exportJob);

        if (saved.getType() == AiObservabilityExportJobType.SCHEDULED) {
            Validate.notBlank(saved.getCronExpression(),
                "cronExpression must not be blank for SCHEDULED export job");

            exportScheduler.scheduleExport(saved.getId(), saved.getCronExpression());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityExportJob getExportJob(long id) {
        return aiObservabilityExportJobRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("AiObservabilityExportJob not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityExportJob> getExportJobsByWorkspace(Long workspaceId) {
        return aiObservabilityExportJobRepository.findAllByWorkspaceIdOrderByCreatedDateDesc(workspaceId);
    }

    @Override
    public AiObservabilityExportJob update(AiObservabilityExportJob exportJob) {
        Validate.notNull(exportJob, "exportJob must not be null");
        Validate.notNull(exportJob.getId(), "exportJob id must not be null for update");

        return aiObservabilityExportJobRepository.save(exportJob);
    }

    /**
     * Marks a pending or in-flight export job CANCELLED. The running executor polls the status before each batch and
     * aborts when it sees CANCELLED (see {@code AiObservabilityExportExecutor}). Completed/failed jobs cannot be
     * cancelled.
     */
    @Override
    public AiObservabilityExportJob cancel(long id) {
        AiObservabilityExportJob exportJob = getExportJob(id);

        AiObservabilityExportJobStatus currentStatus = exportJob.getStatus();

        if (currentStatus == AiObservabilityExportJobStatus.COMPLETED
            || currentStatus == AiObservabilityExportJobStatus.FAILED
            || currentStatus == AiObservabilityExportJobStatus.CANCELLED) {

            throw new IllegalStateException(
                "AiObservabilityExportJob " + id + " cannot be cancelled: status is " + currentStatus);
        }

        exportJob.setStatus(AiObservabilityExportJobStatus.CANCELLED);

        if (exportJob.getType() == AiObservabilityExportJobType.SCHEDULED) {
            exportScheduler.cancelExport(exportJob.getId());
        }

        return aiObservabilityExportJobRepository.save(exportJob);
    }
}
