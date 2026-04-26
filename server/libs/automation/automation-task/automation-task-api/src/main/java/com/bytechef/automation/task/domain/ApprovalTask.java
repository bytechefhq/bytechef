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

package com.bytechef.automation.task.domain;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table
public final class ApprovalTask {

    public enum Status {
        OPEN, IN_PROGRESS, COMPLETED
    }

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    @Id
    private Long id;

    @Column
    private String name;

    @Column
    private String description;

    @Column("job_resume_id")
    private String jobResumeId;

    @Column
    private int status;

    @Column
    private int priority;

    @Column("assignee_id")
    private Long assigneeId;

    @Column("due_date")
    private Instant dueDate;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Version
    private int version;

    public ApprovalTask() {
    }

    @PersistenceCreator
    public ApprovalTask(
        Long id, String name, String description, String jobResumeId, int status, int priority, Long assigneeId,
        Instant dueDate, int version) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.jobResumeId = jobResumeId;
        this.status = status;
        this.priority = priority;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ApprovalTask approvalTask = (ApprovalTask) o;

        return Objects.equals(id, approvalTask.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getJobResumeId() {
        return jobResumeId;
    }

    public Status getStatus() {
        return Status.values()[status];
    }

    public Priority getPriority() {
        return Priority.values()[priority];
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setJobResumeId(String jobResumeId) {
        this.jobResumeId = jobResumeId;
    }

    public void setStatus(Status status) {
        this.status = status == null ? Status.OPEN.ordinal() : status.ordinal();
    }

    public void setPriority(Priority priority) {
        this.priority = priority == null ? Priority.MEDIUM.ordinal() : priority.ordinal();
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ApprovalTask{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", jobResumeId='" + jobResumeId + '\'' +
            ", status=" + status +
            ", priority=" + priority +
            ", assigneeId=" + assigneeId +
            ", dueDate=" + dueDate +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }

    /**
     * Builder for {@link ApprovalTask}.
     */
    public static class Builder {

        private Long id;
        private String name;
        private String description;
        private String jobResumeId;
        private Status status;
        private Priority priority;
        private Long assigneeId;
        private Instant dueDate;
        private int version;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;

            return this;
        }

        public Builder name(String name) {
            this.name = name;

            return this;
        }

        public Builder description(String description) {
            this.description = description;

            return this;
        }

        public Builder jobResumeId(String jobResumeId) {
            this.jobResumeId = jobResumeId;

            return this;
        }

        public Builder status(Status status) {
            this.status = status;

            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;

            return this;
        }

        public Builder assigneeId(Long assigneeId) {
            this.assigneeId = assigneeId;

            return this;
        }

        public Builder dueDate(Instant dueDate) {
            this.dueDate = dueDate;

            return this;
        }

        public Builder version(int version) {
            this.version = version;

            return this;
        }

        public ApprovalTask build() {
            ApprovalTask approvalTask = new ApprovalTask();

            approvalTask.id = this.id;
            approvalTask.name = this.name;
            approvalTask.description = this.description;
            approvalTask.jobResumeId = jobResumeId;
            approvalTask.status = status == null ? Status.OPEN.ordinal() : status.ordinal();
            approvalTask.priority = priority == null ? Priority.MEDIUM.ordinal() : priority.ordinal();
            approvalTask.assigneeId = assigneeId;
            approvalTask.dueDate = dueDate;
            approvalTask.version = this.version;

            return approvalTask;
        }
    }
}
