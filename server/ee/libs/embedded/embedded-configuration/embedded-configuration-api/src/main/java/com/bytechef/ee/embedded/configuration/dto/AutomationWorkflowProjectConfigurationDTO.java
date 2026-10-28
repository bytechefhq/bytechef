/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AutomationWorkflowProjectConfigurationDTO(
    long id, long projectId, long environmentId, int projectVersion, boolean enabled, boolean automationHubVisible,
    List<Workflow> workflows, Instant lastModifiedDate) {

    /**
     * @version ee
     */
    public record Workflow(String workflowUuid, String workflowId, String label, boolean enabled) {
    }
}
