/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.grpc;

import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistry;
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
@ConditionalOnClass(name = "com.bytechef.atlas.worker.config.TaskHandlerRegistryConfiguration")
public class TaskHandlerGrpcServerConfiguration {

    @Bean
    TaskHandlerRemoteServiceGrpcServer taskHandlerRemoteServiceGrpcServer(
        TaskHandlerRegistry taskHandlerRegistry, ObjectMapper objectMapper) {

        return new TaskHandlerRemoteServiceGrpcServer(
            taskHandlerRegistry, new TaskExecutionProtoMapper(objectMapper));
    }
}
