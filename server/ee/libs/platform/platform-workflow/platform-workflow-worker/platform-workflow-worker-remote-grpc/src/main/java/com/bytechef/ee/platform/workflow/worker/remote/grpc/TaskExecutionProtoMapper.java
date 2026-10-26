/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.worker.remote.grpc;

import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.ee.platform.workflow.worker.remote.grpc.proto.RemoteTaskExecution;
import com.bytechef.ee.remote.grpc.ProtoJsonMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    private final ProtoJsonMapper protoJsonMapper;

    public TaskExecutionProtoMapper(ObjectMapper objectMapper) {
        this.protoJsonMapper = new ProtoJsonMapper(
            objectMapper.rebuild()
                .addMixIn(TaskExecution.class, TaskExecutionMixIn.class)
                .build());
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
