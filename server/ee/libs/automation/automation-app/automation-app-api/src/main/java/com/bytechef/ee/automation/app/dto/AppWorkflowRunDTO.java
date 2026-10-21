/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;

/**
 * The state of a workflow run of an app. A failed run is data: {@code status} is {@code FAILED} and
 * {@code errorMessage} says why.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppWorkflowRunDTO(
    String runId, String workflowKey, AppWorkflowRunStatus status, @Nullable Object result,
    @Nullable String errorMessage, @Nullable String approvalFormUrl) {

    public boolean isFinished() {
        return status == AppWorkflowRunStatus.COMPLETED || status == AppWorkflowRunStatus.FAILED ||
            status == AppWorkflowRunStatus.CANCELLED;
    }
}
