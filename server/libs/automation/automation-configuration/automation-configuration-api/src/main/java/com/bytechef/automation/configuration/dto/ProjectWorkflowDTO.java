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

package com.bytechef.automation.configuration.dto;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class ProjectWorkflowDTO extends WorkflowDTO {

    private final long projectWorkflowId;
    private final String workflowReferenceCode;

    public ProjectWorkflowDTO(Workflow workflow, ProjectWorkflow projectWorkflow) {
        super(
            workflow.getCreatedBy(), workflow.getCreatedDate(), workflow.getDefinition(), workflow.getDescription(),
            workflow.getFormat(), workflow.getId(), workflow.getInputs(), workflow.getLabel(),
            workflow.getLastModifiedBy(), workflow.getLastModifiedDate(), workflow.getOutputs(),
            workflow.getSourceType(), workflow.getMaxRetries(),
            CollectionUtils.map(
                workflow.getAllTasks(), workflowTask -> new WorkflowTaskDTO(workflowTask, List.of(), null)),
            List.of(), workflow.getVersion(), workflow);

        this.projectWorkflowId = projectWorkflow.getId();
        this.workflowReferenceCode = projectWorkflow.getWorkflowReferenceCode();
    }

    public ProjectWorkflowDTO(WorkflowDTO workflowDTO, ProjectWorkflow projectWorkflow) {
        super(
            workflowDTO.getCreatedBy(), workflowDTO.getCreatedDate(), workflowDTO.getDefinition(),
            workflowDTO.getDescription(), workflowDTO.getFormat(), workflowDTO.getId(), workflowDTO.getInputs(),
            workflowDTO.getLabel(), workflowDTO.getLastModifiedBy(), workflowDTO.getLastModifiedDate(),
            workflowDTO.getOutputs(), workflowDTO.getSourceType(), workflowDTO.getMaxRetries(), workflowDTO.getTasks(),
            workflowDTO.getTriggers(), workflowDTO.getVersion(), workflowDTO.getWorkflow());

        this.projectWorkflowId = projectWorkflow.getId();
        this.workflowReferenceCode = projectWorkflow.getWorkflowReferenceCode();
    }

    public long getProjectWorkflowId() {
        return projectWorkflowId;
    }

    public String getWorkflowReferenceCode() {
        return workflowReferenceCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ProjectWorkflowDTO that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return projectWorkflowId == that.projectWorkflowId &&
            Objects.equals(workflowReferenceCode, that.workflowReferenceCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectWorkflowId, workflowReferenceCode);
    }

    @Override
    public String toString() {
        return "WorkflowDTO{" +
            "projectWorkflowId=" + projectWorkflowId +
            ", workflowReferenceCode='" + workflowReferenceCode + '\'' +
            "} " + super.toString();
    }
}
