/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.tool.invocation.log.remote.grpc;

import com.bytechef.ee.platform.tool.invocation.log.remote.grpc.proto.RecordToolExecutionRequest;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.tool.execution.ToolExecutionEvent;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ToolExecutionEventProtoMapper {

    private static final TypeReference<ToolExecutionEvent> TOOL_EXECUTION_EVENT_TYPE_REFERENCE =
        new TypeReference<>() {};

    private final JsonBytes jsonBytes;

    public ToolExecutionEventProtoMapper(ObjectMapper objectMapper) {
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    public RecordToolExecutionRequest toProto(ToolExecutionEvent toolExecutionEvent) {
        return RecordToolExecutionRequest.newBuilder()
            .setToolExecutionEvent(jsonBytes.write(toolExecutionEvent))
            .build();
    }

    public ToolExecutionEvent toToolExecutionEvent(RecordToolExecutionRequest recordToolExecutionRequest) {
        return Objects.requireNonNull(
            jsonBytes.read(recordToolExecutionRequest.getToolExecutionEvent(), TOOL_EXECUTION_EVENT_TYPE_REFERENCE));
    }
}
