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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.connection.domain.Connection;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("integration_instance")
public class IntegrationInstance {

    @Column("connected_user_id")
    private Long connectedUserId;

    @Column("connection_id")
    private AggregateReference<Connection, Long> connectionId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

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

    @Column("integration_instance_configuration_id")
    private AggregateReference<IntegrationInstanceConfiguration, Long> integrationInstanceConfigurationId;

    @MappedCollection(idColumn = "integration_instance_id")
    private final Set<IntegrationInstanceWorkflow> integrationInstanceWorkflows = new HashSet<>();

    @Version
    private int version;

    public IntegrationInstance() {
    }

    public void addIntegrationWorkflow(
        long integrationInstanceConfigurationWorkflowId, Map<String, ?> inputs, boolean enabled) {

        integrationInstanceWorkflows.add(
            new IntegrationInstanceWorkflow(integrationInstanceConfigurationWorkflowId, inputs, enabled));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntegrationInstance integration = (IntegrationInstance) o;

        return Objects.equals(id, integration.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getConnectedUserId() {
        return connectedUserId;
    }

    public long getConnectionId() {
        return Validate.notNull(connectionId.getId(), "id");
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public Long getIntegrationInstanceConfigurationId() {
        return integrationInstanceConfigurationId.getId();
    }

    public Set<IntegrationInstanceWorkflow> getIntegrationInstanceWorkflows() {
        return Collections.unmodifiableSet(integrationInstanceWorkflows);
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setConnectedUserId(Long connectedUserId) {
        this.connectedUserId = connectedUserId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = AggregateReference.to(connectionId);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegrationInstanceConfigurationId(long integrationInstanceConfigurationId) {
        this.integrationInstanceConfigurationId = AggregateReference.to(integrationInstanceConfigurationId);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "IntegrationInstance{" +
            "id=" + id +
            ", connectedUserId=" + connectedUserId +
            ", integrationInstanceConfigurationId=" + integrationInstanceConfigurationId +
            ", enabled='" + enabled +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }

    public IntegrationInstanceWorkflow updateWorkflowEnabled(
        long integrationInstanceConfigurationWorkflowId, boolean enable) {

        IntegrationInstanceWorkflow integrationInstanceWorkflow = CollectionUtils.getFirst(
            integrationInstanceWorkflows,
            curIntegrationInstanceWorkflow -> curIntegrationInstanceWorkflow
                .getIntegrationInstanceConfigurationWorkflowId() == integrationInstanceConfigurationWorkflowId);

        integrationInstanceWorkflow.setEnabled(enable);

        return integrationInstanceWorkflow;
    }
}
