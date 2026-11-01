/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.remote.grpc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnClass(name = ToolInvocationLogGrpcServerConfiguration.TOOL_INVOCATION_LOG_EVENT_LISTENER)
public class ToolInvocationLogGrpcServerConfiguration {

    public static final String TOOL_INVOCATION_LOG_EVENT_LISTENER =
        "com.bytechef.ee.platform.tool.invocation.log.ToolInvocationLogEventListener";

    @Bean
    ToolInvocationLogRemoteServiceGrpcServer toolInvocationLogRemoteServiceGrpcServer(
        ApplicationEventPublisher applicationEventPublisher, ObjectMapper objectMapper) {

        return new ToolInvocationLogRemoteServiceGrpcServer(
            applicationEventPublisher, new ToolExecutionEventProtoMapper(objectMapper));
    }
}
