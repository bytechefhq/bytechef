/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.JobRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.JobResult;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteJob;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.WorkflowIdRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
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
