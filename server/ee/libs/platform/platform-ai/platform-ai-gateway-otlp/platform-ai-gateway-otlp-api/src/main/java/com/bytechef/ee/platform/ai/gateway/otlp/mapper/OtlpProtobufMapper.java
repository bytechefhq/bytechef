/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.mapper;

import com.bytechef.ee.platform.ai.gateway.otlp.dto.OtelSpanBatch;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;

/**
 * Maps an OTLP {@link ExportTraceServiceRequest} (protobuf or parsed JSON) into the platform's neutral span batch.
 * Implementations must not persist state — they are pure functions over the proto envelope.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface OtlpProtobufMapper {

    OtelSpanBatch toBatch(ExportTraceServiceRequest request);
}
