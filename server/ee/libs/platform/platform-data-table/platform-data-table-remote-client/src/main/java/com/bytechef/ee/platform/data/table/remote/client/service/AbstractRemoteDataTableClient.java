/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.ee.platform.data.table.remote.grpc.DataTableProtoMapper;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.function.Supplier;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
abstract class AbstractRemoteDataTableClient {

    private static final String CONFIGURATION_APP = "configuration-app";

    protected final DataTableProtoMapper dataTableProtoMapper;

    private final GrpcChannels grpcChannels;

    AbstractRemoteDataTableClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.dataTableProtoMapper = new DataTableProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
    }

    protected static <T> T call(Supplier<T> supplier) {
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

    protected DataTableRemoteServiceGrpc.DataTableRemoteServiceBlockingStub getStub() {
        return DataTableRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }
}
