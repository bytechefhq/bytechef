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
import com.bytechef.atlas.workflow.WorkflowFormat;
import com.bytechef.commons.collection.MapUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
public final class Workflow implements Errorable, Persistable<String> {

    @Column
    private String content;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Transient
    private ExecutionError error;

    @Column
    private WorkflowFormat format;

    @Id
    private String id;

    @Transient
    private List<Map<String, Object>> inputs;

    @Transient
    private String label;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Transient
    private List<Map<String, Object>> outputs;

    @Transient
    private List<WorkflowTask> tasks;

    @Transient
    private int retry;

    // TODO Add version
    // @Version
    @SuppressFBWarnings("UuF")
    private int version;

    public Workflow() {
        this(Collections.emptyMap());
    }

    public Workflow(Map<String, Object> source) {
        Assert.notNull(source, "source cannot be null");

        id = MapUtils.getString(source, WorkflowConstants.ID);
        inputs = MapUtils.getList(
                source, WorkflowConstants.INPUTS, new ParameterizedTypeReference<>() {}, Collections.emptyList());
        label = MapUtils.getString(source, WorkflowConstants.LABEL);
        outputs = MapUtils.getList(
                source, WorkflowConstants.OUTPUTS, new ParameterizedTypeReference<>() {}, Collections.emptyList());
        retry = MapUtils.getInteger(source, WorkflowConstants.RETRY, 0);
        tasks = MapUtils.getList(source, WorkflowConstants.TASKS, Map.class).stream()
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

    public String getContent() {
        return content;
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

    public WorkflowFormat getFormat() {
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

    /**
     * Defines the maximum number of times that this message may be retries.
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

    public void setContent(String content) {
        this.content = content;
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

    public void setFormat(WorkflowFormat format) {
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

    @Override
    public String toString() {
        return "Workflow{" + "content='"
                + content + '\'' + ", createdBy='"
                + createdBy + '\'' + ", createdDate="
                + createdDate + ", error="
                + error + ", format="
                + format + ", id='"
                + id + '\'' + ", inputs="
                + inputs + ", label='"
                + label + '\'' + ", lastModifiedBy='"
                + lastModifiedBy + '\'' + ", lastModifiedDate="
                + lastModifiedDate + ", outputs="
                + outputs + ", tasks="
                + tasks + ", retry="
                + retry + '}';
    }
}
