
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.domain;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.error.Errorable;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.commons.util.MapValueUtils;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

/**
 * Workflows are the blueprints that describe the execution of a job.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Table
public final class Workflow implements Errorable, Persistable<String>, Serializable {

    public enum Format {
        JSON,
        YAML;

        public static Format parse(String filename) {
            Assert.notNull(filename, "Filename '%s' can not be null".formatted(filename));

            String extension = Optional.of(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1))
                .orElse("");

            return Objects.equals(extension.toLowerCase(), "json") ? JSON : YAML;
        }

    }

    public enum SourceType {
        CLASSPATH, FILESYSTEM, GIT, JDBC;
    }

    @Column
    private String definition;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Transient
    private String description;

    @Transient
    private ExecutionError error;

    @Column
    private Format format;

    @Id
    private String id;

    @Transient
    private List<Map<String, Object>> inputs = Collections.emptyList();

    @Transient
    private boolean isNew;

    @Transient
    private String label;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Transient
    private List<Map<String, Object>> outputs = Collections.emptyList();

    @Transient
    private SourceType sourceType;

    @Transient
    private int retry;

    @Transient
    private List<WorkflowTask> tasks = Collections.emptyList();

    @Version
    private int version;

    public Workflow() {
    }

    public Workflow(String id, String definition, Format format, Map<String, Object> source) {
        Assert.notNull(id, "'id' must not be null");
        Assert.notNull(format, "'format' must not be null");
        Assert.notNull(source, "'source' must not be null");

        this.id = id;
        this.format = format;
        this.definition = definition;
        this.description = MapValueUtils.getString(source, WorkflowConstants.DESCRIPTION);
        this.inputs = MapValueUtils.getList(
            source, WorkflowConstants.INPUTS, new ParameterizedTypeReference<>() {}, Collections.emptyList());
        this.label = MapValueUtils.getString(source, WorkflowConstants.LABEL);
        this.outputs = MapValueUtils.getList(
            source, WorkflowConstants.OUTPUTS, new ParameterizedTypeReference<>() {}, Collections.emptyList());
        this.retry = MapValueUtils.getInteger(source, WorkflowConstants.RETRY, 0);
        this.tasks = MapValueUtils
            .getList(source, WorkflowConstants.TASKS, new ParameterizedTypeReference<Map<String, Object>>() {})
            .stream()
            .map(WorkflowTask::of)
            .toList();
    }

    public Workflow(String definition, Format format) {
        this.definition = definition;
        this.format = format;
    }

    public Workflow(String definition, Format format, SourceType sourceType) {
        this.definition = definition;
        this.format = format;
        this.sourceType = sourceType;
    }

    public Workflow(String id, String definition) {
        this.id = id;
        this.definition = definition;
    }

    public Workflow(String id, ExecutionError error) {
        this.id = id;
        this.error = error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Workflow workflow = (Workflow) o;

        return Objects.equals(id, workflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getDefinition() {
        return definition;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public ExecutionError getError() {
        return error;
    }

    public Format getFormat() {
        return format;
    }

    /** Returns the unique identifier of the workflow. */
    public String getId() {
        return id;
    }

    /** Returns the workflow's expected inputs */
    public List<Map<String, Object>> getInputs() {
        return Collections.unmodifiableList(inputs);
    }

    /** Returns a descriptive name for the workflow. */
    public String getLabel() {
        return label;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /** Returns the workflow's expected outputs */
    public List<Map<String, Object>> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    /**
     * Defines the maximum number of times a task may retry.
     *
     * @return int the maximum number of retries.
     */
    public int getRetry() {
        return retry;
    }

    /** Returns the steps that make up the workflow. */
    public List<WorkflowTask> getTasks() {
        return tasks;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Workflow{" +
            ", id=" + id +
            ", label='" + label + '\'' +
            ", definition='" + definition + '\'' +
            ", format=" + format +
            ", sourceType=" + sourceType +
            ", inputs=" + inputs +
            ", outputs=" + outputs +
            ", error=" + error +
            ", retry=" + retry +
            ", tasks=" + tasks +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }

    public Workflow update(Workflow workflow) {
        this.definition = workflow.definition;
        this.description = workflow.description;
        this.error = workflow.error;
        this.format = workflow.format;
        this.id = workflow.id;
        this.isNew = workflow.isNew;
        this.inputs = workflow.inputs;
        this.label = workflow.label;
        this.outputs = workflow.outputs;
        this.sourceType = workflow.sourceType;
        this.retry = workflow.retry;
        this.tasks = workflow.tasks;

        return this;
    }
}
