/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.service;

import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkflowExecutionCost;
import com.bytechef.ee.automation.workflow.execution.cost.repository.WorkflowExecutionCostRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkflowExecutionCostServiceImpl implements WorkflowExecutionCostService {

    private final WorkflowExecutionCostRepository workflowExecutionCostRepository;

    public WorkflowExecutionCostServiceImpl(WorkflowExecutionCostRepository workflowExecutionCostRepository) {
        this.workflowExecutionCostRepository = workflowExecutionCostRepository;
    }

    @Override
    public WorkflowExecutionCost create(WorkflowExecutionCost workflowExecutionCost) {
        return workflowExecutionCostRepository.save(workflowExecutionCost);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowExecutionCost> fetchByJobId(long jobId) {
        return workflowExecutionCostRepository.findByJobId(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal sumTotalCostSince(Instant since) {
        BigDecimal totalCost = workflowExecutionCostRepository.sumTotalCostSince(since);

        return totalCost == null ? BigDecimal.ZERO : totalCost;
    }
}
