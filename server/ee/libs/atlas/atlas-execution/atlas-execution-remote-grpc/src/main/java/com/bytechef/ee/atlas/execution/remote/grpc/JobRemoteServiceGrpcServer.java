/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.BoolResponse;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.Int64ListResponse;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.JobRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.JobResult;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteJob;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteJobs;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.StaleJobsRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.WorkflowIdRequest;
import com.google.protobuf.Timestamp;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 */
public class JobRemoteServiceGrpcServer extends JobRemoteServiceGrpc.JobRemoteServiceImplBase {

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final JobService jobService;

    @SuppressFBWarnings("EI")
    public JobRemoteServiceGrpcServer(
        JobService jobService, AtlasExecutionProtoMapper atlasExecutionProtoMapper) {

        this.atlasExecutionProtoMapper = atlasExecutionProtoMapper;
        this.jobService = jobService;
    }

    @Override
    public void fetchJob(IdRequest request, StreamObserver<JobResult> responseObserver) {
        responseObserver.onNext(toJobResult(jobService.fetchJob(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void fetchLastWorkflowJob(WorkflowIdRequest request, StreamObserver<JobResult> responseObserver) {
        responseObserver.onNext(toJobResult(jobService.fetchLastWorkflowJob(request.getWorkflowId())));
        responseObserver.onCompleted();
    }

    @Override
    public void fetchTaskExecutionJob(IdRequest request, StreamObserver<JobResult> responseObserver) {
        responseObserver.onNext(toJobResult(jobService.fetchTaskExecutionJob(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getChildJobIds(IdRequest request, StreamObserver<Int64ListResponse> responseObserver) {
        List<Long> childJobIds = jobService.getChildJobIds(request.getId());

        responseObserver.onNext(
            Int64ListResponse.newBuilder()
                .addAllValues(childJobIds)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getStaleJobs(StaleJobsRequest request, StreamObserver<RemoteJobs> responseObserver) {
        Timestamp lastModifiedDateBefore = request.getLastModifiedDateBefore();

        List<Job> staleJobs = jobService.getStaleJobs(
            Job.Status.valueOf(request.getStatus()),
            Instant.ofEpochSecond(lastModifiedDateBefore.getSeconds(), lastModifiedDateBefore.getNanos()));

        RemoteJobs.Builder builder = RemoteJobs.newBuilder();

        for (Job staleJob : staleJobs) {
            builder.addJobs(atlasExecutionProtoMapper.toProto(staleJob));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getJob(IdRequest request, StreamObserver<RemoteJob> responseObserver) {
        responseObserver.onNext(atlasExecutionProtoMapper.toProto(jobService.getJob(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void getTaskExecutionJob(IdRequest request, StreamObserver<RemoteJob> responseObserver) {
        responseObserver.onNext(atlasExecutionProtoMapper.toProto(jobService.getTaskExecutionJob(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void resumeToStatusStarted(IdRequest request, StreamObserver<RemoteJob> responseObserver) {
        responseObserver.onNext(atlasExecutionProtoMapper.toProto(jobService.resumeToStatusStarted(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void setStatusToStarted(IdRequest request, StreamObserver<RemoteJob> responseObserver) {
        responseObserver.onNext(atlasExecutionProtoMapper.toProto(jobService.setStatusToStarted(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void setStatusToStopped(IdRequest request, StreamObserver<RemoteJob> responseObserver) {
        responseObserver.onNext(atlasExecutionProtoMapper.toProto(jobService.setStatusToStopped(request.getId())));
        responseObserver.onCompleted();
    }

    @Override
    public void tryClaimResume(RemoteJob request, StreamObserver<BoolResponse> responseObserver) {
        boolean claimed = jobService.tryClaimResume(atlasExecutionProtoMapper.toJob(request));

        responseObserver.onNext(
            BoolResponse.newBuilder()
                .setValue(claimed)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void update(RemoteJob request, StreamObserver<RemoteJob> responseObserver) {
        responseObserver
            .onNext(atlasExecutionProtoMapper.toProto(jobService.update(atlasExecutionProtoMapper.toJob(request))));
        responseObserver.onCompleted();
    }

    private JobResult toJobResult(Optional<Job> jobOptional) {
        JobResult.Builder builder = JobResult.newBuilder();

        jobOptional.ifPresent(job -> builder.setJob(atlasExecutionProtoMapper.toProto(job)));

        return builder.build();
    }
}
