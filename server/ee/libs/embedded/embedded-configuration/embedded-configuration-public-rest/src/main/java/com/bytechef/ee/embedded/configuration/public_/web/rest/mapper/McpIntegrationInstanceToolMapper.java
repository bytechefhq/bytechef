/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.config.EmbeddedConfigurationPublicMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.McpIntegrationInstanceToolModel;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceTool;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class McpIntegrationInstanceToolMapper {

    @Mapper(config = EmbeddedConfigurationPublicMapperSpringConfig.class)
    public interface McpIntegrationInstanceToolToMcpIntegrationInstanceToolModelMapper
        extends Converter<McpIntegrationInstanceTool, McpIntegrationInstanceToolModel> {

        @Override
        McpIntegrationInstanceToolModel convert(McpIntegrationInstanceTool mcpIntegrationInstanceTool);
    }
}
