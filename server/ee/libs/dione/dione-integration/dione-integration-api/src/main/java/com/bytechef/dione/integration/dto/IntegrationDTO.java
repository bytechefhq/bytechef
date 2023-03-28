
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

package com.bytechef.dione.integration.dto;

import com.bytechef.category.domain.Category;
import com.bytechef.dione.integration.domain.Integration;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationDTO(
    Category category, String createdBy, LocalDateTime createdDate, String description, Long id, int integrationVersion,
    String name, String lastModifiedBy, LocalDateTime lastModifiedDate, LocalDateTime publishedDate,
    Integration.Status status, List<Tag> tags, int version, List<String> workflowIds) {

    public IntegrationDTO(Integration integration, Category category, List<Tag> tags) {
        this(
            category, integration.getCreatedBy(), integration.getCreatedDate(),
            integration.getDescription(), integration.getId(), integration.getIntegrationVersion(),
            integration.getName(), integration.getLastModifiedBy(), integration.getLastModifiedDate(),
            integration.getPublishedDate(), integration.getStatus(), tags, integration.getVersion(),
            integration.getWorkflowIds());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integration toIntegration() {
        Integration integration = new Integration();

        integration.setCategory(category);
        integration.setDescription(description);
        integration.setId(id);
        integration.setName(name);
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
        private String createdBy;
        private LocalDateTime createdDate;
        private String description;
        private Long id;
        private String name;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private int integrationVersion;
        private LocalDateTime publishedDate;
        private Integration.Status status;
        private List<Tag> tags;
        private int version;
        private List<String> workflowIds;

        private Builder() {
        }

        public Builder category(Category category) {
            this.category = category;
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

        public Builder name(String name) {
            this.name = name;
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

        public Builder publishedDate(LocalDateTime publishedDate) {
            this.publishedDate = publishedDate;
            return this;
        }

        public Builder status(Integration.Status status) {
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
                category, createdBy, createdDate, description, id, integrationVersion, name, lastModifiedBy,
                lastModifiedDate,
                publishedDate, status, tags, version, workflowIds);
        }
    }
}
