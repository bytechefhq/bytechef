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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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

    @MappedCollection(idColumn = "integration_id")
    private Set<IntegrationWorkflow> integrationWorkflows = new HashSet<>();

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public Integration() {
    }

    @PersistenceCreator
    public Integration(
        AggregateReference<Category, Long> categoryId, String description, Long id,
        Set<IntegrationTag> integrationTags, Set<IntegrationVersion> integrationVersions,
        Set<IntegrationWorkflow> integrationWorkflows, int version) {

        this.categoryId = categoryId;
        this.description = description;
        this.id = id;
        this.integrationTags.addAll(integrationTags);
        this.integrationVersions.addAll(integrationVersions);
        this.integrationWorkflows.addAll(integrationWorkflows);
        this.version = version;
    }

    public void addVersion(List<String> duplicatedVersionWorkflowIds) {
        int version = fetchLastIntegrationVersion()
            .map(IntegrationVersion::getVersion)
            .orElse(0);

        int newVersion = version + 1;

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflows) {
            if (integrationWorkflow.getIntegrationVersion() == version) {
                integrationWorkflow.setIntegrationVersion(newVersion);
            }
        }

        integrationVersions.add(new IntegrationVersion(newVersion));

        if (version > 0) {
            for (String workflowId : duplicatedVersionWorkflowIds) {
                integrationWorkflows.add(new IntegrationWorkflow(workflowId, version));
            }
        }
    }

    public void addWorkflowId(String workflowId) {
        integrationWorkflows.add(
            new IntegrationWorkflow(
                workflowId,
                fetchLastVersion().orElseGet(() -> {
                    integrationVersions.add(new IntegrationVersion(1));

                    return 1;
                })));
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

    public Optional<IntegrationVersion> fetchLastIntegrationVersion() {
        return integrationVersions.stream()
            .max(Comparator.comparingInt(IntegrationVersion::getVersion));
    }

    public Optional<LocalDateTime> fetchLastPublishedDate() {
        return fetchLastIntegrationVersion()
            .map(IntegrationVersion::getPublishedDate);
    }

    public Optional<Status> fetchLastStatus() {
        return fetchLastIntegrationVersion()
            .map(IntegrationVersion::getStatus);
    }

    public Optional<Integer> fetchLastVersion() {
        return fetchLastIntegrationVersion()
            .map(IntegrationVersion::getVersion);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public List<String> getAllWorkflowIds() {
        return CollectionUtils.map(integrationWorkflows, IntegrationWorkflow::getWorkflowId);
    }

    public Map<Integer, List<String>> getAllWorkflowIdMap() {
        return integrationWorkflows.stream()
            .collect(Collectors.groupingBy(
                IntegrationWorkflow::getIntegrationVersion,
                Collectors.collectingAndThen(
                    Collectors.toList(), list -> CollectionUtils.map(list, IntegrationWorkflow::getWorkflowId))));
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
        return fetchLastPublishedDate().orElse(null);
    }

    @Nullable
    public Status getLastStatus() {
        return fetchLastStatus().orElse(null);
    }

    @Nullable
    public Integer getLastVersion() {
        return fetchLastVersion().orElse(null);
    }

    public List<Long> getTagIds() {
        return integrationTags
            .stream()
            .map(IntegrationTag::getTagId)
            .toList();
    }

    public int getVersion() {
        return version;
    }

    public List<String> getWorkflowIds(int integrationVersion) {
        Map<Integer, List<String>> workflowIdMap = getAllWorkflowIdMap();

        if (workflowIdMap.containsKey(integrationVersion)) {
            return workflowIdMap
                .get(integrationVersion)
                .stream()
                .toList();
        } else {
            return List.of();
        }
    }

    public boolean isAllowMultipleInstances() {
        return allowMultipleInstances;
    }

    public boolean isPublished() {
        return integrationVersions.stream()
            .anyMatch(projectVersion -> projectVersion.getStatus() == Status.PUBLISHED);
    }

    public void publish(String description) {
        fetchLastIntegrationVersion().ifPresent(lastIntegrationVersion -> {
            lastIntegrationVersion.setDescription(description);
            lastIntegrationVersion.setPublishedDate(LocalDateTime.now());
            lastIntegrationVersion.setStatus(Status.PUBLISHED);
        });
    }

    public void removeWorkflow(String workflowId) {
        integrationWorkflows.stream()
            .filter(integrationWorkflow -> Objects.equals(integrationWorkflow.getWorkflowId(), workflowId))
            .findFirst()
            .ifPresent(integrationWorkflows::remove);

        if (integrationWorkflows.isEmpty()) {
            integrationVersions.clear();
        }
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
        this.integrationVersions = new HashSet<>(integrationVersions);
    }

    public void setIntegrationWorkflows(Set<IntegrationWorkflow> integrationWorkflows) {
        this.integrationWorkflows = new HashSet<>(integrationWorkflows);
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

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private Long categoryId;
        private String componentName;
        private Long id;
        private List<Long> tagIds;
        private int version;
        private List<String> workflowIds = List.of();

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
            integration.setTagIds(tagIds);
            integration.setVersion(version);

            if (!workflowIds.isEmpty()) {
                integration.addVersion(workflowIds);

                workflowIds.forEach(integration::addWorkflowId);
            }

            return integration;
        }
    }
}
