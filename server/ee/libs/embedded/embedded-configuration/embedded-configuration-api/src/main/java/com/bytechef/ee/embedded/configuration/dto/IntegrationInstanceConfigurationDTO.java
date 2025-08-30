/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record IntegrationInstanceConfigurationDTO(
    AuthorizationType authorizationType, Map<String, ?> connectionAuthorizationParameters,
    Map<String, ?> connectionConnectionParameters, Map<String, ?> connectionParameters, String createdBy,
    Instant createdDate, String description, boolean enabled, Environment environment, Long id, String lastModifiedBy,
    Instant lastModifiedDate, IntegrationDTO integration, long integrationId, Integer integrationVersion, String name,
    List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceConfigurationWorkflows, List<Tag> tags,
    int version) {

    public IntegrationInstanceConfigurationDTO(IntegrationInstanceConfiguration integrationInstanceConfiguration) {
        this(
            integrationInstanceConfiguration.getAuthorizationType(), Map.of(), Map.of(),
            integrationInstanceConfiguration.getConnectionParameters(),
            integrationInstanceConfiguration.getCreatedBy(), integrationInstanceConfiguration.getCreatedDate(),
            integrationInstanceConfiguration.getDescription(), integrationInstanceConfiguration.isEnabled(),
            integrationInstanceConfiguration.getEnvironment(), integrationInstanceConfiguration.getId(),
            integrationInstanceConfiguration.getLastModifiedBy(),
            integrationInstanceConfiguration.getLastModifiedDate(), null,
            integrationInstanceConfiguration.getIntegrationId(),
            integrationInstanceConfiguration.getIntegrationVersion(), integrationInstanceConfiguration.getName(),
            List.of(), List.of(), integrationInstanceConfiguration.getVersion());
    }

    public IntegrationInstanceConfigurationDTO(
        Map<String, ?> connectionAuthorizationParameters, Map<String, ?> connectionConnectionParameters,
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<IntegrationInstanceConfigurationWorkflowDTO> integrationInstanceWorkflows, IntegrationDTO integration,
        List<Tag> tags) {

        this(
            integrationInstanceConfiguration.getAuthorizationType(), connectionAuthorizationParameters,
            connectionConnectionParameters,
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
        integrationInstance.setAuthorizationType(authorizationType);
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
        private AuthorizationType authorizationType;
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

        public Builder authorizationType(AuthorizationType authorizationType) {
            this.authorizationType = authorizationType;

            return this;
        }

        public IntegrationInstanceConfigurationDTO build() {
            return new IntegrationInstanceConfigurationDTO(
                authorizationType, Map.of(), Map.of(), connectionParameters, createdBy, createdDate, description,
                enabled, environment, id,
                lastModifiedBy, lastModifiedDate, integration, integrationId, integrationVersion,
                name, integrationInstanceConfigurationWorkflows, tags, version);
        }
    }
}
