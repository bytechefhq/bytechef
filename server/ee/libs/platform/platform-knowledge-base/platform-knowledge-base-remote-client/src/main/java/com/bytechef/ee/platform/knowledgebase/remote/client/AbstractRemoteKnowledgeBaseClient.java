/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.knowledgebase.remote.client;

import com.bytechef.ee.platform.knowledgebase.remote.grpc.KnowledgeBaseProtoMapper;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.IdRequest;
import com.bytechef.ee.platform.knowledgebase.remote.grpc.proto.KnowledgeBaseRemoteServiceGrpc;
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
abstract class AbstractRemoteKnowledgeBaseClient {

    private static final String CONFIGURATION_APP = "configuration-app";

    protected final KnowledgeBaseProtoMapper knowledgeBaseProtoMapper;

    private final GrpcChannels grpcChannels;

    AbstractRemoteKnowledgeBaseClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.grpcChannels = grpcChannels;
        this.knowledgeBaseProtoMapper = new KnowledgeBaseProtoMapper(objectMapper);
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

    protected KnowledgeBaseRemoteServiceGrpc.KnowledgeBaseRemoteServiceBlockingStub getStub() {
        return KnowledgeBaseRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(CONFIGURATION_APP));
    }

    protected static IdRequest idRequest(long id) {
        return IdRequest.newBuilder()
            .setId(id)
            .build();
    }
}
