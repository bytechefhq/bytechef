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
 * Entity that represents a connection for a ConnectedUserProjectWorkflow.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_project_workflow_connection")
public class ConnectedUserProjectWorkflowConnection {

    @Column("connection_id")
    private AggregateReference<Connection, Long> connectionId;

    public ConnectedUserProjectWorkflowConnection() {
    }

    @Default
    public ConnectedUserProjectWorkflowConnection(long connectionId) {
        this.connectionId = AggregateReference.to(connectionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConnectedUserProjectWorkflowConnection that)) {
            return false;
        }

        return Objects.equals(connectionId, that.connectionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId);
    }

    public Long getConnectionId() {
        return connectionId.getId();
    }

    @Override
    public String toString() {
        return "ConnectedUserProjectWorkflowConnection{" +
            "connectionId=" + connectionId +
            '}';
    }

    /**
     * Used by MapStruct.
     */
    public @interface Default {
    }
}
