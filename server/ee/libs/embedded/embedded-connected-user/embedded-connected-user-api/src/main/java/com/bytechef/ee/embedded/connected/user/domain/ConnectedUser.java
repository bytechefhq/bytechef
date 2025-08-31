/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.domain;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Instant;
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
 * @version ee
 *
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
    private Instant createdDate;

    @Column
    private String email;

    @Column
    private boolean enabled;

    @Column
    private int environment;

    @Column("external_id")
    private String externalId;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Version
    private int version;

    public ConnectedUser() {
    }

    @PersistenceCreator
    public ConnectedUser(
        Map<String, ConnectedUserMetadata> connectedUserMetadata, String email, boolean enabled,
        String externalId, Long id, String name, int version) {

        this.connectedUserMetadata.putAll(connectedUserMetadata);

        this.email = email;
        this.enabled = enabled;
        this.externalId = externalId;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getEmail() {
        return email;
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
    }

    public long getEnvironmentId() {
        return environment;
    }

    public String getExternalId() {
        return externalId;
    }

    public Long getId() {
        return id;
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

    public String getName() {
        return name;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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
            ", externalId='" + externalId + '\'' +
            ", email='" + email + '\'' +
            ", enabled='" + enabled + '\'' +
            ", environment='" + environment + '\'' +
            ", connectedUserMetadata=" + connectedUserMetadata +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", name='" + name + '\'' +
            ", version=" + version +
            '}';
    }
}
