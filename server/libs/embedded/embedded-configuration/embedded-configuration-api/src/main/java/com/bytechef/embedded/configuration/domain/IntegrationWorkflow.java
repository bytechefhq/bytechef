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

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("integration_workflow")
public final class IntegrationWorkflow {

    @Id
    private Long id;

    @Column("integration_id")
    private long integrationId;

    @Column("integration_version")
    private int integrationVersion;

    @Column("workflow_id")
    private String workflowId;

    public IntegrationWorkflow() {
    }

    public IntegrationWorkflow(long integrationId, int integrationVersion, String workflowId) {
        this.integrationId = integrationId;
        this.integrationVersion = integrationVersion;
        this.workflowId = workflowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntegrationWorkflow integrationWorkflow = (IntegrationWorkflow) o;

        return Objects.equals(id, integrationWorkflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public long getIntegrationId() {
        return integrationId;
    }

    public int getIntegrationVersion() {
        return integrationVersion;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setIntegrationVersion(int integrationVersion) {
        this.integrationVersion = integrationVersion;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String toString() {
        return "IntegrationWorkflow{" +
            "id=" + id +
            ", integrationId=" + integrationId +
            "integrationVersion=" + integrationVersion +
            ", workflowId='" + workflowId + '\'' +
            '}';
    }
}
