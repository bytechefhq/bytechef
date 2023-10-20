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
import com.bytechef.atlas.priority.Prioritizable;
import com.bytechef.atlas.task.Progressable;
import com.bytechef.atlas.task.Retryable;
import com.bytechef.atlas.task.Task;
import com.bytechef.atlas.task.WorkflowTask;
import com.bytechef.atlas.task.WorkflowTaskParameters;
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.collection.MapUtils;
import com.bytechef.commons.uuid.UUIDGenerator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
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
 * WRaps the {@link WorkflowTask} instance to add execution semantics to the task.
 *
 * <p>{@link TaskExecution} instances capture the life cycle of a single execution of a task. By single execution is
 * meant that the task goes through the following states:
 *
 * <ol>
 *   <li><code>CREATED</code>
 *   <li><code>STARTED</code>
 *   <li><code>COMPLETED</code> or <code>FAILED</code> or <code>CANCELLED</code>
 * </ol>
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 */
@Table
public final class TaskExecution
        implements WorkflowTaskParameters,
                Errorable,
                Persistable<String>,
                Prioritizable,
                Progressable,
                Retryable,
                Task {

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
    private String id;

    @Transient
    private boolean isNew;

    @Column("job_id")
    private AggregateReference<Job, String> job;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Transient
    private Object output;

    @Column("parent_id")
    private AggregateReference<TaskExecution, String> parent;

    @Column
    private int priority;

    @Column
    private int progress = 0;

    @Column
    private int retry = 0;

    @Column("retry_attempts")
    private int retryAttempts = 0;

    @Column("retry_delay")
    private String retryDelay = "1s";

    @Column("retry_delay_factor")
    private int retryDelayFactor = 2;

    @Column("start_time")
    private LocalDateTime startTime;

    @Column
    private TaskStatus status;

    @Column("task_number")
    private int taskNumber = -1;

    // TODO Add version
    // @Version
    @SuppressFBWarnings("UuF")
    private int version;

    @Column("workflow_task")
    private WorkflowTask workflowTask;

    public TaskExecution() {
        workflowTask = new WorkflowTask();
    }

    public TaskExecution(Map<String, Object> source) {
        Assert.notNull(source, "source cannot be null");

        this.endTime = source.containsKey(WorkflowConstants.END_TIME)
                ? LocalDateTime.from(
                        MapUtils.getDate(source, WorkflowConstants.END_TIME).toInstant())
                : null;
        this.error = MapUtils.get(source, WorkflowConstants.ERROR, ExecutionError.class, null);
        this.executionTime = MapUtils.getLong(source, WorkflowConstants.EXECUTION_TIME, executionTime);
        this.job = source.containsKey(WorkflowConstants.JOB_ID)
                ? new AggregateReference.IdOnlyAggregateReference<>(
                        MapUtils.getString(source, WorkflowConstants.JOB_ID))
                : null;
        this.output = MapUtils.get(source, WorkflowConstants.OUTPUT);
        this.parent = source.containsKey(WorkflowConstants.PARENT_ID)
                ? new AggregateReference.IdOnlyAggregateReference<>(
                        MapUtils.getString(source, WorkflowConstants.PARENT_ID))
                : null;
        this.priority = MapUtils.getInteger(source, WorkflowConstants.PRIORITY, DEFAULT_PRIORITY);
        this.progress = MapUtils.getInteger(source, WorkflowConstants.PROGRESS, progress);
        this.retry = MapUtils.getInteger(source, WorkflowConstants.RETRY, retry);
        this.retryAttempts = MapUtils.getInteger(source, WorkflowConstants.RETRY_ATTEMPTS, retryAttempts);
        this.retryDelay = MapUtils.getString(source, WorkflowConstants.RETRY_DELAY, retryDelay);
        this.retryDelayFactor = MapUtils.getInteger(source, WorkflowConstants.RETRY_DELAY_FACTOR, retryDelayFactor);
        this.startTime = source.containsKey(WorkflowConstants.START_TIME)
                ? LocalDateTime.from(
                        MapUtils.getDate(source, WorkflowConstants.START_TIME).toInstant())
                : null;
        this.status = MapUtils.get(source, WorkflowConstants.STATUS, TaskStatus.class);
        this.taskNumber = MapUtils.getInteger(source, WorkflowConstants.TASK_NUMBER, taskNumber);
        this.workflowTask = new WorkflowTask(source);
    }

    public TaskExecution(TaskExecution taskExecution) {
        Assert.notNull(taskExecution, "taskExecution cannot be null");

        this.createdBy = taskExecution.createdBy;
        this.createdDate = taskExecution.createdDate;
        this.endTime = taskExecution.endTime;
        this.error = taskExecution.error;
        this.executionTime = taskExecution.executionTime;
        this.id = taskExecution.id;
        this.lastModifiedBy = taskExecution.lastModifiedBy;
        this.lastModifiedDate = taskExecution.lastModifiedDate;
        this.job = taskExecution.job;
        this.output = taskExecution.output;
        this.parent = taskExecution.parent;
        this.priority = taskExecution.priority;
        this.progress = taskExecution.progress;
        this.retry = taskExecution.retry;
        this.retryAttempts = taskExecution.retryAttempts;
        this.retryDelay = taskExecution.retryDelay;
        this.retryDelayFactor = taskExecution.retryDelayFactor;
        this.startTime = taskExecution.startTime;
        this.status = taskExecution.status;
        this.taskNumber = taskExecution.taskNumber;
        this.workflowTask = new WorkflowTask(taskExecution.workflowTask.asMap());
    }

    /**
     * Creates a {@link TaskExecution} instance for the given Key-Value pair.
     *
     * @return The new {@link TaskExecution}.
     */
    public static TaskExecution of(String key, Object value) {
        Assert.notNull(key, "key cannot be null");

        return new TaskExecution(Collections.singletonMap(key, value));
    }

    public static TaskExecution of(TaskExecution taskExecution, WorkflowTask workflowTask) {
        taskExecution = new TaskExecution(taskExecution);

        taskExecution.workflowTask = workflowTask;

        return taskExecution;
    }

    public static TaskExecution of(WorkflowTask workflowTask, Map<String, Object> source) {
        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(source, "source cannot be null");

        Map<String, Object> map = new HashMap<>();

        map.putAll(workflowTask.asMap());
        map.putAll(source);

        return new TaskExecution(map);
    }

    public static TaskExecution of(WorkflowTask workflowTask, String jobId) {
        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(jobId, "jobId cannot be null");

        TaskExecution taskExecution = new TaskExecution(workflowTask.asMap());

        taskExecution.setId(UUIDGenerator.generate());
        taskExecution.setJobId(jobId);
        taskExecution.setStatus(TaskStatus.CREATED);

        return taskExecution;
    }

    public static TaskExecution of(WorkflowTask workflowTask, String jobId, int priority) {
        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(jobId, "jobId cannot be null");

        TaskExecution taskExecution = new TaskExecution(workflowTask.asMap());

        taskExecution.setId(UUIDGenerator.generate());
        taskExecution.setJobId(jobId);
        taskExecution.setPriority(priority);
        taskExecution.setStatus(TaskStatus.CREATED);

        return taskExecution;
    }

    public static TaskExecution of(WorkflowTask workflowTask, String jobId, String parentId, int priority) {
        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(jobId, "jobId cannot be null");
        Assert.notNull(parentId, "parentId cannot be null");

        TaskExecution taskExecution = new TaskExecution(workflowTask.asMap());

        taskExecution.setId(UUIDGenerator.generate());
        taskExecution.setJobId(jobId);
        taskExecution.setParentId(parentId);
        taskExecution.setPriority(priority);
        taskExecution.setStatus(TaskStatus.CREATED);

        return taskExecution;
    }

    public static TaskExecution of(
            WorkflowTask workflowTask, String jobId, String parentId, int priority, int taskNumber) {
        TaskExecution taskExecution = new TaskExecution(workflowTask.asMap());

        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(jobId, "jobId cannot be null");
        Assert.notNull(parentId, "parentId cannot be null");

        taskExecution.setId(UUIDGenerator.generate());
        taskExecution.setJobId(jobId);
        taskExecution.setParentId(parentId);
        taskExecution.setPriority(priority);
        taskExecution.setStatus(TaskStatus.CREATED);
        taskExecution.setTaskNumber(taskNumber);

        return taskExecution;
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
     * Returns the total time in ms for this task to execute (excluding wait time of the task in
     * transit). i.e. actual execution time on a worker node.
     *
     * @return long
     */
    public long getExecutionTime() {
        return executionTime;
    }

    /**
     * Get the unique id of the task instance.
     *
     * @return String the id
     */
    public String getId() {
        return this.id;
    }

    @JsonIgnore
    public AggregateReference<Job, String> getJob() {
        return job;
    }

    /**
     * Get the id of the job for which this task belongs to.
     *
     * @return String the id of the job
     */
    public String getJobId() {
        return job == null ? null : job.getId();
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
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
    public AggregateReference<TaskExecution, String> getParent() {
        return parent;
    }

    /**
     * Get the id of the parent task, if this is a sub-task.
     *
     * @return String the id of the parent task.
     */
    public String getParentId() {
        return parent == null ? null : parent.getId();
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
        long delay = Duration.parse("PT" + getRetryDelay()).toMillis();
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

    /**
     * Get the numeric order of the task in the workflow.
     *
     * @return int
     */
    public int getTaskNumber() {
        return taskNumber;
    }

    public WorkflowTask getWorkflowTask() {
        return workflowTask;
    }

    @Override
    public String getType() {
        Assert.notNull(workflowTask.getType(), "Type cannot be null");

        return workflowTask.getType();
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setError(ExecutionError error) {
        this.error = error;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void setJob(AggregateReference<Job, String> job) {
        this.job = job;
    }

    public void setJobId(String jobId) {
        if (jobId != null) {
            this.job = new AggregateReference.IdOnlyAggregateReference<>(jobId);
        }
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public void setOutput(Object output) {
        this.output = output;
    }

    public void setParent(AggregateReference<TaskExecution, String> parent) {
        this.parent = parent;
    }

    public void setParentId(String parentId) {
        if (parentId != null) {
            this.parent = new AggregateReference.IdOnlyAggregateReference<>(parentId);
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

    // Workflow Task

    @JsonIgnore
    public List<WorkflowTask> getFinalize() {
        return workflowTask.getFinalize();
    }

    @JsonIgnore
    public String getName() {
        return workflowTask.getName();
    }

    @JsonIgnore
    public String getNode() {
        return workflowTask.getNode();
    }

    @JsonIgnore
    public List<WorkflowTask> getPre() {
        return workflowTask.getPre();
    }

    @JsonIgnore
    public List<WorkflowTask> getPost() {
        return workflowTask.getPost();
    }

    @JsonIgnore
    public String getTimeout() {
        return workflowTask.getTimeout();
    }

    @JsonIgnore
    public WorkflowTask getWorkflowTask(String key) {
        return workflowTask.getWorkflowTask(key);
    }

    @JsonIgnore
    public Map<String, Object> getWorkflowTaskParameters() {
        return workflowTask.getParameters();
    }

    @JsonIgnore
    public List<WorkflowTask> getWorkflowTasks(String key) {
        return workflowTask.getWorkflowTasks(key);
    }

    // WorkflowTaskParameters

    @Override
    public Map<String, Object> asMap() {
        return workflowTask.asMap();
    }

    @Override
    public boolean containsKey(String key) {
        return workflowTask.containsKey(key);
    }

    @Override
    public Object get(String key) {
        return workflowTask.get(key);
    }

    @Override
    public <T> T get(String key, Class<T> returnType, T defaultValue) {
        return workflowTask.get(key, returnType, defaultValue);
    }

    @Override
    public <T> T get(String key, Class<T> returnType) {
        return workflowTask.get(key, returnType);
    }

    @Override
    public <T> T[] getArray(String key, Class<T> elementType) {
        return workflowTask.getArray(key, elementType);
    }

    @Override
    public Boolean getBoolean(String key) {
        return workflowTask.getBoolean(key);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return workflowTask.getBoolean(key, defaultValue);
    }

    @Override
    public Date getDate(String key) {
        return workflowTask.getDate(key);
    }

    @Override
    public Double getDouble(String key) {
        return workflowTask.getDouble(key);
    }

    @Override
    public Double getDouble(String key, double defaultValue) {
        return workflowTask.getDouble(key, defaultValue);
    }

    @Override
    public Duration getDuration(String key) {
        return workflowTask.getDuration(key);
    }

    @Override
    public Duration getDuration(String key, String defaultDuration) {
        return workflowTask.getDuration(key, defaultDuration);
    }

    @Override
    public Float getFloat(String key) {
        return workflowTask.getFloat(key);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return workflowTask.getFloat(key, defaultValue);
    }

    @Override
    public Integer getInteger(String key) {
        return workflowTask.getInteger(key);
    }

    @Override
    public int getInteger(String key, int defaultValue) {
        return workflowTask.getInteger(key, defaultValue);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType) {
        return workflowTask.getList(key, elementType);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue) {
        return workflowTask.getList(key, elementType, defaultValue);
    }

    @Override
    public Long getLong(String key) {
        return workflowTask.getLong(key);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return workflowTask.getLong(key, defaultValue);
    }

    public Map<String, Object> getMap(String key) {
        return workflowTask.getMap(key);
    }

    public Map<String, Object> getMap(String key, Map<String, Object> defaultValue) {
        return workflowTask.getMap(key, defaultValue);
    }

    @Override
    public <T> T getRequired(String key) {
        return workflowTask.getRequired(key);
    }

    @Override
    public <T> T getRequired(String key, Class<T> returnType) {
        return workflowTask.getRequired(key, returnType);
    }

    public String getRequiredString(String key) {
        return workflowTask.getRequiredString(key);
    }

    @Override
    public String getString(String key) {
        return workflowTask.getString(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return workflowTask.getString(key, defaultValue);
    }

    @Override
    public String toString() {
        return "TaskExecution{" + "createdBy='"
                + createdBy + '\'' + ", createdDate="
                + createdDate + ", endTime="
                + endTime + ", error="
                + error + ", executionTime="
                + executionTime + ", id='"
                + id + '\'' + ", isNew="
                + isNew + ", job="
                + job + ", lastModifiedBy='"
                + lastModifiedBy + '\'' + ", lastModifiedDate="
                + lastModifiedDate + ", output="
                + output + ", parent="
                + parent + ", priority="
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
