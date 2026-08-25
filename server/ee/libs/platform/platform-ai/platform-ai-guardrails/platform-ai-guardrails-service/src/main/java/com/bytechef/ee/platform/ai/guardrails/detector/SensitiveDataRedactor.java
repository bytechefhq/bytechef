/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import com.bytechef.ee.platform.ai.guardrails.AiGuardrailMetrics;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs every registered {@link SensitiveDataDetector} over a piece of text, resolves the overlapping spans they report
 * into one non-overlapping accepted set, and applies the winners as {@code [REDACTED_*]} placeholders.
 *
 * <p>
 * This replaces the sequential {@code String.replaceAll} chain the guardrail engine used previously, in which each
 * pattern rewrote the text the next pattern was about to scan. That chain leaked part of any secret whose body
 * contained a credit-card-shaped digit run: the PII pass claimed the digits first and destroyed the text the secret
 * pattern needed, so {@code xoxb-1234567890123456-abcdef} was emitted as {@code xoxb-[REDACTED_CC]-abcdef} with the
 * token's prefix and suffix intact. Detecting against the original text and resolving centrally fixes that.
 * </p>
 *
 * <p>
 * <b>Resolution order</b> is total, so the outcome cannot depend on detector registration order: SECRET before PII,
 * then longer before shorter, then earlier before later, then category ascending. Candidates are taken greedily in that
 * order and a candidate overlapping an already-accepted span is dropped. The length tiebreak also reproduces, for a
 * reason that does not depend on list position, the one ordering property the old chain got right — an enclosing match
 * swallows a nested one.
 * </p>
 *
 * <p>
 * <b>Failure is open, per detector.</b> A detector that throws (or reports a span outside the text) is logged, counted
 * as {@code detector_failed}, and skipped for that call; the others still run. A model-backed detector's transient
 * failure must not take down every AI surface in the product. The residual risk is that content the failed detector
 * would have redacted proceeds unredacted, and where the caller passes no {@link AiGuardrailMetrics} the log line is
 * the only signal.
 * </p>
 *
 * <p>
 * Immutable and thread-safe, provided every registered detector is.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class SensitiveDataRedactor {

    static final String DETECTOR_FAILED_EVENT = "detector_failed";

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataRedactor.class);

    private static final Comparator<SensitiveSpan> RESOLUTION_ORDER = Comparator
        .comparingInt((SensitiveSpan span) -> span.kind() == SensitiveKind.SECRET ? 0 : 1)
        .thenComparing(
            Comparator.comparingInt(SensitiveSpan::length)
                .reversed())
        .thenComparingInt(SensitiveSpan::start)
        .thenComparing(SensitiveSpan::category);

    private final List<SensitiveDataDetector> detectors;

    public SensitiveDataRedactor(List<SensitiveDataDetector> detectors) {
        this.detectors = List.copyOf(detectors);
    }

    /**
     * Returns a redactor over only the {@link SensitiveDataDetector#streamSafe()} subset of this one's detectors, for
     * the streaming path which can offer a detector no more than a bounded lookahead window.
     *
     * @return a redactor restricted to stream-safe detectors, or {@code this} when every detector already is
     */
    public SensitiveDataRedactor streamSafeView() {
        List<SensitiveDataDetector> streamSafeDetectors = new ArrayList<>(detectors.size());

        for (SensitiveDataDetector detector : detectors) {
            if (detector.streamSafe()) {
                streamSafeDetectors.add(detector);
            } else {
                log.info(
                    "Detector '{}' is not stream-safe and is excluded from streaming response redaction",
                    detector.name());
            }
        }

        if (streamSafeDetectors.size() == detectors.size()) {
            return this;
        }

        return new SensitiveDataRedactor(streamSafeDetectors);
    }

    /**
     * Returns every candidate span reported by every detector, unresolved and possibly overlapping. The streaming
     * redactor needs the unresolved set: a span that loses an overlap still occupies characters, and a cut landing
     * inside one must still be pulled back.
     *
     * @param text    the text to scan, never {@code null}
     * @param metrics the metrics instance to count detector failures through, or {@code null}
     * @return the candidate spans, empty when {@code text} is empty or none are found
     */
    public List<SensitiveSpan> detectCandidates(String text, @Nullable AiGuardrailMetrics metrics) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<SensitiveSpan> candidates = new ArrayList<>();

        for (SensitiveDataDetector detector : detectors) {
            collectSpans(detector, text, candidates, metrics);
        }

        return candidates;
    }

    /**
     * Returns {@code text} with every accepted span whose kind is in {@code kinds} replaced by its placeholder.
     *
     * <p>
     * The kind filter is applied to the CANDIDATES, before resolution. Filtering afterwards would let a span the caller
     * did not ask for consume an overlap and then be discarded, so a PII-only call over a secret containing a digit run
     * would return the text unredacted.
     * </p>
     *
     * @param text    the text to redact
     * @param kinds   the kinds the caller's policy has enabled
     * @param metrics the metrics instance to count detector failures through, or {@code null}
     * @return the redacted text, or {@code text} unchanged when nothing applies
     */
    public String redact(
        String text, Set<SensitiveKind> kinds, @Nullable AiGuardrailMetrics metrics) {

        return redactWithSpans(text, kinds, metrics).text();
    }

    /**
     * As {@link #redact}, but also returning the spans that were actually applied.
     *
     * <p>
     * This is the single implementation of the pipeline; {@link #redact} is a projection of it. The spans are needed by
     * callers that must report on WHAT was redacted rather than only substitute it — {@code AiGuardrails} counts its
     * {@code pii_redacted} / {@code secret_redacted} metrics off the accepted set, which is more accurate than
     * comparing strings. Before this existed that caller drove {@link #filterByKind}, {@link #resolve} and
     * {@link #apply} itself, giving the codebase two implementations of a pipeline whose whole point is that there is
     * exactly one: a step added here would silently not have reached the request-direction path.
     * </p>
     *
     * @param text    the text to redact
     * @param kinds   the kinds the caller's policy has enabled
     * @param metrics the metrics instance to count detector failures through, or {@code null}
     * @return the redacted text and the spans applied to produce it; the spans are empty when nothing applied
     */
    public RedactionResult redactWithSpans(
        String text, Set<SensitiveKind> kinds, @Nullable AiGuardrailMetrics metrics) {

        // text is non-null by contract -- callers (AiGuardrails' redactPii/redactSecrets/redactAll) guard null/empty
        // before ever delegating here. The `text == null` arm is kept anyway as defence-in-depth: this sits on a
        // redaction path, where failing soft (returning the input unchanged) beats throwing on a future caller that
        // does not honour the contract. It does not mean this parameter is expected to receive null.
        if (text == null || text.isEmpty() || kinds.isEmpty()) {
            return new RedactionResult(text, List.of());
        }

        List<SensitiveSpan> candidates = filterByKind(detectCandidates(text, metrics), kinds);

        if (candidates.isEmpty()) {
            return new RedactionResult(text, List.of());
        }

        List<SensitiveSpan> accepted = resolve(candidates);

        return new RedactionResult(apply(text, accepted), accepted);
    }

    /**
     * The outcome of one redaction: the resulting text, and the non-overlapping spans that produced it.
     *
     * @param text     the redacted text
     * @param accepted the spans applied, in resolution order; empty when nothing was redacted
     */
    public record RedactionResult(String text, List<SensitiveSpan> accepted) {

        // Defensive copy rather than @SuppressFBWarnings: nothing needs the caller's live list, and a value record
        // handed to metric-counting code should not be able to change under it.
        public RedactionResult {
            accepted = List.copyOf(accepted);
        }
    }

    /**
     * Returns the subset of {@code candidates} whose kind is in {@code kinds}. Public because {@code AiGuardrails} sits
     * in the parent package and drives the three stages separately, so that it can count which kinds actually won
     * before applying them.
     *
     * @param candidates the unresolved candidate spans
     * @param kinds      the kinds to keep
     * @return the matching candidates, in their original order
     */
    static List<SensitiveSpan> filterByKind(List<SensitiveSpan> candidates, Set<SensitiveKind> kinds) {
        List<SensitiveSpan> filtered = new ArrayList<>(candidates.size());

        for (SensitiveSpan candidate : candidates) {
            if (kinds.contains(candidate.kind())) {
                filtered.add(candidate);
            }
        }

        return filtered;
    }

    /**
     * Reduces overlapping candidates to a non-overlapping accepted set using the total order documented on this class.
     *
     * @param candidates the candidate spans, possibly overlapping and in any order
     * @return the accepted spans, none of which overlap another
     */
    static List<SensitiveSpan> resolve(List<SensitiveSpan> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<SensitiveSpan> ordered = new ArrayList<>(candidates);

        ordered.sort(RESOLUTION_ORDER);

        List<SensitiveSpan> accepted = new ArrayList<>();

        for (SensitiveSpan candidate : ordered) {
            if (!overlapsAny(candidate, accepted)) {
                accepted.add(candidate);
            }
        }

        return accepted;
    }

    /**
     * Replaces every accepted span with its placeholder, working right to left so that each replacement leaves the
     * offsets of the spans not yet applied valid.
     *
     * @param text     the original text the spans were located in
     * @param accepted non-overlapping spans, in any order
     * @return the redacted text
     */
    static String apply(String text, List<SensitiveSpan> accepted) {
        if (accepted.isEmpty()) {
            return text;
        }

        List<SensitiveSpan> ordered = new ArrayList<>(accepted);

        ordered.sort(
            Comparator.comparingInt(SensitiveSpan::start)
                .reversed());

        StringBuilder builder = new StringBuilder(text);

        for (SensitiveSpan span : ordered) {
            builder.replace(span.start(), span.end(), span.placeholder());
        }

        return builder.toString();
    }

    private void collectSpans(
        SensitiveDataDetector detector, String text, List<SensitiveSpan> candidates,
        @Nullable AiGuardrailMetrics metrics) {

        try {
            List<SensitiveSpan> spans = detector.detect(text);

            if (spans == null) {
                return;
            }

            for (SensitiveSpan span : spans) {
                if (span.end() > text.length()) {
                    throw new IllegalStateException(
                        "detector reported a span ending at " + span.end() + ", past the end of a " + text.length() +
                            "-character input");
                }
            }

            candidates.addAll(spans);
        } catch (RuntimeException exception) {
            log.warn("Sensitive-data detector '{}' failed; continuing without its spans", detector.name(), exception);

            if (metrics != null) {
                metrics.record(DETECTOR_FAILED_EVENT);
            }
        }
    }

    private static boolean overlapsAny(SensitiveSpan candidate, List<SensitiveSpan> accepted) {
        for (SensitiveSpan span : accepted) {
            if (candidate.overlaps(span)) {
                return true;
            }
        }

        return false;
    }
}
