/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.ee.platform.data.table.remote.grpc.proto.ResolveWorkspaceByJobPrincipalIdRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ResolveWorkspaceByWorkflowIdRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ResolveWorkspaceByWorkflowUuidRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.WorkspaceIdResponse;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteDataTableWorkspaceResolverClient extends AbstractRemoteDataTableClient
    implements DataTableWorkspaceResolver {

    public RemoteDataTableWorkspaceResolverClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public OptionalLong resolveByWorkflowId(String workflowId) {
        return toOptionalLong(
            call(
                () -> getStub().resolveWorkspaceByWorkflowId(
                    ResolveWorkspaceByWorkflowIdRequest.newBuilder()
                        .setWorkflowId(workflowId)
                        .build())));
    }

    @Override
    public OptionalLong resolveByJobPrincipalId(long jobPrincipalId, PlatformType platformType) {
        return toOptionalLong(
            call(
                () -> getStub().resolveWorkspaceByJobPrincipalId(
                    ResolveWorkspaceByJobPrincipalIdRequest.newBuilder()
                        .setJobPrincipalId(jobPrincipalId)
                        .setPlatformType(platformType.name())
                        .build())));
    }

    @Override
    public OptionalLong resolveByWorkflowUuid(String workflowUuid) {
        return toOptionalLong(
            call(
                () -> getStub().resolveWorkspaceByWorkflowUuid(
                    ResolveWorkspaceByWorkflowUuidRequest.newBuilder()
                        .setWorkflowUuid(workflowUuid)
                        .build())));
    }

    private static OptionalLong toOptionalLong(WorkspaceIdResponse workspaceIdResponse) {
        if (!workspaceIdResponse.hasWorkspaceId()) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(workspaceIdResponse.getWorkspaceId());
    }
}
