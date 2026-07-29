/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.service;

import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkflowExecutionCost;
import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkspaceWorkflowExecutionCost;
import com.bytechef.ee.automation.workflow.execution.cost.repository.WorkspaceWorkflowExecutionCostRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional
public class WorkspaceWorkflowExecutionCostServiceImpl implements WorkspaceWorkflowExecutionCostService {

    private final WorkflowExecutionCostService workflowExecutionCostService;
    private final WorkspaceWorkflowExecutionCostRepository workspaceWorkflowExecutionCostRepository;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceWorkflowExecutionCostServiceImpl(
        WorkflowExecutionCostService workflowExecutionCostService,
        WorkspaceWorkflowExecutionCostRepository workspaceWorkflowExecutionCostRepository) {

        this.workflowExecutionCostService = workflowExecutionCostService;
        this.workspaceWorkflowExecutionCostRepository = workspaceWorkflowExecutionCostRepository;
    }

    @Override
    public WorkflowExecutionCost createInWorkspace(
        WorkflowExecutionCost workflowExecutionCost, @Nullable Long workspaceId) {

        WorkflowExecutionCost savedWorkflowExecutionCost = workflowExecutionCostService.create(workflowExecutionCost);

        if (workspaceId != null) {
            workspaceWorkflowExecutionCostRepository.save(
                new WorkspaceWorkflowExecutionCost(savedWorkflowExecutionCost.getId(), workspaceId));
        }

        return savedWorkflowExecutionCost;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceIdByWorkflowExecutionCostId(long workflowExecutionCostId) {
        return workspaceWorkflowExecutionCostRepository.findByWorkflowExecutionCostId(workflowExecutionCostId)
            .map(WorkspaceWorkflowExecutionCost::getWorkspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumTotalCostByWorkspaceSince(long workspaceId, Instant since) {
        BigDecimal sum = workspaceWorkflowExecutionCostRepository.sumTotalCostByWorkspaceIdSince(workspaceId, since);

        return sum == null ? BigDecimal.ZERO : sum;
    }
}
