/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.platform.workflow.execution.service.TriggerExecutionServiceImpl")
public class PlatformWorkflowExecutionGrpcServerConfiguration {

    @Bean
    ConnectionLifecycleFacadeRemoteServiceGrpcServer connectionLifecycleFacadeRemoteServiceGrpcServer(
        ConnectionLifecycleFacade connectionLifecycleFacade,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        return new ConnectionLifecycleFacadeRemoteServiceGrpcServer(connectionLifecycleFacade,
            platformWorkflowExecutionProtoMapper);
    }

    @Bean
    JobResumeFacadeRemoteServiceGrpcServer jobResumeFacadeRemoteServiceGrpcServer(
        JobResumeFacade jobResumeFacade, PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        return new JobResumeFacadeRemoteServiceGrpcServer(jobResumeFacade, platformWorkflowExecutionProtoMapper);
    }

    @Bean
    PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper(ObjectMapper objectMapper) {
        return new PlatformWorkflowExecutionProtoMapper(objectMapper);
    }

    @Bean
    PrincipalJobFacadeRemoteServiceGrpcServer principalJobFacadeRemoteServiceGrpcServer(
        PrincipalJobFacade principalJobFacade,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        return new PrincipalJobFacadeRemoteServiceGrpcServer(principalJobFacade, platformWorkflowExecutionProtoMapper);
    }

    @Bean
    PrincipalJobRemoteServiceGrpcServer principalJobRemoteServiceGrpcServer(
        PrincipalJobService principalJobService,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        return new PrincipalJobRemoteServiceGrpcServer(principalJobService, platformWorkflowExecutionProtoMapper);
    }

    @Bean
    TriggerExecutionRemoteServiceGrpcServer triggerExecutionRemoteServiceGrpcServer(
        TriggerExecutionService triggerExecutionService,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        return new TriggerExecutionRemoteServiceGrpcServer(triggerExecutionService,
            platformWorkflowExecutionProtoMapper);
    }

    @Bean
    TriggerLifecycleFacadeRemoteServiceGrpcServer triggerLifecycleFacadeRemoteServiceGrpcServer(
        TriggerLifecycleFacade triggerLifecycleFacade,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        return new TriggerLifecycleFacadeRemoteServiceGrpcServer(triggerLifecycleFacade,
            platformWorkflowExecutionProtoMapper);
    }

    @Bean
    TriggerStateRemoteServiceGrpcServer triggerStateRemoteServiceGrpcServer(
        TriggerStateService triggerStateService,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        return new TriggerStateRemoteServiceGrpcServer(triggerStateService, platformWorkflowExecutionProtoMapper);
    }
}
