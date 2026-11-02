/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.repository.jdbc.JdbcWorkflowRepository;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.event.StartJobEvent;
import com.bytechef.atlas.coordinator.event.StopJobEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.repository.jdbc.JdbcContextRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.ee.atlas.execution.remote.client.config.AtlasExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.JobFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
    classes = AtlasExecutionRemoteClientIntTestConfiguration.class,
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@RecordApplicationEvents
class RemoteJobFacadeClientIntTest {

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
    private JobFacade jobFacade;

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TaskFileStorage taskFileStorage;

    @Autowired
    private JdbcWorkflowRepository workflowRepository;

    @Autowired
    private WorkflowService workflowService;

    private ManagedChannel managedChannel;
    private RemoteJobFacadeClient remoteJobFacadeClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new JobFacadeRemoteServiceGrpcServer(jobFacade, new AtlasExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteJobFacadeClient = new RemoteJobFacadeClient(new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        for (Job job : jobRepository.findAll()) {
            if (job.getParentTaskExecutionId() != null) {
                jobRepository.delete(job);
            }
        }

        taskExecutionRepository.deleteAll();
        jobRepository.deleteAll();
        contextRepository.deleteAll();
        workflowRepository.deleteAll();
    }

    @Test
    void testCreateJobStoresTheJobAndItsContext() {
        Workflow workflow = workflowService.create(
            WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        long jobId = remoteJobFacadeClient.createJob(
            new JobParametersDTO(
                workflow.getId(), Map.of("orderId", 9_007_199_254_740_993L), Map.of("source", "test")));

        Job job = jobService.getJob(jobId);

        assertThat(job.getStatus()).isEqualTo(Job.Status.CREATED);
        assertThat(job.getLabel()).isEqualTo("Process orders");
        assertThat(job.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(job.getInputs()).isEqualTo(Map.of("orderId", 9_007_199_254_740_993L));
        assertThat(job.getMetadata()).isEqualTo(Map.of("source", "test"));
        assertThat(taskFileStorage.readContextValue(contextService.peek(jobId, Context.Classname.JOB)))
            .isEqualTo(Map.of("orderId", 9_007_199_254_740_993L));
        assertThat(applicationEvents.stream(StartJobEvent.class))
            .extracting(StartJobEvent::getJobId)
            .containsExactly(jobId);
    }

    @Test
    void testCreateJobWithoutARequiredInputFails() {
        Workflow workflow = workflowService.create(
            WORKFLOW_DEFINITION, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        assertThatThrownBy(
            () -> remoteJobFacadeClient.createJob(new JobParametersDTO(workflow.getId(), Map.of())))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("Missing required param: orderId")
                .extracting(exception -> ((StatusRuntimeException) exception).getStatus()
                    .getCode())
                .isEqualTo(Status.Code.INVALID_ARGUMENT);
        assertThat(jobRepository.findAll()).isEmpty();
        assertThat(applicationEvents.stream(StartJobEvent.class)).isEmpty();
    }

    @Test
    void testDeleteJobDeletesChildJobsAndTaskExecutions() {
        long parentJobId = saveJob(Job.Status.COMPLETED, null);
        long parentTaskExecutionId = saveTaskExecution(parentJobId);
        long childJobId = saveJob(Job.Status.COMPLETED, parentTaskExecutionId);
        long childTaskExecutionId = saveTaskExecution(childJobId);
        long otherJobId = saveJob(Job.Status.COMPLETED, null);

        remoteJobFacadeClient.deleteJob(parentJobId);

        assertThat(jobRepository.findById(parentJobId)).isEmpty();
        assertThat(jobRepository.findById(childJobId)).isEmpty();
        assertThat(taskExecutionRepository.findById(parentTaskExecutionId)).isEmpty();
        assertThat(taskExecutionRepository.findById(childTaskExecutionId)).isEmpty();
        assertThat(jobRepository.findById(otherJobId)).isPresent();
    }

    @Test
    void testStopJobPublishesTheStopEvent() {
        long jobId = saveJob(Job.Status.STARTED, null);

        remoteJobFacadeClient.stopJob(jobId);

        assertThat(applicationEvents.stream(StopJobEvent.class))
            .extracting(StopJobEvent::getJobId)
            .containsExactly(jobId);
    }

    @Test
    void testResumeApprovalRecordsTheDecisionAndCompletesTheWaitingTask() {
        long jobId = saveJob(Job.Status.STOPPED, null);

        contextService.push(
            jobId, Context.Classname.JOB,
            taskFileStorage.storeContextValue(jobId, Context.Classname.JOB, Map.of("orderId", 5)));

        long taskExecutionId = saveTaskExecution(jobId);

        remoteJobFacadeClient.resumeApproval(jobId, "approval-uuid", true);

        assertThat(jobService.getJob(jobId)
            .getStatus()).isEqualTo(Job.Status.STARTED);

        Map<String, ?> jobContext = taskFileStorage.readContextValue(
            contextService.peek(jobId, Context.Classname.JOB));

        assertThat(jobContext.get("orderId")).isEqualTo(5);
        assertThat(jobContext.get("approvals")).isEqualTo(Map.of("approval-uuid", true));

        List<TaskExecutionCompleteEvent> taskExecutionCompleteEvents = applicationEvents.stream(
            TaskExecutionCompleteEvent.class)
            .toList();

        assertThat(taskExecutionCompleteEvents).hasSize(1);

        TaskExecution completedTaskExecution = taskExecutionCompleteEvents.getFirst()
            .getTaskExecution();

        assertThat(completedTaskExecution.getId()).isEqualTo(taskExecutionId);
        assertThat(completedTaskExecution.getEndDate()).isNotNull();
        assertThat(taskFileStorage.readTaskExecutionOutput(completedTaskExecution.getOutput()))
            .isEqualTo(Map.of("approved", true));
    }

    @Test
    void testResumeAnAlreadyProcessedApprovalFailsWithItsMessage() {
        long jobId = saveJob(Job.Status.STOPPED, null);

        Map<String, Object> jobContext = new HashMap<>();

        jobContext.put("approvals", Map.of("approval-uuid", true));

        contextService.push(
            jobId, Context.Classname.JOB, taskFileStorage.storeContextValue(jobId, Context.Classname.JOB, jobContext));

        saveTaskExecution(jobId);

        assertThatThrownBy(() -> remoteJobFacadeClient.resumeApproval(jobId, "approval-uuid", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Approval already processed");
        assertThat(applicationEvents.stream(TaskExecutionCompleteEvent.class)).isEmpty();
        assertThat(jobService.getJob(jobId)
            .getStatus()).isEqualTo(Job.Status.STOPPED);
    }

    private long saveJob(Job.Status status, Long parentTaskExecutionId) {
        Job job = new Job();

        job.setParentTaskExecutionId(parentTaskExecutionId);
        job.setStatus(status);
        job.setWorkflowId("workflow-1");

        Job savedJob = jobRepository.save(job);

        return Objects.requireNonNull(savedJob.getId());
    }

    private long saveTaskExecution(long jobId) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .status(TaskExecution.Status.STARTED)
            .workflowTask(new WorkflowTask(Map.of("name", "approval", "type", "waitForApproval/v1")))
            .build();

        taskExecution.setTaskNumber(1);

        TaskExecution createdTaskExecution = taskExecutionService.create(taskExecution);

        return Objects.requireNonNull(createdTaskExecution.getId());
    }
}
