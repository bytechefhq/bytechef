/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.ee.embedded.workflow.execution.dto.WorkflowExecutionDTO;
import com.bytechef.platform.constant.Environment;
import java.time.Instant;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface IntegrationWorkflowExecutionFacade {

    WorkflowExecutionDTO getWorkflowExecution(long id);

    Page<WorkflowExecutionDTO> getWorkflowExecutions(
        Environment environment, Status jobStatus, Instant jobStartDate, Instant jobEndDate, Long projectId,
        Long integrationInstanceConfigurationId, String workflowId, int pageNumber);
}
