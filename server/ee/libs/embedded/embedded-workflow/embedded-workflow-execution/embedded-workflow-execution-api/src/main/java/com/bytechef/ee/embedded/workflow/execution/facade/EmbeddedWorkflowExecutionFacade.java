/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.ee.embedded.workflow.execution.dto.EmbeddedWorkflowExecutionDTO;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedWorkflowExecutionFacade {

    Page<EmbeddedWorkflowExecutionDTO> getWorkflowExecutions(
        @Nullable Long environmentId, @Nullable Status jobStatus, @Nullable Instant jobStartDate,
        @Nullable Instant jobEndDate, @Nullable Long connectedUserId, long workspaceId, int pageNumber);
}
