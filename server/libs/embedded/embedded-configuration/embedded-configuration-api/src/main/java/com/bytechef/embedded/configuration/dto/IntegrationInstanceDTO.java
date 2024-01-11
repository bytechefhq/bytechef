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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationInstanceDTO(
    String createdBy, LocalDateTime createdDate, String description, boolean enabled, Long id, String name,
    LocalDateTime lastExecutionDate, String lastModifiedBy, LocalDateTime lastModifiedDate, Integration integration,
    Long integrationId, List<IntegrationInstanceWorkflowDTO> integrationInstanceWorkflows, List<Tag> tags,
    int version) {

    public IntegrationInstanceDTO(
        LocalDateTime lastExecutionDate, IntegrationInstance integrationInstance,
        List<IntegrationInstanceWorkflowDTO> integrationInstanceWorkflows, Integration integration, List<Tag> tags) {

        this(
            integrationInstance.getCreatedBy(), integrationInstance.getCreatedDate(),
            integrationInstance.getDescription(), integrationInstance.isEnabled(), integrationInstance.getId(),
            integrationInstance.getName(), lastExecutionDate, integrationInstance.getLastModifiedBy(),
            integrationInstance.getLastModifiedDate(), integration, integrationInstance.getIntegrationId(),
            CollectionUtils.sort(integrationInstanceWorkflows), tags, integrationInstance.getVersion());
    }

    public static Builder builder() {
        return new Builder();
    }

    public IntegrationInstance toIntegrationInstance() {
        IntegrationInstance integrationInstance = new IntegrationInstance();

        integrationInstance.setDescription(description);
        integrationInstance.setEnabled(enabled);
        integrationInstance.setId(id);
        integrationInstance.setName(name);
        integrationInstance.setIntegrationId(integrationId);
        integrationInstance.setTags(tags);
        integrationInstance.setVersion(version);

        return integrationInstance;
    }

    public static final class Builder {
        private String createdBy;
        private LocalDateTime createdDate;
        private String description;
        private boolean enabled;
        private Long id;
        private String name;
        private LocalDateTime lastExecutionDate;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private Integration integration;
        private Long integrationId;
        private List<IntegrationInstanceWorkflowDTO> integrationInstanceWorkflows;
        private List<Tag> tags;
        private int version;

        private Builder() {
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

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
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

        public Builder lastExecutionDate(LocalDateTime lastExecutionDate) {
            this.lastExecutionDate = lastExecutionDate;
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

        public Builder integration(Integration integration) {
            this.integration = integration;
            return this;
        }

        public Builder integrationId(Long integrationId) {
            this.integrationId = integrationId;
            return this;
        }

        public Builder integrationInstanceWorkflows(List<IntegrationInstanceWorkflowDTO> integrationInstanceWorkflows) {
            this.integrationInstanceWorkflows = integrationInstanceWorkflows;
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

        public IntegrationInstanceDTO build() {
            return new IntegrationInstanceDTO(
                createdBy, createdDate, description, enabled, id, name, lastExecutionDate, lastModifiedBy,
                lastModifiedDate, integration, integrationId, integrationInstanceWorkflows, tags, version);
        }
    }
}
