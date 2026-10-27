/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowResolver;
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
    ChildJobPrincipalFactoryRemoteServiceGrpcServer childJobPrincipalFactoryRemoteServiceGrpcServer(
        ChildJobPrincipalFactory childJobPrincipalFactory,
        AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        return new ChildJobPrincipalFactoryRemoteServiceGrpcServer(
            childJobPrincipalFactory, automationConfigurationProtoMapper);
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
    ProjectFacadeRemoteServiceGrpcServer projectFacadeRemoteServiceGrpcServer(
        ProjectFacade projectFacade, AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        return new ProjectFacadeRemoteServiceGrpcServer(projectFacade, automationConfigurationProtoMapper);
    }

    @Bean
    ProjectRemoteServiceGrpcServer projectRemoteServiceGrpcServer(
        ProjectService projectService, AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        return new ProjectRemoteServiceGrpcServer(projectService, automationConfigurationProtoMapper);
    }

    @Bean
    ProjectWorkflowRemoteServiceGrpcServer projectWorkflowRemoteServiceGrpcServer(
        ProjectWorkflowService projectWorkflowService,
        AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        return new ProjectWorkflowRemoteServiceGrpcServer(projectWorkflowService, automationConfigurationProtoMapper);
    }

    @Bean
    SubflowResolverRemoteServiceGrpcServer subflowResolverRemoteServiceGrpcServer(SubflowResolver subflowResolver) {
        return new SubflowResolverRemoteServiceGrpcServer(subflowResolver);
    }
}
