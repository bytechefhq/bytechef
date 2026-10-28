/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.remote.grpc;

import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnClass(
    name = "com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerServiceImpl")
public class CodeWorkflowContainerGrpcServerConfiguration {

    @Bean
    CodeWorkflowContainerRemoteServiceGrpcServer codeWorkflowContainerRemoteServiceGrpcServer(
        CodeWorkflowContainerService codeWorkflowContainerService) {

        return new CodeWorkflowContainerRemoteServiceGrpcServer(
            codeWorkflowContainerService, new CodeWorkflowContainerProtoMapper());
    }
}
