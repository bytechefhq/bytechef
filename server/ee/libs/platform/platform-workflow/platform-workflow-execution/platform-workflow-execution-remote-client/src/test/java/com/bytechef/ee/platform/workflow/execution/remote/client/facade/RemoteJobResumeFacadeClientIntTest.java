/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;

import com.bytechef.atlas.coordinator.event.ResumeJobEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.ee.platform.workflow.execution.remote.client.config.PlatformWorkflowExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.JobResumeFacadeRemoteServiceGrpcServer;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade.JobResumeOutcome;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import org.springframework.jdbc.core.JdbcTemplate;
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
class RemoteJobResumeFacadeClientIntTest {

    private static final long TASK_EXECUTION_RESUME_ID = 4_242L;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private ApprovalTokens approvalTokens;

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private JobResumeFacade jobResumeFacade;

    @Autowired
    private JobService jobService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ExecutorService concurrentResumeExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    private ManagedChannel managedChannel;
    private RemoteJobResumeFacadeClient remoteJobResumeFacadeClient;
    private final ExecutorService resumeExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new JobResumeFacadeRemoteServiceGrpcServer(
                        jobResumeFacade, new PlatformWorkflowExecutionProtoMapper(objectMapper),
                        resumeExecutorService),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remoteJobResumeFacadeClient = new RemoteJobResumeFacadeClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();
        resumeExecutorService.shutdownNow();
        concurrentResumeExecutorService.shutdownNow();

        jobRepository.deleteAll();
    }

    @Test
    void testResumeJobWithASignedTokenResumesTheSuspendedJob() {
        JobResumeId jobResumeId = saveSuspendedJob(false);

        String token = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        assertThat(remoteJobResumeFacadeClient.resumeJob(token, Map.of("approved", true)))
            .isEqualTo(JobResumeOutcome.OK);

        Job job = jobService.getJob(jobResumeId.getJobId());

        assertThat(job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isEqualTo(jobResumeId.toString());

        List<ResumeJobEvent> resumeJobEvents = applicationEvents.stream(ResumeJobEvent.class)
            .toList();

        assertThat(resumeJobEvents).hasSize(1);
        assertThat(resumeJobEvents.getFirst()
            .getJobId()).isEqualTo(jobResumeId.getJobId());
        assertThat(resumeJobEvents.getFirst()
            .getTaskExecutionId()).isEqualTo(TASK_EXECUTION_RESUME_ID);
        assertThat(resumeJobEvents.getFirst()
            .getData()).isEqualTo(Map.of("approved", true));
        assertThat(applicationEvents.stream(JobResumedEvent.class))
            .extracting(JobResumedEvent::jobResumeId)
            .containsExactly(token);
        assertThat(remoteJobResumeFacadeClient.resumeJob(token, Map.of("approved", true)))
            .isEqualTo(JobResumeOutcome.GONE);
    }

    @Test
    void testResumeJobWithATamperedTokenIsRejected() {
        JobResumeId jobResumeId = saveSuspendedJob(false);

        String token = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        assertThat(remoteJobResumeFacadeClient.resumeJob(token + "x", Map.of()))
            .isEqualTo(JobResumeOutcome.INVALID_ID);
        assertThat(jobService.getJob(jobResumeId.getJobId())
            .getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isNull();
        assertThat(applicationEvents.stream(ResumeJobEvent.class)).isEmpty();
    }

    @Test
    void testResumeJobThatIsNotSuspendedYet() {
        JobResumeId jobResumeId = saveJob(Job.Status.STARTED, false);

        String token = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        assertThat(remoteJobResumeFacadeClient.resumeJob(token, Map.of()))
            .isEqualTo(JobResumeOutcome.NOT_YET_SUSPENDED);
    }

    @Test
    void testResumeExpiredJobRestartsTheJob() {
        JobResumeId jobResumeId = JobResumeId.of("public", saveJobId(Job.Status.STOPPED));

        long jobId = jobResumeId.getJobId();

        Job job = jobService.getJob(jobId);

        job.setMetadata(Map.of(MetadataConstants.JOB_RESUME_ID, jobResumeId.toString()));

        jobService.update(job);

        assertThat(remoteJobResumeFacadeClient.resumeExpiredJob(jobResumeId.toString()))
            .isEqualTo(JobResumeOutcome.OK);

        Job resumedJob = jobService.getJob(jobId);

        assertThat(resumedJob.getStatus()).isEqualTo(Job.Status.STARTED);
        assertThat(resumedJob.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID))
            .isEqualTo(jobResumeId.toString());
        assertThat(applicationEvents.stream(ResumeJobEvent.class))
            .extracting(ResumeJobEvent::getJobId, ResumeJobEvent::isStarted)
            .containsExactly(tuple(jobId, true));
        assertThat(remoteJobResumeFacadeClient.resumeExpiredJob("not-a-resume-id"))
            .isEqualTo(JobResumeOutcome.INVALID_ID);
    }

    @Test
    void testResumeJobStreamingRunsTheConsumerBeforeTheJobResumes() {
        JobResumeId jobResumeId = saveSuspendedJob(true);

        String token = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        AtomicLong acceptedJobId = new AtomicLong();
        AtomicLong consumedJobResumeIdCount = new AtomicLong();

        JobResumeOutcome jobResumeOutcome = remoteJobResumeFacadeClient.resumeJobStreaming(
            token, Map.of("answer", "yes"), jobId -> {
                acceptedJobId.set(jobId);

                Job job = jobService.getJob(jobId);

                if (job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID) != null) {
                    consumedJobResumeIdCount.incrementAndGet();
                }
            });

        assertThat(jobResumeOutcome).isEqualTo(JobResumeOutcome.OK);
        assertThat(acceptedJobId.get()).isEqualTo(jobResumeId.getJobId());
        assertThat(consumedJobResumeIdCount.get()).isZero();
        assertThat(jobService.getJob(jobResumeId.getJobId())
            .getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isEqualTo(jobResumeId.toString());
    }

    @Test
    void testResumeJobStreamingThatIsNotAllowedSkipsTheConsumer() {
        JobResumeId jobResumeId = saveSuspendedJob(false);

        String token = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        AtomicLong acceptedJobId = new AtomicLong();

        assertThat(remoteJobResumeFacadeClient.resumeJobStreaming(token, Map.of(), acceptedJobId::set))
            .isEqualTo(JobResumeOutcome.STREAMING_NOT_ALLOWED);
        assertThat(acceptedJobId.get()).isZero();
        assertThat(jobService.getJob(jobResumeId.getJobId())
            .getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isNull();
    }

    @Test
    void testResumeJobStreamingWithAFailingConsumerAbortsTheResume() {
        JobResumeId jobResumeId = saveSuspendedJob(true);

        String token = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        assertThatThrownBy(
            () -> remoteJobResumeFacadeClient.resumeJobStreaming(
                token, Map.of(), jobId -> {
                    throw new IllegalStateException("Registration failed");
                }))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Registration failed");

        assertThat(remoteJobResumeFacadeClient.resumeJobStreaming(token, Map.of(), jobId -> {}))
            .isEqualTo(JobResumeOutcome.OK);
        assertThat(jobService.getJob(jobResumeId.getJobId())
            .getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID)).isEqualTo(jobResumeId.toString());
    }

    @Test
    void testConcurrentResumeKeepsItsOptimisticLockingFailure() throws Exception {
        JobResumeId jobResumeId = saveSuspendedJob(true);

        String token = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        Future<JobResumeOutcome> concurrentResumeFuture = resumeWhileTheFirstResumeHoldsTheJob(
            token, () -> remoteJobResumeFacadeClient.resumeJob(token, Map.of()));

        assertThatThrownBy(() -> concurrentResumeFuture.get(30, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void testConcurrentStreamingResumeKeepsItsOptimisticLockingFailure() throws Exception {
        JobResumeId jobResumeId = saveSuspendedJob(true);

        String token = approvalTokens.toSignedToken(jobResumeId.toString(), Duration.ofHours(1));

        AtomicLong concurrentAcceptedJobId = new AtomicLong();

        Future<JobResumeOutcome> concurrentResumeFuture = resumeWhileTheFirstResumeHoldsTheJob(
            token, () -> remoteJobResumeFacadeClient.resumeJobStreaming(token, Map.of(), concurrentAcceptedJobId::set));

        assertThatThrownBy(() -> concurrentResumeFuture.get(30, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(OptimisticLockingFailureException.class);
        assertThat(concurrentAcceptedJobId.get()).isZero();
    }

    @Test
    void testResumeJobStreamingOfAMissingJobFails() {
        JobResumeId jobResumeId = JobResumeId.of("public", Long.MAX_VALUE);

        assertThatThrownBy(
            () -> remoteJobResumeFacadeClient.resumeJobStreaming(jobResumeId.toString(), Map.of(), jobId -> {}))
                .isInstanceOf(StatusRuntimeException.class)
                .hasMessageContaining("NoSuchElementException")
                .extracting(exception -> ((StatusRuntimeException) exception).getStatus()
                    .getCode())
                .isEqualTo(Status.Code.INTERNAL);
    }

    private Future<JobResumeOutcome> resumeWhileTheFirstResumeHoldsTheJob(
        String token, Callable<JobResumeOutcome> concurrentResumeCallable) {

        AtomicReference<Future<JobResumeOutcome>> concurrentResumeFutureReference = new AtomicReference<>();

        JobResumeOutcome jobResumeOutcome = remoteJobResumeFacadeClient.resumeJobStreaming(
            token, Map.of(), jobId -> {
                concurrentResumeFutureReference.set(concurrentResumeExecutorService.submit(concurrentResumeCallable));

                await()
                    .atMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofMillis(10))
                    .until(this::isAnUpdateWaitingOnARowLock);
            });

        assertThat(jobResumeOutcome).isEqualTo(JobResumeOutcome.OK);

        return concurrentResumeFutureReference.get();
    }

    private boolean isAnUpdateWaitingOnARowLock() {
        Integer waitingUpdateCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND query LIKE 'UPDATE%'",
            Integer.class);

        return waitingUpdateCount != null && waitingUpdateCount > 0;
    }

    private JobResumeId saveSuspendedJob(boolean streaming) {
        return saveJob(Job.Status.STOPPED, streaming);
    }

    private JobResumeId saveJob(Job.Status status, boolean streaming) {
        JobResumeId jobResumeId = JobResumeId.of("public", saveJobId(status));

        Job job = jobService.getJob(jobResumeId.getJobId());

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(MetadataConstants.JOB_RESUME_ID, jobResumeId.toString());
        metadata.put(MetadataConstants.TASK_EXECUTION_RESUME_ID, TASK_EXECUTION_RESUME_ID);

        if (streaming) {
            metadata.put(MetadataConstants.STREAMING_RESUME, true);
        }

        job.setMetadata(metadata);

        jobService.update(job);

        return jobResumeId;
    }

    private long saveJobId(Job.Status status) {
        Job job = new Job();

        job.setStatus(status);
        job.setWorkflowId("workflow-1");

        Job savedJob = jobRepository.save(job);

        return Objects.requireNonNull(savedJob.getId());
    }
}
