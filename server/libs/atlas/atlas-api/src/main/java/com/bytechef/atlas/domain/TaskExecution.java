
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
import com.bytechef.atlas.task.Progressable;
import com.bytechef.atlas.task.Retryable;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.evaluator.TaskEvaluator;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.utils.LocalDateTimeUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Duration;
import java.time.LocalDateTime;
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
@Table
public final class TaskExecution
    implements Errorable, Persistable<Long>, Prioritizable, Progressable, Retryable, Task {

    private static final int DEFAULT_TASK_NUMBER = -1;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column("end_time")
    private LocalDateTime endTime;

    @Column("error")
    private ExecutionError error;

    @Column("execution_time")
    private long executionTime = 0;

    @Id
    private Long id;

    @Column("job_id")
    private AggregateReference<Job, Long> jobRef;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Transient
    private Object output;

    @Column("parent_id")
    private AggregateReference<TaskExecution, Long> parentRef;

    @Column
    private int priority;

    @Column
    private int progress;

    @Column
    private int retry;

    @Column("retry_attempts")
    private int retryAttempts;

    @Column("retry_delay")
    private String retryDelay = "1s";

    @Column("retry_delay_factor")
    private int retryDelayFactor = 2;

    @Column("start_time")
    private LocalDateTime startTime;

    @Column
    private TaskStatus status;

    @Column("task_number")
    private int taskNumber = DEFAULT_TASK_NUMBER;

    @Column("workflow_task")
    private WorkflowTask workflowTask = WorkflowTask.EMPTY_WORKFLOW_TASK;

    public TaskExecution() {
    }

    public TaskExecution(long id) {
        this.id = id;
    }

    public TaskExecution(long id, @NonNull WorkflowTask workflowTask) {
        this(workflowTask);

        this.id = id;
        this.workflowTask = workflowTask;
    }

    public TaskExecution(@NonNull WorkflowTask workflowTask) {
        Assert.notNull(workflowTask, "'workflowTask' must not be null.");

        this.workflowTask = workflowTask;
    }

    private TaskExecution(Long jobId, Long parentId, int priority, int taskNumber, WorkflowTask workflowTask) {
        this.jobRef = new AggregateReference.IdOnlyAggregateReference<>(jobId);

        if (parentId != null) {
            this.parentRef = new AggregateReference.IdOnlyAggregateReference<>(parentId);
        }

        this.priority = priority;
        this.status = TaskStatus.CREATED;
        this.taskNumber = taskNumber;
        this.workflowTask = workflowTask;
    }

    public static TaskExecution of(long jobId, @NonNull WorkflowTask workflowTask) {
        Assert.notNull(workflowTask, "'workflowTask' must not be null.");

        return new TaskExecution(jobId, null, 0, DEFAULT_TASK_NUMBER, workflowTask);
    }

    public static TaskExecution of(long jobId, int priority, WorkflowTask workflowTask) {
        Assert.notNull(workflowTask, "'workflowTask' must not be null.");

        return new TaskExecution(jobId, null, priority, DEFAULT_TASK_NUMBER, workflowTask);
    }

    public static TaskExecution of(
        long jobId, long parentId, int priority, @NonNull WorkflowTask workflowTask) {
        Assert.notNull(workflowTask, "'workflowTask' must not be null.");

        return new TaskExecution(jobId, parentId, priority, DEFAULT_TASK_NUMBER, workflowTask);
    }

    public static TaskExecution of(
        long jobId, long parentId, int priority, int taskNumber, @NonNull WorkflowTask workflowTask) {

        Assert.notNull(workflowTask, "'workflowTask' must not be null.");

        return new TaskExecution(jobId, parentId, priority, taskNumber, workflowTask);
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

    public TaskExecution evaluate(TaskEvaluator taskEvaluator, Map<String, Object> context) {
        this.workflowTask = taskEvaluator.evaluate(this.workflowTask, context);

        return this;
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
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * Return the time when this task instance ended (CANCELLED, FAILED, COMPLETED)
     *
     * @return Date
     */
    public LocalDateTime getEndTime() {
        return endTime;
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
    public Long getId() {
        return this.id;
    }

    @JsonIgnore
    public AggregateReference<Job, Long> getJobRef() {
        return jobRef;
    }

    /**
     * Get the id of the job for which this task belongs to.
     *
     * @return String the id of the job
     */
    public Long getJobId() {
        return jobRef == null ? null : jobRef.getId();
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
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
    public Object getOutput() {
        return output;
    }

    @JsonIgnore
    public Map<String, Object> getParameters() {
        return workflowTask.getParameters();
    }

    @JsonIgnore
    public AggregateReference<TaskExecution, Long> getParentRef() {
        return parentRef;
    }

    /**
     * Get the id of the parent task, if this is a sub-task.
     *
     * @return String the id of the parent task.
     */
    public Long getParentId() {
        return parentRef == null ? null : parentRef.getId();
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
    public int getRetry() {
        return retry;
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
        long delay = Duration.parse("PT" + getRetryDelay())
            .toMillis();
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
    public LocalDateTime getStartTime() {
        return startTime;
    }

    /**
     * Get the current status of this task.
     *
     * @return The status of the task.
     */
    public TaskStatus getStatus() {
        return status;
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
    public String getType() {
        Assert.notNull(workflowTask.getType(), "Type must not be null");

        return workflowTask.getType();
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;

        if (endTime != null && startTime != null) {
            this.executionTime = LocalDateTimeUtils.getTime(endTime) - LocalDateTimeUtils.getTime(startTime);
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

    public void setJobRef(AggregateReference<Job, Long> jobRef) {
        this.jobRef = jobRef;
    }

    public void setJobId(Long jobId) {
        if (jobId != null) {
            this.jobRef = new AggregateReference.IdOnlyAggregateReference<>(jobId);
        }
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public void setParentRef(AggregateReference<TaskExecution, Long> parentRef) {
        this.parentRef = parentRef;
    }

    public void setParentId(Long parentId) {
        if (parentId != null) {
            this.parentRef = new AggregateReference.IdOnlyAggregateReference<>(parentId);
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setRetry(int retry) {
        this.retry = retry;
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

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setTaskNumber(int taskNumber) {
        this.taskNumber = taskNumber;
    }

    @Override
    public String toString() {
        return "TaskExecution{" + "createdBy='"
            + createdBy + '\'' + ", createdDate="
            + createdDate + ", endTime="
            + endTime + ", error="
            + error + ", executionTime="
            + executionTime + ", id='"
            + id + '\'' + ", job="
            + jobRef + ", lastModifiedBy='"
            + lastModifiedBy + '\'' + ", lastModifiedDate="
            + lastModifiedDate + ", output="
            + output + ", parent="
            + parentRef + ", priority="
            + priority + ", progress="
            + progress + ", retry="
            + retry + ", retryAttempts="
            + retryAttempts + ", retryDelay='"
            + retryDelay + '\'' + ", retryDelayFactor="
            + retryDelayFactor + ", startTime="
            + startTime + ", status="
            + status + ", taskNumber="
            + taskNumber + ", workflowTask="
            + workflowTask + '}';
    }
}
