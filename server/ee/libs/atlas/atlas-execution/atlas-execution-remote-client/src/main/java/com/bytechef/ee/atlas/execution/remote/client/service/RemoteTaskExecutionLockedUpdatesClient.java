/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.client.service;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskExecutionLockedUpdates;
import com.bytechef.ee.atlas.execution.remote.grpc.AtlasExecutionProtoMapper;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.PutOutputEntryRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.SetEndDateRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.TaskExecutionLockedUpdatesRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteTaskExecutionLockedUpdatesClient implements TaskExecutionLockedUpdates {

    private static final String EXECUTION_APP = "execution-app";

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final GrpcChannels grpcChannels;
    private final JsonBytes jsonBytes;

    public RemoteTaskExecutionLockedUpdatesClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        this.atlasExecutionProtoMapper = new AtlasExecutionProtoMapper(objectMapper);
        this.grpcChannels = grpcChannels;
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    @Override
    public TaskExecution putOutputEntry(long taskExecutionId, String key, @Nullable Object value) {
        PutOutputEntryRequest putOutputEntryRequest = PutOutputEntryRequest.newBuilder()
            .setTaskExecutionId(taskExecutionId)
            .setKey(key)
            .setValue(jsonBytes.write(value))
            .build();

        return atlasExecutionProtoMapper.toTaskExecution(getStub().putOutputEntry(putOutputEntryRequest));
    }

    @Override
    public TaskExecution setEndDate(long taskExecutionId, Instant endDate) {
        SetEndDateRequest setEndDateRequest = SetEndDateRequest.newBuilder()
            .setTaskExecutionId(taskExecutionId)
            .setEndDate(
                Timestamp.newBuilder()
                    .setSeconds(endDate.getEpochSecond())
                    .setNanos(endDate.getNano())
                    .build())
            .build();

        return atlasExecutionProtoMapper.toTaskExecution(getStub().setEndDate(setEndDateRequest));
    }

    private TaskExecutionLockedUpdatesRemoteServiceGrpc.TaskExecutionLockedUpdatesRemoteServiceBlockingStub
        getStub() {

        return TaskExecutionLockedUpdatesRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(EXECUTION_APP));
    }
}
