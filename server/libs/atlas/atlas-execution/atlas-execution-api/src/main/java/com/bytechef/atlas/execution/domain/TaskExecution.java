/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.atlas.execution.domain;

import com.bytechef.atlas.configuration.domain.Task;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.error.Errorable;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.message.Prioritizable;
import com.bytechef.message.Retryable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
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
import org.springframework.util.Assert;

/**
 * Wraps the {@link WorkflowTask} instance to add execution semantics to the task.
 *
 * <p>
 * {@link TaskExecution} instances capture the life cycle of a single execution of a task. By single execution is meant
 * that the task goes through the following states:
 *
 * <ol>
 * <li><code>CREATED</code>
 * <li><code>STARTED</code>
 * <li><code>COMPLETED</code> or <code>FAILED</code> or <code>CANCELLED</code>
 * </ol>
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Table
public final class TaskExecution
    implements Errorable, Cloneable, Persistable<Long>, Prioritizable, Progressable, Retryable, Task {

    /**
     * Defines the various states that a {@link TaskExecution} can be in at any give moment in time.
     */
    public enum Status {
        CREATED(false),
        STARTED(false),
        COMPLETED(true),
        FAILED(true),
        CANCELLED(true);

        private final boolean terminated;

        Status(boolean terminated) {
            this.terminated = terminated;
        }

        public boolean isTerminated() {
            return terminated;
        }
    }

    private static final int DEFAULT_TASK_NUMBER = -1;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("end_date")
    private Instant endDate;

    @Column("error")
    private ExecutionError error;

    @Column("execution_time")
    private long executionTime;

    @Id
    private Long id;

    @Column("job_id")
    private AggregateReference<Job, Long> jobId;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Transient
    private Map<String, Object> metadata = new HashMap<>();

    @Column
    private int maxRetries;

    @Column
    private FileEntry output;

    @Column("parent_id")
    private AggregateReference<TaskExecution, Long> parentId;

    @Column
    private int priority;

    @Column
    private int progress;

    @Column("retry_attempts")
    private int retryAttempts;

    @Column("retry_delay")
    private String retryDelay;

    @Column("retry_delay_factor")
    private int retryDelayFactor;

    @Column("start_date")
    private Instant startDate;

    @Column
    private int status;

    @Column("task_number")
    private int taskNumber;

    @Column("workflow_task")
    private WorkflowTask workflowTask;

    public TaskExecution() {
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Evaluate the {@link WorkflowTask}
     *
     * @param context The context value to evaluate the task against
     * @return the evaluated {@link TaskExecution} instance.
     */
    public TaskExecution evaluate(Map<String, ?> context, Evaluator evaluator) {
        WorkflowTask workflowTask = getWorkflowTask();

        Map<String, Object> map = evaluator.evaluate(workflowTask.toMap(), context);

        setWorkflowTask(new WorkflowTask(map));

        return this;
    }

    @Override
    public TaskExecution clone() throws CloneNotSupportedException {
        return (TaskExecution) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TaskExecution taskExecution = (TaskExecution) o;

        return Objects.equals(id, taskExecution.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Get the time when this task instance was created.
     *
     * @return Date
     */
    public Instant getCreatedDate() {
        return createdDate;
    }

    /**
     * Return the time when this task instance ended (CANCELLED, FAILED, COMPLETED)
     *
     * @return Date
     */
    public Instant getEndDate() {
        return endDate;
    }

    @Override
    public ExecutionError getError() {
        return error;
    }

    /**
     * Returns the total time in ms for this task to execute (excluding wait time of the task in transit). i.e. actual
     * execution time on a worker node.
     *
     * @return long
     */
    public long getExecutionTime() {
        return executionTime;
    }

    @JsonIgnore
    public List<WorkflowTask> getFinalize() {
        return workflowTask.getFinalize();
    }

    /**
     * Get the unique id of the task instance.
     *
     * @return String the id of the task execution.
     */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
     * Get the id of the job for which this task belongs to.
     *
     * @return String the id of the job
     */
    public Long getJobId() {
        return jobId == null ? null : jobId.getId();
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    public Map<String, ?> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @JsonIgnore
    public String getName() {
        return workflowTask.getName();
    }

    @JsonIgnore
    public String getNode() {
        return workflowTask.getNode();
    }

    /**
     * Get the result output generated by the task handler which executed this task.
     *
     * @return Object the output of the task
     */
    public FileEntry getOutput() {
        return output;
    }

    @JsonIgnore
    public Map<String, ?> getParameters() {
        Assert.notNull(workflowTask, "workflowTask");

        return workflowTask.getParameters();
    }

    /**
     * Get the id of the parent task, if this is a sub-task.
     *
     * @return String the id of the parent task.
     */
    public Long getParentId() {
        return parentId == null ? null : parentId.getId();
    }

    @JsonIgnore
    public List<WorkflowTask> getPost() {
        return workflowTask.getPost();
    }

    @JsonIgnore
    public List<WorkflowTask> getPre() {
        return workflowTask.getPre();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public int getRetryAttempts() {
        return retryAttempts;
    }

    @Override
    public String getRetryDelay() {
        return retryDelay;
    }

    @Override
    public long getRetryDelayMillis() {
        Duration duration = Duration.parse("PT" + getRetryDelay());

        long delay = duration.toMillis();
        int retryAttempts = getRetryAttempts();
        int retryDelayFactor = getRetryDelayFactor();

        return delay * retryAttempts * retryDelayFactor;
    }

    @Override
    public int getRetryDelayFactor() {
        return retryDelayFactor;
    }

    /**
     * Get the time when this task instance was started.
     *
     * @return Date
     */
    public Instant getStartDate() {
        return startDate;
    }

    /**
     * Get the current status of this task.
     *
     * @return The status of the task.
     */
    public Status getStatus() {
        return Status.values()[status];
    }

    public WorkflowTask getWorkflowTask() {
        return workflowTask;
    }

    /**
     * Get the numeric order of the task in the workflow.
     *
     * @return int
     */
    public int getTaskNumber() {
        return taskNumber;
    }

    @JsonIgnore
    public String getTimeout() {
        return workflowTask.getTimeout();
    }

    @Override
    @JsonIgnore
    public String getType() {
        Assert.notNull(workflowTask.getType(), "Type must not be null");

        return workflowTask.getType();
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public TaskExecution putMetadata(String key, Object value) {
        metadata.put(key, value);

        return this;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;

        if (endDate != null && startDate != null) {
            this.executionTime = endDate.toEpochMilli() - startDate.toEpochMilli();
        }
    }

    public void setError(ExecutionError error) {
        this.error = error;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setJobId(Long jobId) {
        if (jobId != null) {
            this.jobId = AggregateReference.to(jobId);
        }
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setMetadata(Map<String, ?> metadata) {
        if (metadata == null) {
            this.metadata = new HashMap<>();
        } else {
            this.metadata = new HashMap<>(metadata);
        }
    }

    public void setOutput(FileEntry output) {
        this.output = output;
    }

    public void setParentId(Long parentId) {
        if (parentId != null) {
            this.parentId = AggregateReference.to(parentId);
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setRetryDelay(String retryDelay) {
        this.retryDelay = retryDelay;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public void setRetryDelayFactor(int retryDelayFactor) {
        this.retryDelayFactor = retryDelayFactor;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public void setStatus(Status status) {
        this.status = status.ordinal();
    }

    public void setTaskNumber(int taskNumber) {
        this.taskNumber = taskNumber;
    }

    public void setWorkflowTask(WorkflowTask workflowTask) {
        this.workflowTask = workflowTask;
    }

    @Override
    public String toString() {
        return "TaskExecution{" + "id="
            + id + ", jobId="
            + getJobId() + ", parentId="
            + getParentId() + ", status="
            + status + ", startDate="
            + startDate + ", endDate="
            + endDate + ", executionTime="
            + executionTime + ", output="
            + output + ", error="
            + error + ", priority="
            + priority + ", progress="
            + progress + ", taskNumber="
            + taskNumber + ", maxRetries="
            + maxRetries + ", retryAttempts="
            + retryAttempts + ", retryDelay='"
            + retryDelay + '\'' + ", retryDelayFactor="
            + retryDelayFactor + ", workflowTask="
            + workflowTask + ", metadata="
            + metadata + ", createdBy='"
            + createdBy + '\'' + ", createdDate="
            + createdDate + ", lastModifiedBy='"
            + lastModifiedBy + '\'' + ", lastModifiedDate="
            + lastModifiedDate + '}';
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {

        private Instant endDate;
        private ExecutionError error;
        private Long id;
        private Long jobId;
        private FileEntry output;
        private Long parentId;
        private int priority;
        private int progress;
        private Map<String, ?> metadata;
        private int maxRetries;
        private int retryAttempts;
        private String retryDelay = "1s";
        private int retryDelayFactor = 2;
        private Instant startDate;
        private Status status = Status.CREATED;
        private int taskNumber = DEFAULT_TASK_NUMBER;
        private WorkflowTask workflowTask;

        private Builder() {
        }

        public Builder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder error(ExecutionError error) {
            this.error = error;
            return this;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder jobId(Long jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder metadata(Map<String, ?> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder output(FileEntry output) {
            this.output = output;
            return this;
        }

        public Builder parentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder progress(int progress) {
            this.progress = progress;
            return this;
        }

        public Builder retryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
            return this;
        }

        public Builder retryDelay(String retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public Builder retryDelayFactor(int retryDelayFactor) {
            this.retryDelayFactor = retryDelayFactor;
            return this;
        }

        public Builder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder taskNumber(int taskNumber) {
            this.taskNumber = taskNumber;
            return this;
        }

        public Builder workflowTask(WorkflowTask workflowTask) {
            Assert.notNull(workflowTask, "'workflowTask' must not be null");

            this.workflowTask = workflowTask;
            return this;
        }

        public TaskExecution build() {
            TaskExecution taskExecution = new TaskExecution();

            taskExecution.setEndDate(endDate);
            taskExecution.setError(error);
            taskExecution.setId(id);
            taskExecution.setJobId(jobId);
            taskExecution.setMaxRetries(maxRetries);
            taskExecution.setMetadata(metadata);
            taskExecution.setOutput(output);
            taskExecution.setParentId(parentId);
            taskExecution.setPriority(priority);
            taskExecution.setProgress(progress);
            taskExecution.setRetryAttempts(retryAttempts);
            taskExecution.setRetryDelay(retryDelay);
            taskExecution.setRetryDelayFactor(retryDelayFactor);
            taskExecution.setStartDate(startDate);
            taskExecution.setStatus(status);
            taskExecution.setTaskNumber(taskNumber);
            taskExecution.setWorkflowTask(workflowTask);

            return taskExecution;
        }
    }
}
