/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.configuration.dto;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
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
            workflow.getAllTasks()
                .stream()
                .map(workflowTask -> new WorkflowTaskDTO(workflowTask, List.of(), null))
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
