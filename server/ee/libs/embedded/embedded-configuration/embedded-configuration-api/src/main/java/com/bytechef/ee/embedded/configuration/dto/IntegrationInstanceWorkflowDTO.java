/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationInstanceWorkflowDTO(
    String createdBy, Instant createdDate, Long id, long integrationInstanceConfigurationWorkflowId,
    Map<String, ?> inputs, String lastModifiedBy, Instant lastModifiedDate, boolean enabled, String workflowId) {

    public IntegrationInstanceWorkflowDTO(IntegrationInstanceWorkflow integrationInstanceWorkflow, String workflowId) {
        this(
            integrationInstanceWorkflow.getCreatedBy(), integrationInstanceWorkflow.getCreatedDate(),
            integrationInstanceWorkflow.getId(),
            integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId(),
            integrationInstanceWorkflow.getInputs(), integrationInstanceWorkflow.getLastModifiedBy(),
            integrationInstanceWorkflow.getLastModifiedDate(), integrationInstanceWorkflow.isEnabled(), workflowId);
    }
}
