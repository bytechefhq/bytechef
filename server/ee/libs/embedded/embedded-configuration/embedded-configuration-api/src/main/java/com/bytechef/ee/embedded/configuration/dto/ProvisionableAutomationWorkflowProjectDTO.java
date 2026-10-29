/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * A released automation workflow project an administrator may provision for one connected user, with the workflows of
 * the pinned version that are turned on in the connected user's environment.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProvisionableAutomationWorkflowProjectDTO(
    long projectId, String name, int projectVersion, List<Workflow> workflows) {

    /**
     * @version ee
     */
    @SuppressFBWarnings("EI")
    public record Workflow(
        String workflowUuid, String label, List<ConnectedUserWorkflowTemplateDTO.Input> inputs, boolean provisioned,
        boolean dangling) {
    }
}
