/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.platform.connection.domain.Connection;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_shared_connection")
public class ConnectedUserSharedConnection {

    @Id
    private Long id;

    @Column("connection_id")
    private AggregateReference<Connection, Long> connectionId;

    public ConnectedUserSharedConnection() {
    }

    public ConnectedUserSharedConnection(long connectionId) {
        this.connectionId = AggregateReference.to(connectionId);
    }

    @PersistenceCreator
    public ConnectedUserSharedConnection(Long id, Long connectionId) {
        this.id = id;
        this.connectionId = AggregateReference.to(connectionId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConnectedUserSharedConnection that)) {
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

    public Long getConnectionId() {
        return connectionId.getId();
    }

    @Override
    public String toString() {
        return "ConnectedUserSharedConnection{" +
            "id=" + id +
            ", connectionId=" + connectionId +
            '}';
    }
}
