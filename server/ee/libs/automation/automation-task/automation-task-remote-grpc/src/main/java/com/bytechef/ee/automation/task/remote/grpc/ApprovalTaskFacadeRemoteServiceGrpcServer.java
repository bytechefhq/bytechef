/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.task.remote.grpc;

import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import com.bytechef.ee.automation.task.remote.grpc.proto.ApprovalTaskFacadeRemoteServiceGrpc;
import com.bytechef.ee.automation.task.remote.grpc.proto.ApprovalTaskMessage;
import com.bytechef.ee.automation.task.remote.grpc.proto.PendingApprovalsRequest;
import com.bytechef.ee.automation.task.remote.grpc.proto.PendingApprovalsResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ApprovalTaskFacadeRemoteServiceGrpcServer
    extends ApprovalTaskFacadeRemoteServiceGrpc.ApprovalTaskFacadeRemoteServiceImplBase {

    private final ApprovalTaskFacade approvalTaskFacade;
    private final ApprovalTaskProtoMapper approvalTaskProtoMapper;

    @SuppressFBWarnings("EI")
    public ApprovalTaskFacadeRemoteServiceGrpcServer(
        ApprovalTaskFacade approvalTaskFacade, ApprovalTaskProtoMapper approvalTaskProtoMapper) {

        this.approvalTaskFacade = approvalTaskFacade;
        this.approvalTaskProtoMapper = approvalTaskProtoMapper;
    }

    @Override
    public void createApprovalTask(
        ApprovalTaskMessage request, StreamObserver<ApprovalTaskMessage> responseObserver) {

        responseObserver.onNext(
            ApprovalTaskMessage.newBuilder()
                .setApprovalTask(
                    approvalTaskProtoMapper.write(
                        approvalTaskFacade.createApprovalTask(
                            approvalTaskProtoMapper.toApprovalTask(request.getApprovalTask()))))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getPendingApprovals(
        PendingApprovalsRequest request, StreamObserver<PendingApprovalsResponse> responseObserver) {

        Integer environmentId = request.hasEnvironmentId() ? request.getEnvironmentId() : null;

        responseObserver.onNext(
            PendingApprovalsResponse.newBuilder()
                .setPendingApprovals(
                    approvalTaskProtoMapper.write(approvalTaskFacade.getPendingApprovals(environmentId)))
                .build());
        responseObserver.onCompleted();
    }
}
