/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.config.EmbeddedConfigurationPublicMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.WorkflowModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(
    config = EmbeddedConfigurationPublicMapperSpringConfig.class, implementationName = "EmbeddedPublic<CLASS_NAME>Impl")
public interface IntegrationMapper extends Converter<IntegrationInstanceConfigurationDTO, IntegrationModel> {

    @Override
    @Mapping(target = "multipleInstances", source = "integration.multipleInstances")
    @Mapping(target = "componentName", source = "integration.componentName")
    @Mapping(target = "description", source = "integration.description")
    @Mapping(target = "icon", source = "integration.icon")
    @Mapping(target = "title", source = "integration.title")
    @Mapping(target = "workflows", source = "integrationInstanceConfigurationWorkflows")
    IntegrationModel convert(IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);

    @Mapping(target = "label", source = "workflow.label")
    @Mapping(target = "description", source = "workflow.description")
    WorkflowModel mapToWorkflowModel(
        IntegrationInstanceConfigurationWorkflowDTO integrationInstanceConfigurationWorkflowDTO);
}
