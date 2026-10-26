/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.CounterRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.Int64Response;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.SetCounterRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteCounterServiceClient implements CounterService {

    private static final String EXECUTION_APP = "execution-app";

    private final GrpcChannels grpcChannels;

    public RemoteCounterServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public void delete(long id) {
        getStub().delete(toIdRequest(id));
    }

    @Override
    public long decrement(long id) {
        Int64Response int64Response = getStub().decrement(toIdRequest(id));

        return int64Response.getValue();
    }

    @Override
    public void set(long id, long value) {
        getStub().set(
            SetCounterRequest.newBuilder()
                .setId(id)
                .setValue(value)
                .build());
    }

    private CounterRemoteServiceGrpc.CounterRemoteServiceBlockingStub getStub() {
        return CounterRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }

    private static IdRequest toIdRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }
}
