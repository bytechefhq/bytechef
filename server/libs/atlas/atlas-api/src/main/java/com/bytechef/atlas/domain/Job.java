
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

import com.bytechef.atlas.error.Errorable;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.priority.Prioritizable;
import com.bytechef.commons.data.jdbc.wrapper.MapListWrapper;
import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Represents an instance of a job.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Table
public final class Job implements Errorable, Persistable<String>, Prioritizable {

    public enum Status {
        CREATED,
        STARTED,
        STOPPED,
        FAILED,
        COMPLETED,
    }

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column("current_task")
    private int currentTask = -1;

    @Column("end_time")
    private Date endTime;

    @Column("error")
    private ExecutionError error;

    @Id
    private String id;

    @Transient
    private boolean isNew;

    @Column
    private MapWrapper inputs = new MapWrapper();

    @Column
    private String label;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column
    private MapWrapper outputs = new MapWrapper();

    @Column("parent_task_execution_id")
    private AggregateReference<TaskExecution, String> parentTaskExecutionRef;

    private int priority = Prioritizable.DEFAULT_PRIORITY;

    @Column("start_time")
    private Date startTime;

    @Column
    private Status status;

    // TODO Add version
    // @Version
    @SuppressFBWarnings("UuF")
    private int version;

    @Column
    private MapListWrapper webhooks = new MapListWrapper();

    @Column("workflow_id")
    private String workflowId;

    public Job() {
    }

    public Job(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Job job = (Job) o;

        return Objects.equals(id, job.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Return the time when the job was originally created.
     *
     * @return {@link Date}
     */
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Returns the index of the step on the job's workflow on which the job is working on right now.
     *
     * @return int The step ordinal number
     */
    public int getCurrentTask() {
        return currentTask;
    }

    /** Return the ID of the job. */
    public String getId() {
        return id;
    }

    /**
     * Get time execution entered end status: COMPLETED, STOPPED, FAILED
     *
     * @return {@link Date}
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * Get the key-value map of inputs passed to the job when it was created.
     *
     * @return {@link Map<String, Object>}
     */
    public Map<String, Object> getInputs() {
        return Collections.unmodifiableMap(inputs.getMap());
    }

    /**
     * Return the job's human-readable name.
     *
     * @return {@link Workflow}
     */
    public String getLabel() {
        return label;
    }

    @Override
    public ExecutionError getError() {
        return error;
    }

    @JsonIgnore
    public AggregateReference<TaskExecution, String> getParentTaskExecutionRef() {
        return parentTaskExecutionRef;
    }

    /**
     * For sub-flows. Return the ID of the parent task that created this job.
     *
     * @return The ID of the parent task if this is a subflow job or <code>null</code> otherwise.
     */
    public String getParentTaskExecutionId() {
        return parentTaskExecutionRef == null ? null : parentTaskExecutionRef.getId();
    }

    /**
     * Return the {@link Status}
     *
     * @return The job's status.
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Return the time of when the job began execution.
     *
     * @return {@link Date}
     */
    public Date getStartTime() {
        return startTime;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Get the key-value map of outputs returned by the job.
     *
     * @return {@link Map<String, Object>}
     */
    public Map<String, Object> getOutputs() {
        return Collections.unmodifiableMap(outputs.getMap());
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    /**
     * Get the list of webhooks configured for this job.
     *
     * @return {@link List}
     */
    public List<Map<String, Object>> getWebhooks() {
        return Collections.unmodifiableList(webhooks.getList());
    }

    /**
     * Return the job's workflow id.
     *
     * @return {@link Workflow}
     */
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setCurrentTask(int currentTask) {
        this.currentTask = currentTask;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setError(ExecutionError error) {
        this.error = error;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = new MapWrapper(inputs);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void setOutputs(Map<String, Object> outputs) {
        this.outputs = new MapWrapper(outputs);
    }

    public void setParentTaskExecutionRef(AggregateReference<TaskExecution, String> parentTaskExecutionRef) {
        this.parentTaskExecutionRef = parentTaskExecutionRef;
    }

    public void setParentTaskExecutionId(String parentTaskExecutionId) {
        if (parentTaskExecutionId != null) {
            this.parentTaskExecutionRef = new AggregateReference.IdOnlyAggregateReference<>(parentTaskExecutionId);
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setWebhooks(List<Map<String, Object>> webhooks) {
        this.webhooks = new MapListWrapper(webhooks);
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String toString() {
        return "Job{" + "createdBy='"
            + createdBy + '\'' + ", createdDate="
            + createdDate + ", currentTask="
            + currentTask + ", endTime="
            + endTime + ", error="
            + error + ", id='"
            + id + '\'' + ", isNew="
            + isNew + ", inputs="
            + inputs + ", label='"
            + label + '\'' + ", lastModifiedBy='"
            + lastModifiedBy + '\'' + ", lastModifiedDate="
            + lastModifiedDate + ", outputs="
            + outputs + ", parentTaskExecution="
            + parentTaskExecutionRef + ", priority="
            + priority + ", startTime="
            + startTime + ", status="
            + status + ", webhooks="
            + webhooks + ", workflowId='"
            + workflowId + '\'' + '}';
    }
}
