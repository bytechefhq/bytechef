/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * What an embedded MCP tool runs for a connected user's activated automation: the user's own reference deployment and
 * the deployment workflow at the deployment's current project version.
 *
 * @param connectedUserProjectWorkflowId the connected user's reference id
 * @param projectDeploymentId            the reference's project deployment id
 * @param workflowId                     the workflow id at the deployment's project version
 * @param inputs                         the deployment workflow's input values, by input name
 * @param missingConnectionComponentName the name of a component whose required connection the deployment workflow
 *                                       lacks, or null when every required connection is set
 * @param workflowCallTriggerName        the name of the enabled {@code workflow/v1/newWorkflowCall} trigger of the
 *                                       workflow at the deployment's project version, or null when it has none and so
 *                                       cannot be called as a tool
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record CallableAutomationDTO(
    long connectedUserProjectWorkflowId, long projectDeploymentId, String workflowId, Map<String, ?> inputs,
    @Nullable String missingConnectionComponentName, @Nullable String workflowCallTriggerName) {

    public boolean isToolCallable() {
        return workflowCallTriggerName != null;
    }
}
