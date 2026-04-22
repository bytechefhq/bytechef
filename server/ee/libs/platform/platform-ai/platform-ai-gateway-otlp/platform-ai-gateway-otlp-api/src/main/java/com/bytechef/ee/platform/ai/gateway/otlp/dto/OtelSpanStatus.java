/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.dto;

/**
 * Mirrors {@code opentelemetry.proto.trace.v1.Status.StatusCode} — UNSET/OK/ERROR — as a platform-neutral enum used at
 * the OTLP DTO boundary so the api module does not leak the protobuf dependency to downstream callers.
 *
 * <p>
 * Not persisted directly. The OTLP ingest facade maps each value into {@code AiObservabilitySpanStatus} before the span
 * row is written. Downstream {@code switch} statements (e.g., {@code AiObservabilityOtlpIngestFacadeImpl}) match case
 * labels by name, so reordering variants does NOT silently misroute — a compile-time exhaustiveness check fires if a
 * new variant is added without updating the switch, and renaming a variant breaks the case label outright.
 *
 * <p>
 * The order is still pinned by {@code OtelSpanStatusTest#testOrdinalContract} as defence-in-depth: a future code path
 * that switches to ordinal-based serialization (e.g., a wire-format optimisation) would silently misroute under reorder
 * otherwise.
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum OtelSpanStatus {
    UNSET,
    OK,
    ERROR
}
