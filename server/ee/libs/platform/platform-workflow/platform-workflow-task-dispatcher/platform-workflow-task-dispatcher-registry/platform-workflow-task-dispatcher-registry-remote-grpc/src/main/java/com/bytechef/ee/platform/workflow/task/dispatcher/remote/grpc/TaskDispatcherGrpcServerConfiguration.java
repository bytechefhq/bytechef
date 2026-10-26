/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.task.dispatcher.remote.grpc;

import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
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
@ConditionalOnClass(name = "com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionServiceImpl")
public class TaskDispatcherGrpcServerConfiguration {

    @Bean
    TaskDispatcherDefinitionRemoteServiceGrpcServer taskDispatcherDefinitionRemoteServiceGrpcServer(
        TaskDispatcherDefinitionService taskDispatcherDefinitionService, ObjectMapper objectMapper) {

        return new TaskDispatcherDefinitionRemoteServiceGrpcServer(
            taskDispatcherDefinitionService, new TaskDispatcherProtoMapper(objectMapper));
    }
}
