/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
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
 * Relates a connected user to their reference deployment of an automation workflow project.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_reference_deployment")
public class ConnectedUserReferenceDeployment {

    @Id
    private Long id;

    @Column("connected_user_id")
    private AggregateReference<ConnectedUser, Long> connectedUserId;

    @Column("project_id")
    private AggregateReference<Project, Long> projectId;

    @Column("project_deployment_id")
    private AggregateReference<ProjectDeployment, Long> projectDeploymentId;

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

    public ConnectedUserReferenceDeployment() {
    }

    public ConnectedUserReferenceDeployment(long connectedUserId, long projectId, long projectDeploymentId) {
        this.connectedUserId = AggregateReference.to(connectedUserId);
        this.projectId = AggregateReference.to(projectId);
        this.projectDeploymentId = AggregateReference.to(projectDeploymentId);
    }

    @PersistenceCreator
    public ConnectedUserReferenceDeployment(
        Long connectedUserId, Long id, Long projectDeploymentId, Long projectId, int version) {

        this.connectedUserId = AggregateReference.to(connectedUserId);
        this.id = id;
        this.projectDeploymentId = AggregateReference.to(projectDeploymentId);
        this.projectId = AggregateReference.to(projectId);
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ConnectedUserReferenceDeployment that)) {
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

    public Long getProjectDeploymentId() {
        return projectDeploymentId.getId();
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

    @Override
    public String toString() {
        return "ConnectedUserReferenceDeployment{" +
            "id=" + id +
            ", connectedUserId=" + connectedUserId +
            ", projectId=" + projectId +
            ", projectDeploymentId=" + projectDeploymentId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
