/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.remote.grpc;

import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
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
@ConditionalOnClass(
    name = "com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacadeImpl")
public class ProjectWorkflowExecutionGrpcServerConfiguration {

    @Bean
    ProjectWorkflowExecutionRemoteServiceGrpcServer projectWorkflowExecutionRemoteServiceGrpcServer(
        ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade, ObjectMapper objectMapper) {

        return new ProjectWorkflowExecutionRemoteServiceGrpcServer(
            projectWorkflowExecutionFacade, new WorkflowExecutionProtoMapper(objectMapper));
    }
}
