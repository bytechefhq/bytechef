/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationInstanceConfigurationWorkflowDTO(
    List<IntegrationInstanceConfigurationWorkflowConnection> connections, String createdBy, Instant createdDate,
    Map<String, ?> inputs, boolean enabled, Long id, Instant lastExecutionDate, String lastModifiedBy,
    Instant lastModifiedDate, Long integrationInstanceConfigurationId, int version, Workflow workflow,
    String workflowId, String workflowReferenceCode)
    implements Comparable<IntegrationInstanceConfigurationWorkflowDTO> {

    public IntegrationInstanceConfigurationWorkflowDTO(
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, Instant lastExecutionDate,
        Workflow workflow, String workflowReferenceCode) {

        this(
            integrationInstanceConfigurationWorkflow.getConnections(),
            integrationInstanceConfigurationWorkflow.getCreatedBy(),
            integrationInstanceConfigurationWorkflow.getCreatedDate(),
            integrationInstanceConfigurationWorkflow.getInputs(),
            integrationInstanceConfigurationWorkflow.isEnabled(),
            integrationInstanceConfigurationWorkflow.getId(),
            lastExecutionDate, integrationInstanceConfigurationWorkflow.getLastModifiedBy(),
            integrationInstanceConfigurationWorkflow.getLastModifiedDate(),
            integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
            integrationInstanceConfigurationWorkflow.getVersion(), workflow,
            integrationInstanceConfigurationWorkflow.getWorkflowId(), workflowReferenceCode);
    }

    @Override
    public int compareTo(IntegrationInstanceConfigurationWorkflowDTO integrationInstanceConfigurationWorkflowDTO) {
        return workflowId.compareTo(integrationInstanceConfigurationWorkflowDTO.workflowId);
    }

    public IntegrationInstanceConfigurationWorkflow toIntegrationInstanceConfigurationWorkflow() {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            new IntegrationInstanceConfigurationWorkflow();

        integrationInstanceConfigurationWorkflow.setConnections(connections);
        integrationInstanceConfigurationWorkflow.setEnabled(enabled);
        integrationInstanceConfigurationWorkflow.setId(id);
        integrationInstanceConfigurationWorkflow.setInputs(inputs);
        integrationInstanceConfigurationWorkflow.setVersion(version);
        integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);

        return integrationInstanceConfigurationWorkflow;
    }
}
