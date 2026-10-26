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

package com.bytechef.platform.component.log.domain;

import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A row of the {@code component_log_entry} table, which keeps component logs for the {@code jdbc} file storage
 * provider.
 *
 * @author Ivica Cardic
 */
@Table("component_log_entry")
public final class ComponentLogEntry {

    @Id
    private @Nullable Long id;

    @Column("component_name")
    private @Nullable String componentName;

    @Column("component_operation_name")
    private @Nullable String componentOperationName;

    @Column("exception_message")
    private @Nullable String exceptionMessage;

    @Column("exception_type")
    private @Nullable String exceptionType;

    @Column("job_id")
    private long jobId;

    @Column
    private @Nullable Integer level;

    @Column("logged_at")
    private Instant loggedAt;

    @Column
    private @Nullable String message;

    @Column
    private int scope;

    @Column("stack_trace")
    private @Nullable String stackTrace;

    @Column("task_execution_id")
    private long taskExecutionId;

    @Column("trigger_execution_id")
    private @Nullable Long triggerExecutionId;

    public ComponentLogEntry() {
    }

    public ComponentLogEntry(int scope, long jobId, long taskExecutionId, LogEntry logEntry) {
        LogEntry.Level logEntryLevel = logEntry.level();

        this.componentName = logEntry.componentName();
        this.componentOperationName = logEntry.componentOperationName();
        this.exceptionMessage = logEntry.exceptionMessage();
        this.exceptionType = logEntry.exceptionType();
        this.jobId = jobId;
        this.level = logEntryLevel == null ? null : logEntryLevel.ordinal();
        this.loggedAt = logEntry.timestamp();
        this.message = logEntry.message();
        this.scope = scope;
        this.stackTrace = logEntry.stackTrace();
        this.taskExecutionId = taskExecutionId;
        this.triggerExecutionId = logEntry.triggerExecutionId();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        ComponentLogEntry componentLogEntry = (ComponentLogEntry) object;

        return Objects.equals(id, componentLogEntry.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public @Nullable Long getId() {
        return id;
    }

    public long getJobId() {
        return jobId;
    }

    public int getScope() {
        return scope;
    }

    public long getTaskExecutionId() {
        return taskExecutionId;
    }

    public LogEntry toLogEntry() {
        return new LogEntry(
            loggedAt, level == null ? null : LogEntry.Level.values()[level], componentName, componentOperationName,
            taskExecutionId, triggerExecutionId, message, exceptionType, exceptionMessage, stackTrace);
    }

    @Override
    public String toString() {
        return "ComponentLogEntry{" +
            "id=" + id +
            ", scope=" + scope +
            ", jobId=" + jobId +
            ", taskExecutionId=" + taskExecutionId +
            ", loggedAt=" + loggedAt +
            '}';
    }
}
