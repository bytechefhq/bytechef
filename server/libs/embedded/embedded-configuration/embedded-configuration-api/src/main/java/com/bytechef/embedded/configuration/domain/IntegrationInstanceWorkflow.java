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

package com.bytechef.embedded.configuration.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.commons.util.MapUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("integration_instance_workflow")
public class IntegrationInstanceWorkflow implements Persistable<Long>, Comparable<IntegrationInstanceWorkflow> {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private MapWrapper inputs = new MapWrapper();

    @Column
    private boolean enabled;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column("integration_instance_id")
    private AggregateReference<Integration, Long> integrationInstanceId;

    @MappedCollection(idColumn = "integration_instance_workflow_id")
    private Set<IntegrationInstanceWorkflowConnection> integrationInstanceWorkflowConnections = Collections.emptySet();

    @Version
    private int version;

    @Column("workflow_id")
    private String workflowId;

    public IntegrationInstanceWorkflow() {
    }

    public IntegrationInstanceWorkflow(
        List<IntegrationInstanceWorkflowConnection> connections, Map<String, Object> inputs, String workflowId) {

        this.integrationInstanceWorkflowConnections = new HashSet<>(connections);
        this.inputs = new MapWrapper(inputs);
        this.workflowId = workflowId;
    }

    @Override
    public int compareTo(IntegrationInstanceWorkflow integrationInstanceWorkflow) {
        return workflowId.compareTo(integrationInstanceWorkflow.workflowId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntegrationInstanceWorkflow that = (IntegrationInstanceWorkflow) o;

        return Objects.equals(id, that.id) && Objects.equals(workflowId, that.workflowId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public List<IntegrationInstanceWorkflowConnection> getConnections() {
        return List.copyOf(integrationInstanceWorkflowConnections);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Map<String, ?> getInputs() {
        return Collections.unmodifiableMap(inputs.getMap());
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getIntegrationInstanceId() {
        return integrationInstanceId.getId();
    }

    public int getVersion() {
        return version;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setConnections(List<IntegrationInstanceWorkflowConnection> integrationInstanceWorkflowConnections) {
        if (integrationInstanceWorkflowConnections != null) {
            this.integrationInstanceWorkflowConnections = new HashSet<>(integrationInstanceWorkflowConnections);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setInputs(Map<String, ?> inputs) {
        if (!MapUtils.isEmpty(inputs)) {
            this.inputs = new MapWrapper(inputs);
        }
    }

    public void setIntegrationInstanceId(Long integrationInstanceId) {
        if (integrationInstanceId != null) {
            this.integrationInstanceId = AggregateReference.to(integrationInstanceId);
        }
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String toString() {
        return "IntegrationTag{" + ", id='"
            + id + '\'' + ", workflowId='"
            + workflowId + '\'' + ", inputs="
            + inputs + ", integrationInstanceWorkflowConnections="
            + integrationInstanceWorkflowConnections + ", enabled="
            + enabled + '}';
    }
}
