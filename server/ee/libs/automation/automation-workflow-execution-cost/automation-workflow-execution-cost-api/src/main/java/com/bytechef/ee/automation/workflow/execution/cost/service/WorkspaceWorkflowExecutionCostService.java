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
import org.jspecify.annotations.Nullable;

/**
 * Workspace-membership write/read side for execution cost rows. {@code createInWorkspace} with a {@code null} workspace
 * persists the cost row without a membership row (editor runs and embedded executions have no workspace).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceWorkflowExecutionCostService {

    WorkflowExecutionCost createInWorkspace(WorkflowExecutionCost workflowExecutionCost, @Nullable Long workspaceId);

    Optional<Long> fetchWorkspaceIdByWorkflowExecutionCostId(long workflowExecutionCostId);

    /** Sum of the workspace's execution total costs created at or after {@code since}; zero when none. */
    BigDecimal sumTotalCostByWorkspaceSince(long workspaceId, Instant since);
}
