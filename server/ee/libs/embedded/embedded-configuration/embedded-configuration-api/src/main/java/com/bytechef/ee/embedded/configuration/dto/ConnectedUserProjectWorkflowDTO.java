/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import java.time.Instant;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ConnectedUserProjectWorkflowDTO(
    long id, long connectedUserId, boolean enabled, Instant lastExecutionDate, long projectId, WorkflowDTO workflow,
    String workflowReferenceCode, Integer workflowVersion) {

    public ConnectedUserProjectWorkflowDTO(
        long connectedUserId, ConnectedUserProjectWorkflow connectedUserProjectWorkflow,
        boolean enabled, Instant lastExecutionDate, ProjectWorkflow projectWorkflow, WorkflowDTO workflow) {

        this(
            connectedUserProjectWorkflow.getId(), connectedUserId, enabled, lastExecutionDate,
            projectWorkflow.getProjectId(),
            workflow, projectWorkflow.getWorkflowReferenceCode(), connectedUserProjectWorkflow.getWorkflowVersion());
    }
}
