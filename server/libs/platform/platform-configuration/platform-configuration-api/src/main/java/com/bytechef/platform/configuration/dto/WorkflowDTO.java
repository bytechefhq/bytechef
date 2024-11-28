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

package com.bytechef.platform.configuration.dto;

import com.bytechef.atlas.configuration.domain.Workflow;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class WorkflowDTO {

    private final String createdBy;
    private final Instant createdDate;
    private final String definition;
    private final String description;
    private final Workflow.Format format;
    private final String id;
    private final List<Workflow.Input> inputs;
    private final String label;
    private final String lastModifiedBy;
    private final Instant lastModifiedDate;
    private final List<Workflow.Output> outputs;
    private final Workflow.SourceType sourceType;
    private final int maxRetries;
    private final List<WorkflowTaskDTO> tasks;
    private final List<WorkflowTriggerDTO> triggers;
    private final int version;
    private final Workflow workflow;

    /**
     *
     */
    public WorkflowDTO(
        String createdBy, Instant createdDate, String definition, String description, Workflow.Format format,
        String id, List<Workflow.Input> inputs, String label, String lastModifiedBy, Instant lastModifiedDate,
        List<Workflow.Output> outputs, Workflow.SourceType sourceType, int maxRetries, List<WorkflowTaskDTO> tasks,
        List<WorkflowTriggerDTO> triggers, int version, Workflow workflow) {

        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.definition = definition;
        this.description = description;
        this.format = format;
        this.id = id;
        this.inputs = inputs;
        this.label = label;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedDate = lastModifiedDate;
        this.outputs = outputs;
        this.sourceType = sourceType;
        this.maxRetries = maxRetries;
        this.tasks = tasks;
        this.triggers = triggers;
        this.version = version;
        this.workflow = workflow;
    }

    public WorkflowDTO(Workflow workflow, List<WorkflowTaskDTO> tasks, List<WorkflowTriggerDTO> triggers) {
        this(
            workflow.getCreatedBy(), workflow.getCreatedDate(), workflow.getDefinition(), workflow.getDescription(),
            workflow.getFormat(), workflow.getId(), workflow.getInputs(), workflow.getLabel(),
            workflow.getLastModifiedBy(), workflow.getLastModifiedDate(), workflow.getOutputs(),
            workflow.getSourceType(), workflow.getMaxRetries(), tasks, triggers, workflow.getVersion(), workflow);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getDefinition() {
        return definition;
    }

    public String getDescription() {
        return description;
    }

    public Workflow.Format getFormat() {
        return format;
    }

    public String getId() {
        return id;
    }

    public List<Workflow.Input> getInputs() {
        return inputs;
    }

    public String getLabel() {
        return label;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public List<Workflow.Output> getOutputs() {
        return outputs;
    }

    public Workflow.SourceType getSourceType() {
        return sourceType;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public List<WorkflowTaskDTO> getTasks() {
        return tasks;
    }

    public List<WorkflowTriggerDTO> getTriggers() {
        return triggers;
    }

    public int getVersion() {
        return version;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        var that = (WorkflowDTO) obj;

        return Objects.equals(this.createdBy, that.createdBy) &&
            Objects.equals(this.createdDate, that.createdDate) &&
            Objects.equals(this.definition, that.definition) &&
            Objects.equals(this.description, that.description) &&
            Objects.equals(this.format, that.format) &&
            Objects.equals(this.id, that.id) &&
            Objects.equals(this.inputs, that.inputs) &&
            Objects.equals(this.label, that.label) &&
            Objects.equals(this.lastModifiedBy, that.lastModifiedBy) &&
            Objects.equals(this.lastModifiedDate, that.lastModifiedDate) &&
            Objects.equals(this.outputs, that.outputs) &&
            Objects.equals(this.sourceType, that.sourceType) &&
            this.maxRetries == that.maxRetries &&
            Objects.equals(this.tasks, that.tasks) &&
            Objects.equals(this.triggers, that.triggers) &&
            this.version == that.version &&
            Objects.equals(this.workflow, that.workflow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            createdBy, createdDate, definition, description, format, id, inputs, label, lastModifiedBy,
            lastModifiedDate, outputs, sourceType, maxRetries, tasks, triggers, version, workflow);
    }

    @Override
    public String toString() {
        return "WorkflowDTO[" +
            "createdBy=" + createdBy + ", " +
            "createdDate=" + createdDate + ", " +
            "definition=" + definition + ", " +
            "description=" + description + ", " +
            "format=" + format + ", " +
            "id=" + id + ", " +
            "inputs=" + inputs + ", " +
            "label=" + label + ", " +
            "lastModifiedBy=" + lastModifiedBy + ", " +
            "lastModifiedDate=" + lastModifiedDate + ", " +
            "outputs=" + outputs + ", " +
            "sourceType=" + sourceType + ", " +
            "maxRetries=" + maxRetries + ", " +
            "tasks=" + tasks + ", " +
            "triggers=" + triggers + ", " +
            "version=" + version + ", " +
            "workflow=" + workflow + ']';
    }
}
