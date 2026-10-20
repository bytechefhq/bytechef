/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.dto;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.workflow.execution.dto.FocusPathElementDTO;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record WorkflowExecutionDTO(
    long id, Integration integration, @Nullable IntegrationInstanceConfiguration integrationInstanceConfiguration,
    @Nullable IntegrationInstance integrationInstance, JobDTO job, Workflow workflow,
    @Nullable TriggerExecutionDTO triggerExecution, @Nullable List<FocusPathElementDTO> focusPath,
    @Nullable Long taskExecutionCount) {

    public WorkflowExecutionDTO(
        long id, Integration integration, @Nullable IntegrationInstanceConfiguration integrationInstanceConfiguration,
        @Nullable IntegrationInstance integrationInstance, JobDTO job, Workflow workflow,
        @Nullable TriggerExecutionDTO triggerExecution) {

        this(
            id, integration, integrationInstanceConfiguration, integrationInstance, job, workflow, triggerExecution,
            null, null);
    }

    public WorkflowExecutionDTO withoutDetails() {
        return new WorkflowExecutionDTO(
            id, integration, integrationInstanceConfiguration, integrationInstance,
            job == null ? null : job.withoutDetails(), workflow,
            triggerExecution == null ? null : triggerExecution.withoutDetails(), focusPath, taskExecutionCount);
    }

    public boolean isTriggerOnly() {
        return job == null;
    }
}
