/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import java.util.List;

/**
 * Stateful, single-threaded redactor for masking PII/secrets in a streamed completion whose text arrives in fragments
 * that may split a sensitive value across two chunks. It buffers a bounded lookahead window and, each step, emits only
 * the leading portion of the buffer that ends at a "safe cut" — a position no matched span crosses. Any value that
 * straddles the tentative cut pulls the cut back to that value's start so the whole match stays buffered and is
 * redacted as one unit; any value still being received sits inside the retained window until it completes (or is
 * flushed at stream end). A secret split across chunk boundaries is therefore never emitted in the clear.
 *
 * <p>
 * Correctness: because no complete match crosses the safe cut, redacting the emitted segment in isolation equals the
 * corresponding portion of redacting the whole buffer, so the concatenation of every {@link #push} result plus the
 * final {@link #flush} equals {@code redactAll(fullStream)}. The window bounds latency and also the worst case: a value
 * that is still incomplete (not yet matchable) and longer than the window may have a prefix emitted before its pattern
 * can match — the documented trade-off of scanning a stream without buffering it whole. Set the window comfortably
 * above the longest value you need to guarantee; the default covers every fixed-shape key/token and typical JWTs.
 * </p>
 *
 * <p>
 * Not thread-safe; use one instance per stream subscription.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class StreamingResponseRedactor {

    private static final int DEFAULT_WINDOW = 512;

    private final StringBuilder carry = new StringBuilder();
    private final int window;

    private boolean redacted;

    public StreamingResponseRedactor() {
        this(DEFAULT_WINDOW);
    }

    StreamingResponseRedactor(int window) {
        this.window = window;
    }

    /**
     * Accepts the next streamed text fragment and returns the portion that is now safe to emit (redacted), which may be
     * empty when everything so far must still be held to protect against a value straddling a chunk boundary.
     *
     * @param text the next streamed fragment (may be {@code null} or empty)
     * @return the redacted text safe to emit now, possibly empty
     */
    public String push(String text) {
        if (text != null && !text.isEmpty()) {
            carry.append(text);
        }

        if (carry.length() <= window) {
            return "";
        }

        // Never emit within `window` chars of the end, so a value still arriving has room to complete before its span
        // is judged.
        int safeCut = carry.length() - window;

        List<int[]> ranges = AiGuardrails.sensitiveMatchRanges(carry.toString());

        // Pull the cut back to the start of any matched span it lands inside, to a fixpoint (an earlier span may
        // in turn straddle the pulled-back cut).
        boolean pulled = true;

        while (pulled) {
            pulled = false;

            for (int[] range : ranges) {
                if (range[0] < safeCut && range[1] > safeCut) {
                    safeCut = range[0];
                    pulled = true;
                }
            }
        }

        if (safeCut <= 0) {
            return "";
        }

        String rawSegment = carry.substring(0, safeCut);
        String emitted = AiGuardrails.redactAll(rawSegment);

        if (!emitted.equals(rawSegment)) {
            redacted = true;
        }

        carry.delete(0, safeCut);

        return emitted;
    }

    /**
     * Redacts and returns everything still buffered, to be emitted once the stream completes. Idempotent afterwards
     * (returns empty until more is pushed).
     *
     * @return the redacted remainder, possibly empty
     */
    public String flush() {
        if (carry.length() == 0) {
            return "";
        }

        String raw = carry.toString();
        String remainder = AiGuardrails.redactAll(raw);

        if (!remainder.equals(raw)) {
            redacted = true;
        }

        carry.setLength(0);

        return remainder;
    }

    /**
     * Returns whether any {@link #push} or {@link #flush} so far actually masked content, so the caller can emit a
     * single redaction metric per stream instead of one per chunk.
     *
     * @return {@code true} if at least one value has been redacted over the stream's lifetime
     */
    public boolean isRedacted() {
        return redacted;
    }
}
