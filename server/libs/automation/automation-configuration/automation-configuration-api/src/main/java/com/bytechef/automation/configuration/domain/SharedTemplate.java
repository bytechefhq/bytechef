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

import com.bytechef.file.storage.domain.FileEntry;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("shared_template")
public final class SharedTemplate {

    @Id
    private Long id;

    @Column("uuid")
    private UUID uuid;

    @Column
    private FileEntry template;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Version
    private int version;

    public SharedTemplate() {
    }

    @PersistenceCreator
    public SharedTemplate(Long id, UUID uuid, FileEntry template, int version) {
        this.id = id;
        this.uuid = uuid;
        this.template = template;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SharedTemplate sharedTemplate = (SharedTemplate) o;

        return Objects.equals(id, sharedTemplate.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public FileEntry getTemplate() {
        return template;
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

    public void setId(Long id) {
        this.id = id;
    }

    public void setTemplate(FileEntry template) {
        this.template = template;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "SharedTemplate{" +
            "id=" + id +
            ", uuid='" + uuid + '\'' +
            ", template=" + template +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }

    public static class Builder {
        private Long id;
        private UUID uuid;
        private FileEntry template;
        private int version;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder uuid(UUID uuid) {
            this.uuid = uuid;
            return this;
        }

        public Builder template(FileEntry template) {
            this.template = template;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public SharedTemplate build() {
            SharedTemplate sharedTemplate = new SharedTemplate();

            sharedTemplate.setId(id);
            sharedTemplate.setUuid(uuid);
            sharedTemplate.setTemplate(template);
            sharedTemplate.setVersion(version);

            return sharedTemplate;
        }
    }
}
