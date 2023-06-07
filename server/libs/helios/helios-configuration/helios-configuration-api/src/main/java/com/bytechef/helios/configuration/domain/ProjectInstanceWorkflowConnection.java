
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.configuration.domain;

import com.bytechef.hermes.connection.domain.Connection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Table("project_instance_workflow_connection")
public class ProjectInstanceWorkflowConnection implements Persistable<Long> {

    @Id
    private Long id;

    @Column("connection_id")
    private AggregateReference<Connection, Long> connectionId;

    @Column
    private String key;

    @Column("task_name")
    private String taskName;

    public ProjectInstanceWorkflowConnection() {
    }

    @Default
    @SuppressFBWarnings("NP")
    public ProjectInstanceWorkflowConnection(Long connectionId, String key, String taskName) {
        this.connectionId = connectionId == null ? null : AggregateReference.to(connectionId);
        this.key = key;
        this.taskName = taskName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectInstanceWorkflowConnection that = (ProjectInstanceWorkflowConnection) o;

        return Objects.equals(id, that.id) && Objects.equals(connectionId, that.connectionId)
            && Objects.equals(key, that.key) && Objects.equals(taskName, that.taskName);
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

    public String getTaskName() {
        return taskName;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public String toString() {
        return "ProjectInstanceWorkflowConnection{" +
            "id=" + id +
            ", connectionId=" + connectionId +
            ", key='" + key + '\'' +
            ", taskName='" + taskName + '\'' +
            '}';
    }

    /**
     * Used by MapStruct.
     */
    public @interface Default {
    }
}
