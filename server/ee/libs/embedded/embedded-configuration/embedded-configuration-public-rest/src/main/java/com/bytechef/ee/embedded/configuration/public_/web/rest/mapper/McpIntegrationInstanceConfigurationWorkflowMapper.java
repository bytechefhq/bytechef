/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.config.EmbeddedConfigurationPublicMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationWorkflowModel;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class McpIntegrationInstanceConfigurationWorkflowMapper {

    @Mapper(config = EmbeddedConfigurationPublicMapperSpringConfig.class)
    public interface McpIntegrationInstanceConfigurationWorkflowToIntegrationWorkflowModelMapper
        extends Converter<McpIntegrationInstanceConfigurationWorkflow, IntegrationWorkflowModel> {

        @Override
        @Mapping(target = "description", ignore = true)
        @Mapping(target = "inputs", ignore = true)
        @Mapping(target = "label", ignore = true)
        @Mapping(target = "workflowUuid", ignore = true)
        IntegrationWorkflowModel
            convert(McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow);
    }
}
