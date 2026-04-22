/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import com.bytechef.ee.automation.ai.observability.service.AiObservabilityExportExecutor;
import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityExportJobService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportFormat;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJob;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportJobType;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityExportScope;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityExportJobService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiObservabilityExportJobFacade}. Delegates to the shared export-job services and carries the
 * authorization guards so they are enforced for every caller of the facade rather than only the GraphQL entry point,
 * and keeps them off the shared {@code AiObservabilityExportJobService} which the export executor relies on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiObservabilityExportJobFacadeImpl implements AiObservabilityExportJobFacade {

    private final AiObservabilityExportExecutor aiObservabilityExportExecutor;
    private final AiObservabilityExportJobService aiObservabilityExportJobService;
    private final WorkspaceAiObservabilityExportJobService workspaceAiObservabilityExportJobService;

    @SuppressFBWarnings("EI")
    AiObservabilityExportJobFacadeImpl(
        AiObservabilityExportExecutor aiObservabilityExportExecutor,
        AiObservabilityExportJobService aiObservabilityExportJobService,
        WorkspaceAiObservabilityExportJobService workspaceAiObservabilityExportJobService) {

        this.aiObservabilityExportExecutor = aiObservabilityExportExecutor;
        this.aiObservabilityExportJobService = aiObservabilityExportJobService;
        this.workspaceAiObservabilityExportJobService = workspaceAiObservabilityExportJobService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityExportJob cancelExportJob(long id) {
        return aiObservabilityExportJobService.cancel(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityExportJob createExportJob(
        Long workspaceId, Long projectId, AiObservabilityExportFormat format, AiObservabilityExportScope scope,
        String filters, AiObservabilityExportJobType type, String cronExpression) {

        String currentUserLogin = SecurityUtils.getCurrentUserLogin();

        AiObservabilityExportJobType effectiveType = type != null ? type : AiObservabilityExportJobType.ON_DEMAND;

        AiObservabilityExportJob exportJob = new AiObservabilityExportJob(
            effectiveType, format, scope, currentUserLogin);

        exportJob.setProjectId(projectId);
        exportJob.setFilters(filters);
        exportJob.setCronExpression(cronExpression);

        AiObservabilityExportJob savedExportJob =
            workspaceAiObservabilityExportJobService.createInWorkspace(exportJob, workspaceId);

        // ON_DEMAND: run once immediately. SCHEDULED: service layer registered the cron; first run will come from the
        // scheduler event listener.
        if (effectiveType == AiObservabilityExportJobType.ON_DEMAND) {
            aiObservabilityExportExecutor.executeExport(savedExportJob.getId());
        }

        return savedExportJob;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiObservabilityExportJob getExportJob(long id) {
        return aiObservabilityExportJobService.getExportJob(id);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiObservabilityExportJob> getExportJobsByWorkspace(Long workspaceId) {
        return workspaceAiObservabilityExportJobService.getExportJobsByWorkspace(workspaceId);
    }
}
