/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log.remote.grpc;

import com.bytechef.ee.platform.external.invocation.log.remote.grpc.proto.RecordExternalInvocationRequest;
import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ExternalInvocationEventProtoMapper {

    private static final TypeReference<ExternalInvocationEvent> EXTERNAL_INVOCATION_EVENT_TYPE_REFERENCE =
        new TypeReference<>() {};

    private final JsonBytes jsonBytes;

    public ExternalInvocationEventProtoMapper(ObjectMapper objectMapper) {
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    public RecordExternalInvocationRequest toProto(ExternalInvocationEvent externalInvocationEvent) {
        return RecordExternalInvocationRequest.newBuilder()
            .setExternalInvocationEvent(jsonBytes.write(externalInvocationEvent))
            .build();
    }

    public ExternalInvocationEvent
        toExternalInvocationEvent(RecordExternalInvocationRequest recordExternalInvocationRequest) {
        return Objects.requireNonNull(
            jsonBytes.read(recordExternalInvocationRequest.getExternalInvocationEvent(),
                EXTERNAL_INVOCATION_EVENT_TYPE_REFERENCE));
    }
}
