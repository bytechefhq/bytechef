
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

import com.bytechef.hermes.component.Context;
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
@Table("data_storage")
public class DataStorage implements Persistable<Long> {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

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

    @Column
    private DataStorageValue value;

    @Version
    private int version;

    public DataStorage() {
    }

    public DataStorage(String key, int scope, long scopeId, Object value) {
        this.key = key;
        this.scope = scope;
        this.scopeId = scopeId;
        this.value = new DataStorageValue(value, value.getClass());
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

    public String getKey() {
        return key;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Context.DataStorageScope getScope() {
        return Context.DataStorageScope.valueOf(scope);
    }

    public Long getScopeId() {
        return scopeId;
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

    public void setKey(String key) {
        this.key = key;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public void setScope(Context.DataStorageScope scope) {
        this.scope = scope.getId();
    }

    public void setValue(Object value) {
        this.value = new DataStorageValue(value, value.getClass());
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "DataStorage{" +
            "id=" + id +
            ", scope='" + scope + '\'' +
            ", scopeId='" + scopeId + '\'' +
            ", key='" + key + '\'' +
            ", value='" + value + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }

    public record DataStorageValue(Object value, String classname) {
        public DataStorageValue(Object value, Class<?> classValue) {
            this(value, classValue.getName());
        }
    }
}
