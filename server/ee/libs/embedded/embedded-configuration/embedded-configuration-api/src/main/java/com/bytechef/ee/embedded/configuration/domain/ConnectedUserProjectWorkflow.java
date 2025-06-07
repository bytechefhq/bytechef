/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity that connects ConnectedUserProject and Workflow.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_project_workflow")
public class ConnectedUserProjectWorkflow {

    @Id
    private Long id;

    @Column("connected_user_project_id")
    private AggregateReference<ConnectedUserProject, Long> connectedUserProjectId;

    @Column("project_workflow_id")
    private AggregateReference<ProjectWorkflow, Long> projectWorkflowId;

    @Column("workflow_version")
    private Integer workflowVersion;

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

    @MappedCollection(idColumn = "connected_user_project_workflow_id")
    private Set<ConnectedUserProjectWorkflowConnection> connections = new HashSet<>();

    public ConnectedUserProjectWorkflow() {
    }

    @PersistenceCreator
    public ConnectedUserProjectWorkflow(
        Long id, Long connectedUserProjectId, Long projectWorkflowId, Integer workflowVersion, int version) {

        this.id = id;
        this.connectedUserProjectId = AggregateReference.to(connectedUserProjectId);
        this.projectWorkflowId = AggregateReference.to(projectWorkflowId);
        this.workflowVersion = workflowVersion;
        this.version = version;
    }

    public void addConnection(long connectionId) {
        connections.add(new ConnectedUserProjectWorkflowConnection(connectionId));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConnectedUserProjectWorkflow that)) {
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

    public Long getConnectedUserProjectId() {
        return connectedUserProjectId.getId();
    }

    public Long getProjectWorkflowId() {
        return projectWorkflowId.getId();
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

    public Integer getWorkflowVersion() {
        return workflowVersion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConnectedUserProjectId(Long connectedUserProjectId) {
        this.connectedUserProjectId = AggregateReference.to(connectedUserProjectId);
    }

    public void setProjectWorkflowId(Long projectWorkflowId) {
        this.projectWorkflowId = AggregateReference.to(projectWorkflowId);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkflowVersion(Integer workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    public List<ConnectedUserProjectWorkflowConnection> getConnections() {
        return List.copyOf(connections);
    }

    public int getConnectionsCount() {
        return connections.size();
    }

    public void setConnections(List<ConnectedUserProjectWorkflowConnection> connections) {
        if (connections != null) {
            this.connections = new HashSet<>(connections);
        }
    }

    @Override
    public String toString() {
        return "ConnectedUserProjectWorkflow{" +
            "id=" + id +
            ", connectedUserProjectId=" + connectedUserProjectId +
            ", projectWorkflowId=" + projectWorkflowId +
            ", workflowVersion=" + workflowVersion +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            ", connections=" + connections +
            '}';
    }
}
