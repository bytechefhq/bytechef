/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.task.remote.client.facade;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import com.bytechef.ee.automation.task.remote.grpc.ApprovalTaskProtoMapper;
import com.bytechef.ee.automation.task.remote.grpc.proto.ApprovalTaskFacadeRemoteServiceGrpc;
import com.bytechef.ee.automation.task.remote.grpc.proto.ApprovalTaskMessage;
import com.bytechef.ee.automation.task.remote.grpc.proto.PendingApprovalsRequest;
import com.bytechef.ee.automation.task.remote.grpc.proto.PendingApprovalsResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteApprovalTaskFacadeClient implements ApprovalTaskFacade {

    private static final String CONFIGURATION_APP = "configuration-app";

    private final ApprovalTaskProtoMapper approvalTaskProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteApprovalTaskFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.approvalTaskProtoMapper = new ApprovalTaskProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public ApprovalTask createApprovalTask(ApprovalTask approvalTask) {
        ApprovalTaskMessage approvalTaskMessage = call(
            () -> getStub().createApprovalTask(
                ApprovalTaskMessage.newBuilder()
                    .setApprovalTask(approvalTaskProtoMapper.write(approvalTask))
                    .build()));

        return approvalTaskProtoMapper.toApprovalTask(approvalTaskMessage.getApprovalTask());
    }

    @Override
    public ApprovalTask createManualApprovalTask(ApprovalTask approvalTask) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteApprovalTask(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApprovalTask getApprovalTask(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ApprovalTask> getApprovalTasks(@Nullable Integer environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ApprovalTask> getApprovalTasks(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PendingApproval> getPendingApprovals(@Nullable Integer environmentId) {
        PendingApprovalsRequest.Builder builder = PendingApprovalsRequest.newBuilder();

        if (environmentId != null) {
            builder.setEnvironmentId(environmentId);
        }

        PendingApprovalsResponse pendingApprovalsResponse = call(() -> getStub().getPendingApprovals(builder.build()));

        return approvalTaskProtoMapper.toPendingApprovals(pendingApprovalsResponse.getPendingApprovals());
    }

    @Override
    public ApprovalTask updateApprovalTask(ApprovalTask approvalTask) {
        throw new UnsupportedOperationException();
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (StatusRuntimeException statusRuntimeException) {
            Status status = statusRuntimeException.getStatus();

            if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                throw new IllegalArgumentException(status.getDescription(), statusRuntimeException);
            }

            throw statusRuntimeException;
        }
    }

    private ApprovalTaskFacadeRemoteServiceGrpc.ApprovalTaskFacadeRemoteServiceBlockingStub getStub() {
        return ApprovalTaskFacadeRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }
}
