
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
        ACCOUNT(4, "Account"),
        CURRENT_EXECUTION(1, "Current Execution"),
        WORKFLOW(3, "Workflow"),
        WORKFLOW_INSTANCE(2, "Workflow instance");

        private final int id;
        private final String label;

        Scope(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public static Scope valueOf(int id) {
            return switch (id) {
                case 1 -> Scope.CURRENT_EXECUTION;
                case 2 -> Scope.WORKFLOW_INSTANCE;
                case 3 -> Scope.WORKFLOW;
                case 4 -> Scope.ACCOUNT;
                default -> throw new IllegalStateException("Unexpected value: %s".formatted(id));
            };
        }

        public int getId() {
            return id;
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
    private Object value;

    @Version
    private int version;

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
        return Scope.valueOf(scope);
    }

    public Long getScopeId() {
        return scopeId;
    }

    public Object getValue() {
        return value;
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

    public void setScope(Scope scope) {
        this.scope = scope.getId();
    }

    public void setValue(Object value) {
        this.value = value;
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
}
