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

package com.bytechef.automation.configuration.domain;

import com.bytechef.platform.connection.domain.Connection;
import java.util.Objects;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("project_instance_workflow_connection")
public class ProjectInstanceWorkflowConnection {

    @Column("connection_id")
    private AggregateReference<Connection, Long> connectionId;

    @Column
    private String key;

    @Column("workflow_node_name")
    private String workflowNodeName;

    public ProjectInstanceWorkflowConnection() {
    }

    @Default
    public ProjectInstanceWorkflowConnection(long connectionId, String key, String workflowNodeName) {
        this.connectionId = AggregateReference.to(connectionId);
        this.key = key;
        this.workflowNodeName = workflowNodeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ProjectInstanceWorkflowConnection that)) {
            return false;
        }

        return Objects.equals(connectionId, that.connectionId) && Objects.equals(key, that.key)
            && Objects.equals(workflowNodeName, that.workflowNodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId, key, workflowNodeName);
    }

    public Long getConnectionId() {
        return connectionId.getId();
    }

    public String getKey() {
        return key;
    }

    public String getWorkflowNodeName() {
        return workflowNodeName;
    }

    @Override
    public String toString() {
        return "ProjectInstanceWorkflowConnection{" +
            "connectionId=" + connectionId +
            ", key='" + key + '\'' +
            ", workflowNodeName='" + workflowNodeName + '\'' +
            '}';
    }

    /**
     * Used by MapStruct.
     */
    public @interface Default {
    }
}
