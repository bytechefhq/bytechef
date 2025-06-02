/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity that connects ConnectedUser and Project.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_project")
public class ConnectedUserProject {

    @Id
    private Long id;

    @Column("connected_user_id")
    private AggregateReference<ConnectedUser, Long> connectedUserId;

    @Column("project_id")
    private AggregateReference<Project, Long> projectId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    public ConnectedUserProject() {
    }

    @PersistenceCreator
    public ConnectedUserProject(Long connectedUserId, Long id, Long projectId, int version) {
        this.connectedUserId = AggregateReference.to(connectedUserId);
        this.id = id;
        this.projectId = AggregateReference.to(projectId);
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConnectedUserProject that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public Long getConnectedUserId() {
        return connectedUserId.getId();
    }

    public Long getProjectId() {
        return projectId.getId();
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

    public void setConnectedUserId(Long connectedUserId) {
        this.connectedUserId = AggregateReference.to(connectedUserId);
    }

    public void setProjectId(Long projectId) {
        this.projectId = AggregateReference.to(projectId);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ConnectedUserProject{" +
            "id=" + id +
            ", connectedUserId=" + connectedUserId +
            ", projectId=" + projectId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
