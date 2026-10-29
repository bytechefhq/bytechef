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
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AutomationWorkflowProjectConfigurationVersionImpactDTO(List<Workflow> workflows) {

    @SuppressFBWarnings("EI")
    public record Workflow(
        String workflowUuid, String label, List<String> newRequiredInputs, List<String> newRequiredConnections,
        int affectedConnectedUserCount) {
    }
}
