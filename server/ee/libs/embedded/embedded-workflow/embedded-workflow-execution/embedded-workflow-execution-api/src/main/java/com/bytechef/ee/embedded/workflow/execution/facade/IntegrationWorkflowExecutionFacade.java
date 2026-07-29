/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import java.time.Instant;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationWorkflowExecutionFacade {

    /**
     * Returns the workflow execution with the given id only if it belongs to the connected user identified by the given
     * external user id; throws {@link org.springframework.security.access.AccessDeniedException} otherwise.
     */
    WorkflowExecutionDTO getConnectedUserWorkflowExecution(String externalUserId, long id);

    /**
     * Returns the page of workflow executions that belong to the connected user identified by the given external user
     * id within the given environment. Results are scoped to the connected user's own integration instances.
     */
    Page<WorkflowExecutionDTO> getConnectedUserWorkflowExecutions(
        String externalUserId, long environmentId, Status jobStatus, Instant jobStartDate, Instant jobEndDate,
        Long integrationInstanceConfigurationId, int pageNumber);

    WorkflowExecutionDTO getWorkflowExecution(long id);

    TaskExecutionDTO getWorkflowExecutionTaskExecution(long id, long taskExecutionId);

    Page<WorkflowExecutionDTO> getWorkflowExecutions(
        Long environmentId, Status jobStatus, Instant jobStartDate, Instant jobEndDate, Long projectId,
        Long integrationInstanceConfigurationId, String workflowId, int pageNumber);
}
