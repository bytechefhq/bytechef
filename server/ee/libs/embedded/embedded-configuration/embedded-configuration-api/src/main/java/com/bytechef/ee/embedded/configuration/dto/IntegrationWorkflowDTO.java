/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class IntegrationWorkflowDTO extends WorkflowDTO {

    private final long integrationWorkflowId;
    private final String workflowReferenceCode;

    public IntegrationWorkflowDTO(Workflow workflow, IntegrationWorkflow integrationWorkflow) {
        super(
            workflow.getCreatedBy(), workflow.getCreatedDate(), workflow.getDefinition(), workflow.getDescription(),
            workflow.getFormat(), workflow.getId(), workflow.getInputs(), workflow.getLabel(),
            workflow.getLastModifiedBy(), workflow.getLastModifiedDate(), workflow.getOutputs(),
            workflow.getSourceType(), workflow.getMaxRetries(),
            workflow.getTasks(true)
                .stream()
                .map(workflowTask -> new WorkflowTaskDTO(workflowTask, null, List.of()))
                .toList(),
            List.of(), workflow.getVersion(), workflow);

        this.integrationWorkflowId = integrationWorkflow.getId();
        this.workflowReferenceCode = integrationWorkflow.getWorkflowReferenceCode();
    }

    public IntegrationWorkflowDTO(WorkflowDTO workflow, IntegrationWorkflow integrationWorkflow) {
        super(
            workflow.getCreatedBy(), workflow.getCreatedDate(), workflow.getDefinition(), workflow.getDescription(),
            workflow.getFormat(), workflow.getId(), workflow.getInputs(), workflow.getLabel(),
            workflow.getLastModifiedBy(), workflow.getLastModifiedDate(), workflow.getOutputs(),
            workflow.getSourceType(), workflow.getMaxRetries(), workflow.getTasks(), workflow.getTriggers(),
            workflow.getVersion(), workflow.getWorkflow());

        this.integrationWorkflowId = integrationWorkflow.getId();
        this.workflowReferenceCode = integrationWorkflow.getWorkflowReferenceCode();
    }

    public long getIntegrationWorkflowId() {
        return integrationWorkflowId;
    }

    public String getWorkflowReferenceCode() {
        return workflowReferenceCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IntegrationWorkflowDTO that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return integrationWorkflowId == that.integrationWorkflowId &&
            Objects.equals(workflowReferenceCode, that.workflowReferenceCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), integrationWorkflowId, workflowReferenceCode);
    }

    @Override
    public String toString() {
        return "WorkflowDTO{" +
            "integrationWorkflowId=" + integrationWorkflowId +
            ", workflowReferenceCode='" + workflowReferenceCode + '\'' +
            "} " + super.toString();
    }
}
