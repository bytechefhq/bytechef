/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.workflow.execution.domain;

import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("trigger_state")
public class TriggerState {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private TriggerStateValue value;

    @Version
    private int version;

    @Column("workflow_execution_id")
    private WorkflowExecutionId workflowExecutionId;

    public TriggerState() {
    }

    public TriggerState(WorkflowExecutionId workflowExecutionId, Object value) {
        this.value = new TriggerStateValue(value, value.getClass());
        this.workflowExecutionId = workflowExecutionId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Object getValue() {
        if (value == null) {
            return null;
        }

        return value.value;
    }

    public int getVersion() {
        return version;
    }

    public WorkflowExecutionId getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TriggerState triggerState = (TriggerState) o;

        return Objects.equals(id, triggerState.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setValue(Object value) {
        this.value = new TriggerStateValue(value, value.getClass());
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkflowExecutionId(WorkflowExecutionId workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    @Override
    public String toString() {
        return "DataStorage{" +
            "id=" + id +
            ", workflowExecutionId='" + workflowExecutionId + '\'' +
            ", value='" + value + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }

    public record TriggerStateValue(Object value, String classname) {
        public TriggerStateValue(Object value, Class<?> classValue) {
            this(value, classValue.getName());
        }
    }
}
