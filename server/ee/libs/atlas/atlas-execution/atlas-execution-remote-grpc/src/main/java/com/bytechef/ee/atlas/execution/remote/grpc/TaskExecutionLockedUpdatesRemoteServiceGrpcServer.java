/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.file.storage.TaskExecutionLockedUpdates;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.PutOutputEntryRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteTaskExecution;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.SetEndDateRequest;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.TaskExecutionLockedUpdatesRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.google.protobuf.Timestamp;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TaskExecutionLockedUpdatesRemoteServiceGrpcServer
    extends TaskExecutionLockedUpdatesRemoteServiceGrpc.TaskExecutionLockedUpdatesRemoteServiceImplBase {

    private final AtlasExecutionProtoMapper atlasExecutionProtoMapper;
    private final JsonBytes jsonBytes;
    private final TaskExecutionLockedUpdates taskExecutionLockedUpdates;

    @SuppressFBWarnings("EI")
    public TaskExecutionLockedUpdatesRemoteServiceGrpcServer(
        TaskExecutionLockedUpdates taskExecutionLockedUpdates, AtlasExecutionProtoMapper atlasExecutionProtoMapper,
        ObjectMapper objectMapper) {

        this.atlasExecutionProtoMapper = atlasExecutionProtoMapper;
        this.jsonBytes = new JsonBytes(objectMapper);
        this.taskExecutionLockedUpdates = taskExecutionLockedUpdates;
    }

    @Override
    public void putOutputEntry(
        PutOutputEntryRequest putOutputEntryRequest, StreamObserver<RemoteTaskExecution> responseObserver) {

        TaskExecution taskExecution = taskExecutionLockedUpdates.putOutputEntry(
            putOutputEntryRequest.getTaskExecutionId(), putOutputEntryRequest.getKey(),
            jsonBytes.read(putOutputEntryRequest.getValue()));

        responseObserver.onNext(atlasExecutionProtoMapper.toProto(taskExecution));
        responseObserver.onCompleted();
    }

    @Override
    public void setEndDate(SetEndDateRequest setEndDateRequest, StreamObserver<RemoteTaskExecution> responseObserver) {
        Timestamp endDate = setEndDateRequest.getEndDate();

        TaskExecution taskExecution = taskExecutionLockedUpdates.setEndDate(
            setEndDateRequest.getTaskExecutionId(), Instant.ofEpochSecond(endDate.getSeconds(), endDate.getNanos()));

        responseObserver.onNext(atlasExecutionProtoMapper.toProto(taskExecution));
        responseObserver.onCompleted();
    }
}
