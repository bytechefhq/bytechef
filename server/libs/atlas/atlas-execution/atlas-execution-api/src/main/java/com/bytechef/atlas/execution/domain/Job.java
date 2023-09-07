
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

package com.bytechef.atlas.execution.domain;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.error.Errorable;
import com.bytechef.error.ExecutionError;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.Prioritizable;
import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
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
public final class Job implements Errorable, Persistable<Long>, Prioritizable {

    public enum Status {
        COMPLETED(3),
        CREATED(1),
        FAILED(5),
        STOPPED(4),
        STARTED(2);

        private final int id;

        Status(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Status valueOf(int id) {
            return switch (id) {
                case 1 -> Status.CREATED;
                case 2 -> Status.STARTED;
                case 3 -> Status.COMPLETED;
                case 4 -> Status.STOPPED;
                case 5 -> Status.FAILED;
                default -> throw new IllegalStateException("Unexpected value: %s".formatted(id));
            };
        }
    }

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column("current_task")
    private int currentTask = -1;

    @Column("end_date")
    private LocalDateTime endDate;

    @Column("error")
    private ExecutionError error;

    @Id
    private Long id;

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
    private MapWrapper metadata = new MapWrapper();

    @Column
    private FileEntry outputs;

    @Column("parent_task_execution_id")
    private AggregateReference<TaskExecution, Long> parentTaskExecutionId;

    private int priority = Prioritizable.DEFAULT_PRIORITY;

    @Column("start_date")
    private LocalDateTime startDate;

    @Column
    private int status;

    @Version
    private int version;

    @Column
    private Webhooks webhooks;

    @Column("workflow_id")
    private String workflowId;

    public Job() {
    }

    public Job(long id) {
        this.id = id;
    }

    public Job(ResultSet rs) throws SQLException {
        this.currentTask = rs.getInt("current_task");

        Timestamp endDateTimestamp = rs.getTimestamp("end_date");

        if (endDateTimestamp != null) {
            this.endDate = LocalDateTimeUtils.getLocalDateTime(endDateTimestamp);
        }

        this.label = rs.getString("label");
        this.id = rs.getLong("id");
        this.priority = rs.getInt("priority");

        Timestamp startDateTimestamp = rs.getTimestamp("start_date");

        if (startDateTimestamp != null) {
            this.startDate = LocalDateTimeUtils.getLocalDateTime(startDateTimestamp);
        }

        this.status = rs.getInt("status");
        this.workflowId = rs.getString("workflow_id");
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
     * @return {@link LocalDateTime}
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
    public Long getId() {
        return id;
    }

    /**
     * Get time execution entered end status: COMPLETED, STOPPED, FAILED
     *
     * @return {@link LocalDateTime}
     */
    public LocalDateTime getEndDate() {
        return endDate;
    }

    /**
     * Get the key-value map of inputs passed to the job when it was created.
     *
     * @return {@link Map<String, ?>}
     */
    public Map<String, ?> getInputs() {
        return Collections.unmodifiableMap(inputs.getMap());
    }

    /**
     * Return the job's human-readable name.
     *
     * @return the workflow label
     */
    public String getLabel() {
        return label;
    }

    @Override
    public ExecutionError getError() {
        return error;
    }

    /**
     * For sub-flows. Return the ID of the parent task that created this job.
     *
     * @return The ID of the parent task if this is a subflow job or <code>null</code> otherwise.
     */
    public Long getParentTaskExecutionId() {
        return parentTaskExecutionId == null ? null : parentTaskExecutionId.getId();
    }

    /**
     * Return the {@link Status}
     *
     * @return The job's status.
     */
    public Status getStatus() {
        return Status.valueOf(status);
    }

    /**
     * Return the time of when the job began execution.
     *
     * @return {@link LocalDateTime}
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public Map<String, ?> getMetadata() {
        return Collections.unmodifiableMap(metadata.getMap());
    }

    /**
     * Get the key-value map of outputs returned by the job.
     *
     * @return {@link Map<String, Object>}
     */
    public FileEntry getOutputs() {
        return outputs;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Get the list of list configured for this job.
     *
     * @return {@link List}
     */
    public List<Webhook> getWebhooks() {
        return webhooks == null ? List.of() : webhooks.list;
    }

    /**
     * Return the job's workflow id.
     *
     * @return the workflow id
     */
    public String getWorkflowId() {
        return workflowId;
    }

    @JsonIgnore
    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setCurrentTask(int currentTask) {
        this.currentTask = currentTask;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public void setError(ExecutionError error) {
        this.error = error;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setInputs(Map<String, ?> inputs) {
        this.inputs = new MapWrapper(inputs);
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setMetadata(Map<String, ?> metadata) {
        this.metadata = new MapWrapper(metadata);
    }

    public void setOutputs(FileEntry outputs) {
        this.outputs = outputs;
    }

    public void setParentTaskExecutionId(Long parentTaskExecutionId) {
        if (parentTaskExecutionId != null) {
            this.parentTaskExecutionId = AggregateReference.to(parentTaskExecutionId);
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setStatus(Status status) {
        this.status = status.getId();
    }

    public void setWebhooks(List<Webhook> webhooks) {
        this.webhooks = new Webhooks(webhooks);
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String toString() {
        return "Job{" + "id="
            + id + ", workflowId='"
            + workflowId + '\'' + ", status="
            + status + ", label='"
            + label + '\'' + ", startDate="
            + startDate + ", endDate="
            + endDate + ", inputs="
            + inputs + ", outputs="
            + outputs + ", list="
            + webhooks + ", error="
            + error + ", parentTaskExecutionId="
            + getParentTaskExecutionId() + ", priority="
            + priority + ", currentTask="
            + currentTask + ", createdBy='"
            + createdBy + '\'' + ", createdDate="
            + createdDate + ", lastModifiedBy='"
            + lastModifiedBy + '\'' + ", lastModifiedDate="
            + lastModifiedDate + '}';
    }

    public record Retry(Integer initialInterval, Integer maxInterval, Integer maxAttempts, Integer multiplier) {
    }

    public record Webhook(String type, String url, Retry retry) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();

            map.put(WorkflowConstants.TYPE, type);
            map.put(WorkflowConstants.URL, url);
            map.put(
                WorkflowConstants.MAX_RETRIES,
                Map.of(
                    "initialInterval", retry.initialInterval(),
                    "maxInterval", retry.maxInterval(),
                    "maxAttempts", retry.maxAttempts(),
                    "multiplier", retry.multiplier()));

            return map;
        }
    }

    @SuppressFBWarnings("EI")
    public record Webhooks(List<Webhook> list) {
    }
}
