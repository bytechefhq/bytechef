/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.repository.jdbc.JdbcContextRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.workflow.execution.remote.client.config.PlatformWorkflowExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PrincipalJobFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = PlatformWorkflowExecutionRemoteClientIntTestConfiguration.class,
    properties = {
        "bytechef.approval.signed-token.secret=Ynl0ZWNoZWYtYXBwcm92YWwtdG9rZW4tdGVzdC1zZWNyZXQh",
        "bytechef.workflow.repository.jdbc.enabled=true", "spring.quartz.auto-startup=false"
    })
@Import(PostgreSQLContainerConfiguration.class)
@RecordApplicationEvents
class RemotePrincipalJobFacadeClientIntTest {

    private static final long PRINCIPAL_ID = 1051L;

    private static final String WORKFLOW_DEFINITION = """
        {
          "label": "Process orders",
          "inputs": [
            {"name": "orderId", "type": "integer", "required": true}
          ],
          "tasks": [
            {"name": "setValue", "type": "var/v1/set", "parameters": {"type": "STRING", "value": "x"}}
          ]
        }
        """;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private JdbcContextRepository contextRepository;

    @Autowired
    private ContextService contextService;

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrincipalJobFacade principalJobFacade;

    @Autowired
    private PrincipalJobRepository principalJobRepository;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private JdbcWorkflowRepository workflowRepository;

    @Autowired
    private WorkflowService workflowService;

    private ManagedChannel managedChannel;
    private RemotePrincipalJobFacadeClient remotePrincipalJobFacadeClient;
    private Server server;
    private Workflow workflow;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new PrincipalJobFacadeRemoteServiceGrpcServer(
                        principalJobFacade, new PlatformWorkflowExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Remote remote = applicationProperties.getRemote();

        ApplicationProperties.Remote.Grpc grpc = remote.getGrpc();

        grpc.setExecutionDeadline(Duration.ofSeconds(30));

        remotePrincipalJobFacadeClient = new RemotePrincipalJobFacadeClient(
            applicationProperties, new FixedGrpcChannels(managedChannel), objectMapper);

        workflow = workflowService.create(WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        principalJobRepository.deleteAll();
        jobRepository.deleteAll();
        contextRepository.deleteAll();
        workflowRepository.deleteAll();
    }

    @Test
    void testCreateJobStoresTheJobAndItsPrincipal() {
        long jobId = remotePrincipalJobFacadeClient.createJob(
            new JobParametersDTO(workflow.getId(), Map.of("orderId", 9_007_199_254_740_993L)), PRINCIPAL_ID,
            PlatformType.AUTOMATION);

        Job job = jobService.getJob(jobId);

        assertThat(job.getStatus()).isEqualTo(Job.Status.CREATED);
        assertThat(job.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(job.getInputs()).isEqualTo(Map.of("orderId", 9_007_199_254_740_993L));
        assertThat(principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION)).contains(PRINCIPAL_ID);
        assertThat(taskFileStorage.readContextValue(contextService.peek(jobId, Context.Classname.JOB)))
            .isEqualTo(Map.of("orderId", 9_007_199_254_740_993L));
        assertThat(applicationEvents.stream(StartJobEvent.class))
            .extracting(StartJobEvent::getJobId)
            .containsExactly(jobId);
    }

    @Test
    void testCreateJobWithoutARequiredInputFails() {
        assertThatThrownBy(
            () -> remotePrincipalJobFacadeClient.createJob(
                new JobParametersDTO(workflow.getId(), Map.of()), PRINCIPAL_ID, PlatformType.AUTOMATION))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("Missing required param: orderId")
                    .extracting(exception -> ((StatusRuntimeException) exception).getStatus()
                        .getCode())
                    .isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(jobRepository.count()).isZero();
        assertThat(principalJobRepository.count()).isZero();
    }

    @Test
    void testCreateSyncJobReturnsTheStoredJob() {
        Job job = remotePrincipalJobFacadeClient.createSyncJob(
            new JobParametersDTO(
                workflow.getId(), Map.of("orderId", 9_007_199_254_740_993L), Map.of("source", "test")),
            PRINCIPAL_ID, PlatformType.EMBEDDED);

        long jobId = Objects.requireNonNull(job.getId());

        assertThat(job.getStatus()).isEqualTo(Job.Status.CREATED);
        assertThat(job.getLabel()).isEqualTo("Process orders");
        assertThat(job.getInputs()).isEqualTo(Map.of("orderId", 9_007_199_254_740_993L));
        assertThat(job.getMetadata()).isEqualTo(Map.of("source", "test"));

        Job storedJob = jobService.getJob(jobId);

        assertThat(storedJob.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(principalJobService.fetchJobPrincipalId(jobId, PlatformType.EMBEDDED))
            .contains(PRINCIPAL_ID);
        assertThat(applicationEvents.stream(StartJobEvent.class)).isEmpty();
    }

    @Test
    void testCreateChildJobInheritsThePrincipalOfItsParent() {
        long parentJobId = remotePrincipalJobFacadeClient.createJob(
            new JobParametersDTO(workflow.getId(), Map.of("orderId", 1)), PRINCIPAL_ID, PlatformType.EMBEDDED);

        long childJobId = remotePrincipalJobFacadeClient.createChildJob(
            parentJobId, new JobParametersDTO(workflow.getId(), Map.of("orderId", 2)), PlatformType.EMBEDDED);

        assertThat(childJobId).isNotEqualTo(parentJobId);
        assertThat(jobService.getJob(childJobId)
            .getInputs()).isEqualTo(Map.of("orderId", 2));
        assertThat(principalJobService.fetchJobPrincipalId(childJobId, PlatformType.EMBEDDED)).contains(PRINCIPAL_ID);
    }

    @Test
    void testCreatePrincipalLinkedJobOfAReferenceWithoutPrincipalFails() {
        long referenceJobId = remotePrincipalJobFacadeClient.createJob(
            new JobParametersDTO(workflow.getId(), Map.of("orderId", 1)), PRINCIPAL_ID, PlatformType.AUTOMATION);

        assertThatThrownBy(
            () -> remotePrincipalJobFacadeClient.createPrincipalLinkedJob(
                referenceJobId, new JobParametersDTO(workflow.getId(), Map.of("orderId", 2)), PlatformType.EMBEDDED))
                    .isInstanceOf(StatusRuntimeException.class)
                    .hasMessageContaining("no principal found for reference job " + referenceJobId);
    }

    @Test
    void testCreatePrincipalLinkedJobForAPrincipalStoresTheJobAndLinksThePrincipal() {
        long jobId = remotePrincipalJobFacadeClient.createPrincipalLinkedJob(
            new JobParametersDTO(workflow.getId(), Map.of("orderId", 3)), PRINCIPAL_ID, PlatformType.AUTOMATION);

        Job job = jobService.getJob(jobId);

        assertThat(job.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(job.getInputs()).isEqualTo(Map.of("orderId", 3));
        assertThat(principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION)).contains(PRINCIPAL_ID);
        assertThat(applicationEvents.stream(StartJobEvent.class))
            .extracting(StartJobEvent::getJobId)
            .containsExactly(jobId);
    }

    @Test
    void testCreateChildJobOfAParentWithoutPrincipalHasNoPrincipal() {
        long parentJobId = remotePrincipalJobFacadeClient.createJob(
            new JobParametersDTO(workflow.getId(), Map.of("orderId", 1)), PRINCIPAL_ID, PlatformType.AUTOMATION);

        long childJobId = remotePrincipalJobFacadeClient.createChildJob(
            parentJobId, new JobParametersDTO(workflow.getId(), Map.of("orderId", 2)), PlatformType.EMBEDDED);

        assertThat(jobService.fetchJob(childJobId)).isPresent();
        assertThat(principalJobService.fetchJobPrincipalId(childJobId, PlatformType.EMBEDDED)).isEmpty();
    }
}
