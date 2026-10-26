/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.client;

import com.bytechef.ee.platform.scheduler.remote.grpc.SchedulerProtoTimestamps;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ConnectionRefreshRequest;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ConnectionRefreshSchedulerRemoteServiceGrpc;
import com.bytechef.ee.platform.scheduler.remote.grpc.proto.ConnectionRefreshSchedulerRemoteServiceGrpc.ConnectionRefreshSchedulerRemoteServiceBlockingStub;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionRefreshSchedulerClient implements ConnectionRefreshScheduler {

    private static final String SCHEDULER_APP = "scheduler-app";

    private final GrpcChannels grpcChannels;

    public RemoteConnectionRefreshSchedulerClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public void cancelConnectionRefresh(Long connectionId, String tenantId) {
        getStub().cancelConnectionRefresh(toConnectionRefreshRequest(connectionId, null, tenantId));
    }

    @Override
    public void scheduleConnectionRefresh(Long connectionId, Instant tokenExpirationTime, String tenantId) {
        getStub().scheduleConnectionRefresh(toConnectionRefreshRequest(connectionId, tokenExpirationTime, tenantId));
    }

    private ConnectionRefreshSchedulerRemoteServiceBlockingStub getStub() {
        return ConnectionRefreshSchedulerRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SCHEDULER_APP));
    }

    private static ConnectionRefreshRequest toConnectionRefreshRequest(
        Long connectionId, Instant tokenExpirationTime, String tenantId) {

        ConnectionRefreshRequest.Builder builder = ConnectionRefreshRequest.newBuilder();

        if (connectionId != null) {
            builder.setConnectionId(connectionId);
        }

        if (tokenExpirationTime != null) {
            builder.setTokenExpirationTime(SchedulerProtoTimestamps.toTimestamp(tokenExpirationTime));
        }

        if (tenantId != null) {
            builder.setTenantId(tenantId);
        }

        return builder.build();
    }
}
