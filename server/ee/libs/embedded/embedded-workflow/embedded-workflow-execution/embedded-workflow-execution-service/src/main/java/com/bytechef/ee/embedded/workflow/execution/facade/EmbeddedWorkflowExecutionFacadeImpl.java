/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.ee.embedded.workflow.execution.dto.EmbeddedWorkflowExecutionDTO;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.domain.PrincipalJob;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO.Kind;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionScopeDTO;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class EmbeddedWorkflowExecutionFacadeImpl implements EmbeddedWorkflowExecutionFacade {

    private final IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade;
    private final PrincipalJobService principalJobService;
    private final ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;
    private final WorkflowExecutionRowService workflowExecutionRowService;

    @SuppressFBWarnings("EI")
    public EmbeddedWorkflowExecutionFacadeImpl(
        IntegrationWorkflowExecutionFacade integrationWorkflowExecutionFacade, PrincipalJobService principalJobService,
        ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade,
        WorkflowExecutionRowService workflowExecutionRowService) {

        this.integrationWorkflowExecutionFacade = integrationWorkflowExecutionFacade;
        this.principalJobService = principalJobService;
        this.projectWorkflowExecutionFacade = projectWorkflowExecutionFacade;
        this.workflowExecutionRowService = workflowExecutionRowService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public Page<EmbeddedWorkflowExecutionDTO> getWorkflowExecutions(
        @Nullable Long environmentId, @Nullable Status jobStatus, @Nullable Instant jobStartDate,
        @Nullable Instant jobEndDate, @Nullable Long connectedUserId, long workspaceId, int pageNumber) {

        List<WorkflowExecutionScopeDTO> workflowExecutionScopeDTOs = Stream.of(
            integrationWorkflowExecutionFacade.getWorkflowExecutionScope(environmentId, connectedUserId),
            projectWorkflowExecutionFacade.getEmbeddedWorkflowExecutionScope(
                environmentId, jobStatus, connectedUserId, workspaceId))
            .filter(Objects::nonNull)
            .toList();

        if (workflowExecutionScopeDTOs.isEmpty()) {
            return Page.empty();
        }

        Page<WorkflowExecutionRowDTO> rowsPage = workflowExecutionRowService.getWorkflowExecutionRows(
            jobStatus, jobStartDate, jobEndDate, workflowExecutionScopeDTOs, true, pageNumber);

        List<WorkflowExecutionRowDTO> rows = rowsPage.getContent();

        if (rows.isEmpty()) {
            return new PageImpl<>(List.of(), rowsPage.getPageable(), rowsPage.getTotalElements());
        }

        Set<Long> integrationJobIds = getIntegrationJobIds(rows);

        Map<WorkflowExecutionRowDTO, EmbeddedWorkflowExecutionDTO> embeddedWorkflowExecutionDTOMap = new HashMap<>();

        for (WorkflowExecutionDTO workflowExecutionDTO : integrationWorkflowExecutionFacade.getWorkflowExecutions(
            List.copyOf(integrationJobIds))) {

            embeddedWorkflowExecutionDTOMap.put(
                new WorkflowExecutionRowDTO(Kind.JOB, workflowExecutionDTO.id()),
                EmbeddedWorkflowExecutionDTO.of(workflowExecutionDTO));
        }

        List<WorkflowExecutionRowDTO> automationRows = rows.stream()
            .filter(row -> !isIntegrationRow(row, integrationJobIds))
            .toList();

        for (com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO workflowExecutionDTO : projectWorkflowExecutionFacade
            .getWorkflowExecutions(automationRows)) {

            Kind kind = workflowExecutionDTO.job() == null ? Kind.TRIGGER_EXECUTION : Kind.JOB;

            embeddedWorkflowExecutionDTOMap.put(
                new WorkflowExecutionRowDTO(kind, workflowExecutionDTO.id()),
                EmbeddedWorkflowExecutionDTO.of(workflowExecutionDTO));
        }

        List<EmbeddedWorkflowExecutionDTO> embeddedWorkflowExecutionDTOs = rows.stream()
            .map(embeddedWorkflowExecutionDTOMap::get)
            .filter(Objects::nonNull)
            .toList();

        return new PageImpl<>(embeddedWorkflowExecutionDTOs, rowsPage.getPageable(), rowsPage.getTotalElements());
    }

    private Set<Long> getIntegrationJobIds(List<WorkflowExecutionRowDTO> rows) {
        List<Long> jobIds = rows.stream()
            .filter(row -> row.kind() == Kind.JOB)
            .map(WorkflowExecutionRowDTO::id)
            .toList();

        if (jobIds.isEmpty()) {
            return Set.of();
        }

        return principalJobService.getPrincipalJobs(jobIds, PlatformType.EMBEDDED)
            .stream()
            .map(PrincipalJob::getJobId)
            .collect(Collectors.toSet());
    }

    private static boolean isIntegrationRow(WorkflowExecutionRowDTO row, Set<Long> integrationJobIds) {
        return row.kind() == Kind.JOB && integrationJobIds.contains(row.id());
    }
}
