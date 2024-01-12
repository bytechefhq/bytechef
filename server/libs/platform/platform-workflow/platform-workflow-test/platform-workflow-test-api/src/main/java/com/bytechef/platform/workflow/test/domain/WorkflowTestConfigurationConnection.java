/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.workflow.test.domain;

import com.bytechef.platform.connection.domain.Connection;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("workflow_test_configuration_connection")
public class WorkflowTestConfigurationConnection implements Persistable<Long> {

    @Id
    private Long id;

    @Column("connection_id")
    private AggregateReference<Connection, Long> connectionId;

    @Column
    private String key;

    @Column("operation_name")
    private String operationName;

    public WorkflowTestConfigurationConnection() {
    }

    @Default
    public WorkflowTestConfigurationConnection(Long connectionId, String key, String operationName) {
        this.connectionId = connectionId == null ? null : AggregateReference.to(connectionId);
        this.key = key;
        this.operationName = operationName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WorkflowTestConfigurationConnection that = (WorkflowTestConfigurationConnection) o;

        return Objects.equals(connectionId, that.connectionId) && Objects.equals(key, that.key) &&
            Objects.equals(operationName, that.operationName);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public Long getId() {
        return id;
    }

    public Long getConnectionId() {
        return connectionId.getId();
    }

    public String getKey() {
        return key;
    }

    public String getOperationName() {
        return operationName;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public String toString() {
        return "WorkflowTestConfigurationConnection{" +
            "id=" + id +
            ", connectionId=" + connectionId +
            ", key='" + key + '\'' +
            ", taskName='" + operationName + '\'' +
            '}';
    }

    /**
     * Used by MapStruct.
     */
    public @interface Default {
    }
}
