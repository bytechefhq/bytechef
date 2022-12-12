
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
import com.bytechef.atlas.task.execution.TaskStatus;
import com.bytechef.commons.utils.LocalDateTimeUtils;
import com.bytechef.commons.utils.UUIDUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.LocalDateTime;
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
@Table
public final class TaskExecution
    implements Errorable, Persistable<String>, Prioritizable, Progressable, Retryable, Task {

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

//    @Column("workflow_task")
    @Transient
    private WorkflowTask workflowTask = WorkflowTask.EMPTY_WORKFLOW_TASK;

    public TaskExecution() {
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
        this.workflowTask = new WorkflowTask(taskExecution.workflowTask);
    }

    public TaskExecution(TaskExecution taskExecution, WorkflowTask workflowTask) {
        this(taskExecution);

        this.workflowTask = workflowTask;
    }

    public TaskExecution(WorkflowTask workflowTask) {
        Assert.notNull(workflowTask, "workflowTask cannot be null");

        this.workflowTask = new WorkflowTask(workflowTask);
    }

    public TaskExecution(WorkflowTask workflowTask, Map<String, Object> source) {
        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(source, "source cannot be null");

        Map<String, Object> map = new HashMap<>();

        map.putAll(workflowTask.toMap());
        map.putAll(source);

        this.workflowTask = new WorkflowTask(map);
    }

    public TaskExecution(WorkflowTask workflowTask, String jobId) {
        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(jobId, "jobId cannot be null");

        this.id = UUIDUtils.generate();
        this.job = new AggregateReference.IdOnlyAggregateReference<>(jobId);
        this.status = TaskStatus.CREATED;
        this.workflowTask = new WorkflowTask(workflowTask);
    }

    public TaskExecution(WorkflowTask workflowTask, String jobId, int priority) {
        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(jobId, "jobId cannot be null");

        this.id = UUIDUtils.generate();
        this.job = new AggregateReference.IdOnlyAggregateReference<>(jobId);
        this.priority = priority;
        this.status = TaskStatus.CREATED;
        this.workflowTask = new WorkflowTask(workflowTask);
    }

    public TaskExecution(WorkflowTask workflowTask, String jobId, String parentId, int priority) {
        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(jobId, "jobId cannot be null");
        Assert.notNull(parentId, "parentId cannot be null");

        this.id = UUIDUtils.generate();
        this.job = new AggregateReference.IdOnlyAggregateReference<>(jobId);
        this.parent = new AggregateReference.IdOnlyAggregateReference<>(parentId);
        this.priority = priority;
        this.status = TaskStatus.CREATED;
        this.workflowTask = new WorkflowTask(workflowTask);
    }

    public TaskExecution(WorkflowTask workflowTask, String jobId, String parentId, int priority, int taskNumber) {

        Assert.notNull(workflowTask, "workflowTask cannot be null");
        Assert.notNull(jobId, "jobId cannot be null");
        Assert.notNull(parentId, "parentId cannot be null");

        this.id = UUIDUtils.generate();
        this.job = new AggregateReference.IdOnlyAggregateReference<>(jobId);
        this.parent = new AggregateReference.IdOnlyAggregateReference<>(parentId);
        this.priority = priority;
        this.status = TaskStatus.CREATED;
        this.taskNumber = taskNumber;
        this.workflowTask = new WorkflowTask(workflowTask);
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
     * Returns the total time in ms for this task to execute (excluding wait time of the task in transit). i.e. actual
     * execution time on a worker node.
     *
     * @return long
     */
    public long getExecutionTime() {
        return executionTime;
    }

    /**
     * Get the unique id of the task instance.
     *
     * @return String the id of the task execution.
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
    public Map<String, Object> getParameters() {
        return workflowTask.getParameters();
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

    // WorkflowTaskParameter

    // @Override
    // public Map<String, Object> asMap() {
    // return workflowTask.asMap();
    // }
    //
    // @Override
    // public boolean containsKey(String key) {
    // return MapUtils.containsKey(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public Object get(String key) {
    // return MapUtils.get(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public <T> T get(String key, ParameterizedTypeReference<T> returnType) {
    // return MapUtils.get(workflowTask.getParameters(), key, returnType);
    // }
    //
    // @Override
    // public <T> T get(String key, Class<T> returnType, T defaultValue) {
    // return MapUtils.get(workflowTask.getParameters(), key, returnType, defaultValue);
    // }
    //
    // @Override
    // public <T> T get(String key, ParameterizedTypeReference<T> returnType, T defaultValue) {
    // return MapUtils.get(workflowTask.getParameters(), key, returnType, defaultValue);
    // }
    //
    // @Override
    // public <T> T get(String key, Class<T> returnType) {
    // return MapUtils.get(workflowTask.getParameters(), key, returnType);
    // }
    //
    // @Override
    // public <T> T[] getArray(String key, Class<T> elementType) {
    // return MapUtils.getArray(workflowTask.getParameters(), key, elementType);
    // }
    //
    // @Override
    // public Boolean getBoolean(String key) {
    // return MapUtils.getBoolean(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public boolean getBoolean(String key, boolean defaultValue) {
    // return MapUtils.getBoolean(workflowTask.getParameters(), key, defaultValue);
    // }
    //
    // @Override
    // public Date getDate(String key) {
    // return MapUtils.getDate(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public Date getDate(String key, Date defaultValue) {
    // return MapUtils.getDate(workflowTask.getParameters(), key, defaultValue);
    // }
    //
    // @Override
    // public Double getDouble(String key) {
    // return MapUtils.getDouble(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public Double getDouble(String key, double defaultValue) {
    // return MapUtils.getDouble(workflowTask.getParameters(), key, defaultValue);
    // }
    //
    // @Override
    // public Duration getDuration(String key) {
    // return MapUtils.getDuration(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public Duration getDuration(String key, Duration defaultDuration) {
    // return MapUtils.getDuration(workflowTask.getParameters(), key, defaultDuration);
    // }
    //
    // @Override
    // public Float getFloat(String key) {
    // return MapUtils.getFloat(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public float getFloat(String key, float defaultValue) {
    // return MapUtils.getFloat(workflowTask.getParameters(), key, defaultValue);
    // }
    //
    // @Override
    // public Integer getInteger(String key) {
    // return MapUtils.getInteger(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public int getInteger(String key, int defaultValue) {
    // return MapUtils.getInteger(workflowTask.getParameters(), key, defaultValue);
    // }
    //
    // @Override
    // public <T> List<T> getList(String key, Class<T> elementType) {
    // return MapUtils.getList(workflowTask.getParameters(), key, elementType);
    // }
    //
    // @Override
    // public <T> List<T> getList(String key, Class<T> elementType, List<T> defaultValue) {
    // return MapUtils.getList(workflowTask.getParameters(), key, elementType, defaultValue);
    // }
    //
    // @Override
    // public <T> List<T> getList(String key, ParameterizedTypeReference<T> elementType) {
    // return MapUtils.getList(workflowTask.getParameters(), key, elementType);
    // }
    //
    // @Override
    // public <T> List<T> getList(String key, ParameterizedTypeReference<T> elementType, List<T> defaultValue) {
    // return MapUtils.getList(workflowTask.getParameters(), key, elementType, defaultValue);
    // }
    //
    // @Override
    // public LocalDate getLocalDate(String key) {
    // return MapUtils.getLocalDate(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public LocalDate getLocalDate(String key, LocalDate defaultValue) {
    // return MapUtils.getLocalDate(workflowTask.getParameters(), key, defaultValue);
    // }
    //
    // @Override
    // public LocalDateTime getLocalDateTime(String key) {
    // return MapUtils.getLocalDateTime(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue) {
    // return MapUtils.getLocalDateTime(workflowTask.getParameters(), key, defaultValue);
    // }
    //
    // @Override
    // public Long getLong(String key) {
    // return MapUtils.getLong(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public long getLong(String key, long defaultValue) {
    // return MapUtils.getLong(workflowTask.getParameters(), key, defaultValue);
    // }
    //
    // public <V> Map<String, V> getMap(String key) {
    // return MapUtils.getMap(workflowTask.getParameters(), key);
    // }
    //
    // public <V> Map<String, V> getMap(String key, Map<String, V> defaultValue) {
    // return MapUtils.getMap(workflowTask.getParameters(), key, defaultValue);
    // }
    //
    // @Override
    // public <T> T getRequired(String key) {
    // return MapUtils.getRequired(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public <T> T getRequired(String key, Class<T> returnType) {
    // return MapUtils.getRequired(workflowTask.getParameters(), key, returnType);
    // }
    //
    // @Override
    // public Boolean getRequiredBoolean(String key) {
    // return MapUtils.getRequiredBoolean(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public Date getRequiredDate(String key) {
    // return MapUtils.getRequiredDate(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public Double getRequiredDouble(String key) {
    // return MapUtils.getRequiredDouble(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public Float getRequiredFloat(String key) {
    // return MapUtils.getRequiredFloat(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public Integer getRequiredInteger(String key) {
    // return MapUtils.getRequiredInteger(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public LocalDate getRequiredLocalDate(String key) {
    // return MapUtils.getRequiredLocalDate(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public LocalDateTime getRequiredLocalDateTime(String key) {
    // return MapUtils.getRequiredLocalDateTime(workflowTask.getParameters(), key);
    // }
    //
    // public String getRequiredString(String key) {
    // return MapUtils.getRequiredString(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public String getString(String key) {
    // return MapUtils.getString(workflowTask.getParameters(), key);
    // }
    //
    // @Override
    // public String getString(String key, String defaultValue) {
    // return MapUtils.getString(workflowTask.getParameters(), key, defaultValue);
    // }

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
