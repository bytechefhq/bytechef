
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

package com.bytechef.dione.integration.domain;

import com.bytechef.category.domain.Category;
import com.bytechef.tag.domain.Tag;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.CollectionUtils;

/**
 * @author Ivica Cardic
 */
@Table
public final class Integration implements Persistable<Long> {

    public enum Status {
        PUBLISHED, UNPUBLISHED
    }

    @Transient
    private Category category;

    @Column("category_id")
    private AggregateReference<Category, Long> categoryId;

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

    @Column
    private int integrationVersion;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column("last_published_date")
    private LocalDateTime lastPublishedDate;

    @Column
    private Status status;

    @Transient
    private List<Tag> tags = new ArrayList<>();

    @Version
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
        if (tag.getId() != null) {
            integrationTags.add(new IntegrationTag(tag));
        }

        tags.add(tag);
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

    public Long getCategoryId() {
        return categoryId == null ? null : categoryId.getId();
    }

    @SuppressFBWarnings("EI")
    public Category getCategory() {
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

    public int getIntegrationVersion() {
        return integrationVersion;
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

    public LocalDateTime getLastPublishedDate() {
        return lastPublishedDate;
    }

    public Status getStatus() {
        return status;
    }

    public List<Long> getTagIds() {
        return integrationTags
            .stream()
            .map(IntegrationTag::getTagId)
            .toList();
    }

    public List<Tag> getTags() {
        return List.copyOf(tags);
    }

    public List<String> getWorkflowIds() {
        return integrationWorkflows.stream()
            .map(IntegrationWorkflow::getWorkflowId)
            .toList();
    }

    public int getVersion() {
        return version;
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

    @SuppressFBWarnings({
        "EI", "NP"
    })
    public void setCategory(Category category) {
        this.category = category;

        if (category != null && !category.isNew()) {
            this.categoryId = AggregateReference.to(category.getId());
        }
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = AggregateReference.to(categoryId);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegrationVersion(int integrationVersion) {
        this.integrationVersion = integrationVersion;
    }

    public void setLastPublishedDate(LocalDateTime lastPublishedDate) {
        this.lastPublishedDate = lastPublishedDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setTags(List<Tag> tags) {
        this.integrationTags = new HashSet<>();
        this.tags = new ArrayList<>();

        if (!CollectionUtils.isEmpty(tags)) {
            for (Tag tag : tags) {
                addTag(tag);
            }
        }
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkflowIds(List<String> workflowIds) {
        integrationWorkflows = new HashSet<>();

        for (String workflowId : workflowIds) {
            addWorkflow(workflowId);
        }
    }

    @Override
    public String toString() {
        return "Integration{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", integrationVersion='" + integrationVersion + '\'' +
            ", status='" + status + '\'' +
            ", lastPublishedDate='" + lastPublishedDate + '\'' +
            ", categoryId=" + getCategoryId() +
            ", description='" + description + '\'' +
            ", integrationTags=" + integrationTags +
            ", integrationWorkflows=" + integrationWorkflows +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }

    public Integration update(Integration integration) {
        this.category = integration.category;
        this.categoryId = integration.categoryId;
        this.description = integration.description;
        this.id = integration.id;
        this.integrationTags = integration.integrationTags;
        this.integrationWorkflows = integration.integrationWorkflows;
        this.name = integration.name;
        this.tags = integration.tags;

        return this;
    }
}
