/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.dto.AppWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowRunDTO;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Workflow operations of the calling app. Every method resolves the app from the request's
 * {@link com.bytechef.ee.automation.app.security.AppPrincipal} and refuses a disabled app.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppWorkflowExecutionFacade {

    /**
     * Runs the workflow bound under {@code workflowKey} and waits up to {@code waitSeconds} (the configured default
     * when null) for it to settle. A run still going, waiting on an approval or failed is returned as data. Once the
     * job is created, a failure to record the run or to read its state propagates to the caller without a run id, and
     * the job keeps running.
     */
    AppWorkflowRunDTO executeWorkflow(String workflowKey, Map<String, Object> input, @Nullable Integer waitSeconds);

    /**
     * Returns the current state of a run started by the calling app.
     */
    AppWorkflowRunDTO getWorkflowRun(String runId);

    /**
     * Returns the workflows bound to the calling app, with their availability and schemas.
     */
    List<AppWorkflowDTO> getWorkflows();
}
