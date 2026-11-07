/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record EmbeddedWorkflowExecutionDTO(
    Source source,
    com.bytechef.automation.workflow.execution.dto.@Nullable WorkflowExecutionDTO automationWorkflowExecution,
    @Nullable WorkflowExecutionDTO integrationWorkflowExecution) {

    public static EmbeddedWorkflowExecutionDTO of(
        com.bytechef.automation.workflow.execution.dto.WorkflowExecutionDTO automationWorkflowExecution) {

        return new EmbeddedWorkflowExecutionDTO(Source.AUTOMATION, automationWorkflowExecution, null);
    }

    public static EmbeddedWorkflowExecutionDTO of(WorkflowExecutionDTO integrationWorkflowExecution) {
        return new EmbeddedWorkflowExecutionDTO(Source.INTEGRATION, null, integrationWorkflowExecution);
    }

    public enum Source {
        AUTOMATION, INTEGRATION
    }
}
