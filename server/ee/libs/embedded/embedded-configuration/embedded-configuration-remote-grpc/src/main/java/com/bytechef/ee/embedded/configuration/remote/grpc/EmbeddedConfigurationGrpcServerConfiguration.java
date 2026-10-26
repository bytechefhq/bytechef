/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.grpc;

import com.bytechef.automation.configuration.service.ConnectedUserProjectDeploymentResolver;
import com.bytechef.ee.embedded.configuration.facade.AutomationWorkflowProjectFacade;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
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
@ConditionalOnClass(name = "com.bytechef.ee.embedded.configuration.service.IntegrationServiceImpl")
public class EmbeddedConfigurationGrpcServerConfiguration {

    @Bean
    AppEventRemoteServiceGrpcServer appEventRemoteServiceGrpcServer(
        AppEventService appEventService, EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        return new AppEventRemoteServiceGrpcServer(appEventService, embeddedConfigurationProtoMapper);
    }

    @Bean
    AutomationWorkflowProjectFacadeRemoteServiceGrpcServer automationWorkflowProjectFacadeRemoteServiceGrpcServer(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        return new AutomationWorkflowProjectFacadeRemoteServiceGrpcServer(automationWorkflowProjectFacade,
            embeddedConfigurationProtoMapper);
    }

    @Bean
    ConnectedUserProjectDeploymentResolverRemoteServiceGrpcServer
        connectedUserProjectDeploymentResolverRemoteServiceGrpcServer(
            ConnectedUserProjectDeploymentResolver connectedUserProjectDeploymentResolver) {

        return new ConnectedUserProjectDeploymentResolverRemoteServiceGrpcServer(
            connectedUserProjectDeploymentResolver);
    }

    @Bean
    ConnectedUserProjectFacadeRemoteServiceGrpcServer connectedUserProjectFacadeRemoteServiceGrpcServer(
        ConnectedUserProjectFacade connectedUserProjectFacade) {

        return new ConnectedUserProjectFacadeRemoteServiceGrpcServer(connectedUserProjectFacade);
    }

    @Bean
    EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper(ObjectMapper objectMapper) {
        return new EmbeddedConfigurationProtoMapper(objectMapper);
    }

    @Bean
    IntegrationInstanceConfigurationRemoteServiceGrpcServer integrationInstanceConfigurationRemoteServiceGrpcServer(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        return new IntegrationInstanceConfigurationRemoteServiceGrpcServer(integrationInstanceConfigurationService,
            embeddedConfigurationProtoMapper);
    }

    @Bean
    IntegrationInstanceConfigurationWorkflowRemoteServiceGrpcServer
        integrationInstanceConfigurationWorkflowRemoteServiceGrpcServer(
            IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
            EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        return new IntegrationInstanceConfigurationWorkflowRemoteServiceGrpcServer(
            integrationInstanceConfigurationWorkflowService, embeddedConfigurationProtoMapper);
    }

    @Bean
    IntegrationInstanceWorkflowRemoteServiceGrpcServer integrationInstanceWorkflowRemoteServiceGrpcServer(
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        return new IntegrationInstanceWorkflowRemoteServiceGrpcServer(integrationInstanceWorkflowService,
            embeddedConfigurationProtoMapper);
    }

    @Bean
    IntegrationRemoteServiceGrpcServer integrationRemoteServiceGrpcServer(
        IntegrationService integrationService, EmbeddedConfigurationProtoMapper embeddedConfigurationProtoMapper) {

        return new IntegrationRemoteServiceGrpcServer(integrationService, embeddedConfigurationProtoMapper);
    }

    @Bean
    ConnectedUserProjectRemoteServiceGrpcServer connectedUserProjectRemoteServiceGrpcServer(
        ConnectedUserProjectService connectedUserProjectService) {

        return new ConnectedUserProjectRemoteServiceGrpcServer(connectedUserProjectService);
    }
}
