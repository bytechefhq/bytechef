
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

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.error.Errorable;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.commons.utils.MapUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
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

        public static Format parse(String fileName) {
            Assert.notNull(fileName, "Filename '%s' can not be null".formatted(fileName));

            String extension = FilenameUtils.getExtension(fileName);

            return Objects.equals(extension.toLowerCase(), "json") ? JSON : YAML;
        }
    }

    public enum SourceType {
        CLASSPATH, FILESYSTEM, GIT, JDBC
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
    private ExecutionError error;

    @Column
    private Format format;

    @Id
    private String id;

    @Transient
    private final List<Map<String, Object>> inputs;

    @Transient
    private final String label;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Transient
    private final List<Map<String, Object>> outputs;

    @Transient
    private SourceType sourceType;

    @Transient
    private final int retry;

    @Transient
    private final List<WorkflowTask> tasks;

    // TODO Add version
    // @Version
    @SuppressFBWarnings("UuF")
    private int version;

    public Workflow() {
        this(Collections.emptyMap());
    }

    public Workflow(Map<String, Object> source) {
        Assert.notNull(source, "'source' must not be null.");

        id = MapUtils.getString(source, WorkflowConstants.ID);
        inputs = MapUtils.getList(
            source, WorkflowConstants.INPUTS, new ParameterizedTypeReference<>() {}, Collections.emptyList());
        label = MapUtils.getString(source, WorkflowConstants.LABEL);
        outputs = MapUtils.getList(
            source, WorkflowConstants.OUTPUTS, new ParameterizedTypeReference<>() {}, Collections.emptyList());
        retry = MapUtils.getInteger(source, WorkflowConstants.RETRY, 0);
        tasks = MapUtils
            .getList(source, WorkflowConstants.TASKS, new ParameterizedTypeReference<Map<String, Object>>() {})
            .stream()
            .map(WorkflowTask::new)
            .toList();
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

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setError(ExecutionError error) {
        this.error = error;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    @Override
    public String toString() {
        return "Workflow{" +
            "definition='" + definition + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", error=" + error +
            ", format=" + format +
            ", id='" + id + '\'' +
            ", inputs=" + inputs +
            ", label='" + label + '\'' +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", outputs=" + outputs +
            ", sourceType=" + sourceType +
            ", retry=" + retry +
            ", tasks=" + tasks +
            ", version=" + version +
            '}';
    }
}
