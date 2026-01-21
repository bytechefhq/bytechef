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

package com.bytechef.automation.mcp.web.graphql.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.mcp.facade.McpProjectFacade;
import com.bytechef.automation.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.mcp.service.McpProjectService;
import com.bytechef.automation.mcp.service.McpProjectWorkflowService;
import com.bytechef.config.ApplicationProperties;
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
public class AutomationMcpGraphQlTestConfiguration {

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
    public McpProjectFacade mcpProjectFacade() {
        return Mockito.mock(McpProjectFacade.class);
    }

    @Bean
    @Primary
    public McpProjectService mcpProjectService() {
        return Mockito.mock(McpProjectService.class);
    }

    @Bean
    @Primary
    public McpProjectWorkflowService mcpProjectWorkflowService() {
        return Mockito.mock(McpProjectWorkflowService.class);
    }

    @Bean
    @Primary
    public ProjectDeploymentService projectDeploymentService() {
        return Mockito.mock(ProjectDeploymentService.class);
    }

    @Bean
    @Primary
    public ProjectDeploymentWorkflowService projectDeploymentWorkflowService() {
        return Mockito.mock(ProjectDeploymentWorkflowService.class);
    }

    @Bean
    @Primary
    public ProjectService projectService() {
        return Mockito.mock(ProjectService.class);
    }

    @Bean
    @Primary
    public WorkflowService workflowService() {
        return Mockito.mock(WorkflowService.class);
    }

    @Bean
    @Primary
    public WorkspaceMcpServerFacade workspaceMcpServerFacade() {
        return Mockito.mock(WorkspaceMcpServerFacade.class);
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
