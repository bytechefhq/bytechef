/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.execution.remote.client.facade;

import com.bytechef.ee.platform.workflow.execution.remote.grpc.PlatformWorkflowExecutionProtoMapper;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ConnectionLifecycleFacadeRemoteServiceGrpc;
import com.bytechef.ee.platform.workflow.execution.remote.grpc.proto.ConnectionRefreshRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionLifecycleFacadeClient implements ConnectionLifecycleFacade {

    private static final String EXECUTION_APP = "execution-app";

    private final PlatformWorkflowExecutionProtoMapper platformWorkflowExecutionProtoMapper;
    private final GrpcChannels grpcChannels;

    public RemoteConnectionLifecycleFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.platformWorkflowExecutionProtoMapper = new PlatformWorkflowExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    @Override
    public void scheduleConnectionRefresh(
        Long connectionId, Map<String, ?> parameters, String tenantId) {

        getStub().scheduleConnectionRefresh(toConnectionRefreshRequest(connectionId, parameters, tenantId));
    }

    @Override
    public void deleteScheduledConnectionRefresh(
        Long connectionId, String tenantId) {

        getStub().deleteScheduledConnectionRefresh(toConnectionRefreshRequest(connectionId, null, tenantId));
    }

    private ConnectionLifecycleFacadeRemoteServiceGrpc.ConnectionLifecycleFacadeRemoteServiceBlockingStub getStub() {
        return ConnectionLifecycleFacadeRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private ConnectionRefreshRequest toConnectionRefreshRequest(
        @Nullable Long connectionId, @Nullable Map<String, ?> parameters, @Nullable String tenantId) {

        ConnectionRefreshRequest.Builder builder = ConnectionRefreshRequest.newBuilder()
            .setParameters(platformWorkflowExecutionProtoMapper.toBytes(parameters));

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        if (tenantId != null) {
            builder.setTenantId(tenantId);
        }

        return builder.build();
    }
}
