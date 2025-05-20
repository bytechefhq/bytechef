/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.platform.connection.domain.Connection;
import java.util.Objects;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("integration_instance_configuration_workflow_connection")
public class IntegrationInstanceConfigurationWorkflowConnection {

    @Column("connection_id")
    private AggregateReference<Connection, Long> connectionId;

    @Column("workflow_connection_key")
    private String workflowConnectionKey;

    @Column("workflow_node_name")
    private String workflowNodeName;

    public IntegrationInstanceConfigurationWorkflowConnection() {
    }

    @Default
    public IntegrationInstanceConfigurationWorkflowConnection(
        Long connectionId, String workflowConnectionKey, String workflowNodeName) {

        this.connectionId = connectionId == null ? null : AggregateReference.to(connectionId);
        this.workflowConnectionKey = workflowConnectionKey;
        this.workflowNodeName = workflowNodeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IntegrationInstanceConfigurationWorkflowConnection that)) {
            return false;
        }

        return Objects.equals(connectionId, that.connectionId) &&
            Objects.equals(workflowConnectionKey, that.workflowConnectionKey) &&
            Objects.equals(workflowNodeName, that.workflowNodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId, workflowConnectionKey, workflowNodeName);
    }

    public Long getConnectionId() {
        return connectionId.getId();
    }

    public String getWorkflowConnectionKey() {
        return workflowConnectionKey;
    }

    public String getWorkflowNodeName() {
        return workflowNodeName;
    }

    @Override
    public String toString() {
        return "IntegrationInstanceConfigurationWorkflowConnection{" +
            "connectionId=" + connectionId +
            ", workflowConnectionKey='" + workflowConnectionKey + '\'' +
            ", workflowNodeName='" + workflowNodeName + '\'' +
            '}';
    }

    /**
     * Used by MapStruct.
     */
    public @interface Default {
    }
}
