/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcTaskExecutionRepository;
import com.bytechef.ee.platform.workflow.execution.remote.client.config.PlatformWorkflowExecutionRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.PrincipalJobRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
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
class RemotePrincipalJobServiceClientIntTest {

    private static final long PRINCIPAL_ID = 1051L;
    private static final long OTHER_PRINCIPAL_ID = 1052L;

    @Autowired
    private JdbcJobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrincipalJobRepository principalJobRepository;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private JdbcTaskExecutionRepository taskExecutionRepository;

    private ManagedChannel managedChannel;
    private RemotePrincipalJobServiceClient remotePrincipalJobServiceClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new PrincipalJobRemoteServiceGrpcServer(
                        principalJobService, new PlatformWorkflowExecutionProtoMapper(objectMapper)),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        remotePrincipalJobServiceClient = new RemotePrincipalJobServiceClient(
            new FixedGrpcChannels(managedChannel), objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        principalJobRepository.deleteAll();
        taskExecutionRepository.deleteAll();
        jobRepository.deleteAll();
    }

    @Test
    void testFetchAndGetJobPrincipalId() {
        long jobId = saveJob("workflow-a", Job.Status.COMPLETED);

        principalJobService.create(jobId, PRINCIPAL_ID, PlatformType.AUTOMATION);

        assertThat(remotePrincipalJobServiceClient.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION))
            .contains(PRINCIPAL_ID);
        assertThat(remotePrincipalJobServiceClient.getJobPrincipalId(jobId, PlatformType.AUTOMATION))
            .isEqualTo(PRINCIPAL_ID);
        assertThat(remotePrincipalJobServiceClient.fetchJobPrincipalId(jobId, PlatformType.EMBEDDED)).isEmpty();
        assertThatThrownBy(() -> remotePrincipalJobServiceClient.getJobPrincipalId(jobId, PlatformType.EMBEDDED))
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void testFetchLastJobId() {
        long firstJobId = saveJob("workflow-a", Job.Status.COMPLETED);
        long lastJobId = saveJob("workflow-b", Job.Status.COMPLETED);

        principalJobService.create(firstJobId, PRINCIPAL_ID, PlatformType.AUTOMATION);
        principalJobService.create(lastJobId, PRINCIPAL_ID, PlatformType.AUTOMATION);

        assertThat(remotePrincipalJobServiceClient.fetchLastJobId(PRINCIPAL_ID, PlatformType.AUTOMATION))
            .contains(lastJobId);
        assertThat(remotePrincipalJobServiceClient.fetchLastJobId(OTHER_PRINCIPAL_ID, PlatformType.AUTOMATION))
            .isEmpty();
    }

    @Test
    void testFetchLastWorkflowJobId() {
        long workflowAJobId = saveJob("workflow-a", Job.Status.COMPLETED);
        long workflowBJobId = saveJob("workflow-b", Job.Status.COMPLETED);

        principalJobService.create(workflowAJobId, PRINCIPAL_ID, PlatformType.AUTOMATION);
        principalJobService.create(workflowBJobId, PRINCIPAL_ID, PlatformType.AUTOMATION);

        assertThat(
            remotePrincipalJobServiceClient.fetchLastWorkflowJobId(
                PRINCIPAL_ID, List.of("workflow-a"), PlatformType.AUTOMATION))
                    .contains(workflowAJobId);
        assertThat(
            remotePrincipalJobServiceClient.fetchLastWorkflowJobId(
                PRINCIPAL_ID, List.of("workflow-a", "workflow-b"), PlatformType.AUTOMATION))
                    .contains(workflowBJobId);
        assertThat(
            remotePrincipalJobServiceClient.fetchLastWorkflowJobId(
                PRINCIPAL_ID, List.of("workflow-c"), PlatformType.AUTOMATION))
                    .isEmpty();
    }

    @Test
    void testGetJobIdsOfAPrincipal() {
        long firstJobId = saveJob("workflow-a", Job.Status.COMPLETED);
        long secondJobId = saveJob("workflow-a", Job.Status.COMPLETED);
        long otherJobId = saveJob("workflow-a", Job.Status.COMPLETED);

        principalJobService.create(firstJobId, PRINCIPAL_ID, PlatformType.AUTOMATION);
        principalJobService.create(secondJobId, PRINCIPAL_ID, PlatformType.AUTOMATION);
        principalJobService.create(otherJobId, OTHER_PRINCIPAL_ID, PlatformType.AUTOMATION);

        assertThat(remotePrincipalJobServiceClient.getJobIds(PRINCIPAL_ID, PlatformType.AUTOMATION))
            .containsExactlyInAnyOrder(firstJobId, secondJobId);
        assertThat(remotePrincipalJobServiceClient.getJobIds(PRINCIPAL_ID, PlatformType.EMBEDDED)).isEmpty();
    }

    @Test
    void testGetJobIdsPageFiltersByStatusDatesPrincipalsAndWorkflows() {
        Instant startDate = Instant.parse("2026-10-26T12:00:00Z");
        Instant endDate = Instant.parse("2026-10-27T12:00:00Z");

        long matchingJobId = saveJob("workflow-a", Job.Status.STARTED, startDate, endDate);
        long otherStatusJobId = saveJob("workflow-a", Job.Status.COMPLETED, startDate, endDate);
        long otherWorkflowJobId = saveJob("workflow-b", Job.Status.STARTED, startDate, endDate);
        long otherDateJobId = saveJob("workflow-a", Job.Status.STARTED, startDate.plusSeconds(86_400), endDate);

        for (long jobId : List.of(matchingJobId, otherStatusJobId, otherWorkflowJobId, otherDateJobId)) {
            principalJobService.create(jobId, PRINCIPAL_ID, PlatformType.EMBEDDED);
        }

        Page<Long> page = remotePrincipalJobServiceClient.getJobIds(
            Job.Status.STARTED, startDate, endDate, List.of(PRINCIPAL_ID, OTHER_PRINCIPAL_ID), PlatformType.EMBEDDED,
            List.of("workflow-a"), true, 0);

        assertThat(page.getContent()).containsExactly(matchingJobId);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getNumber()).isZero();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void testGetJobIdsPageWithoutFiltersPagesInDescendingJobOrder() {
        List<Long> jobIds = new ArrayList<>();

        for (int index = 0; index < 25; index++) {
            long jobId = saveJob("workflow-a", Job.Status.COMPLETED);

            principalJobService.create(jobId, PRINCIPAL_ID, PlatformType.AUTOMATION);

            jobIds.add(jobId);
        }

        Page<Long> firstPage = remotePrincipalJobServiceClient.getJobIds(
            null, null, null, null, PlatformType.AUTOMATION, null, false, 0);
        Page<Long> secondPage = remotePrincipalJobServiceClient.getJobIds(
            null, null, null, null, PlatformType.AUTOMATION, null, false, 1);

        assertThat(firstPage.getContent()).hasSize(20)
            .first()
            .isEqualTo(jobIds.getLast());
        assertThat(firstPage.getTotalElements()).isEqualTo(25);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent()).hasSize(5)
            .last()
            .isEqualTo(jobIds.getFirst());
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(
            remotePrincipalJobServiceClient.getJobIds(null, null, null, null, PlatformType.EMBEDDED, null, false, 0)
                .getContent()).isEmpty();
    }

    @Test
    void testDeletePrincipalJobs() {
        long jobId = saveJob("workflow-a", Job.Status.COMPLETED);
        long otherJobId = saveJob("workflow-a", Job.Status.COMPLETED);

        principalJobService.create(jobId, PRINCIPAL_ID, PlatformType.EMBEDDED);
        principalJobService.create(otherJobId, PRINCIPAL_ID, PlatformType.EMBEDDED);

        remotePrincipalJobServiceClient.deletePrincipalJobs(jobId, PlatformType.EMBEDDED);

        assertThat(principalJobService.fetchJobPrincipalId(jobId, PlatformType.EMBEDDED)).isEmpty();
        assertThat(principalJobService.fetchJobPrincipalId(otherJobId, PlatformType.EMBEDDED)).contains(PRINCIPAL_ID);
    }

    private long saveJob(String workflowId, Job.Status status) {
        return saveJob(workflowId, status, null, null);
    }

    private long saveJob(String workflowId, Job.Status status, Instant startDate, Instant endDate) {
        Job job = new Job();

        job.setEndDate(endDate);
        job.setStartDate(startDate);
        job.setStatus(status);
        job.setWorkflowId(workflowId);

        Job savedJob = jobRepository.save(job);

        return Objects.requireNonNull(savedJob.getId());
    }
}
