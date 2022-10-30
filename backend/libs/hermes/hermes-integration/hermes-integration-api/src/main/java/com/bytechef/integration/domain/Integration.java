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

package com.bytechef.integration.domain;

import com.bytechef.integration.domain.annotation.Default;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table
public final class Integration implements Persistable<String> {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private String description;

    @Id
    private String id;

    @Column
    private String name;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    // TODO Add version
    // @Version
    @SuppressFBWarnings("UuF")
    private int version;

    @MappedCollection(idColumn = "integration_id")
    final Set<IntegrationWorkflow> integrationWorkflows = new HashSet<>();

    public Integration() {}

    @PersistenceCreator
    public Integration(String name, String description, Set<IntegrationWorkflow> integrationWorkflows) {
        this.name = name;
        this.description = description;
        this.integrationWorkflows.addAll(integrationWorkflows);
    }

    @Default
    public Integration(String name, String description, Collection<String> workflowIds) {
        this.name = name;
        this.description = description;

        if (workflowIds != null) {
            workflowIds.forEach(this::addWorkflow);
        }
    }

    public Integration(Integration integration) {
        this.createdBy = integration.createdBy;
        this.createdDate = integration.createdDate;
        this.description = integration.description;
        this.name = integration.name;
        this.id = integration.id;
        this.lastModifiedBy = integration.lastModifiedBy;
        this.lastModifiedDate = integration.lastModifiedDate;
        this.version = integration.version;
    }

    public void addWorkflow(String workflowId) {
        integrationWorkflows.add(new IntegrationWorkflow(workflowId, this));
    }

    public boolean containsWorkflows() {
        return !integrationWorkflows.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Integration integration = (Integration) o;

        return Objects.equals(id, integration.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Set<IntegrationWorkflow> getIntegrationWorkflows() {
        return new HashSet<>(integrationWorkflows);
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void removeWorkflow(String workflowId) {
        integrationWorkflows.stream()
                .filter(integrationWorkflow -> Objects.equals(integrationWorkflow.getWorkflowId(), workflowId))
                .findFirst()
                .ifPresent(integrationWorkflows::remove);
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Integration{" + "createdBy='"
                + createdBy + '\'' + ", createdDate="
                + createdDate + ", description='"
                + description + '\'' + ", id='"
                + id + '\'' + ", name='"
                + name + '\'' + ", lastModifiedBy='"
                + lastModifiedBy + '\'' + ", lastModifiedDate="
                + lastModifiedDate + ", version="
                + version + '}';
    }
}
