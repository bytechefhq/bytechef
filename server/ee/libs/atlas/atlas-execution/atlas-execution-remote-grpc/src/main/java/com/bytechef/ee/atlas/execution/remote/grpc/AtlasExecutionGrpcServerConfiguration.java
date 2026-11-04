/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskExecutionLockedUpdatesImpl;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository")
public class AtlasExecutionGrpcServerConfiguration {

    @Bean
    AtlasExecutionProtoMapper atlasExecutionProtoMapper(ObjectMapper objectMapper) {
        return new AtlasExecutionProtoMapper(objectMapper);
    }

    @Bean
    ContextRemoteServiceGrpcServer contextRemoteServiceGrpcServer(
        ContextService contextService, AtlasExecutionProtoMapper atlasExecutionProtoMapper) {

        return new ContextRemoteServiceGrpcServer(contextService, atlasExecutionProtoMapper);
    }

    @Bean
    CounterRemoteServiceGrpcServer counterRemoteServiceGrpcServer(CounterService counterService) {
        return new CounterRemoteServiceGrpcServer(counterService);
    }

    @Bean
    JobFacadeRemoteServiceGrpcServer jobFacadeRemoteServiceGrpcServer(
        JobFacade jobFacade, AtlasExecutionProtoMapper atlasExecutionProtoMapper) {

        return new JobFacadeRemoteServiceGrpcServer(jobFacade, atlasExecutionProtoMapper);
    }

    @Bean
    JobRemoteServiceGrpcServer jobRemoteServiceGrpcServer(
        JobService jobService, AtlasExecutionProtoMapper atlasExecutionProtoMapper) {

        return new JobRemoteServiceGrpcServer(jobService, atlasExecutionProtoMapper);
    }

    @Bean
    TaskExecutionLockedUpdatesRemoteServiceGrpcServer taskExecutionLockedUpdatesRemoteServiceGrpcServer(
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        AtlasExecutionProtoMapper atlasExecutionProtoMapper, ObjectMapper objectMapper) {

        return new TaskExecutionLockedUpdatesRemoteServiceGrpcServer(
            new TaskExecutionLockedUpdatesImpl(taskExecutionService, taskFileStorage), atlasExecutionProtoMapper,
            objectMapper);
    }

    @Bean
    TaskExecutionRemoteServiceGrpcServer taskExecutionRemoteServiceGrpcServer(
        TaskExecutionService taskExecutionService, AtlasExecutionProtoMapper atlasExecutionProtoMapper) {

        return new TaskExecutionRemoteServiceGrpcServer(taskExecutionService, atlasExecutionProtoMapper);
    }
}
