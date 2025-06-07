/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.platform.configuration.dto.WorkflowDTO;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ConnectUserProjectWorkflowDTO(
    long connectedUserId, boolean enabled, long projectId, WorkflowDTO workflow, String workflowReferenceCode,
    Integer workflowVersion) {

    public ConnectUserProjectWorkflowDTO(
        long connectedUserId, boolean enabled, ProjectWorkflow projectWorkflow, WorkflowDTO workflow,
        Integer workflowVersion) {

        this(
            connectedUserId, enabled, projectWorkflow.getProjectId(), workflow,
            projectWorkflow.getWorkflowReferenceCode(), workflowVersion);
    }
}
