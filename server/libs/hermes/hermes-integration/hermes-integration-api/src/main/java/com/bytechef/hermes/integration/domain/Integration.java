
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

package com.bytechef.hermes.integration.domain;

import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.CollectionUtils;

/**
 * @author Ivica Cardic
 */
@Table
public final class Integration implements Persistable<Long> {

    @Column
    private String category;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private String description;

    @Id
    private Long id;

    @MappedCollection(idColumn = "integration_id")
    private Set<IntegrationTag> integrationTags = new HashSet<>();

    @MappedCollection(idColumn = "integration_id")
    private Set<IntegrationWorkflow> integrationWorkflows = new HashSet<>();

    @Column
    private String name;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Version
    @SuppressFBWarnings("UuF")
    private int version;

    public Integration() {
    }

    @PersistenceCreator
    public Integration(
        String name, String description, Set<IntegrationTag> integrationTags,
        Set<IntegrationWorkflow> integrationWorkflows) {
        this.name = name;
        this.description = description;
        this.integrationTags.addAll(integrationTags);
        this.integrationWorkflows.addAll(integrationWorkflows);
    }

    public void addTag(Tag tag) {
        integrationTags.add(new IntegrationTag(tag));
    }

    public void addWorkflow(String workflowId) {
        integrationWorkflows.add(new IntegrationWorkflow(workflowId));
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

    public String getCategory() {
        return category;
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
    public Long getId() {
        return id;
    }

    public Set<String> getWorkflowIds() {
        return integrationWorkflows.stream()
            .map(IntegrationWorkflow::getWorkflowId)
            .collect(Collectors.toSet());
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

    public Set<Long> getTagIds() {
        return integrationTags
            .stream()
            .map(IntegrationTag::getTagId)
            .collect(Collectors.toSet());
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

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWorkflowIds(Set<String> workflowIds) {
        integrationWorkflows = new HashSet<>();

        for (String workflowId : workflowIds) {
            addWorkflow(workflowId);
        }
    }

    public void setTags(Set<Tag> tags) {
        integrationTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tags)) {
            for (Tag tag : tags) {
                addTag(tag);
            }
        }
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
