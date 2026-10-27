/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.GetJobIdsPageRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobIdsPage;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobIdsResponse;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobTypeRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.OptionalIdResponse;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.PrincipalJobRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.PrincipalTypeRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * @version ee
 */
public class PrincipalJobRemoteServiceGrpcServer
    extends PrincipalJobRemoteServiceGrpc.PrincipalJobRemoteServiceImplBase {

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final PrincipalJobService principalJobService;

    @SuppressFBWarnings("EI")
    public PrincipalJobRemoteServiceGrpcServer(
        PrincipalJobService principalJobService,
        PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper) {

        this.platformWorkflowExecutionProtoMapper = platformWorkflowExecutionProtoMapper;
        this.principalJobService = principalJobService;
    }

    @Override
    public void deletePrincipalJobs(JobTypeRequest request, StreamObserver<Empty> responseObserver) {
        principalJobService.deletePrincipalJobs(request.getJobId(), PlatformType.valueOf(request.getPlatformType()));

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchJobPrincipalId(JobTypeRequest request, StreamObserver<OptionalIdResponse> responseObserver) {
        Optional<Long> id = principalJobService.fetchJobPrincipalId(
            request.getJobId(), PlatformType.valueOf(request.getPlatformType()));

        OptionalIdResponse.Builder builder = OptionalIdResponse.newBuilder();

        id.ifPresent(builder::setId);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchLastJobId(PrincipalTypeRequest request, StreamObserver<OptionalIdResponse> responseObserver) {
        Optional<Long> id = principalJobService.fetchLastJobId(
            request.getPrincipalId(), PlatformType.valueOf(request.getPlatformType()));

        OptionalIdResponse.Builder builder = OptionalIdResponse.newBuilder();

        id.ifPresent(builder::setId);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchLastWorkflowJobId(
        PrincipalTypeRequest request, StreamObserver<OptionalIdResponse> responseObserver) {

        Optional<Long> id = principalJobService.fetchLastWorkflowJobId(
            request.getPrincipalId(), request.getWorkflowIdsList(), PlatformType.valueOf(request.getPlatformType()));

        OptionalIdResponse.Builder builder = OptionalIdResponse.newBuilder();

        id.ifPresent(builder::setId);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getJobIds(PrincipalTypeRequest request, StreamObserver<JobIdsResponse> responseObserver) {
        List<Long> jobIds = principalJobService.getJobIds(
            request.getPrincipalId(), PlatformType.valueOf(request.getPlatformType()));

        responseObserver.onNext(
            JobIdsResponse.newBuilder()
                .addAllJobIds(jobIds)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getJobIdsPage(GetJobIdsPageRequest request, StreamObserver<JobIdsPage> responseObserver) {
        Page<Long> jobIdsPage = principalJobService.getJobIds(
            request.hasStatus() ? Job.Status.valueOf(request.getStatus()) : null,
            request.hasStartDate() ? platformWorkflowExecutionProtoMapper.toInstant(request.getStartDate()) : null,
            request.hasEndDate() ? platformWorkflowExecutionProtoMapper.toInstant(request.getEndDate()) : null,
            request.getPrincipalIdsList(), PlatformType.valueOf(request.getPlatformType()),
            request.getWorkflowIdsList(), request.getOnlyRootJobs(), request.getPageNumber());

        responseObserver.onNext(platformWorkflowExecutionProtoMapper.toProto(jobIdsPage));
        responseObserver.onCompleted();
    }
}
