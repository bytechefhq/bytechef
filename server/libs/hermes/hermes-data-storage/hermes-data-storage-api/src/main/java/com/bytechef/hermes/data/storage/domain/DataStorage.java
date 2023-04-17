
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

package com.bytechef.hermes.data.storage.domain;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public class DataStorage implements Persistable<Long> {

    public enum Scope {
        CURRENT_EXECUTION("Current Execution"),
        WORKFLOW_INSTANCE("Workflow instance"),
        WORKFLOW("Workflow"),
        ACCOUNT("Account");

        private final String label;

        Scope(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private Long id;

    @Column("reference_id")
    private String key;

    @Column("instance_id")
    private Long instanceId;

    @Column("job_id")
    private Long jobId;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column
    private Scope scope;

    @Version
    private String value;

    @Version
    private int version;

    @Column("workflow_id")
    private String workflowId;

    public DataStorage() {
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public Long getId() {
        return id;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getKey() {
        return key;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Scope getScope() {
        return scope;
    }

    public String getValue() {
        return value;
    }

    public int getVersion() {
        return version;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataStorage dataStorage = (DataStorage) o;

        return Objects.equals(id, dataStorage.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String toString() {
        return "DataStorage{" +
            "id=" + id +
            ", workflowId='" + workflowId + '\'' +
            ", instanceId='" + instanceId + '\'' +
            ", jobId='" + jobId + '\'' +
            ", key='" + key + '\'' +
            ", scope='" + scope + '\'' +
            ", value='" + value + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
