/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.service;

import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkflowExecutionCost;
import com.bytechef.ee.automation.workflow.execution.cost.repository.WorkflowExecutionCostRepository;
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

    private final WorkflowExecutionCostRepository workflowExecutionCostRepository;
    private final WorkflowExecutionCostService workflowExecutionCostService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public WorkspaceWorkflowExecutionCostServiceImpl(
        WorkflowExecutionCostRepository workflowExecutionCostRepository,
        WorkflowExecutionCostService workflowExecutionCostService) {

        this.workflowExecutionCostRepository = workflowExecutionCostRepository;
        this.workflowExecutionCostService = workflowExecutionCostService;
    }

    @Override
    public WorkflowExecutionCost createInWorkspace(
        WorkflowExecutionCost workflowExecutionCost, @Nullable Long workspaceId) {

        workflowExecutionCost.setWorkspaceId(workspaceId);

        return workflowExecutionCostService.create(workflowExecutionCost);
    }

    /**
     * Reads the owning workspace straight off the cost row. An unknown id and a workspace-less row both answer
     * {@link Optional#empty()} — the same result the missing membership row used to give.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Long> fetchWorkspaceIdByWorkflowExecutionCostId(long workflowExecutionCostId) {
        return workflowExecutionCostRepository.findById(workflowExecutionCostId)
            .map(WorkflowExecutionCost::getWorkspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumTotalCostByWorkspaceSince(long workspaceId, Instant since) {
        BigDecimal sum = workflowExecutionCostRepository.sumTotalCostByWorkspaceIdSince(workspaceId, since);

        return sum == null ? BigDecimal.ZERO : sum;
    }
}
