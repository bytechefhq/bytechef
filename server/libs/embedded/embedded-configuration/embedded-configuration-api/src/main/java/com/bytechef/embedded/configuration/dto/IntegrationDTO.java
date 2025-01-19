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

package com.bytechef.embedded.configuration.dto;

import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.embedded.configuration.domain.IntegrationVersion.Status;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationDTO(
    boolean allowMultipleInstances, Category category, String componentName, String createdBy,
    Instant createdDate, String description, String icon, Long id, List<IntegrationVersion> integrationVersions,
    List<Long> integrationWorkflowIds, String lastModifiedBy, Instant lastModifiedDate,
    Instant lastPublishedDate, Status lastStatus, Integer lastIntegrationVersion, String name, List<Tag> tags,
    String title, int version) {

    public IntegrationDTO(Integration integration) {
        this(
            integration.isAllowMultipleInstances(),
            integration.getCategoryId() == null ? null : new Category(integration.getCategoryId()),
            integration.getComponentName(), integration.getCreatedBy(), integration.getCreatedDate(),
            integration.getDescription(), null, integration.getId(), integration.getIntegrationVersions(), null,
            integration.getLastModifiedBy(), integration.getLastModifiedDate(), integration.getLastPublishedDate(),
            integration.getLastStatus(), integration.getLastIntegrationVersion(), integration.getName(), List.of(),
            null, integration.getVersion());
    }

    public IntegrationDTO(
        Category category, ComponentDefinition componentDefinition, Integration integration,
        List<Long> integrationWorkflowIds, List<Tag> tags) {

        this(
            integration.isAllowMultipleInstances(), category, integration.getComponentName(),
            integration.getCreatedBy(), integration.getCreatedDate(), getDescription(componentDefinition, integration),
            componentDefinition.getIcon(), integration.getId(), integration.getIntegrationVersions(),
            integrationWorkflowIds, integration.getLastModifiedBy(), integration.getLastModifiedDate(),
            integration.getLastPublishedDate(), integration.getLastStatus(), integration.getLastIntegrationVersion(),
            integration.getName(), tags, componentDefinition.getTitle(), integration.getVersion());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integration toIntegration() {
        Integration integration = new Integration();

        integration.setAllowMultipleInstances(allowMultipleInstances);
        integration.setComponentName(componentName);
        integration.setCategory(category);
        integration.setDescription(description);
        integration.setId(id);
        integration.setIntegrationVersions(integrationVersions == null ? List.of() : integrationVersions);
        integration.setName(name);
        integration.setTags(tags);
        integration.setVersion(version);

        return integration;
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private boolean allowMultipleInstances;
        private Category category;
        private String componentName;
        private String createdBy;
        private Instant createdDate;
        private String description;
        private Long id;
        private List<IntegrationVersion> integrationVersions;
        private List<Long> integrationWorkflowIds;
        private String lastModifiedBy;
        private Instant lastModifiedDate;
        private Instant lastPublishedDate;
        private Status lastStatus = Status.DRAFT;
        private int lastIntegrationVersion;
        private String name;
        private List<Tag> tags;
        private int version;

        private Builder() {
        }

        public Builder allowMultipleInstances(boolean allowMultipleInstances) {
            this.allowMultipleInstances = allowMultipleInstances;

            return this;
        }

        public Builder category(Category category) {
            this.category = category;

            return this;
        }

        public Builder componentName(String componentName) {
            this.componentName = componentName;

            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;

            return this;
        }

        public Builder createdDate(Instant createdDate) {
            this.createdDate = createdDate;

            return this;
        }

        public Builder description(String description) {
            this.description = description;

            return this;
        }

        public Builder id(Long id) {
            this.id = id;

            return this;
        }

        public Builder integrationVersions(List<IntegrationVersion> integrationVersions) {
            this.integrationVersions = integrationVersions;

            return this;
        }

        public Builder integrationWorkflowIds(List<Long> integrationWorkflowIds) {
            this.integrationWorkflowIds = integrationWorkflowIds;

            return this;
        }

        public Builder lastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;

            return this;
        }

        public Builder lastModifiedDate(Instant lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;

            return this;
        }

        public Builder lastPublishedDate(Instant lastPublishedDate) {
            this.lastPublishedDate = lastPublishedDate;

            return this;
        }

        public Builder lastStatus(Status lastStatus) {
            this.lastStatus = lastStatus;

            return this;
        }

        public Builder lastIntegrationVersion(int lastIntegrationVersion) {
            this.lastIntegrationVersion = lastIntegrationVersion;

            return this;
        }

        public Builder name(String name) {
            this.name = name;

            return this;
        }

        public Builder tags(List<Tag> tags) {
            this.tags = tags;

            return this;
        }

        public Builder version(int version) {
            this.version = version;

            return this;
        }

        public IntegrationDTO build() {
            return new IntegrationDTO(
                allowMultipleInstances, category, componentName, createdBy, createdDate, description, null, id,
                integrationVersions, integrationWorkflowIds, lastModifiedBy, lastModifiedDate, lastPublishedDate,
                lastStatus, lastIntegrationVersion, name, tags, null, version);
        }
    }

    private static String getDescription(
        ComponentDefinition componentDefinition, Integration integration) {
        return StringUtils.isEmpty(integration.getDescription())
            ? componentDefinition.getDescription() : integration.getDescription();
    }
}
