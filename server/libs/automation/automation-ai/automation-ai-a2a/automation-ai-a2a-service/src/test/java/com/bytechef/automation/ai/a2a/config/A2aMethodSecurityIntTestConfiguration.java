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

package com.bytechef.automation.ai.a2a.config;

import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacade;
import com.bytechef.automation.ai.a2a.facade.A2aProjectFacadeImpl;
import com.bytechef.automation.ai.a2a.repository.A2aProjectWorkflowRepository;
import com.bytechef.automation.ai.a2a.repository.A2aServerRepository;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowServiceImpl;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.ai.a2a.service.A2aServerServiceImpl;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * @author Ivica Cardic
 */
@SpringBootConfiguration
@EnableMethodSecurity
@ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
public class A2aMethodSecurityIntTestConfiguration {

    @Bean
    A2aProjectFacade a2aProjectFacade(
        A2aProjectService a2aProjectService, A2aProjectWorkflowService a2aProjectWorkflowService,
        A2aServerService a2aServerService, ComponentConnectionFacade componentConnectionFacade,
        ConnectionService connectionService, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        return new A2aProjectFacadeImpl(
            a2aProjectService, a2aProjectWorkflowService, a2aServerService, componentConnectionFacade,
            connectionService, projectDeploymentFacade, projectDeploymentService, projectDeploymentWorkflowService,
            projectService, projectWorkflowService, workflowService, workflowTestConfigurationService);
    }

    @Bean
    A2aProjectService a2aProjectService() {
        return mock(A2aProjectService.class);
    }

    @Bean
    A2aProjectWorkflowRepository a2aProjectWorkflowRepository() {
        return mock(A2aProjectWorkflowRepository.class);
    }

    @Bean
    A2aProjectWorkflowService a2aProjectWorkflowService(A2aProjectWorkflowRepository a2aProjectWorkflowRepository) {
        return new A2aProjectWorkflowServiceImpl(a2aProjectWorkflowRepository);
    }

    @Bean
    A2aServerRepository a2aServerRepository() {
        return mock(A2aServerRepository.class);
    }

    @Bean
    A2aServerService a2aServerService(A2aServerRepository a2aServerRepository) {
        return new A2aServerServiceImpl(a2aServerRepository);
    }

    @Bean
    ComponentConnectionFacade componentConnectionFacade() {
        return mock(ComponentConnectionFacade.class);
    }

    @Bean
    ConnectionService connectionService() {
        return mock(ConnectionService.class);
    }

    @Bean
    PermissionService permissionService() {
        return mock(PermissionService.class);
    }

    @Bean
    ProjectDeploymentFacade projectDeploymentFacade() {
        return mock(ProjectDeploymentFacade.class);
    }

    @Bean
    ProjectDeploymentService projectDeploymentService() {
        return mock(ProjectDeploymentService.class);
    }

    @Bean
    ProjectDeploymentWorkflowService projectDeploymentWorkflowService() {
        return mock(ProjectDeploymentWorkflowService.class);
    }

    @Bean
    ProjectService projectService() {
        return mock(ProjectService.class);
    }

    @Bean
    ProjectWorkflowService projectWorkflowService() {
        return mock(ProjectWorkflowService.class);
    }

    @Bean
    WorkflowService workflowService() {
        return mock(WorkflowService.class);
    }

    @Bean
    WorkflowTestConfigurationService workflowTestConfigurationService() {
        return mock(WorkflowTestConfigurationService.class);
    }
}
