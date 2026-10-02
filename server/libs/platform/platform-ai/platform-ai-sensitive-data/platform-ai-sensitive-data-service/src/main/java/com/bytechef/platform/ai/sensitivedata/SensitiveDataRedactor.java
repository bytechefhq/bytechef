/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.sensitivedata;

import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenSession;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class SensitiveDataRedactor {

    public static final double DEFAULT_MIN_CONFIDENCE = 0.4;

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataRedactor.class);

    private static final Comparator<SensitiveSpan> RESOLUTION_ORDER = Comparator
        .comparingInt((SensitiveSpan span) -> span.kind() == SensitiveKind.SECRET ? 0 : 1)
        .thenComparing(
            Comparator.comparingInt(SensitiveSpan::length)
                .reversed())
        .thenComparingInt(SensitiveSpan::start)
        .thenComparing(SensitiveSpan::category);

    private final List<SensitiveDataDetector> detectors;
    private final DetectionBounds bounds;

    @Autowired
    public SensitiveDataRedactor(List<SensitiveDataDetector> detectors) {
        this(detectors, DetectionBounds.DEFAULTS);
    }

    public SensitiveDataRedactor(List<SensitiveDataDetector> detectors, DetectionBounds bounds) {
        this.detectors = List.copyOf(detectors);
        this.bounds = Objects.requireNonNull(bounds, "bounds must not be null");
    }

    public record DetectionBounds(Duration timeout, int maxUnwindowableInput) {

        public static final DetectionBounds DEFAULTS = new DetectionBounds(Duration.ofSeconds(2), 262144);

        public DetectionBounds {
            Objects.requireNonNull(timeout, "timeout must not be null");

            if (maxUnwindowableInput < 0) {
                throw new IllegalArgumentException(
                    "maxUnwindowableInput must be >= 0, got: " + maxUnwindowableInput);
            }
        }
    }

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

        return new SensitiveDataRedactor(streamSafeDetectors, bounds);
    }

    public List<SensitiveSpan> detectCandidates(String text, @Nullable SensitiveDataMetrics metrics) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<SensitiveSpan> candidates = new ArrayList<>();
        long deadline = System.nanoTime() + bounds.timeout()
            .toNanos();
        MatchDeadline matchDeadline = MatchDeadline.at(deadline);

        for (SensitiveDataDetector detector : detectors) {
            if (!detector.streamSafe()) {
                continue;
            }

            if (timedOut(deadline, detector, metrics)) {
                return List.copyOf(candidates);
            }

            collectSpans(detector, text, candidates, matchDeadline, metrics);
        }

        for (SensitiveDataDetector detector : detectors) {
            if (detector.streamSafe()) {
                continue;
            }

            if (text.length() > bounds.maxUnwindowableInput()) {
                log.warn(
                    "Sensitive-data detector '{}' cannot be applied to a fragment and was skipped for a {}-character " +
                        "input exceeding the {}-character limit; its coverage is absent for this call",
                    detector.name(), text.length(), bounds.maxUnwindowableInput());

                if (metrics != null) {
                    metrics.recordDetectorSkippedOversize(detector.name());
                }

                continue;
            }

            if (timedOut(deadline, detector, metrics)) {
                return List.copyOf(candidates);
            }

            collectSpans(detector, text, candidates, matchDeadline, metrics);
        }

        return List.copyOf(candidates);
    }

    private static boolean timedOut(
        long deadline, SensitiveDataDetector detector, @Nullable SensitiveDataMetrics metrics) {

        if (System.nanoTime() < deadline) {
            return false;
        }

        log.warn(
            "Sensitive-data detection exceeded its budget while running '{}'; the spans returned for this call are " +
                "partial and some of the content was not scanned",
            detector.name());

        if (metrics != null) {
            metrics.recordDetectorTimedOut(detector.name());
        }

        return true;
    }

    public String redact(
        String text, Set<SensitiveKind> kinds, @Nullable SensitiveDataMetrics metrics) {

        return redact(text, kinds, DEFAULT_MIN_CONFIDENCE, metrics);
    }

    public String redact(
        String text, Set<SensitiveKind> kinds, double minConfidence, @Nullable SensitiveDataMetrics metrics) {

        return redactWithSpans(text, kinds, minConfidence, metrics).text();
    }

    public RedactionResult redactWithSpans(
        String text, Set<SensitiveKind> kinds, @Nullable SensitiveDataMetrics metrics) {

        return redactWithSpans(text, kinds, DEFAULT_MIN_CONFIDENCE, metrics);
    }

    public RedactionResult redactWithSpans(
        String text, Set<SensitiveKind> kinds, double minConfidence, @Nullable SensitiveDataMetrics metrics) {

        return redactWithSpans(text, kinds, minConfidence, metrics, List.of());
    }

    public RedactionResult redactWithSpans(
        String text, Set<SensitiveKind> kinds, double minConfidence, @Nullable SensitiveDataMetrics metrics,
        List<SensitiveSpan> extraCandidates) {

        return redactWithSpans(text, kinds, minConfidence, metrics, extraCandidates, SensitiveSpan::placeholder);
    }

    public RedactionResult redactWithSpans(
        String text, Set<SensitiveKind> kinds, double minConfidence, @Nullable SensitiveDataMetrics metrics,
        List<SensitiveSpan> extraCandidates, Function<SensitiveSpan, String> replacer) {

        if (text == null || text.isEmpty() || kinds.isEmpty()) {
            return new RedactionResult(text, List.of());
        }

        List<SensitiveSpan> candidates = filterByKind(
            filterByConfidence(withExtras(detectCandidates(text, metrics), extraCandidates), minConfidence, metrics),
            kinds);

        if (candidates.isEmpty()) {
            return new RedactionResult(text, List.of());
        }

        List<SensitiveSpan> accepted = resolve(candidates);

        return new RedactionResult(apply(text, accepted, replacer), accepted);
    }

    public RedactionResult tokenizeWithSpans(
        String text, Set<SensitiveKind> kinds, PiiTokenSession session, @Nullable SensitiveDataMetrics metrics) {

        return tokenizeWithSpans(text, kinds, session, DEFAULT_MIN_CONFIDENCE, metrics);
    }

    public RedactionResult tokenizeWithSpans(
        String text, Set<SensitiveKind> kinds, PiiTokenSession session, double minConfidence,
        @Nullable SensitiveDataMetrics metrics) {

        return tokenizeWithSpans(text, kinds, session, minConfidence, metrics, List.of());
    }

    public RedactionResult tokenizeWithSpans(
        String text, Set<SensitiveKind> kinds, PiiTokenSession session, double minConfidence,
        @Nullable SensitiveDataMetrics metrics, List<SensitiveSpan> extraCandidates) {

        if (text == null || text.isEmpty() || kinds.isEmpty()) {
            return new RedactionResult(text, List.of());
        }

        List<SensitiveSpan> candidates = filterByKind(
            filterByConfidence(withExtras(detectCandidates(text, metrics), extraCandidates), minConfidence, metrics),
            kinds);

        if (candidates.isEmpty()) {
            return new RedactionResult(text, List.of());
        }

        List<SensitiveSpan> accepted = resolve(candidates);

        String tokenized = apply(text, accepted, span -> {
            if (span.kind() == SensitiveKind.SECRET) {
                return span.placeholder();
            }

            return session.tokenFor(span.category(), text.substring(span.start(), span.end()));
        });

        return new RedactionResult(tokenized, accepted);
    }

    public record RedactionResult(String text, List<SensitiveSpan> accepted) {

        public RedactionResult {
            accepted = List.copyOf(accepted);
        }
    }

    static List<SensitiveSpan> filterByKind(List<SensitiveSpan> candidates, Set<SensitiveKind> kinds) {
        List<SensitiveSpan> filtered = new ArrayList<>(candidates.size());

        for (SensitiveSpan candidate : candidates) {
            if (kinds.contains(candidate.kind())) {
                filtered.add(candidate);
            }
        }

        return filtered;
    }

    static List<SensitiveSpan> filterByConfidence(
        List<SensitiveSpan> candidates, double minConfidence, @Nullable SensitiveDataMetrics metrics) {

        List<SensitiveSpan> kept = new ArrayList<>(candidates.size());
        boolean dropped = false;

        for (SensitiveSpan candidate : candidates) {
            if (candidate.confidence() >= minConfidence) {
                kept.add(candidate);
            } else {
                dropped = true;
            }
        }

        if (dropped && metrics != null) {
            metrics.recordBelowConfidenceThreshold();
        }

        return kept;
    }

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

    static String apply(String text, List<SensitiveSpan> accepted) {
        return apply(text, accepted, SensitiveSpan::placeholder);
    }

    static String apply(String text, List<SensitiveSpan> accepted, Function<SensitiveSpan, String> replacer) {
        if (accepted.isEmpty()) {
            return text;
        }

        List<SensitiveSpan> ordered = new ArrayList<>(accepted);

        ordered.sort(
            Comparator.comparingInt(SensitiveSpan::start)
                .reversed());

        StringBuilder builder = new StringBuilder(text);

        for (SensitiveSpan span : ordered) {
            builder.replace(span.start(), span.end(), replacer.apply(span));
        }

        return builder.toString();
    }

    private static List<SensitiveSpan> withExtras(
        List<SensitiveSpan> detected, List<SensitiveSpan> extraCandidates) {

        if (extraCandidates.isEmpty()) {
            return detected;
        }

        List<SensitiveSpan> merged = new ArrayList<>(detected.size() + extraCandidates.size());

        merged.addAll(detected);
        merged.addAll(extraCandidates);

        return merged;
    }

    private void collectSpans(
        SensitiveDataDetector detector, String text, List<SensitiveSpan> candidates,
        MatchDeadline matchDeadline, @Nullable SensitiveDataMetrics metrics) {

        try {
            List<SensitiveSpan> spans = detector.detect(text, matchDeadline);

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
        } catch (DetectionTimeoutException detectionTimeoutException) {
            log.warn(
                "Sensitive-data detector '{}' exceeded its match deadline; continuing without its spans: {}",
                detector.name(), detectionTimeoutException.getMessage());

            if (metrics != null) {
                metrics.recordDetectorTimedOut(detector.name());
            }
        } catch (StackOverflowError stackOverflowError) {
            log.warn(
                "Sensitive-data detector '{}' overflowed the stack; continuing without its spans", detector.name());

            if (metrics != null) {
                metrics.recordDetectorFailure(detector.name());
            }
        } catch (RuntimeException exception) {
            log.warn("Sensitive-data detector '{}' failed; continuing without its spans", detector.name(), exception);

            if (metrics != null) {
                metrics.recordDetectorFailure(detector.name());
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
