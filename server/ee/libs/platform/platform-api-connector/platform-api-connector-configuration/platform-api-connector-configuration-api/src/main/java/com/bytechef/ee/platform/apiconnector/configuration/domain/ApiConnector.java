/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.domain;

import com.bytechef.file.storage.domain.FileEntry;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("api_connector")
public class ApiConnector {

    @Column("connector_version")
    private int connectorVersion;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private FileEntry definition;

    @Column
    private String description;

    @Column
    private boolean enabled;

    @Column
    private String icon;

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

    @Column
    private FileEntry specification;

    @Column
    private String title;

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ApiConnector apiConnector)) {
            return false;
        }

        return Objects.equals(id, apiConnector.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public int getConnectorVersion() {
        return connectorVersion;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public FileEntry getDefinition() {
        return definition;
    }

    public String getDescription() {
        return description;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public String getIcon() {
        return icon;
    }

    public Long getId() {
        return id;
    }

    public FileEntry getSpecification() {
        return specification;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public int getVersion() {
        return version;
    }

    public void setConnectorVersion(int version) {
        this.connectorVersion = version;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDefinition(FileEntry definition) {
        this.definition = definition;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSpecification(FileEntry specification) {
        this.specification = specification;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ApiConnector{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", connectorVersion=" + connectorVersion +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", icon='" + icon + '\'' +
            ", specification='" + specification + '\'' +
            ", definition='" + definition + '\'' +
            ", enabled=" + enabled +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
