
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.error.Errorable;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.hermes.trigger.Trigger;
import com.bytechef.hermes.trigger.WorkflowTrigger;
import com.bytechef.hermes.workflow.WorkflowExecutionId;
import com.bytechef.message.Prioritizable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Table("trigger_execution")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TriggerExecution implements Cloneable, Errorable, Persistable<Long>, Prioritizable, Trigger {

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

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Transient
    private Map<String, Object> metadata = new HashMap<>();

    @Column
    private MapWrapper output;

    @Column
    private int priority;

    @Column("start_date")
    private LocalDateTime startDate;

    @Column
    private Status status;

    @Column
    private WorkflowExecutionId workflowExecutionId;

    @Column("workflow_trigger")
    private WorkflowTrigger workflowTrigger;

    public TriggerExecution() {
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Evaluate the {@link WorkflowTrigger}
     *
     * @param context The context value to evaluate the task against
     * @return the evaluated {@link TriggerExecution} instance.
     */
    public TriggerExecution evaluate(Map<String, Object> context) {
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

    /**
     * Get the unique id of the task instance.
     *
     * @return String the id of the task execution.
     */
    @Override
    public Long getId() {
        return this.id;
    }

    public Map<String, Object> getMetadata() {
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
    public Object getOutput() {
        Object outputValue = null;

        if (output != null) {
            Map<String, Object> map = output.getMap();

            outputValue = map.get("output");
        }

        return outputValue;
    }

    @JsonIgnore
    public Map<String, ?> getParameters() {
        return workflowTrigger.getParameters();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Get the time when this task instance was started.
     *
     * @return Date
     */
    public LocalDateTime getStartDate() {
        return startDate;
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
        Assert.notNull(workflowTrigger.getType(), "Type must not be null");

        return workflowTrigger.getType();
    }

    @SuppressFBWarnings("EI")
    public WorkflowExecutionId getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public WorkflowTrigger getWorkflowTrigger() {
        return workflowTrigger;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public TriggerExecution putMetadata(String key, Object value) {
        metadata.put(key, value);

        return this;
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

    public void setOutput(Object output) {
        if (output != null) {
            this.output = new MapWrapper(Map.of("output", output));
        }
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
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
        return "TaskExecution{" + "id="
            + id + ", jobId="
            + status + ", startDate="
            + startDate + ", endDate="
            + endDate + ", executionTime="
            + executionTime + ", output="
            + output + ", error="
            + error + ", priority="
            + priority + ", workflowExecutionId="
            + workflowExecutionId + ", workflowTrigger="
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
        private Object output;
        private int priority;
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

        public Builder output(Object output) {
            this.output = output;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
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
            Assert.notNull(workflowExecutionId, "'workflowExecutionId' must not be null");

            this.workflowExecutionId = workflowExecutionId;
            return this;
        }

        public Builder workflowTrigger(WorkflowTrigger workflowTrigger) {
            Assert.notNull(workflowTrigger, "'workflowTrigger' must not be null");

            this.workflowTrigger = workflowTrigger;
            return this;
        }

        public TriggerExecution build() {
            TriggerExecution triggerExecution = new TriggerExecution();

            triggerExecution.setEndDate(endDate);
            triggerExecution.setError(error);
            triggerExecution.setId(id);
            triggerExecution.setOutput(output);
            triggerExecution.setPriority(priority);
            triggerExecution.setStartDate(startDate);
            triggerExecution.setStatus(status);
            triggerExecution.setWorkflowExecutionId(workflowExecutionId);
            triggerExecution.setWorkflowTrigger(workflowTrigger);

            return triggerExecution;
        }
    }
}
