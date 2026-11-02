/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.task.remote.grpc;

import com.bytechef.automation.task.facade.ApprovalTaskFacade;
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
@ConditionalOnClass(name = "com.bytechef.automation.task.facade.ApprovalTaskFacadeImpl")
public class ApprovalTaskGrpcServerConfiguration {

    @Bean
    ApprovalTaskFacadeRemoteServiceGrpcServer approvalTaskFacadeRemoteServiceGrpcServer(
        ApprovalTaskFacade approvalTaskFacade, ObjectMapper objectMapper) {

        return new ApprovalTaskFacadeRemoteServiceGrpcServer(
            approvalTaskFacade, new ApprovalTaskProtoMapper(objectMapper));
    }
}
