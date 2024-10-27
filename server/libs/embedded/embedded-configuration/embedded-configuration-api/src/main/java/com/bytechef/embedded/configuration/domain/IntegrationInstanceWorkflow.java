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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("integration_instance_workflow")
public class IntegrationInstanceWorkflow {

    @Id
    private Long id;

    @Column("integration_instance_configuration_workflow_id")
    private AggregateReference<IntegrationInstanceConfigurationWorkflow, Long> integrationInstanceConfigurationWorkflowId;

    @Column("integration_instance_id")
    private AggregateReference<IntegrationInstance, Long> integrationInstanceId;

    @Column
    private boolean enabled;

    @Column
    private MapWrapper inputs;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public IntegrationInstanceWorkflow() {
    }

    public IntegrationInstanceWorkflow(
        long integrationInstanceConfigurationWorkflowId, Map<String, ?> inputs, boolean enabled) {

        this.integrationInstanceConfigurationWorkflowId = AggregateReference.to(
            integrationInstanceConfigurationWorkflowId);
        this.inputs = new MapWrapper(inputs);
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public Long getIntegrationInstanceConfigurationWorkflowId() {
        return integrationInstanceConfigurationWorkflowId.getId();
    }

    public Long getIntegrationInstanceId() {
        return integrationInstanceId.getId();
    }

    public Map<String, ?> getInputs() {
        return inputs.getMap();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
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

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegrationInstanceConfigurationWorkflowId(long integrationInstanceConfigurationWorkflowId) {
        this.integrationInstanceConfigurationWorkflowId = AggregateReference.to(
            integrationInstanceConfigurationWorkflowId);
    }

    public void setIntegrationInstanceId(long integrationInstanceId) {
        this.integrationInstanceId = AggregateReference.to(integrationInstanceId);
    }

    public void setInputs(Map<String, ?> inputs) {
        this.inputs = new MapWrapper(inputs);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntegrationInstanceWorkflow integrationInstanceWorkflow = (IntegrationInstanceWorkflow) o;

        return Objects.equals(id, integrationInstanceWorkflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "IntegrationInstanceWorkflow{" +
            "id=" + id +
            ", integrationInstanceConfigurationWorkflowId=" + integrationInstanceConfigurationWorkflowId +
            ", integrationInstanceId=" + integrationInstanceId +
            ", enabled=" + enabled +
            ", inputs=" + inputs +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
