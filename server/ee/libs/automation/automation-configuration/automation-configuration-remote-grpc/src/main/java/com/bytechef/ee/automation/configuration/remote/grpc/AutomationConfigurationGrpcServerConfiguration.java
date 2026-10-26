/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.automation.configuration.service.ProjectServiceImpl")
public class AutomationConfigurationGrpcServerConfiguration {

    @Bean
    AutomationConfigurationProtoMapper automationConfigurationProtoMapper(ObjectMapper objectMapper) {
        return new AutomationConfigurationProtoMapper(objectMapper);
    }

    @Bean
    ProjectDeploymentRemoteServiceGrpcServer projectDeploymentRemoteServiceGrpcServer(
        ProjectDeploymentService projectDeploymentService,
        AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        return new ProjectDeploymentRemoteServiceGrpcServer(
            projectDeploymentService, automationConfigurationProtoMapper);
    }

    @Bean
    ProjectDeploymentWorkflowRemoteServiceGrpcServer projectDeploymentWorkflowRemoteServiceGrpcServer(
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        return new ProjectDeploymentWorkflowRemoteServiceGrpcServer(
            projectDeploymentWorkflowService, automationConfigurationProtoMapper);
    }

    @Bean
    ProjectRemoteServiceGrpcServer projectRemoteServiceGrpcServer(
        ProjectService projectService, AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        return new ProjectRemoteServiceGrpcServer(projectService, automationConfigurationProtoMapper);
    }
}
