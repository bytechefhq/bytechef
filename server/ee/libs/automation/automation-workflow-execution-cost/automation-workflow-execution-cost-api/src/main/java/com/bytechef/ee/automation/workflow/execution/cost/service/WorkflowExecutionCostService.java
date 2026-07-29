/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.service;

import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkflowExecutionCost;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkflowExecutionCostService {

    WorkflowExecutionCost create(WorkflowExecutionCost workflowExecutionCost);

    Optional<WorkflowExecutionCost> fetchByJobId(long jobId);

    /** Total execution cost (USD) accrued by the current tenant since the given instant; zero when no rows. */
    BigDecimal sumTotalCostSince(Instant since);
}
