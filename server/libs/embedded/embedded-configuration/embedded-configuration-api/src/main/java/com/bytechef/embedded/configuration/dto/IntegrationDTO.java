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

import com.bytechef.category.domain.Category;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.Integration.Status;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationDTO(
    Category category, String componentName, int componentVersion, String createdBy, LocalDateTime createdDate,
    Long id, int integrationVersion, String lastModifiedBy, LocalDateTime lastModifiedDate, String overview,
    LocalDateTime publishedDate, Status status, List<Tag> tags, int version, List<String> workflowIds) {

    public IntegrationDTO(Integration integration, Category category, List<Tag> tags) {
        this(
            category, integration.getComponentName(), integration.getComponentVersion(), integration.getCreatedBy(),
            integration.getCreatedDate(), integration.getId(), integration.getIntegrationVersion(),
            integration.getLastModifiedBy(), integration.getLastModifiedDate(), integration.getOverview(),
            integration.getPublishedDate(), integration.getStatus(), tags, integration.getVersion(),
            integration.getWorkflowIds());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integration toIntegration() {
        Integration integration = new Integration();

        integration.setComponentName(componentName);
        integration.setCategory(category);
        integration.setOverview(overview);
        integration.setId(id);
        integration.setIntegrationVersion(integrationVersion);
        integration.setPublishedDate(publishedDate);
        integration.setStatus(status);
        integration.setTags(tags);
        integration.setVersion(version);
        integration.setWorkflowIds(workflowIds);

        return integration;
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private Category category;
        private String componentName;
        private int componentVersion;
        private String createdBy;
        private LocalDateTime createdDate;
        private Long id;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private int integrationVersion;
        private String overview;
        private LocalDateTime publishedDate;
        private Status status;
        private List<Tag> tags;
        private int version;
        private List<String> workflowIds;

        private Builder() {
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

        public Builder id(Long id) {
            this.id = id;
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

        public Builder integrationVersion(int integrationVersion) {
            this.integrationVersion = integrationVersion;
            return this;
        }

        public Builder overview(String overview) {
            this.overview = overview;
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

        public Builder tags(List<Tag> tags) {
            this.tags = tags;
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

        public IntegrationDTO build() {
            return new IntegrationDTO(
                category, componentName, componentVersion, createdBy, createdDate, id, integrationVersion,
                lastModifiedBy, lastModifiedDate, overview, publishedDate, status, tags, version, workflowIds);
        }
    }
}
