/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.grpc;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.JobTypeRequest;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.OptionalIdResponse;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.PrincipalJobRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.PrincipalTypeRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.Optional;

/**
 * @version ee
 */
public class PrincipalJobRemoteServiceGrpcServer
    extends PrincipalJobRemoteServiceGrpc.PrincipalJobRemoteServiceImplBase {

    private final PrincipalJobService principalJobService;

    @SuppressFBWarnings("EI")
    public PrincipalJobRemoteServiceGrpcServer(
        PrincipalJobService principalJobService) {

        this.principalJobService = principalJobService;
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
}
