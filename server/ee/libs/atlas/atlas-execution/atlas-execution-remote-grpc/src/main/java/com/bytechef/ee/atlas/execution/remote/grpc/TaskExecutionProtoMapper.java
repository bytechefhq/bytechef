/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.atlas.execution.remote.grpc;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.ee.atlas.execution.remote.grpc.proto.RemoteTaskExecution;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.protobuf.ByteString;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;

/**
 * Maps task executions to their proto message and back through {@link ProtoJsonMapper}, leaving out the retry delay in
 * milliseconds, which is derived from the retry delay.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TaskExecutionProtoMapper {

    private final JsonBytes jsonBytes;
    private final ProtoJsonMapper protoJsonMapper;

    public TaskExecutionProtoMapper(ObjectMapper objectMapper) {
        this.jsonBytes = new JsonBytes(objectMapper);
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(TaskExecution.class, TaskExecutionMixIn.class)
                .build());
    }

    public ByteString toBytes(@Nullable Object value) {
        return jsonBytes.write(value);
    }

    public @Nullable Map<String, Object> toMap(ByteString bytes) {
        return jsonBytes.readMap(bytes);
    }

    public @Nullable Object toObject(ByteString bytes) {
        return jsonBytes.read(bytes);
    }

    public RemoteTaskExecution toProto(TaskExecution taskExecution) {
        return protoJsonMapper.toProto(taskExecution, RemoteTaskExecution.newBuilder());
    }

    public TaskExecution toTaskExecution(RemoteTaskExecution remoteTaskExecution) {
        return protoJsonMapper.toObject(remoteTaskExecution, TaskExecution.class);
    }

    @JsonIgnoreProperties("retryDelayMillis")
    private abstract static class TaskExecutionMixIn {
    }
}
