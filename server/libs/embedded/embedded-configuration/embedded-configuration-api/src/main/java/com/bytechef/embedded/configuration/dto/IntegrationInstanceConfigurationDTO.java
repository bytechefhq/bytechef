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
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationInstanceConfigurationDTO(
    Map<String, ?> connectionAuthorizationParameters, Map<String, ?> connectionConnectionParameters,
    Map<String, ?> connectionParameters, String createdBy, Instant createdDate, String description,
    boolean enabled, Environment environment, Long id, String lastModifiedBy, Instant lastModifiedDate,
    IntegrationDTO integration, long integrationId, Integer integrationVersion, String name,
    List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceConfigurationWorkflows, List<Tag> tags,
    int version) {

    public IntegrationInstanceConfigurationDTO(
        Map<String, ?> connectionAuthorizationParameters, Map<String, ?> connectionConnectionParameters,
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceWorkflows, IntegrationDTO integration,
        List<Tag> tags) {

        this(
            connectionAuthorizationParameters, connectionConnectionParameters,
            integrationInstanceConfiguration.getConnectionParameters(), integrationInstanceConfiguration.getCreatedBy(),
            integrationInstanceConfiguration.getCreatedDate(), integrationInstanceConfiguration.getDescription(),
            integrationInstanceConfiguration.isEnabled(), integrationInstanceConfiguration.getEnvironment(),
            integrationInstanceConfiguration.getId(), integrationInstanceConfiguration.getLastModifiedBy(),
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

        integrationInstance.setConnectionParameters(connectionParameters);
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
        private Map<String, ?> connectionParameters;
        private String createdBy;
        private Instant createdDate;
        private String description;
        private boolean enabled;
        private Environment environment;
        private Long id;
        private String lastModifiedBy;
        private Instant lastModifiedDate;
        private IntegrationDTO integration;
        private long integrationId;
        private int integrationVersion;
        private List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceConfigurationWorkflows;
        private String name;
        private List<Tag> tags;
        private int version;

        private Builder() {
        }

        public Builder connectionParameters(Map<String, ?> connectionParameters) {
            this.connectionParameters = connectionParameters;

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

        public Builder lastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;

            return this;
        }

        public Builder lastModifiedDate(Instant lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;

            return this;
        }

        public Builder integration(IntegrationDTO integration) {
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
                Map.of(), Map.of(), connectionParameters, createdBy, createdDate, description, enabled, environment, id,
                lastModifiedBy, lastModifiedDate, integration, integrationId, integrationVersion,
                name, integrationInstanceConfigurationWorkflows, tags, version);
        }
    }
}
