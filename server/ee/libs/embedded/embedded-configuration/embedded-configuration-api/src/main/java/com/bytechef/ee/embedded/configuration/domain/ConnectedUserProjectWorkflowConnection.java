/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

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
 * Per-node connection wiring for a connected user's reference to a shared catalog workflow: the same ProjectWorkflow is
 * referenced by every connected user, but each user supplies their own connection per workflow node.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_project_workflow_connection")
public final class ConnectedUserProjectWorkflowConnection {

    @Id
    private Long id;

    @Column("connected_user_project_workflow_id")
    private AggregateReference<ConnectedUserProjectWorkflow, Long> connectedUserProjectWorkflowId;

    @Column("workflow_node_name")
    private String workflowNodeName;

    @Column("connection_id")
    private long connectionId;

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

    public ConnectedUserProjectWorkflowConnection() {
    }

    @PersistenceCreator
    public ConnectedUserProjectWorkflowConnection(
        Long id, Long connectedUserProjectWorkflowId, String workflowNodeName, long connectionId, int version) {

        this.id = id;
        this.connectedUserProjectWorkflowId = AggregateReference.to(connectedUserProjectWorkflowId);
        this.workflowNodeName = workflowNodeName;
        this.connectionId = connectionId;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConnectedUserProjectWorkflowConnection that = (ConnectedUserProjectWorkflowConnection) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public Long getConnectedUserProjectWorkflowId() {
        return connectedUserProjectWorkflowId == null ? null : connectedUserProjectWorkflowId.getId();
    }

    public void setConnectedUserProjectWorkflowId(long connectedUserProjectWorkflowId) {
        this.connectedUserProjectWorkflowId = AggregateReference.to(connectedUserProjectWorkflowId);
    }

    public String getWorkflowNodeName() {
        return workflowNodeName;
    }

    public void setWorkflowNodeName(String workflowNodeName) {
        this.workflowNodeName = workflowNodeName;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(long connectionId) {
        this.connectionId = connectionId;
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
        return "ConnectedUserProjectWorkflowConnection{" +
            "id=" + id +
            ", connectedUserProjectWorkflowId=" + connectedUserProjectWorkflowId +
            ", workflowNodeName='" + workflowNodeName + '\'' +
            ", connectionId=" + connectionId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
