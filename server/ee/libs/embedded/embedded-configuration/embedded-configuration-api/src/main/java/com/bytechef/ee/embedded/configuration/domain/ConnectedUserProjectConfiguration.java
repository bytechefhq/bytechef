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
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Assigns a connected user to one automation workflow project configuration of a project.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_project_configuration")
public class ConnectedUserProjectConfiguration {

    @Id
    private Long id;

    @Column("connected_user_id")
    private AggregateReference<ConnectedUser, Long> connectedUserId;

    @Column("project_id")
    private AggregateReference<Project, Long> projectId;

    @Column("automation_workflow_project_configuration_id")
    private AggregateReference<AutomationWorkflowProjectConfiguration, Long> configurationId;

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

    public ConnectedUserProjectConfiguration() {
    }

    public ConnectedUserProjectConfiguration(long connectedUserId, long projectId, long configurationId) {
        this.connectedUserId = AggregateReference.to(connectedUserId);
        this.projectId = AggregateReference.to(projectId);
        this.configurationId = AggregateReference.to(configurationId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ConnectedUserProjectConfiguration that)) {
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

    public Long getConfigurationId() {
        return configurationId.getId();
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

    public void setConfigurationId(long configurationId) {
        this.configurationId = AggregateReference.to(configurationId);
    }

    @Override
    public String toString() {
        return "ConnectedUserProjectConfiguration{" +
            "id=" + id +
            ", connectedUserId=" + connectedUserId +
            ", projectId=" + projectId +
            ", configurationId=" + configurationId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
