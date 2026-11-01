/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.JobRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.JobResult;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.StaleJobsRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.WorkflowIdRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.LongConsumer;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteJobServiceClient implements JobService {

    private static final String EXECUTION_APP = "execution-app";
    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteJobServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.atlasExecutionProtoMapper = new AtlasExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public Job create(JobParametersDTO jobParametersDTO, Workflow workflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job create(JobParametersDTO jobParametersDTO, Workflow workflow, LongConsumer jobCreatedCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteJob(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> fetchJob(Long id) {
        return toJob(getStub().fetchJob(toIdRequest(id)));
    }

    @Override
    public Optional<Job> fetchLastJob() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> fetchLastWorkflowJob(String workflowId) {
        JobResult jobResult = getStub().fetchLastWorkflowJob(
            WorkflowIdRequest.newBuilder()
                .setWorkflowId(workflowId)
                .build());

        return toJob(jobResult);
    }

    @Override
    public Optional<Job> fetchLastWorkflowJob(List<String> workflowIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Job> fetchTaskExecutionJob(long taskExecutionId) {
        return toJob(getStub().fetchTaskExecutionJob(toIdRequest(taskExecutionId)));
    }

    @Override
    public List<Long> getChildJobIds(long parentJobId) {
        return getStub().getChildJobIds(toIdRequest(parentJobId))
            .getValuesList();
    }

    @Override
    public List<Job> getStaleJobs(Job.Status status, Instant lastModifiedDateBefore) {
        StaleJobsRequest staleJobsRequest = StaleJobsRequest.newBuilder()
            .setStatus(status.name())
            .setLastModifiedDateBefore(
                Timestamp.newBuilder()
                    .setSeconds(lastModifiedDateBefore.getEpochSecond())
                    .setNanos(lastModifiedDateBefore.getNano())
                    .build())
            .build();

        return getStub().getStaleJobs(staleJobsRequest)
            .getJobsList()
            .stream()
            .map(atlasExecutionProtoMapper::toJob)
            .toList();
    }

    @Override
    public List<Job> getChildJobs(List<Long> parentTaskExecutionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Job> getJobs(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job getJob(long id) {
        return atlasExecutionProtoMapper.toJob(getStub().getJob(toIdRequest(id)));
    }

    @Override
    public Page<Job> getJobsPage(int pageNumber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Job getTaskExecutionJob(long taskExecutionId) {
        return atlasExecutionProtoMapper.toJob(getStub().getTaskExecutionJob(toIdRequest(taskExecutionId)));
    }

    @Override
    public Job resumeToStatusStarted(long id) {
        return atlasExecutionProtoMapper.toJob(getStub().resumeToStatusStarted(toIdRequest(id)));
    }

    @Override
    public boolean tryClaimResume(Job job) {
        return getStub().tryClaimResume(atlasExecutionProtoMapper.toProto(job))
            .getValue();
    }

    @Override
    public Job setStatusToStarted(long id) {
        return atlasExecutionProtoMapper.toJob(getStub().setStatusToStarted(toIdRequest(id)));
    }

    @Override
    public Job setStatusToStopped(long id) {
        return atlasExecutionProtoMapper.toJob(getStub().setStatusToStopped(toIdRequest(id)));
    }

    @Override
    public Job update(Job job) {
        return atlasExecutionProtoMapper.toJob(getStub().update(atlasExecutionProtoMapper.toProto(job)));
    }

    private JobRemoteServiceGrpc.JobRemoteServiceBlockingStub getStub() {
        return JobRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private Optional<Job> toJob(JobResult jobResult) {
        if (!jobResult.hasJob()) {
            return Optional.empty();
        }

        return Optional.of(atlasExecutionProtoMapper.toJob(jobResult.getJob()));
    }

    private static IdRequest toIdRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }
}
