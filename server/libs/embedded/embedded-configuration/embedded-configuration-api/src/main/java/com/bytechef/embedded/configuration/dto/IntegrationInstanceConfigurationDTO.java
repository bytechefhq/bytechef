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
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationInstanceConfigurationDTO(
    String createdBy, LocalDateTime createdDate, String description, boolean enabled, Environment environment, Long id,
    LocalDateTime lastExecutionDate, String lastModifiedBy, LocalDateTime lastModifiedDate, Integration integration,
    long integrationId, Integer integrationVersion, String name,
    List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceConfigurationWorkflows, List<Tag> tags,
    int version) {

    public IntegrationInstanceConfigurationDTO(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceWorkflows, Integration integration,
        LocalDateTime lastExecutionDate, List<Tag> tags) {

        this(
            integrationInstanceConfiguration.getCreatedBy(), integrationInstanceConfiguration.getCreatedDate(),
            integrationInstanceConfiguration.getDescription(), integrationInstanceConfiguration.isEnabled(),
            integrationInstanceConfiguration.getEnvironment(), integrationInstanceConfiguration.getId(),
            lastExecutionDate, integrationInstanceConfiguration.getLastModifiedBy(),
            integrationInstanceConfiguration.getLastModifiedDate(), integration,
            integrationInstanceConfiguration.getIntegrationId(),
            integrationInstanceConfiguration.getIntegrationVersion(), integrationInstanceConfiguration.getName(),
            CollectionUtils.sort(integrationInstanceWorkflows), tags, integrationInstanceConfiguration.getVersion());
    }

    public static Builder builder() {
        return new Builder();
    }

    public IntegrationInstanceConfiguration toIntegrationInstanceConfiguration() {
        IntegrationInstanceConfiguration integrationInstance = new IntegrationInstanceConfiguration();

        integrationInstance.setDescription(description);
        integrationInstance.setEnabled(enabled);
        integrationInstance.setEnvironment(environment);
        integrationInstance.setId(id);
        integrationInstance.setIntegrationId(integrationId);
        integrationInstance.setIntegrationVersion(integrationVersion);
        integrationInstance.setName(name);
        integrationInstance.setTags(tags);
        integrationInstance.setVersion(version);

        return integrationInstance;
    }

    public static final class Builder {
        private String createdBy;
        private LocalDateTime createdDate;
        private String description;
        private boolean enabled;
        private Environment environment;
        private Long id;
        private LocalDateTime lastExecutionDate;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private Integration integration;
        private long integrationId;
        private int integrationVersion;
        private List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceConfigurationWorkflows;
        private String name;
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

        public Builder environment(Environment environment) {
            this.environment = environment;

            return this;
        }

        public Builder id(Long id) {
            this.id = id;

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

        public Builder integrationId(long integrationId) {
            this.integrationId = integrationId;

            return this;
        }

        public Builder integrationVersion(int integrationVersion) {
            this.integrationVersion = integrationVersion;

            return this;
        }

        public Builder integrationInstanceConfigurationWorkflows(
            List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceConfigurationWorkflows) {

            this.integrationInstanceConfigurationWorkflows = integrationInstanceConfigurationWorkflows;

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

        public IntegrationInstanceConfigurationDTO build() {
            return new IntegrationInstanceConfigurationDTO(
                createdBy, createdDate, description, enabled, environment, id, lastExecutionDate, lastModifiedBy,
                lastModifiedDate, integration, integrationId, integrationVersion, name,
                integrationInstanceConfigurationWorkflows, tags, version);
        }
    }
}
