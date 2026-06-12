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

package com.bytechef.automation.data.table.configuration.domain;

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
 * Relation entity binding a platform {@code data_table} to a workspace. Keeps workspace scoping out of the platform
 * {@code DataTable} domain (mirrors {@code workspace_knowledge_base}).
 *
 * @author Ivica Cardic
 */
@Table("workspace_data_table")
public class WorkspaceDataTable {

    @Column("data_table_id")
    private Long dataTableId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @Id
    private Long id;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Column("workspace_id")
    private Long workspaceId;

    @Version
    private Long version;

    public WorkspaceDataTable() {
    }

    public WorkspaceDataTable(Long dataTableId, Long workspaceId) {
        this.dataTableId = dataTableId;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WorkspaceDataTable workspaceDataTable)) {
            return false;
        }

        return Objects.equals(id, workspaceDataTable.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getDataTableId() {
        return dataTableId;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
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

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getVersion() {
        return version;
    }

    public void setDataTableId(Long dataTableId) {
        this.dataTableId = dataTableId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "WorkspaceDataTable{" +
            "id=" + id +
            ", dataTableId=" + dataTableId +
            ", workspaceId=" + workspaceId +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
