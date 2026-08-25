/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * One region of sensitive data located in a source string, reported by a {@link SensitiveDataDetector}. Offsets are
 * UTF-16 indices into the string the detector was given, {@code start} inclusive and {@code end} exclusive.
 *
 * <p>
 * Two axes, deliberately. {@link #kind()} is closed and drives policy (which toggle governs this span);
 * {@link #category()} is an open, validated uppercase identifier and drives presentation — {@link #placeholder()}
 * derives {@code [REDACTED_<category>]} from it with no lookup table, which reproduces every placeholder the engine
 * emitted before the SPI existed and lets a detector added later report a new entity type without editing this module.
 * </p>
 *
 * <p>
 * A span never carries the text it covers, and {@link #toString()} therefore cannot leak it into a log line.
 * </p>
 *
 * @param kind       which policy toggle governs this span
 * @param category   uppercase identifier naming the entity type, matching {@code [A-Z][A-Z0-9_]*}
 * @param start      inclusive start offset
 * @param end        exclusive end offset, strictly greater than {@code start}
 * @param confidence detector confidence between {@code 0.0} and {@code 1.0}; deterministic detectors report
 *                   {@code 1.0}. Carried but not used by overlap resolution today — see the design spec's section 6.2
 *                   for why adding a probabilistic detector should not be a record-signature change.
 *
 * @version ee
 */
public record SensitiveSpan(SensitiveKind kind, String category, int start, int end, double confidence) {

    private static final Pattern CATEGORY_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

    public SensitiveSpan {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(category, "category must not be null");

        if (!CATEGORY_PATTERN.matcher(category)
            .matches()) {

            throw new IllegalArgumentException("category must match [A-Z][A-Z0-9_]*, got: " + category);
        }

        if (start < 0) {
            throw new IllegalArgumentException("start must be >= 0");
        }

        if (end <= start) {
            throw new IllegalArgumentException("end must be > start");
        }

        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be between 0.0 and 1.0");
        }
    }

    /**
     * Creates a span with full confidence, for deterministic detectors.
     *
     * @param kind     which policy toggle governs this span
     * @param category uppercase entity-type identifier
     * @param start    inclusive start offset
     * @param end      exclusive end offset
     * @return the span
     */
    public static SensitiveSpan of(SensitiveKind kind, String category, int start, int end) {
        return new SensitiveSpan(kind, category, start, end, 1.0);
    }

    /**
     * Returns the replacement text for this span, derived from {@link #category()}.
     *
     * @return the placeholder, e.g. {@code [REDACTED_EMAIL]}
     */
    public String placeholder() {
        return "[REDACTED_" + category + "]";
    }

    /**
     * Returns the number of characters this span covers.
     *
     * @return {@code end - start}
     */
    public int length() {
        return end - start;
    }

    /**
     * Returns whether this span shares at least one character position with {@code other}. Ranges are half-open, so
     * spans that merely touch ({@code this.end == other.start}) do not overlap and can both be applied.
     *
     * @param other the span to test against
     * @return {@code true} when the two ranges intersect
     */
    public boolean overlaps(SensitiveSpan other) {
        return start < other.end && end > other.start;
    }

    @Override
    public String toString() {
        return "SensitiveSpan[" + kind + "/" + category + "[" + start + "," + end + ") confidence=" + confidence + "]";
    }
}
