/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

/**
 * Machine-readable taxonomy of {@link AiEvalExecution} failures. Persisted as INT ordinal so dashboards and alerts can
 * branch without prose-matching the free-text {@link AiEvalExecution#getErrorMessage()}.
 *
 * <p>
 * <strong>Append-only:</strong> ordinal values are persisted; reordering would silently flip the meaning of historical
 * rows. Add new variants only at the end.
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum AiEvalExecutionFailureReason {

    /**
     * Catch-all for anything not classified below. Indicates a gap in the executor's error taxonomy worth filling
     * explicitly when the operator sees it on a dashboard.
     */
    UNKNOWN,

    /**
     * The model identifier on the rule did not resolve to any registered provider.
     */
    PROVIDER_NOT_FOUND,

    /**
     * The provider was found but the score config or related lookup row was not. Distinct from
     * {@code PROVIDER_NOT_FOUND} so a misconfigured rule (no score config) is debuggable separately from a broken
     * provider catalog.
     */
    SCORE_CONFIG_NOT_FOUND,

    /**
     * The chat model returned, but parsing its text into a typed {@link AiEvalScore} failed (e.g., model emitted prose
     * instead of a number for a NUMERIC score). High-volume occurrence indicates the prompt template needs hardening.
     */
    LLM_RESPONSE_PARSE_FAILED,

    /**
     * Provider HTTP error, rate-limit, or upstream timeout. Operator-actionable through the cooldown tracker /
     * provider-status dashboards rather than through eval-rule changes.
     */
    PROVIDER_REQUEST_FAILED,

    /**
     * Database write failure on the score persist or the execution status update. Surfaced separately so DB-pressure
     * alerts trigger only when this counter climbs (and not when, e.g., the LLM is misbehaving).
     */
    PERSISTENCE_FAILED,

    /**
     * The rule's {@code model} field was null. Distinct from {@code PROVIDER_NOT_FOUND} (which means the model string
     * was present but did not resolve) so a misconfigured rule (model nulled out by a bad migration or a UI bug) is
     * debuggable separately from a healthy rule pointing at a removed provider. Mirrors the
     * {@code MODEL_IDENTIFIER_MISSING} reason emitted by {@code OtlpCostResolver} for the analogous OTLP-side bug.
     */
    MISSING_MODEL,

    /**
     * Provider request that retried internally (per the chat client's RetryTemplate) and finally failed after the retry
     * budget was exhausted. Distinct from {@link #PROVIDER_REQUEST_FAILED} so a workspace whose eval rule is calling a
     * down-hard provider (10/10 attempts fail) is operationally distinguishable on dashboards from one whose provider
     * is intermittently 5xx-ing (1/10 attempts fail). Detection uses {@link Throwable#getSuppressed()}: Spring's
     * {@code RetryTemplate} stacks the per-attempt failures as suppressed exceptions on the final throw — a non-empty
     * suppressed list is the canonical signal that retry was attempted. The signal is conservative: a future retry
     * implementation that does not stack suppressed failures will silently fall back to the
     * {@code PROVIDER_REQUEST_FAILED} bucket, which is correct behavior — under-attribution to this bucket is
     * preferable to over-attribution.
     */
    PROVIDER_REQUEST_FAILED_AFTER_RETRIES
}
