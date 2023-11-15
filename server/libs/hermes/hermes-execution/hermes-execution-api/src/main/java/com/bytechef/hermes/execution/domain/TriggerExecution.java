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

package com.bytechef.hermes.execution.domain;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.error.Errorable;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.hermes.configuration.trigger.Trigger;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import com.bytechef.hermes.execution.WorkflowExecutionId;
import com.bytechef.message.Prioritizable;
import com.bytechef.message.Retryable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("trigger_execution")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TriggerExecution implements Cloneable, Errorable, Persistable<Long>, Prioritizable, Retryable, Trigger {

    public enum Status {
        CREATED(false),
        STARTED(false),
        FAILED(true),
        CANCELLED(true),
        COMPLETED(true);

        private final boolean terminated;

        Status(boolean terminated) {
            this.terminated = terminated;
        }

        public boolean isTerminated() {
            return terminated;
        }
    }

    @Transient
    private boolean batch;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column("end_date")
    private LocalDateTime endDate;

    @Column("error")
    private ExecutionError error;

    @Column("execution_time")
    private long executionTime;

    @Id
    private Long id;

    // TODO
    @Column
    private int maxRetries;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Transient
    private Map<String, Object> metadata = new HashMap<>();

    @Column
    private FileEntry output;

    @Column
    private int priority;

    // TODO
    @Column("retry_attempts")
    private int retryAttempts;

    // TODO
    @Column("retry_delay")
    private String retryDelay;

    // TODO
    @Column("retry_delay_factor")
    private int retryDelayFactor;

    @Column("start_date")
    private LocalDateTime startDate;

    @Transient
    private Object state;

    @Column
    private Status status;

    @MappedCollection(idColumn = "trigger_execution_id")
    private Set<TriggerExecutionJob> triggerExecutionJobs = new HashSet<>();

    @Column
    private WorkflowExecutionId workflowExecutionId;

    @Column("workflow_trigger")
    private WorkflowTrigger workflowTrigger;

    public TriggerExecution() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public void addJobId(long jobId) {
        triggerExecutionJobs.add(new TriggerExecutionJob(jobId));
    }

    /**
     * Evaluate the {@link WorkflowTrigger}
     *
     * @param context The context value to evaluate the task against
     * @return the evaluated {@link TriggerExecution} instance.
     */
    public TriggerExecution evaluate(Map<String, ?> context) {
        WorkflowTrigger workflowTrigger = getWorkflowTrigger();

        Map<String, Object> map = Evaluator.evaluate(workflowTrigger.toMap(), context);

        setWorkflowTrigger(WorkflowTrigger.of(map));

        return this;
    }

    @Override
    public TriggerExecution clone() throws CloneNotSupportedException {
        return (TriggerExecution) super.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TriggerExecution triggerExecution = (TriggerExecution) o;

        return Objects.equals(id, triggerExecution.id);
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
    public LocalDateTime getEndDate() {
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
    public long getInstanceId() {
        return workflowExecutionId.getInstanceId();
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

    public List<Long> getJobIds() {
        return triggerExecutionJobs
            .stream()
            .map(TriggerExecutionJob::getJobId)
            .toList();
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Map<String, ?> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @JsonIgnore
    public String getName() {
        return workflowTrigger.getName();
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
        return workflowTrigger.getParameters();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

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

    public int getRetryDelayFactor() {
        return retryDelayFactor;
    }

    /**
     * Get the time when this task instance was started.
     *
     * @return Date
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    public Object getState() {
        return state;
    }

    /**
     * Get the current status of this task.
     *
     * @return The status of the task.
     */
    public Status getStatus() {
        return status;
    }

    @JsonIgnore
    public String getTimeout() {
        return workflowTrigger.getTimeout();
    }

    @Override
    @JsonIgnore
    public String getType() {
        Validate.notNull(workflowTrigger.getType(), "Type must not be null");

        return workflowTrigger.getType();
    }

    @SuppressFBWarnings("EI")
    public WorkflowExecutionId getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    @JsonIgnore
    public String getWorkflowId() {
        return workflowExecutionId.getWorkflowId();
    }

    public WorkflowTrigger getWorkflowTrigger() {
        return workflowTrigger;
    }

    public boolean isBatch() {
        return batch;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public TriggerExecution putMetadata(String key, Object value) {
        metadata.put(key, value);

        return this;
    }

    public void setBatch(boolean batch) {
        this.batch = batch;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;

        if (endDate != null && startDate != null) {
            this.executionTime = LocalDateTimeUtils.getTime(endDate) - LocalDateTimeUtils.getTime(startDate);
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

    public void setJobIds(List<Long> jobIds) {
        this.triggerExecutionJobs = new HashSet<>();

        if (!CollectionUtils.isEmpty(jobIds)) {
            for (Long tagId : jobIds) {
                triggerExecutionJobs.add(new TriggerExecutionJob(tagId));
            }
        }
    }

    public void setJobs(List<Job> jobs) {
        if (!CollectionUtils.isEmpty(jobs)) {
            setJobIds(CollectionUtils.map(jobs, Job::getId));
        }
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    private void setMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            this.metadata = new HashMap<>();
        } else {
            this.metadata = new HashMap<>(metadata);
        }
    }

    public void setOutput(FileEntry output) {
        this.output = output;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public void setRetryDelay(String retryDelay) {
        this.retryDelay = retryDelay;
    }

    public void setRetryDelayFactor(int retryDelayFactor) {
        this.retryDelayFactor = retryDelayFactor;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public void setState(Object state) {
        this.state = state;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setWorkflowExecutionId(WorkflowExecutionId workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public void setWorkflowTrigger(WorkflowTrigger workflowTrigger) {
        this.workflowTrigger = workflowTrigger;
    }

    @Override
    public String toString() {
        return "TriggerExecution{" + "id="
            + id + ", status="
            + status + ", startDate="
            + startDate + ", endDate="
            + endDate + ", executionTime="
            + executionTime + ", output="
            + output + ", error="
            + error + ", priority="
            + priority + ", workflowExecutionId="
            + workflowExecutionId + ", maxRetries="
            + maxRetries + ", retryAttempts="
            + retryAttempts + ", retryDelay='"
            + retryDelay + '\'' + ", retryDelayFactor="
            + retryDelayFactor + ", workflowTrigger="
            + workflowTrigger + ", metadata="
            + metadata + ", createdBy='"
            + createdBy + '\'' + ", createdDate="
            + createdDate + ", lastModifiedBy='"
            + lastModifiedBy + '\'' + ", lastModifiedDate="
            + lastModifiedDate + '}';
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private LocalDateTime endDate;
        private ExecutionError error;
        private Long id;
        private int maxRetries;
        private Map<String, Object> metadata;
        private FileEntry output;
        private int priority;
        private int retryAttempts;
        private String retryDelay = "1s";
        private int retryDelayFactor = 2;
        private LocalDateTime startDate;
        private Status status = Status.CREATED;
        private WorkflowExecutionId workflowExecutionId;
        private WorkflowTrigger workflowTrigger;

        private Builder() {
        }

        public Builder endDate(LocalDateTime endDate) {
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

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;

            return this;
        }

        public Builder output(FileEntry output) {
            this.output = output;

            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;

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

        public Builder startDate(LocalDateTime startDate) {
            this.startDate = startDate;

            return this;
        }

        public Builder status(Status status) {
            this.status = status;

            return this;
        }

        public Builder workflowExecutionId(WorkflowExecutionId workflowExecutionId) {
            Validate.notNull(workflowExecutionId, "'workflowExecutionId' must not be null");

            this.workflowExecutionId = workflowExecutionId;

            return this;
        }

        public Builder workflowTrigger(WorkflowTrigger workflowTrigger) {
            Validate.notNull(workflowTrigger, "'workflowTrigger' must not be null");

            this.workflowTrigger = workflowTrigger;

            return this;
        }

        public TriggerExecution build() {
            TriggerExecution triggerExecution = new TriggerExecution();

            triggerExecution.setEndDate(endDate);
            triggerExecution.setError(error);
            triggerExecution.setId(id);
            triggerExecution.setMaxRetries(maxRetries);
            triggerExecution.setMetadata(metadata);
            triggerExecution.setOutput(output);
            triggerExecution.setPriority(priority);
            triggerExecution.setRetryAttempts(retryAttempts);
            triggerExecution.setRetryDelay(retryDelay);
            triggerExecution.setRetryDelayFactor(retryDelayFactor);
            triggerExecution.setStartDate(startDate);
            triggerExecution.setStatus(status);
            triggerExecution.setWorkflowExecutionId(workflowExecutionId);
            triggerExecution.setWorkflowTrigger(workflowTrigger);

            return triggerExecution;
        }
    }
}
