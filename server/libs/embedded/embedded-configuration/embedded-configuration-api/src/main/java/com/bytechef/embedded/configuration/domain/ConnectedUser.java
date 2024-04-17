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

package com.bytechef.embedded.configuration.domain;

import com.bytechef.commons.util.MapUtils;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("connected_user")
public class ConnectedUser {

    @MappedCollection(idColumn = "connected_user_id", keyColumn = "key")
    private Map<String, ConnectedUserMetadata> connectedUserMetadata = new HashMap<>();

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private String email;

    @Column
    private boolean enabled;

    @Column
    private String externalReferenceCode;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column
    private String name;

    @Version
    private int version;

    public ConnectedUser() {
    }

    @PersistenceCreator
    public ConnectedUser(
        Map<String, ConnectedUserMetadata> connectedUserMetadata, String email, boolean enabled,
        String externalReferenceCode, Long id, String name, int version) {

        this.connectedUserMetadata.putAll(connectedUserMetadata);

        this.email = email;
        this.enabled = enabled;
        this.externalReferenceCode = externalReferenceCode;
        this.id = id;
        this.name = name;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConnectedUser that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void putMetadata(String key, String value) {
        connectedUserMetadata.put(key, new ConnectedUserMetadata(value));
    }

    public Map<String, String> getMetadata() {
        return connectedUserMetadata.entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> {
                        ConnectedUserMetadata metadata = entry.getValue();

                        return metadata.getValue();
                    }));
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getExternalReferenceCode() {
        return externalReferenceCode;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setExternalReferenceCode(String externalReferenceCode) {
        this.externalReferenceCode = externalReferenceCode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setMetadata(Map<String, String> metadata) {
        connectedUserMetadata = MapUtils.toMap(
            metadata,
            Map.Entry::getKey,
            entry -> new ConnectedUserMetadata(entry.getValue()));
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ConnectedUser{" +
            "id=" + id +
            ", connectedUserMetadata=" + connectedUserMetadata +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", email='" + email + '\'' +
            ", externalReferenceCode='" + externalReferenceCode + '\'' +
            ", enabled='" + enabled + '\'' +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", name='" + name + '\'' +
            ", version=" + version +
            '}';
    }
}
