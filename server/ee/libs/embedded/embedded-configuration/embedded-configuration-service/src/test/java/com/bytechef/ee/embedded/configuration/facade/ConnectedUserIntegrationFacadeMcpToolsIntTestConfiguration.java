/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionServiceImpl;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpComponentServiceImpl;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpServerServiceImpl;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.mcp.service.McpToolServiceImpl;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class, McpComponentServiceImpl.class,
    McpServerServiceImpl.class, McpToolServiceImpl.class
})
@Configuration
public class ConnectedUserIntegrationFacadeMcpToolsIntTestConfiguration {

    static final String COMPONENT_NAME = "mcpToolsTest";

    @Bean
    ClusterElementDefinitionService clusterElementDefinitionService() {
        ActionDefinition getEmailActionDefinition = action("getEmail")
            .description("Get an email");
        ActionDefinition sendEmailActionDefinition = action("sendEmail")
            .title("Send Email")
            .description("Send an email");

        ComponentHandler componentHandler = () -> component(COMPONENT_NAME)
            .title("MCP Tools Test")
            .actions(getEmailActionDefinition, sendEmailActionDefinition)
            .clusterElements(tool(getEmailActionDefinition), tool(sendEmailActionDefinition));

        ApplicationProperties applicationProperties = new ApplicationProperties();
        ApplicationProperties.Component component = new ApplicationProperties.Component();

        component.setRegistry(new ApplicationProperties.Component.Registry());
        applicationProperties.setComponent(component);

        ComponentDefinitionRegistry componentDefinitionRegistry = new ComponentDefinitionRegistry(
            applicationProperties, List.of(componentHandler), List::of, List.of());

        return new ClusterElementDefinitionServiceImpl(componentDefinitionRegistry, null);
    }

    @Bean
    ConnectedUserIntegrationFacadeImpl connectedUserIntegrationFacade(
        ClusterElementDefinitionService clusterElementDefinitionService, McpComponentService mcpComponentService,
        McpServerService mcpServerService, McpToolService mcpToolService) {

        return new ConnectedUserIntegrationFacadeImpl(
            clusterElementDefinitionService, null, null, null, null, null, null, null, null, null, null,
            mcpComponentService, null, null, null, mcpServerService, mcpToolService, null, null, null, null, null);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class McpToolsIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {

        private final ObjectMapper objectMapper;

        @SuppressFBWarnings("EI2")
        public McpToolsIntTestJdbcConfiguration(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        protected List<?> userConverters() {
            return List.of(
                new MapWrapperToStringConverter(objectMapper), new StringToMapWrapperConverter(objectMapper));
        }
    }
}
