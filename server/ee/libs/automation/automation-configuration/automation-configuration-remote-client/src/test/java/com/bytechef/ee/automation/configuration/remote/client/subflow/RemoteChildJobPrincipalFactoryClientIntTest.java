/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.subflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationIntTestData;
import com.bytechef.ee.automation.configuration.remote.client.config.AutomationConfigurationRemoteClientIntTestConfiguration;
import com.bytechef.ee.automation.configuration.remote.grpc.AutomationConfigurationProtoMapper;
import com.bytechef.ee.automation.configuration.remote.grpc.ChildJobPrincipalFactoryRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = AutomationConfigurationRemoteClientIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true",
        "spring.liquibase.contexts=configuration,connection,execution,user"
    })
class RemoteChildJobPrincipalFactoryClientIntTest {

    @Autowired
    private AutomationConfigurationIntTestData automationConfigurationIntTestData;

    @Autowired
    private ChildJobPrincipalFactory childJobPrincipalFactory;

    @Autowired
    private JobFacade jobFacade;

    @Autowired
    private JobService jobService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrincipalJobFacade principalJobFacade;

    @Autowired
    private PrincipalJobService principalJobService;

    private ManagedChannel managedChannel;
    private RemoteChildJobPrincipalFactoryClient remoteChildJobPrincipalFactoryClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new ChildJobPrincipalFactoryRemoteServiceGrpcServer(
                        childJobPrincipalFactory, new AutomationConfigurationProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteChildJobPrincipalFactoryClient = new RemoteChildJobPrincipalFactoryClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        automationConfigurationIntTestData.deleteAll();
    }

    @Test
    void testCreateChildJobStoresTheJobUnderTheParentPrincipal() {
        Workspace workspace = automationConfigurationIntTestData.createWorkspace("workspace");

        Project project = automationConfigurationIntTestData.createProject("Live Project", workspace.getId());

        ProjectDeployment projectDeployment = automationConfigurationIntTestData.createProjectDeployment(
            "Live Deployment", project.getId(), 1, Environment.PRODUCTION, List.of());

        Workflow parentWorkflow = automationConfigurationIntTestData.createWorkflow("Parent");
        Workflow childWorkflow = automationConfigurationIntTestData.createWorkflow("Child");

        long parentJobId = principalJobFacade.createJob(
            new JobParametersDTO(parentWorkflow.getId()), projectDeployment.getId(), PlatformType.AUTOMATION);

        long childJobId = remoteChildJobPrincipalFactoryClient.createChildJob(
            parentJobId,
            new JobParametersDTO(
                childWorkflow.getId(), Map.of("id", 9_007_199_254_740_993L, "name", "child"),
                Map.of("source", "subflow")));

        assertThat(childJobId).isNotEqualTo(parentJobId);

        Job childJob = jobService.getJob(childJobId);

        assertThat(childJob.getWorkflowId()).isEqualTo(childWorkflow.getId());
        assertThat(childJob.getLabel()).isEqualTo("Child");

        Map<String, ?> inputs = childJob.getInputs();

        assertThat(inputs).isEqualTo(Map.of("id", 9_007_199_254_740_993L, "name", "child"));
        assertThat(inputs.get("id")).isInstanceOf(Long.class);
        assertThat(childJob.getMetadata("source")).isEqualTo("subflow");
        assertThat(principalJobService.fetchJobPrincipalId(childJobId, PlatformType.AUTOMATION))
            .contains(projectDeployment.getId());
    }

    @Test
    void testCreateChildJobOfAParentWithoutAPrincipalStoresAJobWithoutAPrincipal() {
        Workflow parentWorkflow = automationConfigurationIntTestData.createWorkflow("Parent");
        Workflow childWorkflow = automationConfigurationIntTestData.createWorkflow("Child");

        long parentJobId = jobFacade.createJob(new JobParametersDTO(parentWorkflow.getId()));

        long childJobId = remoteChildJobPrincipalFactoryClient.createChildJob(
            parentJobId, new JobParametersDTO(childWorkflow.getId()));

        Job childJob = jobService.getJob(childJobId);

        assertThat(childJob.getWorkflowId()).isEqualTo(childWorkflow.getId());
        assertThat(principalJobService.fetchJobPrincipalId(childJobId, PlatformType.AUTOMATION)).isEmpty();
    }

    @Test
    void testCreatePrincipalLinkedJobOfAReferenceWithoutAPrincipalFails() {
        Workflow referenceWorkflow = automationConfigurationIntTestData.createWorkflow("Reference");
        Workflow linkedWorkflow = automationConfigurationIntTestData.createWorkflow("Linked");

        long referenceJobId = jobFacade.createJob(new JobParametersDTO(referenceWorkflow.getId()));

        assertThatThrownBy(() -> remoteChildJobPrincipalFactoryClient.createPrincipalLinkedJob(
            referenceJobId, new JobParametersDTO(linkedWorkflow.getId())))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("no principal found for reference job " + referenceJobId);
    }
}
