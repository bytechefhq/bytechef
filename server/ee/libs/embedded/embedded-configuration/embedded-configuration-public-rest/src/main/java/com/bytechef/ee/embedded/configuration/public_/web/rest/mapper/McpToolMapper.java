/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.config.EmbeddedConfigurationPublicMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.McpToolModel;
import com.bytechef.platform.mcp.domain.McpTool;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class McpToolMapper {

    @Mapper(config = EmbeddedConfigurationPublicMapperSpringConfig.class)
    public interface McpToolToMcpToolModelMapper extends Converter<McpTool, McpToolModel> {

        @Override
        @Mapping(target = "description", ignore = true)
        McpToolModel convert(McpTool mcpTool);
    }
}
