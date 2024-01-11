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
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("integration_workflow")
public final class IntegrationWorkflow implements Persistable<Long> {

    @Id
    private Long id;

    @Column("workflow_id")
    private String workflowId;

    public IntegrationWorkflow() {
    }

    public IntegrationWorkflow(String workflowId) {
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

        return Objects.equals(id, integrationWorkflow.id) && Objects.equals(workflowId, integrationWorkflow.workflowId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public Long getId() {
        return id;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    @Override
    public String toString() {
        return "IntegrationWorkflow{" + ", id='"
            + id + '\'' + ", workflowId='"
            + workflowId + '\'' + '}';
    }
}
