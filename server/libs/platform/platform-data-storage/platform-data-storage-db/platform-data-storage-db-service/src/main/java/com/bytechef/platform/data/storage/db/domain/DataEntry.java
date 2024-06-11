/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.data.storage.db.domain;

import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.platform.constant.AppType;
import java.time.LocalDateTime;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
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
@Table("data_entry")
public class DataEntry {

    @Column("component_name")
    private String componentName;

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
    private Scope scope;

    @Column("scope_id")
    private String scopeId;

    @Column
    private ValueWrapper value;

    @Column
    private int type;

    @Version
    private int version;

    public DataEntry() {
    }

    public DataEntry(
        String componentName, Scope scope, String scopeId, String key, Object value, AppType type) {

        this.componentName = componentName;
        this.key = key;
        this.scope = scope;
        this.scopeId = scopeId;
        this.type = type.ordinal();
        this.value = new ValueWrapper(value, value.getClass());
    }

    public String getComponentName() {
        return componentName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

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

    public Scope getScope() {
        return scope;
    }

    public String getScopeId() {
        return scopeId;
    }

    public AppType getType() {
        return AppType.values()[type];
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

        DataEntry dataEntry = (DataEntry) o;

        return Objects.equals(id, dataEntry.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void setValue(Object value) {
        Validate.notNull(value, "'value' must not be null");

        this.value = new ValueWrapper(value, value.getClass());
    }

    @Override
    public String toString() {
        return "DataEntry{" +
            "id=" + id +
            ", componentName='" + componentName + '\'' +
            ", scope=" + scope +
            ", scopeId='" + scopeId + '\'' +
            ", key='" + key + '\'' +
            ", value=" + value +
            ", type=" + type +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }

    public record ValueWrapper(Object value, String classname) {
        public ValueWrapper(Object value, Class<?> valueClass) {
            this(value, valueClass.getName());
        }
    }
}
