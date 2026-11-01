/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.coordinator;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.BindableService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest
@EnableAutoConfiguration
public class CoordinatorApplicationIntTest {

    @Autowired
    private ObjectProvider<AuditEventRepository> auditEventRepositoryProvider;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Test
    void testAuditEventsAreSentToExecutionApp() {
        AuditEventRepository auditEventRepository = auditEventRepositoryProvider.getIfAvailable();

        assertThat(auditEventRepository).isNotNull();

        Class<?> auditEventRepositoryClass = AopUtils.getTargetClass(auditEventRepository);

        assertThat(auditEventRepositoryClass.getName()).isEqualTo(
            "com.bytechef.ee.platform.audit.remote.client.RemoteAuditEventRepositoryClient");
    }

    @Test
    void testContextLoads() {
    }

    @Test
    void testSharedAndAppConfigurationLoad() {
        assertThat(environment.getProperty("spring.task.execution.thread-name-prefix"))
            .isEqualTo("coordinator-app-task-");
        assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
    }

    @Test
    void testTaskDispatcherGrpcServicesAreServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames)
            .contains("bytechef.platform.workflow.task.dispatcher.remote.v1.TaskDispatcherDefinitionRemoteService");
        assertThat(serviceNames).doesNotContain(
            "bytechef.platform.configuration.remote.v1.WorkflowRemoteService",
            "bytechef.automation.configuration.remote.v1.ProjectRemoteService",
            "bytechef.atlas.execution.remote.v1.JobRemoteService",
            "bytechef.platform.connection.remote.v1.ConnectionRemoteService",
            "bytechef.platform.component.remote.v1.ComponentDefinitionRemoteService",
            "bytechef.platform.workflow.worker.remote.v1.TaskHandlerRemoteService");
    }
}
