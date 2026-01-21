/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.mcp.web.graphql.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.mcp.facade.McpServerFacade;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.graphql.GraphQLScalarTypes;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

/**
 * @author Ivica Cardic
 */
@Configuration
public class McpGraphQlTestConfiguration {

    @Bean
    @Primary
    public ApplicationProperties applicationProperties() {
        ApplicationProperties properties = Mockito.mock(ApplicationProperties.class);

        Mockito.when(properties.getPublicUrl())
            .thenReturn("http://localhost:8080");

        return properties;
    }

    @Bean
    @Primary
    public McpServerFacade mcpServerFacade() {
        return Mockito.mock(McpServerFacade.class);
    }

    @Bean
    @Primary
    public McpServerService mcpServerService() {
        return Mockito.mock(McpServerService.class);
    }

    @Bean
    @Primary
    public McpComponentService mcpComponentService() {
        return Mockito.mock(McpComponentService.class);
    }

    @Bean
    @Primary
    public McpToolService mcpToolService() {
        return Mockito.mock(McpToolService.class);
    }

    @Bean
    @Primary
    public TagService tagService() {
        return Mockito.mock(TagService.class);
    }

    @Bean
    RuntimeWiringConfigurer longScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.longScalar());
    }

    @Bean
    RuntimeWiringConfigurer mapScalarWiringConfigurer() {
        return wiringBuilder -> wiringBuilder.scalar(GraphQLScalarTypes.mapScalar());
    }

}
