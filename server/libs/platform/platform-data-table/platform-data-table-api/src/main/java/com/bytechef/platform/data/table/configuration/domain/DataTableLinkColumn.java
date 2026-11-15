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

package com.bytechef.platform.data.table.configuration.domain;

import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import java.time.Instant;
import java.util.Objects;
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
 * @author Ivica Cardic
 */
@Table("data_table_link_column")
public class DataTableLinkColumn {

    @Id
    private Long id;

    @Column("data_table_id")
    private long dataTableId;

    @Column("environment_id")
    private long environmentId;

    @Column("column_name")
    private String columnName;

    @Column("target_data_table_id")
    private long targetDataTableId;

    @Column("on_delete")
    private String onDelete;

    @Column("display_column_name")
    private String displayColumnName;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @Version
    private Long version;

    public DataTableLinkColumn() {
    }

    public DataTableLinkColumn(
        long dataTableId, long environmentId, String columnName, LinkSpec linkSpec) {

        this.dataTableId = dataTableId;
        this.environmentId = environmentId;
        this.columnName = columnName;
        this.targetDataTableId = linkSpec.targetDataTableId();
        this.onDelete = linkSpec.onDelete()
            .name();
        this.displayColumnName = linkSpec.displayColumnName();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DataTableLinkColumn dataTableLinkColumn)) {
            return false;
        }

        return Objects.equals(id, dataTableLinkColumn.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getDataTableId() {
        return dataTableId;
    }

    public void setDataTableId(long dataTableId) {
        this.dataTableId = dataTableId;
    }

    public long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(long environmentId) {
        this.environmentId = environmentId;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public long getTargetDataTableId() {
        return targetDataTableId;
    }

    public void setTargetDataTableId(long targetDataTableId) {
        this.targetDataTableId = targetDataTableId;
    }

    public LinkDeleteRule getOnDelete() {
        return LinkDeleteRule.valueOf(onDelete);
    }

    public void setOnDelete(LinkDeleteRule onDelete) {
        this.onDelete = onDelete.name();
    }

    public @Nullable String getDisplayColumnName() {
        return displayColumnName;
    }

    public void setDisplayColumnName(@Nullable String displayColumnName) {
        this.displayColumnName = displayColumnName;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LinkSpec toLinkSpec() {
        return new LinkSpec(targetDataTableId, getOnDelete(), displayColumnName);
    }

    @Override
    public String toString() {
        return "DataTableLinkColumn{" +
            "id=" + id +
            ", dataTableId=" + dataTableId +
            ", environmentId=" + environmentId +
            ", columnName='" + columnName + '\'' +
            ", targetDataTableId=" + targetDataTableId +
            ", onDelete='" + onDelete + '\'' +
            ", displayColumnName='" + displayColumnName + '\'' +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
