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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("data_table")
public class DataTable {

    @Id
    private Long id;

    private String name;

    private String description;

    @MappedCollection(idColumn = "data_table_id")
    private Set<DataTableTag> dataTableTags = new HashSet<>();

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

    public DataTable() {
    }

    public DataTable(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataTable dataTable)) {
            return false;
        }

        return Objects.equals(id, dataTable.id);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<Long> getTagIds() {
        return dataTableTags
            .stream()
            .map(DataTableTag::getTagId)
            .toList();
    }

    public void setTagIds(List<Long> tagIds) {
        this.dataTableTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (long tagId : tagIds) {
                dataTableTags.add(new DataTableTag(tagId));
            }
        }
    }

    public void setTags(List<Tag> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            setTagIds(List.of());
        } else {
            setTagIds(CollectionUtils.map(tags, Tag::getId));
        }
    }

    @Override
    public String toString() {
        return "DataTable{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", dataTableTags=" + dataTableTags +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
