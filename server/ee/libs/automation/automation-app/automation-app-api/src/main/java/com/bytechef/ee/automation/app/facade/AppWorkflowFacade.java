/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.dto.AppBindableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppCallableWorkflowDTO;
import com.bytechef.ee.automation.app.dto.AppWorkflowBindingDTO;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The gated admin surface for the workflows an app instance can call.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppWorkflowFacade {

    List<AppWorkflowBindingDTO> getWorkflowBindings(long appInstanceId);

    List<AppBindableWorkflowDTO> getBindableWorkflows(long appInstanceId);

    /**
     * The workflows an app can call in DEVELOPMENT in a workspace, optionally of one project, limited to projects the
     * caller can see.
     */
    List<AppCallableWorkflowDTO> getCallableWorkflows(long workspaceId, @Nullable Long projectId);

    AppWorkflowBindingDTO bindWorkflow(
        long appInstanceId, String workflowKey, long projectDeploymentId, String workflowUuid);

    void unbindWorkflow(
        long appInstanceId, String workflowKey);
}
