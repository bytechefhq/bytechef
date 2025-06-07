/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO.OAuth2;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.config.EmbeddedConfigurationPublicMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationBasicModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.OAuth2Model;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.WorkflowModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserIntegrationMapper {

    @Mapper(config = EmbeddedConfigurationPublicMapperSpringConfig.class)
    interface ConnectedUserIntegrationToIntegrationBasicMapper
        extends Converter<ConnectedUserIntegrationDTO, IntegrationBasicModel> {

        @Override
        @Mapping(target = "componentName", source = "integrationInstanceConfiguration.integration.componentName")
        @Mapping(target = "credentialStatus", source = "credentialStatus")
        @Mapping(target = "description", source = "integrationInstanceConfiguration.integration.description")
        @Mapping(target = "enabled", source = "enabled")
        @Mapping(target = "icon", source = "integrationInstanceConfiguration.integration.icon")
        @Mapping(target = "id", source = "integrationInstanceConfiguration.integrationId")
        @Mapping(target = "integrationVersion", source = "integrationInstanceConfiguration.integrationVersion")
        @Mapping(
            target = "multipleInstances", source = "integrationInstanceConfiguration.integration.multipleInstances")
        @Mapping(target = "title", source = "integrationInstanceConfiguration.integration.title")
        IntegrationBasicModel convert(ConnectedUserIntegrationDTO connectedUserIntegrationDTO);
    }

    @Mapper(
        config = EmbeddedConfigurationPublicMapperSpringConfig.class)
    interface ConnectedUserIntegrationToIntegrationMapper
        extends Converter<ConnectedUserIntegrationDTO, IntegrationModel> {

        @Override
        @Mapping(
            target = "multipleInstances", source = "integrationInstanceConfiguration.integration.multipleInstances")
        @Mapping(target = "componentName", source = "integrationInstanceConfiguration.integration.componentName")
        @Mapping(target = "description", source = "integrationInstanceConfiguration.integration.description")
        @Mapping(target = "icon", source = "integrationInstanceConfiguration.integration.icon")
        @Mapping(target = "id", source = "integrationInstanceConfiguration.integrationId")
        @Mapping(target = "integrationVersion", source = "integrationInstanceConfiguration.integrationVersion")
        @Mapping(target = "title", source = "integrationInstanceConfiguration.integration.title")
        @Mapping(
            target = "workflows", source = "integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows")
        IntegrationModel convert(ConnectedUserIntegrationDTO connectedUserIntegrationDTO);

        @Mapping(target = "definition", source = "workflow.definition")
        @Mapping(target = "description", source = "workflow.description")
        @Mapping(target = "inputs", source = "workflow.inputs")
        @Mapping(target = "label", source = "workflow.label")
        @Mapping(target = "workflowVersion", ignore = true)
        WorkflowModel map(IntegrationInstanceConfigurationWorkflowDTO integrationInstanceConfigurationWorkflowDTO);

        @Mapping(target = "authorizationUrl", source = "oAuth2.oAuth2AuthorizationParameters.authorizationUrl")
        @Mapping(target = "clientId", source = "oAuth2.oAuth2AuthorizationParameters.clientId")
        @Mapping(target = "extraQueryParameters", source = "oAuth2.oAuth2AuthorizationParameters.extraQueryParameters")
        @Mapping(target = "scopes", source = "oAuth2.oAuth2AuthorizationParameters.scopes")
        OAuth2Model map(OAuth2 oAuth2);
    }
}
