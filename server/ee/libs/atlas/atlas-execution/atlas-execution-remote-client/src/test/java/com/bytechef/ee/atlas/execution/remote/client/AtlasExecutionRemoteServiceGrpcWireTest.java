/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.ee.atlas.execution.remote.client.facade.RemoteJobFacadeClient;
import com.bytechef.ee.atlas.execution.remote.client.service.RemoteContextServiceClient;
import com.bytechef.ee.atlas.execution.remote.client.service.RemoteCounterServiceClient;
import com.bytechef.ee.atlas.execution.remote.client.service.RemoteJobServiceClient;
import com.bytechef.ee.atlas.execution.remote.client.service.RemoteTaskExecutionServiceClient;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.ContextRemoteServiceGrpcServer;
import com.bytechef.ee.atlas.execution.remote.grpc.CounterRemoteServiceGrpcServer;
import com.bytechef.ee.atlas.execution.remote.grpc.JobFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.atlas.execution.remote.grpc.JobRemoteServiceGrpcServer;
import com.bytechef.ee.atlas.execution.remote.grpc.TaskExecutionRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AtlasExecutionRemoteServiceGrpcWireTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
        .build();

    private final ContextService contextService = mock(ContextService.class);
    private final CounterService counterService = mock(CounterService.class);
    private final JobFacade jobFacade = mock(JobFacade.class);
    private final JobService jobService = mock(JobService.class);
    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

    private FixedGrpcChannels fixedGrpcChannels;
    private ManagedChannel managedChannel;
    private Server server;

    @BeforeEach
    void beforeEach() throws Exception {
        String name = InProcessServerBuilder.generateName();
        AtlasExecutionProtoMapper atlasExecutionProtoMapper = new AtlasExecutionProtoMapper(JSON_MAPPER);
        ServerInterceptor serverInterceptor = new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler());

        InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(name)
            .directExecutor();

        for (BindableService bindableService : List.<BindableService>of(
            new ContextRemoteServiceGrpcServer(contextService, atlasExecutionProtoMapper),
            new CounterRemoteServiceGrpcServer(counterService),
            new JobFacadeRemoteServiceGrpcServer(jobFacade, atlasExecutionProtoMapper),
            new JobRemoteServiceGrpcServer(jobService, atlasExecutionProtoMapper),
            new TaskExecutionRemoteServiceGrpcServer(taskExecutionService, atlasExecutionProtoMapper))) {

            inProcessServerBuilder.addService(ServerInterceptors.intercept(bindableService, serverInterceptor));
        }

        server = inProcessServerBuilder.build()
            .start();
        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();
        fixedGrpcChannels = new FixedGrpcChannels(managedChannel);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void testCreateJob() {
        JobParametersDTO jobParametersDTO = new JobParametersDTO("workflow-1", Map.of("id", 9_007_199_254_740_993L));

        when(jobFacade.createJob(any(JobParametersDTO.class))).thenAnswer(invocation -> {
            JobParametersDTO receivedJobParametersDTO = invocation.getArgument(0);

            assertThat(receivedJobParametersDTO).usingRecursiveComparison()
                .isEqualTo(jobParametersDTO);

            return 7L;
        });

        assertThat(new RemoteJobFacadeClient(fixedGrpcChannels, JSON_MAPPER).createJob(jobParametersDTO))
            .isEqualTo(7L);
    }

    @Test
    void testDeleteJob() {
        new RemoteJobFacadeClient(fixedGrpcChannels, JSON_MAPPER).deleteJob(7L);

        verify(jobFacade).deleteJob(7L);
    }

    @Test
    void testStopJob() {
        new RemoteJobFacadeClient(fixedGrpcChannels, JSON_MAPPER).stopJob(7L);

        verify(jobFacade).stopJob(7L);
    }

    @Test
    void testResumeApproval() {
        new RemoteJobFacadeClient(fixedGrpcChannels, JSON_MAPPER).resumeApproval(7L, "approval-uuid", true);

        verify(jobFacade).resumeApproval(7L, "approval-uuid", true);
    }

    @Test
    void testResumeAnAlreadyProcessedApprovalFailsWithItsMessage() {
        doThrow(new IllegalArgumentException("Approval already processed"))
            .when(jobFacade)
            .resumeApproval(7L, "approval-uuid", false);

        RemoteJobFacadeClient remoteJobFacadeClient = new RemoteJobFacadeClient(fixedGrpcChannels, JSON_MAPPER);

        assertThatThrownBy(() -> remoteJobFacadeClient.resumeApproval(7L, "approval-uuid", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Approval already processed");
    }

    @Test
    void testJobCalls() {
        Job job = job(7L);

        when(jobService.getJob(7L)).thenReturn(job);
        when(jobService.getTaskExecutionJob(11L)).thenReturn(job);
        when(jobService.fetchLastWorkflowJob("workflow-1")).thenReturn(Optional.of(job));
        when(jobService.resumeToStatusStarted(7L)).thenReturn(job);
        when(jobService.setStatusToStarted(7L)).thenReturn(job);
        when(jobService.setStatusToStopped(7L)).thenReturn(job);
        when(jobService.update(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RemoteJobServiceClient remoteJobServiceClient = new RemoteJobServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteJobServiceClient.getJob(7L)).usingRecursiveComparison()
            .isEqualTo(job);
        assertThat(remoteJobServiceClient.getTaskExecutionJob(11L)).usingRecursiveComparison()
            .isEqualTo(job);
        assertThat(remoteJobServiceClient.fetchLastWorkflowJob("workflow-1")).get()
            .usingRecursiveComparison()
            .isEqualTo(job);
        assertThat(remoteJobServiceClient.resumeToStatusStarted(7L)).usingRecursiveComparison()
            .isEqualTo(job);
        assertThat(remoteJobServiceClient.setStatusToStarted(7L)).usingRecursiveComparison()
            .isEqualTo(job);
        assertThat(remoteJobServiceClient.setStatusToStopped(7L)).usingRecursiveComparison()
            .isEqualTo(job);
        assertThat(remoteJobServiceClient.update(job)).usingRecursiveComparison()
            .isEqualTo(job);
    }

    @Test
    void testJobKeepsNumberTypes() {
        when(jobService.getJob(7L)).thenReturn(job(7L));

        Job readJob = new RemoteJobServiceClient(fixedGrpcChannels, JSON_MAPPER).getJob(7L);

        assertThat(readJob.getInputs()
            .get("id")).isInstanceOf(Long.class);
        assertThat(readJob.getInputs()
            .get("ratio")).isInstanceOf(Double.class);
    }

    @Test
    void testFetchLastWorkflowJobWithoutJobIsEmpty() {
        when(jobService.fetchLastWorkflowJob("workflow-2")).thenReturn(Optional.empty());

        assertThat(new RemoteJobServiceClient(fixedGrpcChannels, JSON_MAPPER).fetchLastWorkflowJob("workflow-2"))
            .isEmpty();
    }

    @Test
    void testFetchJobAndTaskExecutionJob() {
        Job job = job(7L);

        when(jobService.fetchJob(7L)).thenReturn(Optional.of(job));
        when(jobService.fetchTaskExecutionJob(11L)).thenReturn(Optional.of(job));

        RemoteJobServiceClient remoteJobServiceClient = new RemoteJobServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteJobServiceClient.fetchJob(7L)).get()
            .usingRecursiveComparison()
            .isEqualTo(job);
        assertThat(remoteJobServiceClient.fetchTaskExecutionJob(11L)).get()
            .usingRecursiveComparison()
            .isEqualTo(job);
    }

    @Test
    void testFetchJobAndTaskExecutionJobWithoutJobAreEmpty() {
        when(jobService.fetchJob(8L)).thenReturn(Optional.empty());
        when(jobService.fetchTaskExecutionJob(12L)).thenReturn(Optional.empty());

        RemoteJobServiceClient remoteJobServiceClient = new RemoteJobServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteJobServiceClient.fetchJob(8L)).isEmpty();
        assertThat(remoteJobServiceClient.fetchTaskExecutionJob(12L)).isEmpty();
    }

    @Test
    void testGetUnknownJobFailsWithItsMessage() {
        when(jobService.getJob(7L)).thenThrow(new IllegalArgumentException("Job 7 not found"));

        RemoteJobServiceClient remoteJobServiceClient = new RemoteJobServiceClient(fixedGrpcChannels, JSON_MAPPER);

        assertThatThrownBy(() -> remoteJobServiceClient.getJob(7L)).hasMessageContaining("Job 7 not found");
    }

    @Test
    void testTaskExecutionCalls() {
        TaskExecution taskExecution = taskExecution(11L);

        when(taskExecutionService.cancelIfUnfinished(11L)).thenReturn(true);
        when(taskExecutionService.completeIfNotCancelled(11L)).thenReturn(false);
        when(taskExecutionService.create(any(TaskExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskExecutionService.getJobTaskExecutions(7L)).thenReturn(List.of(taskExecution, taskExecution(12L)));
        when(taskExecutionService.getParentTaskExecutions(10L)).thenReturn(List.of(taskExecution));
        when(taskExecutionService.getTaskExecution(11L)).thenReturn(taskExecution);
        when(taskExecutionService.getTaskExecutionForUpdate(11L)).thenReturn(taskExecution);
        when(taskExecutionService.update(any(TaskExecution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RemoteTaskExecutionServiceClient remoteTaskExecutionServiceClient = new RemoteTaskExecutionServiceClient(
            fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteTaskExecutionServiceClient.cancelIfUnfinished(11L)).isTrue();
        assertThat(remoteTaskExecutionServiceClient.completeIfNotCancelled(11L)).isFalse();
        assertThat(remoteTaskExecutionServiceClient.create(taskExecution)
            .getId()).isEqualTo(11L);
        assertThat(remoteTaskExecutionServiceClient.getJobTaskExecutions(7L)).extracting(TaskExecution::getId)
            .containsExactly(11L, 12L);
        assertThat(remoteTaskExecutionServiceClient.getParentTaskExecutions(10L)).extracting(TaskExecution::getId)
            .containsExactly(11L);
        assertThat(remoteTaskExecutionServiceClient.getTaskExecution(11L)
            .getWorkflowTask()
            .toMap()).isEqualTo(taskExecution.getWorkflowTask()
                .toMap());
        assertThat(remoteTaskExecutionServiceClient.getTaskExecutionForUpdate(11L)
            .getId()).isEqualTo(11L);
        assertThat(remoteTaskExecutionServiceClient.update(taskExecution)
            .getId()).isEqualTo(11L);
    }

    @Test
    void testFetchLastJobTaskExecution() {
        when(taskExecutionService.fetchLastJobTaskExecution(7L)).thenReturn(Optional.of(taskExecution(11L)));
        when(taskExecutionService.fetchLastJobTaskExecution(8L)).thenReturn(Optional.empty());

        RemoteTaskExecutionServiceClient remoteTaskExecutionServiceClient = new RemoteTaskExecutionServiceClient(
            fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteTaskExecutionServiceClient.fetchLastJobTaskExecution(7L)).get()
            .extracting(TaskExecution::getId)
            .isEqualTo(11L);
        assertThat(remoteTaskExecutionServiceClient.fetchLastJobTaskExecution(8L)).isEmpty();
    }

    @Test
    void testPeekWithAndWithoutSubStack() {
        FileEntry fileEntry = new FileEntry("context.json", "base64:///abc");
        FileEntry subStackFileEntry = new FileEntry("sub.json", "base64:///def");

        when(contextService.peek(7L, Context.Classname.JOB)).thenReturn(fileEntry);
        when(contextService.peek(7L, 2, Context.Classname.TASK_EXECUTION)).thenReturn(subStackFileEntry);

        RemoteContextServiceClient remoteContextServiceClient = new RemoteContextServiceClient(
            fixedGrpcChannels, JSON_MAPPER);

        assertThat(remoteContextServiceClient.peek(7L, Context.Classname.JOB)).usingRecursiveComparison()
            .isEqualTo(fileEntry);
        assertThat(remoteContextServiceClient.peek(7L, 2, Context.Classname.TASK_EXECUTION))
            .usingRecursiveComparison()
            .isEqualTo(subStackFileEntry);

        remoteContextServiceClient.push(7L, Context.Classname.JOB, fileEntry);
        remoteContextServiceClient.push(7L, 2, Context.Classname.TASK_EXECUTION, subStackFileEntry);

        verify(contextService).push(eq(7L),
            eq(Context.Classname.JOB), any(FileEntry.class));
        verify(contextService).push(eq(7L), eq(2),
            eq(Context.Classname.TASK_EXECUTION), any(FileEntry.class));
    }

    @Test
    void testCounterCalls() {
        when(counterService.decrement(7L)).thenReturn(2L);

        RemoteCounterServiceClient remoteCounterServiceClient = new RemoteCounterServiceClient(fixedGrpcChannels);

        assertThat(remoteCounterServiceClient.decrement(7L)).isEqualTo(2L);

        remoteCounterServiceClient.set(7L, 3L);
        remoteCounterServiceClient.delete(7L);

        verify(counterService).set(7L, 3L);
        verify(counterService).delete(7L);
    }

    private static Job job(long id) {
        Job job = new Job();

        job.setId(id);
        job.setInputs(Map.of("id", 9_007_199_254_740_993L, "ratio", 5.0));
        job.setStatus(Job.Status.STARTED);
        job.setWorkflowId("workflow-1");

        return job;
    }

    private static TaskExecution taskExecution(long id) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(id);
        taskExecution.setJobId(7L);
        taskExecution.setWorkflowTask(new WorkflowTask(Map.of("name", "a", "type", "var/v1/set")));

        return taskExecution;
    }
}
