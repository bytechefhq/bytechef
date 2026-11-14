/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.CallableAutomationDTO;
import java.util.Optional;

/**
 * Resolves, strictly read-only, what a connected user's automation workflow of an automation project configuration runs
 * as an embedded MCP tool. Everything is resolved from the given connected user id, so a caller only ever gets that
 * user's own reference and deployment.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserMcpAutomationResolver {

    /**
     * Returns the callable automation when the configuration is enabled, its workflow with the uuid is enabled, the
     * connected user is assigned to that configuration, the user's reference for the uuid exists and is not dangling,
     * and the reference deployment has the workflow at its current project version. The deployment workflow's own
     * enabled flag is not checked.
     *
     * @param connectedUserId                  the connected user id
     * @param automationProjectConfigurationId the automation project configuration id
     * @param workflowUuid                     the automation workflow uuid
     * @return the callable automation, or empty when the user cannot call it
     */
    Optional<CallableAutomationDTO> fetchCallableAutomation(
        long connectedUserId, long automationProjectConfigurationId, String workflowUuid);

    /**
     * Returns the id of the connected user's reference for the workflow uuid when the user is assigned to the
     * configuration, whatever the on/off state of the configuration, its workflow, the reference or the deployment.
     *
     * @param connectedUserId                  the connected user id
     * @param automationProjectConfigurationId the automation project configuration id
     * @param workflowUuid                     the automation workflow uuid
     * @return the reference id, or empty when the user has no reference or is assigned to another configuration
     */
    Optional<Long> fetchConnectedUserProjectWorkflowId(
        long connectedUserId, long automationProjectConfigurationId, String workflowUuid);
}
