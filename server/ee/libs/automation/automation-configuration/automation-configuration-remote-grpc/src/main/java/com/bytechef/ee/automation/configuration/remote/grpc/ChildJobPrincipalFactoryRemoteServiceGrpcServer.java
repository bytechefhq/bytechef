/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.grpc;

import com.bytechef.ee.automation.configuration.remote.grpc.proto.ChildJobPrincipalFactoryRemoteServiceGrpc;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.CreateChildJobRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.CreatePrincipalLinkedJobRequest;
import com.bytechef.ee.automation.configuration.remote.grpc.proto.IdResponse;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ChildJobPrincipalFactoryRemoteServiceGrpcServer
    extends ChildJobPrincipalFactoryRemoteServiceGrpc.ChildJobPrincipalFactoryRemoteServiceImplBase {

    private final AutomationConfigurationProtoMapper automationConfigurationProtoMapper;
    private final ChildJobPrincipalFactory childJobPrincipalFactory;

    @SuppressFBWarnings("EI")
    public ChildJobPrincipalFactoryRemoteServiceGrpcServer(
        ChildJobPrincipalFactory childJobPrincipalFactory,
        AutomationConfigurationProtoMapper automationConfigurationProtoMapper) {

        this.automationConfigurationProtoMapper = automationConfigurationProtoMapper;
        this.childJobPrincipalFactory = childJobPrincipalFactory;
    }

    @Override
    public void createChildJob(CreateChildJobRequest request, StreamObserver<IdResponse> responseObserver) {
        long childJobId = childJobPrincipalFactory.createChildJob(
            request.getParentJobId(),
            automationConfigurationProtoMapper.toJobParametersDTO(request.getJobParameters()));

        responseObserver.onNext(
            IdResponse.newBuilder()
                .setId(childJobId)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void createPrincipalLinkedJob(
        CreatePrincipalLinkedJobRequest request, StreamObserver<IdResponse> responseObserver) {

        long jobId = childJobPrincipalFactory.createPrincipalLinkedJob(
            request.getReferenceJobId(),
            automationConfigurationProtoMapper.toJobParametersDTO(request.getJobParameters()));

        responseObserver.onNext(
            IdResponse.newBuilder()
                .setId(jobId)
                .build());
        responseObserver.onCompleted();
    }
}
