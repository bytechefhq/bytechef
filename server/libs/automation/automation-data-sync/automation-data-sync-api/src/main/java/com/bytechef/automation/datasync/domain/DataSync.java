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

package com.bytechef.automation.datasync.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A Data Sync: one source, one destination, an optional field mapping, and a trigger. The rows are the source of truth;
 * the project workflow identified by {@code projectWorkflowUuid} holds the workflow {@code DataSyncWorkflowGenerator}
 * renders from them, and its project is the Data Sync's project — {@code data_sync} keeps no project column of its own.
 *
 * @author Ivica Cardic
 */
@Table("data_sync")
public final class DataSync {

    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$");
    public static final int MAX_TITLE_LENGTH = 255;
    public static final int MAX_DESCRIPTION_LENGTH = 1024;

    /** Persisted as its ordinal; append only. */
    public enum TriggerType {
        MANUAL, SCHEDULE
    }

    @Id
    private Long id;

    @Column("name")
    private String name;

    @Column("title")
    private String title;

    @Column("description")
    private @Nullable String description;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    @Column("project_workflow_uuid")
    private UUID projectWorkflowUuid;

    @Column("uuid")
    private UUID uuid;

    @Column("trigger_type")
    private int triggerType;

    @Column("trigger_parameters")
    private MapWrapper triggerParameters = new MapWrapper();

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    public DataSync() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataSync that = (DataSync) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!NAME_PATTERN.matcher(name)
            .matches()) {

            throw new IllegalArgumentException("name '" + name + "' must match " + NAME_PATTERN.pattern());
        }

        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                "DataSync.title must be at most " + MAX_TITLE_LENGTH + " characters (got " + title.length() + ")");
        }

        this.title = title;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                "DataSync.description must be at most " + MAX_DESCRIPTION_LENGTH + " characters (got "
                    + description.length() + ")");
        }

        this.description = description;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public UUID getProjectWorkflowUuid() {
        return projectWorkflowUuid;
    }

    public void setProjectWorkflowUuid(UUID projectWorkflowUuid) {
        this.projectWorkflowUuid = projectWorkflowUuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public TriggerType getTriggerType() {
        return TriggerType.values()[triggerType];
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType.ordinal();
    }

    public Map<String, ?> getTriggerParameters() {
        return triggerParameters.getMap();
    }

    public void setTriggerParameters(@Nullable Map<String, ?> triggerParameters) {
        this.triggerParameters = triggerParameters == null ? new MapWrapper() : new MapWrapper(triggerParameters);
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

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "DataSync{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", title='" + title + '\'' +
            ", workspaceId=" + workspaceId +
            ", projectWorkflowUuid=" + projectWorkflowUuid +
            ", triggerType=" + getTriggerType() +
            '}';
    }
}
