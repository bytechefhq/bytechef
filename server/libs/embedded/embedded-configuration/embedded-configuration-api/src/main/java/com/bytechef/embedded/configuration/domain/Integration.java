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

import com.bytechef.category.domain.Category;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table
public final class Integration implements Persistable<Long> {

    public enum Status {
        PUBLISHED(1), UNPUBLISHED(0);

        private final int id;

        Status(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Status valueOf(int id) {
            return switch (id) {
                case 0 -> Status.UNPUBLISHED;
                case 1 -> Status.PUBLISHED;
                default -> throw new IllegalArgumentException("Unexpected value=%s".formatted(id));
            };
        }
    }

    @Column("category_id")
    private AggregateReference<Category, Long> categoryId;

    @Column("component_name")
    private String componentName;

    @Column("component_version")
    private int componentVersion = 1;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Id
    private Long id;

    @MappedCollection(idColumn = "integration_id")
    private Set<IntegrationTag> integrationTags = new HashSet<>();

    @MappedCollection(idColumn = "integration_id")
    private Set<IntegrationWorkflow> integrationWorkflows = new HashSet<>();

    @Column
    private int integrationVersion;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column
    private String overview;

    @Column("published_date")
    private LocalDateTime publishedDate;

    @Column
    private int status;

    @Version
    private int version;

    public Integration() {
    }

    @PersistenceCreator
    public Integration(
        Long id, Set<IntegrationTag> integrationTags, Set<IntegrationWorkflow> integrationWorkflows, String overview) {

        this.id = id;
        this.integrationTags.addAll(integrationTags);
        this.integrationWorkflows.addAll(integrationWorkflows);
        this.overview = overview;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void addWorkflowId(String workflowId) {
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

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
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

    public int getIntegrationVersion() {
        return integrationVersion;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getOverview() {
        return overview;
    }

    public LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    public Status getStatus() {
        return Status.valueOf(status);
    }

    public List<Long> getTagIds() {
        return integrationTags
            .stream()
            .map(IntegrationTag::getTagId)
            .toList();
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

    public void setCategory(Category category) {
        this.categoryId = category == null
            ? null
            : category.getId() == null ? null : AggregateReference.to(Validate.notNull(category.getId(), "id"));
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId == null ? null : AggregateReference.to(categoryId);
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setComponentVersion(int componentVersion) {
        this.componentVersion = componentVersion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegrationVersion(int integrationVersion) {
        this.integrationVersion = integrationVersion;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public void setPublishedDate(LocalDateTime publishedDate) {
        this.publishedDate = publishedDate;
    }

    public void setStatus(Status status) {
        this.status = status == null ? 0 : status.getId();
    }

    public void setTagIds(List<Long> tagIds) {
        this.integrationTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long tagId : tagIds) {
                integrationTags.add(new IntegrationTag(tagId));
            }
        }
    }

    public void setTags(List<Tag> tags) {
        if (!CollectionUtils.isEmpty(tags)) {
            setTagIds(CollectionUtils.map(tags, Tag::getId));
        }
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkflowIds(List<String> workflowIds) {
        integrationWorkflows = new HashSet<>();

        if (!CollectionUtils.isEmpty(workflowIds)) {
            for (String workflowId : workflowIds) {
                addWorkflowId(workflowId);
            }
        }
    }

    @Override
    public String toString() {
        return "Integration{" +
            "id=" + id +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", integrationVersion=" + integrationVersion +
            ", status='" + status + '\'' +
            ", lastPublishedDate='" + publishedDate + '\'' +
            ", categoryId=" + getCategoryId() +
            ", integrationTags=" + integrationTags +
            ", integrationWorkflows=" + integrationWorkflows +
            ", overview='" + overview + '\'' +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private Long categoryId;
        private String componentName;
        private Long id;
        private int integrationVersion;
        private LocalDateTime publishedDate;
        private Status status;
        private List<Long> tagIds;
        private int version;
        private List<String> workflowIds;

        private Builder() {
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder componentName(String componentName) {
            this.componentName = componentName;
            return this;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder integrationVersion(int integrationVersion) {
            this.integrationVersion = integrationVersion;
            return this;
        }

        public Builder publishedDate(LocalDateTime publishedDate) {
            this.publishedDate = publishedDate;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder tagIds(List<Long> tagIds) {
            this.tagIds = tagIds;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder workflowIds(List<String> workflowIds) {
            this.workflowIds = workflowIds;

            return this;
        }

        public Integration build() {
            Integration integration = new Integration();

            if (categoryId != null) {
                integration.setCategoryId(categoryId);
            }

            integration.setComponentName(componentName);
            integration.setId(id);
            integration.setIntegrationVersion(integrationVersion);
            integration.setPublishedDate(publishedDate);
            integration.setStatus(status);
            integration.setTagIds(tagIds);
            integration.setVersion(version);
            integration.setWorkflowIds(workflowIds);

            return integration;
        }
    }
}
