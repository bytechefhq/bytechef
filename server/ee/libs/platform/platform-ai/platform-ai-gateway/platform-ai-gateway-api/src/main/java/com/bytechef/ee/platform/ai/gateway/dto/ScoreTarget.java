/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

/**
 * Sealed sum type for the score target — exactly one of trace or span. Replaces the previous "two nullable fields with
 * runtime XOR check" pattern with a compile-time-exhaustive dispatch surface, so a future variant added without a
 * matching {@code case} fails the compiler instead of slipping through to a runtime branch.
 *
 * <p>
 * The wire shape on {@link AiExternalScoreBatchItem} keeps the legacy {@code traceId} / {@code spanId} fields for
 * backwards compatibility — JSON callers do not see this type. Server-side dispatch uses {@link #of(Long, Long)} to
 * canonicalise the wire shape into the typed sum at the DTO boundary.
 *
 * @author Ivica Cardic
 * @version ee
 */
public sealed interface ScoreTarget permits ScoreTarget.Trace, ScoreTarget.Span {

    /**
     * Builds a {@link ScoreTarget} from the (traceId, spanId) wire shape. Exactly one must be non-null — both null or
     * both non-null throws {@link IllegalArgumentException} with a message naming the offending field shape (used by
     * the DTO's compact constructor to surface as HTTP 400).
     */
    static ScoreTarget of(Long traceId, Long spanId) {
        if (traceId == null && spanId == null) {
            throw new IllegalArgumentException("Exactly one of traceId or spanId must be non-null");
        }

        if (traceId != null && spanId != null) {
            throw new IllegalArgumentException("Only one of traceId or spanId may be set");
        }

        return spanId != null ? new Span(spanId) : new Trace(traceId);
    }

    record Trace(Long traceId) implements ScoreTarget {

        public Trace {
            if (traceId == null) {
                throw new IllegalArgumentException("traceId must not be null");
            }
        }
    }

    record Span(Long spanId) implements ScoreTarget {

        public Span {
            if (spanId == null) {
                throw new IllegalArgumentException("spanId must not be null");
            }
        }
    }
}
