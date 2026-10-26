/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.service.CounterService;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.CounterRemoteServiceGrpc;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.IdRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.Int64Response;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.SetCounterRequest;
import com.google.protobuf.Empty;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;

/**
 * @version ee
 */
public class CounterRemoteServiceGrpcServer extends CounterRemoteServiceGrpc.CounterRemoteServiceImplBase {

    private final CounterService counterService;

    @SuppressFBWarnings("EI")
    public CounterRemoteServiceGrpcServer(
        CounterService counterService) {

        this.counterService = counterService;
    }

    @Override
    public void decrement(IdRequest request, StreamObserver<Int64Response> responseObserver) {
        responseObserver.onNext(
            Int64Response.newBuilder()
                .setValue(counterService.decrement(request.getId()))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void delete(IdRequest request, StreamObserver<Empty> responseObserver) {
        counterService.delete(request.getId());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void set(SetCounterRequest request, StreamObserver<Empty> responseObserver) {
        counterService.set(request.getId(), request.getValue());

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
