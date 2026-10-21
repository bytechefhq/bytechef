/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppInstance;
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
     * collide after normalisation, and workflows that are not callable by the instance.
     */
    AppWorkflow bind(AppInstance appInstance, String workflowKey, long projectDeploymentId, String workflowUuid);

    /**
     * Records a workflow run started for the instance under a fresh public run id.
     */
    AppWorkflowRun createRun(long appInstanceId, String workflowKey, long jobId);

    /**
     * Deletes the run records created before {@code instant}.
     *
     * @return the number of deleted records
     */
    int deleteRunsCreatedBefore(Instant instant);

    /**
     * Deletes every binding of the instance; its run records stay until retention removes them.
     */
    void deleteBindings(long appInstanceId);

    /**
     * Returns the instance's binding under {@code workflowKey}, if any.
     */
    Optional<AppWorkflow> fetchBinding(long appInstanceId, String workflowKey);

    /**
     * Returns the run of the instance with the public run id {@code runId}, if any.
     */
    Optional<AppWorkflowRun> fetchRun(long appInstanceId, String runId);

    /**
     * Returns the instance's bindings ordered by workflow key.
     */
    List<AppWorkflow> getBindings(long appInstanceId);

    /**
     * Deletes the instance's binding under {@code workflowKey}.
     *
     * @return {@code true} when a binding was deleted
     */
    boolean unbind(long appInstanceId, String workflowKey);
}
