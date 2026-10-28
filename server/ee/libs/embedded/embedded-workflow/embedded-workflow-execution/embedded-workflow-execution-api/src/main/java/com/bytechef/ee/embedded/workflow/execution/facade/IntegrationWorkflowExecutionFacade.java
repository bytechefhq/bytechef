/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionChildrenPageDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import java.time.Instant;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationWorkflowExecutionFacade {

    WorkflowExecutionDTO getWorkflowExecution(long id);

    /**
     * @param lazy when {@code true}, the job carries only its top-level task executions with their child counts, the
     *             focus path and the latest-attempt task execution count; children are loaded through
     *             {@link #getWorkflowExecutionTaskExecutionChildren(long, long, int, int)}
     */
    WorkflowExecutionDTO getWorkflowExecution(long id, boolean lazy);

    TaskExecutionChildrenPageDTO getWorkflowExecutionTaskExecutionChildren(
        long id, long taskExecutionId, int pageNumber, int pageSize);

    TaskExecutionDTO getWorkflowExecutionTaskExecution(long id, long taskExecutionId);

    Page<WorkflowExecutionDTO> getWorkflowExecutions(
        Long environmentId, Status jobStatus, Instant jobStartDate, Instant jobEndDate, Long integrationId,
        Long integrationInstanceConfigurationId, Long connectedUserId, String workflowId, int pageNumber);
}
