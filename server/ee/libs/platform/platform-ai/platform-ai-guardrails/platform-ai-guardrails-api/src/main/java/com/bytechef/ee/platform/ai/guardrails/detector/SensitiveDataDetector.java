/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import java.util.List;

/**
 * Locates sensitive data in text. Implementations are contributed as Spring beans and collected into
 * {@code SensitiveDataRedactor}; contributing one requires no change to the guardrails engine.
 *
 * <p>
 * Every detector is handed the ORIGINAL text and never another detector's output, so a detector's result cannot be
 * corrupted by one that happens to run before it. Overlaps between detectors are resolved centrally by a total order
 * over the spans themselves, which is why registration order cannot affect the redacted result.
 * </p>
 *
 * <p>
 * Implementations must be thread-safe and side-effect-free: one instance serves every workspace and every concurrent
 * request. A detector that throws is caught, logged, counted, and skipped for that call — the other detectors still
 * run. See the design spec's section 8 for the residual risk that fail-open policy accepts.
 * </p>
 *
 * @version ee
 */
public interface SensitiveDataDetector {

    /**
     * Returns a short stable identifier used in log lines and diagnostics.
     *
     * @return the detector name
     */
    String name();

    /**
     * Returns every sensitive region found in {@code text}. Spans may overlap each other and may be returned in any
     * order; the caller resolves and orders them. Offsets must lie within {@code text}.
     *
     * @param text the text to scan; never {@code null} and never empty
     * @return the spans found, empty when none
     */
    List<SensitiveSpan> detect(String text);

    /**
     * Returns whether this detector can be applied to an arbitrary substring of a document and give the same answer it
     * would give for the whole.
     *
     * <p>
     * A regex detector is local in this sense and the default is therefore {@code true}. A detector needing wider
     * context — sentence-level named-entity recognition, say — must return {@code false}: the streaming redactor scans
     * a bounded lookahead window, and feeding such a detector a window that starts mid-sentence produces different and
     * worse answers than feeding it the complete text. The streaming path skips detectors that return {@code false}, so
     * that a detector which cannot honestly cover a stream is visibly absent from it rather than silently contributing
     * nothing usable.
     * </p>
     *
     * @return {@code true} when this detector is safe to run over a windowed fragment
     */
    default boolean streamSafe() {
        return true;
    }
}
