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

import com.bytechef.platform.connection.domain.Connection;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("integration_instance")
public class IntegrationInstance implements Persistable<Long> {

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

    @Version
    private int version;

    public IntegrationInstance() {
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

    public Long getConnectionId() {
        return connectionId == null ? null : connectionId.getId();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
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

    public Long getIntegrationInstanceConfigurationId() {
        return integrationInstanceConfigurationId == null ? null : integrationInstanceConfigurationId.getId();
    }

    public int getVersion() {
        return version;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId == null ? null : AggregateReference.to(connectionId);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegrationInstanceConfigurationId(Long integrationInstanceConfigurationId) {
        this.integrationInstanceConfigurationId = integrationInstanceConfigurationId == null ? null
            : AggregateReference.to(integrationInstanceConfigurationId);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "IntegrationInstance{" +
            "id=" + id +
            ", enabled='" + enabled +
            ", integrationInstanceConfigurationId=" + integrationInstanceConfigurationId +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
