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

package com.bytechef.automation.configuration.domain;

import java.time.Instant;
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
@Table("project_connection")
public class ProjectConnection {

    @Column("connection_id")
    private Long connectionId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("project_id")
    private Long projectId;

    @Version
    private int version;

    private ProjectConnection() {
    }

    public ProjectConnection(Long connectionId, Long projectId) {
        this.connectionId = Validate.notNull(connectionId, "'connectionId' must not be null");
        this.projectId = Validate.notNull(projectId, "'projectId' must not be null");
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
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

    public Long getProjectId() {
        return projectId;
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

        ProjectConnection projectConnection = (ProjectConnection) o;

        // Transient (unsaved) entities compare equal only if they are the same reference. Without
        // this guard Objects.equals(null, null) == true would collapse every unsaved instance into
        // a single element inside a HashSet/HashMap — a classic JPA entity pitfall where callers
        // that stage rows pre-flush accidentally lose siblings.
        if (id == null) {
            return false;
        }

        return id.equals(projectConnection.id);
    }

    @Override
    public int hashCode() {
        // Identity hash for transient entities keeps hashCode consistent with equals: two
        // different unsaved instances must not collide by virtue of both having null id. After
        // persistence, the id-based hash takes over — Spring Data JDBC flushes id assignment
        // within the save() call, so the window where the hash changes is bounded.
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }

    @Override
    public String toString() {
        return "ProjectConnection{" +
            "id=" + id +
            ", connectionId=" + connectionId +
            ", projectId=" + projectId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
