/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.remote.grpc;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
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
@ConditionalOnClass(name = "com.bytechef.platform.configuration.service.WorkflowTestConfigurationServiceImpl")
public class PlatformConfigurationGrpcServerConfiguration {

    @Bean
    ComponentConnectionFacadeRemoteServiceGrpcServer componentConnectionFacadeRemoteServiceGrpcServer(
        ComponentConnectionFacade componentConnectionFacade,
        PlatformConfigurationProtoMapper platformConfigurationProtoMapper) {

        return new ComponentConnectionFacadeRemoteServiceGrpcServer(
            componentConnectionFacade, platformConfigurationProtoMapper);
    }

    @Bean
    PlatformConfigurationProtoMapper platformConfigurationProtoMapper(ObjectMapper objectMapper) {
        return new PlatformConfigurationProtoMapper(objectMapper);
    }

    @Bean
    WorkflowRemoteServiceGrpcServer workflowRemoteServiceGrpcServer(
        WorkflowService workflowService, PlatformConfigurationProtoMapper platformConfigurationProtoMapper) {

        return new WorkflowRemoteServiceGrpcServer(workflowService, platformConfigurationProtoMapper);
    }

    @Bean
    WorkflowTestConfigurationRemoteServiceGrpcServer workflowTestConfigurationRemoteServiceGrpcServer(
        WorkflowTestConfigurationService workflowTestConfigurationService,
        PlatformConfigurationProtoMapper platformConfigurationProtoMapper) {

        return new WorkflowTestConfigurationRemoteServiceGrpcServer(
            workflowTestConfigurationService, platformConfigurationProtoMapper);
    }
}
