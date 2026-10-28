/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.worker;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.BindableService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkerApplication.class)
@Import(PostgreSQLContainerConfiguration.class)
public class WorkerApplicationIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    @Qualifier("componentDefinitionService")
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private Environment environment;

    @Test
    public void testContextLoads() {
    }

    @Test
    void testEnterpriseComponentsAreOffered() {
        List<String> componentNames = componentDefinitionService.getComponentDefinitions()
            .stream()
            .map(ComponentDefinition::getName)
            .toList();

        assertThat(componentNames).contains("apiPlatform", "appEvent", "request");
    }

    @Test
    void testDataStreamComponentIsOffered() {
        List<String> componentNames = componentDefinitionService.getComponentDefinitions()
            .stream()
            .map(ComponentDefinition::getName)
            .toList();

        assertThat(componentNames).contains("dataStream");
        assertThat(applicationContext.containsBean("dataStreamJob")).isTrue();
    }

    @Test
    public void testSharedAndAppConfigurationLoad() {
        assertThat(environment.getProperty("spring.task.execution.thread-name-prefix")).isEqualTo("worker-app-task-");
        assertThat(environment.getProperty("docs.documentation-url")).isEqualTo("https://docs.bytechef.io");
    }

    @Test
    void testComponentGrpcServicesAreServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains("bytechef.platform.component.remote.v1.ComponentDefinitionRemoteService",
            "bytechef.platform.component.remote.v1.ActionDefinitionRemoteService",
            "bytechef.platform.component.remote.v1.TriggerDefinitionRemoteService",
            "bytechef.platform.component.remote.v1.ConnectionDefinitionRemoteService",
            "bytechef.platform.component.remote.v1.ActionDefinitionFacadeRemoteService",
            "bytechef.platform.component.remote.v1.TriggerDefinitionFacadeRemoteService",
            "bytechef.platform.component.remote.v1.ClusterElementDefinitionRemoteService",
            "bytechef.platform.component.remote.v1.ClusterElementDefinitionFacadeRemoteService",
            "bytechef.platform.component.remote.v1.CodeEditorScriptInputFacadeRemoteService",
            "bytechef.platform.component.remote.v1.UnifiedApiDefinitionRemoteService",
            "bytechef.platform.component.remote.v1.ComponentDefinitionFacadeRemoteService");
        assertThat(serviceNames).doesNotContain(
            "bytechef.platform.configuration.remote.v1.WorkflowRemoteService",
            "bytechef.automation.configuration.remote.v1.ProjectRemoteService",
            "bytechef.atlas.execution.remote.v1.JobRemoteService",
            "bytechef.platform.connection.remote.v1.ConnectionRemoteService",
            "bytechef.platform.workflow.task.dispatcher.remote.v1.TaskDispatcherDefinitionRemoteService",
            "bytechef.platform.scheduler.remote.v1.TriggerSchedulerRemoteService");
    }

    @Test
    void testTaskHandlerGrpcServiceIsServed() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains(
            "bytechef.platform.workflow.worker.remote.v1.TaskHandlerRemoteService");
    }
}
