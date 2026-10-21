/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppWorkflow;
import com.bytechef.ee.automation.app.domain.AppWorkflowRun;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppWorkflowService {

    /**
     * Creates the binding, or replaces the one under the same key in place. Rejects keys that break the key rule or
     * collide after normalisation, and workflows that are not callable by the app.
     */
    AppWorkflow bind(long appId, String workflowKey, long projectDeploymentId, String workflowUuid);

    /**
     * Records a workflow run started for the app under a fresh public run id.
     */
    AppWorkflowRun createRun(long appId, String workflowKey, long jobId);

    /**
     * Deletes the run records created before {@code instant}.
     *
     * @return the number of deleted records
     */
    int deleteRunsCreatedBefore(Instant instant);

    /**
     * Returns the app's binding under {@code workflowKey}, if any.
     */
    Optional<AppWorkflow> fetchBinding(long appId, String workflowKey);

    /**
     * Returns the run of the app with the public run id {@code runId}, if any.
     */
    Optional<AppWorkflowRun> fetchRun(long appId, String runId);

    /**
     * Returns the app's bindings ordered by workflow key.
     */
    List<AppWorkflow> getBindings(long appId);

    /**
     * Deletes the app's binding under {@code workflowKey}.
     *
     * @return {@code true} when a binding was deleted
     */
    boolean unbind(long appId, String workflowKey);
}
