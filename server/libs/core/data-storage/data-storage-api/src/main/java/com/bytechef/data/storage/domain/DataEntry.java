
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

package com.bytechef.data.storage.domain;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Table("data_entry")
public class DataEntry implements Persistable<Long> {

    @Column("context")
    private String context;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private DataWrapper data;

    @Id
    private Long id;

    @Column("key")
    private String key;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column
    private int scope;

    @Column("scope_id")
    private Long scopeId;

    @Version
    private int version;

    public DataEntry() {
    }

    public DataEntry(String key, int scope, long scopeId, Object data) {
        this.key = key;
        this.scope = scope;
        this.scopeId = scopeId;
        this.data = new DataWrapper(data, data.getClass());
    }

    public String getContext() {
        return context;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Object getData() {
        if (data == null) {
            return null;
        }

        return data.data;
    }

    @Override
    public Long getId() {
        return id;
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

    public int getScope() {
        return scope;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataEntry dataEntry = (DataEntry) o;

        return Objects.equals(id, dataEntry.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setData(Object value) {
        this.data = new DataWrapper(value, value.getClass());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "DataEntry{" +
            "id=" + id +
            ", context='" + context + '\'' +
            ", scope='" + scope + '\'' +
            ", scopeId='" + scopeId + '\'' +
            ", key='" + key + '\'' +
            ", data='" + data + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }

    public record DataWrapper(Object data, String classname) {
        public DataWrapper(Object data, Class<?> dataClass) {
            this(data, dataClass.getName());
        }
    }
}
