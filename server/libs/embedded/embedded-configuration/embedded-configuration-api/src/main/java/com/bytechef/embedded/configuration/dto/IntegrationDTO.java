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
import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationDTO(
    boolean allowMultipleInstances, Category category, String componentName, int componentVersion, String createdBy,
    LocalDateTime createdDate, String description, String icon, Long id, List<IntegrationVersion> integrationVersions,
    List<Long> integrationWorkflowIds, String lastModifiedBy, LocalDateTime lastModifiedDate,
    LocalDateTime lastPublishedDate, Status lastStatus, Integer lastIntegrationVersion, List<Tag> tags, String title,
    int version) {

    public IntegrationDTO(
        Category category, ComponentDefinition componentDefinition, Integration integration,
        List<Long> integrationWorkflowIds, LocalDateTime lastPublishedDate, Status lastStatus,
        int lastIntegrationVersion) {

        this(
            integration.isAllowMultipleInstances(), category, integration.getComponentName(),
            integration.getComponentVersion(), integration.getCreatedBy(), integration.getCreatedDate(),
            getDescription(componentDefinition, integration), componentDefinition.getIcon(),
            integration.getId(), integration.getIntegrationVersions(), integrationWorkflowIds,
            integration.getLastModifiedBy(), integration.getLastModifiedDate(), lastPublishedDate,
            lastStatus, lastIntegrationVersion, List.of(), componentDefinition.getTitle(), integration.getVersion());
    }

    public IntegrationDTO(
        Category category, Integration integration, List<Long> integrationWorkflowIds, List<Tag> tags) {

        this(
            integration.isAllowMultipleInstances(), category, integration.getComponentName(),
            integration.getComponentVersion(), integration.getCreatedBy(), integration.getCreatedDate(),
            integration.getDescription(), null, integration.getId(), integration.getIntegrationVersions(),
            integrationWorkflowIds, integration.getLastModifiedBy(), integration.getLastModifiedDate(),
            integration.getLastPublishedDate(), integration.getLastStatus(), integration.getLastIntegrationVersion(),
            tags, null, integration.getVersion());
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
        integration.setTags(tags);
        integration.setVersion(version);

        return integration;
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private boolean allowMultipleInstances;
        private Category category;
        private String componentName;
        private int componentVersion;
        private String createdBy;
        private LocalDateTime createdDate;
        private String description;
        private Long id;
        private List<IntegrationVersion> integrationVersions;
        private List<Long> integrationWorkflowIds;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private LocalDateTime lastPublishedDate;
        private Status lastStatus = Status.DRAFT;
        private int lastIntegrationVersion;
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

        public Builder componentVersion(int componentVersion) {
            this.componentVersion = componentVersion;

            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;

            return this;
        }

        public Builder createdDate(LocalDateTime createdDate) {
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

        public Builder lastModifiedDate(LocalDateTime lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;

            return this;
        }

        public Builder lastPublishedDate(LocalDateTime lastPublishedDate) {
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
                allowMultipleInstances, category, componentName, componentVersion, createdBy, createdDate,
                description, null, id, integrationVersions, integrationWorkflowIds, lastModifiedBy, lastModifiedDate,
                lastPublishedDate, lastStatus, lastIntegrationVersion, tags, null, version);
        }
    }

    private static String getDescription(
        ComponentDefinition componentDefinition, Integration integration) {
        return StringUtils.isEmpty(integration.getDescription())
            ? componentDefinition.getDescription() : integration.getDescription();
    }
}
