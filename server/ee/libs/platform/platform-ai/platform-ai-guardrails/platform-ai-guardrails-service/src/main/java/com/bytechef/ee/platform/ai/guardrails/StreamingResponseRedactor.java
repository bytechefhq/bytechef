/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.SensitiveKind;
import com.bytechef.platform.ai.sensitivedata.SensitiveSpan;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiToken;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class StreamingResponseRedactor {

    private static final int DEFAULT_WINDOW = 512;

    private final StringBuilder carry = new StringBuilder();
    private final EnumSet<SensitiveKind> kinds;
    private final @Nullable AiGuardrailMetrics metrics;
    private final double minConfidence;
    private final @Nullable PiiTokenSession session;
    private final SensitiveDataRedactor sensitiveDataRedactor;
    private final int window;

    private boolean redacted;

    public StreamingResponseRedactor(SensitiveDataRedactor sensitiveDataRedactor) {
        this(
            sensitiveDataRedactor, DEFAULT_WINDOW, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, null, null,
            EnumSet.allOf(SensitiveKind.class));
    }

    public StreamingResponseRedactor(
        SensitiveDataRedactor sensitiveDataRedactor, @Nullable AiGuardrailMetrics metrics) {

        this(
            sensitiveDataRedactor, DEFAULT_WINDOW, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, metrics, null,
            EnumSet.allOf(SensitiveKind.class));
    }

    StreamingResponseRedactor(
        SensitiveDataRedactor sensitiveDataRedactor, double minConfidence, @Nullable AiGuardrailMetrics metrics) {

        this(sensitiveDataRedactor, DEFAULT_WINDOW, minConfidence, metrics, null, EnumSet.allOf(SensitiveKind.class));
    }

    StreamingResponseRedactor(
        SensitiveDataRedactor sensitiveDataRedactor, double minConfidence, @Nullable AiGuardrailMetrics metrics,
        EnumSet<SensitiveKind> kinds) {

        this(sensitiveDataRedactor, DEFAULT_WINDOW, minConfidence, metrics, null, kinds);
    }

    public StreamingResponseRedactor(
        SensitiveDataRedactor sensitiveDataRedactor, @Nullable AiGuardrailMetrics metrics,
        @Nullable PiiTokenSession session) {

        this(
            sensitiveDataRedactor, DEFAULT_WINDOW, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, metrics, session,
            EnumSet.allOf(SensitiveKind.class));
    }

    StreamingResponseRedactor(
        SensitiveDataRedactor sensitiveDataRedactor, @Nullable AiGuardrailMetrics metrics, PiiTokenSession session,
        EnumSet<SensitiveKind> kinds) {

        this(
            sensitiveDataRedactor, DEFAULT_WINDOW, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, metrics, session,
            kinds);
    }

    StreamingResponseRedactor(
        SensitiveDataRedactor sensitiveDataRedactor, double minConfidence, @Nullable AiGuardrailMetrics metrics,
        PiiTokenSession session, EnumSet<SensitiveKind> kinds) {

        this(sensitiveDataRedactor, DEFAULT_WINDOW, minConfidence, metrics, session, kinds);
    }

    StreamingResponseRedactor(SensitiveDataRedactor sensitiveDataRedactor, int window) {
        this(
            sensitiveDataRedactor, window, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, null, null,
            EnumSet.allOf(SensitiveKind.class));
    }

    StreamingResponseRedactor(
        SensitiveDataRedactor sensitiveDataRedactor, int window, @Nullable AiGuardrailMetrics metrics) {

        this(
            sensitiveDataRedactor, window, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, metrics, null,
            EnumSet.allOf(SensitiveKind.class));
    }

    StreamingResponseRedactor(
        SensitiveDataRedactor sensitiveDataRedactor, int window, @Nullable PiiTokenSession session) {

        this(
            sensitiveDataRedactor, window, SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE, null, session,
            EnumSet.allOf(SensitiveKind.class));
    }

    StreamingResponseRedactor(
        SensitiveDataRedactor sensitiveDataRedactor, int window, double minConfidence,
        @Nullable AiGuardrailMetrics metrics, @Nullable PiiTokenSession session, EnumSet<SensitiveKind> kinds) {

        this.sensitiveDataRedactor = sensitiveDataRedactor;
        this.window = window;
        this.minConfidence = minConfidence;
        this.metrics = metrics;
        this.session = session;
        this.kinds = EnumSet.copyOf(kinds);
    }

    public String push(String text) {
        if (text != null && !text.isEmpty()) {
            carry.append(text);
        }

        if (carry.length() <= window) {
            return "";
        }

        int safeCut = carry.length() - window;

        List<SensitiveSpan> candidates =
            kinds.isEmpty() ? List.of() : sensitiveDataRedactor.detectCandidates(carry.toString(), metrics);
        List<int[]> tokenRanges = session == null ? List.of() : tokenRanges(carry);

        boolean pulled = true;

        while (pulled) {
            pulled = false;

            for (SensitiveSpan candidate : candidates) {
                if (candidate.start() < safeCut && candidate.end() > safeCut) {
                    safeCut = candidate.start();
                    pulled = true;
                }
            }

            for (int[] tokenRange : tokenRanges) {
                if (tokenRange[0] < safeCut && tokenRange[1] > safeCut) {
                    safeCut = tokenRange[0];
                    pulled = true;
                }
            }
        }

        if (safeCut <= 0) {
            return "";
        }

        String rawSegment = carry.substring(0, safeCut);

        String scanned = sensitiveDataRedactor.redact(rawSegment, kinds, minConfidence, metrics);

        if (!scanned.equals(rawSegment)) {
            redacted = true;
        }

        String emitted = restore(scanned);

        carry.delete(0, safeCut);

        return emitted;
    }

    private static List<int[]> tokenRanges(CharSequence text) {
        List<int[]> ranges = new ArrayList<>();
        Matcher matcher = PiiToken.pattern()
            .matcher(text);

        while (matcher.find()) {
            ranges.add(new int[] {
                matcher.start(), matcher.end()
            });
        }

        return ranges;
    }

    public String flush() {
        if (carry.length() == 0) {
            return "";
        }

        String raw = carry.toString();

        String scanned = sensitiveDataRedactor.redact(raw, kinds, minConfidence, metrics);

        if (!scanned.equals(raw)) {
            redacted = true;
        }

        String remainder = restore(scanned);

        carry.setLength(0);

        return remainder;
    }

    private String restore(String scanned) {
        if (session == null) {
            return scanned;
        }

        PiiTokenSession.RestoreResult restoreResult = session.restoreWithUnresolvedCount(scanned);
        String restored = restoreResult.text();

        if (restoreResult.unresolvedCount() > 0) {
            record("token_unresolved");
        }

        if (!scanned.equals(restored)) {
            record("pii_restored");
        }

        return restored;
    }

    private void record(String event) {
        if (metrics != null) {
            metrics.record(event);
        }
    }

    public boolean isRedacted() {
        return redacted;
    }
}
