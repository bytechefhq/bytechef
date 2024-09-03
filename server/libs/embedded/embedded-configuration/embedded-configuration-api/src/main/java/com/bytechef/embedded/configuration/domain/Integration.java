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
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table
public final class Integration {

    @Column("allow_multiple_instances")
    private boolean allowMultipleInstances;

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

    @Column
    private String description;

    @Id
    private Long id;

    @MappedCollection(idColumn = "integration_id")
    private Set<IntegrationTag> integrationTags = new HashSet<>();

    @MappedCollection(idColumn = "integration_id")
    private Set<IntegrationVersion> integrationVersions = new HashSet<>();

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column
    private String name;

    @Version
    private int version;

    public Integration() {
        integrationVersions.add(new IntegrationVersion(1));
    }

    @PersistenceCreator
    public Integration(
        AggregateReference<Category, Long> categoryId, String description, Long id,
        Set<IntegrationTag> integrationTags, Set<IntegrationVersion> integrationVersions, int version) {

        this.categoryId = categoryId;
        this.description = description;
        this.id = id;
        this.integrationTags.addAll(integrationTags);
        this.integrationVersions.addAll(integrationVersions);
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
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

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public List<IntegrationVersion> getIntegrationVersions() {
        return new ArrayList<>(integrationVersions);
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    @Nullable
    public LocalDateTime getLastPublishedDate() {
        return integrationVersions.stream()
            .sorted((o1, o2) -> Integer.compare(o2.getVersion(), o1.getVersion()))
            .filter(projectVersion -> projectVersion.getStatus() == Status.PUBLISHED)
            .findFirst()
            .map(IntegrationVersion::getPublishedDate)
            .orElse(null);
    }

    public Status getLastStatus() {
        return getMaxIntegrationVersion().getStatus();
    }

    public int getLastIntegrationVersion() {
        return getMaxIntegrationVersion().getVersion();
    }

    public String getName() {
        return name;
    }

    public List<Long> getTagIds() {
        return integrationTags.stream()
            .map(IntegrationTag::getTagId)
            .toList();
    }

    public int getVersion() {
        return version;
    }

    public boolean isAllowMultipleInstances() {
        return allowMultipleInstances;
    }

    public boolean isPublished() {
        return integrationVersions.stream()
            .anyMatch(projectVersion -> projectVersion.getStatus() == Status.PUBLISHED);
    }

    public int publish(String description) {
        IntegrationVersion integrationVersion = getMaxIntegrationVersion();

        integrationVersion.setDescription(description);
        integrationVersion.setPublishedDate(LocalDateTime.now());
        integrationVersion.setStatus(Status.PUBLISHED);

        int newVersion = integrationVersion.getVersion() + 1;

        integrationVersions.add(new IntegrationVersion(newVersion));

        return newVersion;
    }

    public void setAllowMultipleInstances(boolean allowMultipleInstances) {
        this.allowMultipleInstances = allowMultipleInstances;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegrationVersions(List<IntegrationVersion> integrationVersions) {
        if (!CollectionUtils.isEmpty(integrationVersions)) {
            this.integrationVersions = new HashSet<>(integrationVersions);
        }
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return "Integration{" +
            "id=" + id +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", categoryId=" + getCategoryId() +
            ", integrationTags=" + integrationTags +
            ", allowMultipleInstances=" + allowMultipleInstances +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }

    private IntegrationVersion getMaxIntegrationVersion() {
        return integrationVersions.stream()
            .max(Comparator.comparingInt(IntegrationVersion::getVersion))
            .orElseThrow();
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private Long categoryId;
        private String componentName;
        private Long id;
        private String name;
        private List<Long> tagIds;
        private int version;

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

        public Builder name(String name) {
            this.name = name;

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

        public Integration build() {
            Integration integration = new Integration();

            if (categoryId != null) {
                integration.setCategoryId(categoryId);
            }

            integration.setComponentName(componentName);
            integration.setId(id);
            integration.setName(name);
            integration.setTagIds(tagIds);
            integration.setVersion(version);

            return integration;
        }
    }
}
