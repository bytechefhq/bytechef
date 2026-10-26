/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.grpc;

import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
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
@ConditionalOnClass(name = "com.bytechef.platform.component.service.ComponentDefinitionServiceImpl")
public class ComponentGrpcServerConfiguration {

    @Bean
    ActionDefinitionRemoteServiceGrpcServer actionDefinitionRemoteServiceGrpcServer(
        ActionDefinitionService actionDefinitionService, ComponentProtoMapper componentProtoMapper) {

        return new ActionDefinitionRemoteServiceGrpcServer(actionDefinitionService, componentProtoMapper);
    }

    @Bean
    ComponentDefinitionRemoteServiceGrpcServer componentDefinitionRemoteServiceGrpcServer(
        ComponentDefinitionService componentDefinitionService, ComponentProtoMapper componentProtoMapper) {

        return new ComponentDefinitionRemoteServiceGrpcServer(componentDefinitionService, componentProtoMapper);
    }

    @Bean
    ComponentProtoMapper componentProtoMapper(ObjectMapper objectMapper) {
        return new ComponentProtoMapper(objectMapper);
    }

    @Bean
    ConnectionDefinitionRemoteServiceGrpcServer connectionDefinitionRemoteServiceGrpcServer(
        ConnectionDefinitionService connectionDefinitionService, ComponentProtoMapper componentProtoMapper) {

        return new ConnectionDefinitionRemoteServiceGrpcServer(connectionDefinitionService, componentProtoMapper);
    }

    @Bean
    TriggerDefinitionRemoteServiceGrpcServer triggerDefinitionRemoteServiceGrpcServer(
        TriggerDefinitionService triggerDefinitionService, ComponentProtoMapper componentProtoMapper) {

        return new TriggerDefinitionRemoteServiceGrpcServer(triggerDefinitionService, componentProtoMapper);
    }
}
