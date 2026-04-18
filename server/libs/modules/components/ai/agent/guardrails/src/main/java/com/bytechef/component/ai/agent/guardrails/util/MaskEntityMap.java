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

package com.bytechef.component.ai.agent.guardrails.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects {@code type → [substring, ...]} mask entities emitted by preflight guardrail checks and applies them as a
 * single length-sorted pass against a text. Used by {@code CheckForViolationsAdvisor} and {@code SanitizeTextAdvisor}
 * so that overlapping matches (e.g. {@code alice@corp.com} vs {@code corp.com}) never leave stray fragments — the
 * longer substring always wins regardless of which check produced it.
 *
 * <p>
 * <b>Internal utility</b> — not part of the guardrails public SPI. Kept {@code public} only to be reachable from the
 * advisor package within the same module.
 *
 * <p>
 * Values are replaced using word-boundary-anchored regex when the value's edges are word characters, so a short name
 * like {@code "Ann"} will not mask {@code "Announcement"}. Edges that are non-word characters (e.g. a URL starting with
 * {@code /}) are matched without a boundary on that side, because {@code \b} is only meaningful at a word/non-word
 * transition. This is a best-effort defense — detectors that can emit span offsets should prefer the index-based
 * masking in {@code PiiDetector.mask} / {@code SecretKeyDetector.mask}, which is unambiguous.
 *
 * <p>
 * Not thread-safe — instances are created per-request inside the advisors and never shared across threads.
 *
 * @author Ivica Cardic
 */
public final class MaskEntityMap {

    private static final Logger log = LoggerFactory.getLogger(MaskEntityMap.class);

    /**
     * Pair-count above which {@link #applyTo} emits a WARN — each pair triggers a full-text scan via
     * {@code Matcher.find()} to collect spans, so runaway LLM-hallucinated entity lists degrade request latency
     * linearly. Operators seeing this warning should cap their upstream detector's output.
     */
    private static final int LARGE_ENTITY_COUNT_THRESHOLD = 1024;

    private final Map<String, List<String>> entities = new LinkedHashMap<>();

    public void merge(Map<String, List<String>> additions) {
        if (additions == null || additions.isEmpty()) {
            return;
        }

        int addedTypes = 0;

        for (Map.Entry<String, List<String>> entry : additions.entrySet()) {
            String type = entry.getKey();
            List<String> values = entry.getValue();

            if (values == null || values.isEmpty()) {
                // A buggy detector that returns {"EMAIL": null} would otherwise contribute nothing with no
                // observable signal; logging here lets operators distinguish "model genuinely found nothing" from
                // "detector silently degraded to no-op". WARN (not DEBUG) because production log filters drop DEBUG
                // — a silently-degraded detector is exactly the failure class this log was meant to surface, and
                // DEBUG would hide it.
                log.warn("MaskEntityMap.merge: skipping type '{}' with null/empty values list", type);

                continue;
            }

            entities.computeIfAbsent(type, key -> new ArrayList<>())
                .addAll(values);

            addedTypes++;
        }

        if (addedTypes == 0) {
            // WARN (not DEBUG) — every entry filtered means the detector produced output but contributed nothing to
            // masking. This is the silent-degradation signal operators must see in production.
            log.warn(
                "MaskEntityMap.merge: every entry in a non-empty additions map ({} type(s)) was filtered as "
                    + "null/empty — upstream detector likely degraded",
                additions.size());
        }
    }

    public boolean isEmpty() {
        return entities.isEmpty();
    }

    /**
     * Apply every collected entity against {@code text}, longest substring first. Empty and {@code null} values are
     * skipped. Word-boundary anchors guard against spurious substring hits (e.g. {@code "Ann"} inside
     * {@code "Announcement"}) on edges that are word characters.
     *
     * <p>
     * Spans are collected against the <em>original</em> text and replacements are applied in reverse offset order, so a
     * later short value cannot accidentally match inside a {@code <TYPE>} placeholder that an earlier replacement
     * already inserted (e.g. an entity whose value is the literal string {@code "EMAIL_ADDRESS"} cannot be re-masked
     * after another entity has been replaced with {@code <EMAIL_ADDRESS>}). This mirrors {@code PiiDetector.mask} and
     * {@code SecretKeyDetector.mask}.
     */
    public String applyTo(String text) {
        if (text == null || text.isEmpty() || entities.isEmpty()) {
            return text;
        }

        record Pair(String type, String value) {
        }

        List<Pair> pairs = new ArrayList<>();
        int skippedNullOrEmpty = 0;

        for (Map.Entry<String, List<String>> entry : entities.entrySet()) {
            String type = entry.getKey();

            for (String value : entry.getValue()) {
                if (value == null || value.isEmpty()) {
                    skippedNullOrEmpty++;

                    continue;
                }

                pairs.add(new Pair(type, value));
            }
        }

        if (skippedNullOrEmpty > 0) {
            // WARN (not DEBUG) so operators see malformed-pair signal in production. See parallel rationale on
            // merge() above.
            log.warn(
                "MaskEntityMap.applyTo: skipped {} null/empty value(s) across {} type(s) — upstream detector emitted "
                    + "malformed pairs",
                skippedNullOrEmpty, entities.size());
        }

        if (pairs.size() > LARGE_ENTITY_COUNT_THRESHOLD) {
            log.warn(
                "MaskEntityMap received {} entity pairs (> {} threshold). Each pair runs a full-text scan, so latency "
                    + "scales linearly with count. Check upstream detector output for runaway results.",
                pairs.size(), LARGE_ENTITY_COUNT_THRESHOLD);
        }

        List<Span> spans = new ArrayList<>();

        for (Pair pair : pairs) {
            Matcher matcher = boundaryAwarePattern(pair.value()).matcher(text);

            while (matcher.find()) {
                spans.add(new Span(matcher.start(), matcher.end(), pair.type()));
            }
        }

        if (spans.isEmpty()) {
            return text;
        }

        List<Span> deduplicated = deduplicateOverlaps(spans);

        StringBuilder result = new StringBuilder(text);

        for (Span span : deduplicated) {
            result.replace(span.start(), span.end(), "<" + span.type() + ">");
        }

        return result.toString();
    }

    /**
     * Longest-wins overlap dedup, then sort by start descending for the reverse-order replace loop. Mirrors
     * {@code PiiDetector.deduplicateOverlaps} / {@code SecretKeyDetector.deduplicateOverlaps}.
     */
    private static List<Span> deduplicateOverlaps(List<Span> spans) {
        List<Span> byLength = new ArrayList<>(spans);

        byLength.sort(Comparator.<Span>comparingInt(span -> span.end() - span.start())
            .reversed()
            .thenComparingInt(Span::start));

        List<Span> kept = new ArrayList<>();

        for (Span candidate : byLength) {
            boolean overlaps = false;

            for (Span existing : kept) {
                if (candidate.start() < existing.end() && candidate.end() > existing.start()) {
                    overlaps = true;

                    break;
                }
            }

            if (!overlaps) {
                kept.add(candidate);
            }
        }

        kept.sort(Comparator.comparingInt(Span::start)
            .reversed());

        return kept;
    }

    private record Span(int start, int end, String type) {
    }

    /**
     * Build a word-boundary-anchored regex for {@code value}. Exposed so sibling utilities (e.g. {@code LlmPii}) can
     * share the same boundary semantics rather than duplicating the logic. Word-boundary anchors are only applied on
     * edges that are word characters — a URL starting with {@code /} is matched without a leading boundary because
     * {@code \b} is only meaningful at a word/non-word transition.
     */
    public static Pattern boundaryAwarePattern(String value) {
        StringBuilder regex = new StringBuilder();

        if (isWordChar(value.charAt(0))) {
            regex.append("\\b");
        }

        regex.append(Pattern.quote(value));

        if (isWordChar(value.charAt(value.length() - 1))) {
            regex.append("\\b");
        }

        return Pattern.compile(regex.toString(), Pattern.UNICODE_CHARACTER_CLASS);
    }

    private static boolean isWordChar(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }
}
