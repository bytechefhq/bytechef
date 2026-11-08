/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.dto;

/**
 * @author Ivica Cardic
 * @version ee
 */
public sealed interface ScoreTarget permits ScoreTarget.Trace, ScoreTarget.Span {

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
