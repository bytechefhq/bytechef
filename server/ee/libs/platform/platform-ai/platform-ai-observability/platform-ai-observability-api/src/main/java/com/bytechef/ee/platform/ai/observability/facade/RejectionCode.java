/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

/**
 * Stable wire-facing taxonomy of rejection reasons used in {@link RejectionDetail#code()} for {@link OtlpIngestResult}
 * and {@link AiExternalScoreBatchResult}. Surfaced via {@link Enum#name()} so the JSON wire shape stays unchanged while
 * new call sites get compile-time pinning of the token list.
 *
 * <p>
 * <strong>Wire-stable</strong>: dashboards and clients branch on these values. Append new variants only — never remove
 * or rename. The enum is NOT persisted by ordinal anywhere; this constraint is purely about the wire body.
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum RejectionCode {

    /** Mapper-stage protobuf-to-DTO failure (malformed trace/span ids, inverted timestamps, etc.). */
    REJECTED_BY_MAPPER,

    /**
     * Mapper-stage <em>warning</em>: spans were persisted but the resource-attribute decode for their parent
     * {@code ResourceSpans} failed, so they carry empty resource attributes ({@code service.name} and friends are
     * absent). Distinct from {@link #REJECTED_BY_MAPPER} because the spans DID persist — operators see a "degraded
     * outcome" breadcrumb on the wire response rather than a counter-only signal that fails to reach OTel exporters.
     * Surfaced via {@link OtlpIngestResult#warnings()}, not {@code rejectionReasons}, so the partial-success
     * conservation invariant on rejected counts is preserved.
     */
    MAPPER_RESOURCE_DECODE_WARNING,

    /** OTLP span ingest path: span lacked the {@code gen_ai.system} attribute. */
    REJECTED_NO_SYSTEM,

    /** OTLP span ingest path: span lacked a status. */
    REJECTED_NO_STATUS,

    /** Per-span persist failure (DB constraint other than dedup, or transient connection error). */
    REJECTED_PERSIST_FAILED,

    /** JVM-level distress (OOM, StackOverflow) caught mid-batch. */
    REJECTED_INTERNAL,

    /** External scores batch path: cross-tenant write attempt blocked by the workspace boundary. */
    WORKSPACE_BOUNDARY,

    /** External scores batch path: target trace/span did not resolve under this workspace. */
    NOT_FOUND,

    /** External scores batch path: caller-supplied value failed shape validation. */
    VALIDATION_FAILED,

    /** External scores batch path: row-level persist failure distinct from validation. */
    PERSIST_FAILED,

    /** External scores batch path: JVM-level distress mid-batch. */
    BATCH_INTERNAL_ERROR,

    /**
     * External scores batch path: row was persisted but the post-commit listener / counter increment failed. Distinct
     * from {@link #PERSIST_FAILED} so operators can alert on listener pain without chasing phantom DB issues.
     */
    LISTENER_FAILED
}
