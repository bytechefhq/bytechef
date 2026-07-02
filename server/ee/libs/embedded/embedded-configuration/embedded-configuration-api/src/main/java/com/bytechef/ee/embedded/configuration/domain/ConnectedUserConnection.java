/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.platform.connection.domain.Connection;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity that represents a connection owned by a ConnectedUser.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_connection")
public class ConnectedUserConnection {

    @Id
    private Long id;

    @Column("connected_user_id")
    private AggregateReference<ConnectedUser, Long> connectedUserId;

    @Column("connection_id")
    private AggregateReference<Connection, Long> connectionId;

    public ConnectedUserConnection() {
    }

    @PersistenceCreator
    public ConnectedUserConnection(Long id, Long connectedUserId, Long connectionId) {
        this.id = id;
        this.connectedUserId = AggregateReference.to(connectedUserId);
        this.connectionId = AggregateReference.to(connectionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConnectedUserConnection that)) {
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

    public Long getConnectionId() {
        return connectionId.getId();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConnectedUserId(Long connectedUserId) {
        this.connectedUserId = AggregateReference.to(connectedUserId);
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = AggregateReference.to(connectionId);
    }

    @Override
    public String toString() {
        return "ConnectedUserConnection{" +
            "id=" + id +
            ", connectedUserId=" + connectedUserId +
            ", connectionId=" + connectionId +
            '}';
    }
}
