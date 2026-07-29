/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.workflow.execution.cost.domain.WorkflowExecutionCost;
import com.bytechef.ee.automation.workflow.execution.cost.service.WorkflowExecutionCostService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * Read surface for per-execution cost rows written by the terminal-status listener. Returns {@code null} while the job
 * is still running (the row is only written on terminal status) or when cost recording is disabled — the client renders
 * the cost line only when a row exists, so absence needs no error mapping.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class WorkflowExecutionCostGraphQlController {

    private final WorkflowExecutionCostService workflowExecutionCostService;

    @SuppressFBWarnings("EI")
    public WorkflowExecutionCostGraphQlController(WorkflowExecutionCostService workflowExecutionCostService) {
        this.workflowExecutionCostService = workflowExecutionCostService;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public @Nullable WorkflowExecutionCost workflowExecutionCost(@Argument long jobId) {
        return workflowExecutionCostService.fetchByJobId(jobId)
            .orElse(null);
    }
}
