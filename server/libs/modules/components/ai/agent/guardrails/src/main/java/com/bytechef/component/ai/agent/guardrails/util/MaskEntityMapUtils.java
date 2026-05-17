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

import com.bytechef.component.definition.Context;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects mask entities emitted by preflight guardrail checks and applies them as a single length-sorted pass over a
 * text so longer overlapping matches always win. Word-boundary anchored when edges are word characters. Not thread-safe
 * — instances are created per-request.
 *
 * @author Ivica Cardic
 */
public final class MaskEntityMapUtils {

    /**
     * Pair-count above which {@link #applyTo} emits a WARN — each pair triggers a full-text scan via
     * {@code Matcher.find()} to collect spans, so runaway LLM-hallucinated entity lists degrade request latency
     * linearly. Operators seeing this warning should cap their upstream detector's output.
     */
    private static final int LARGE_ENTITY_COUNT_THRESHOLD = 1024;

    private final Map<String, List<String>> entities = new LinkedHashMap<>();
    private final Context context;

    public MaskEntityMapUtils(Context context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public static Pattern boundaryAwarePattern(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("value must be non-empty");
        }

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

    public void merge(Map<String, List<String>> additions) {
        if (additions == null || additions.isEmpty()) {
            return;
        }

        int addedTypes = 0;

        for (Map.Entry<String, List<String>> entry : additions.entrySet()) {
            String type = entry.getKey();
            List<String> values = entry.getValue();

            if (values == null || values.isEmpty()) {
                warn("MaskEntityMap.merge: skipping type '{}' with null/empty values list", type);

                continue;
            }

            entities.computeIfAbsent(type, key -> new ArrayList<>())
                .addAll(values);

            addedTypes++;
        }

        if (addedTypes == 0) {
            warn(
                "MaskEntityMap.merge: every entry in a non-empty additions map ({} type(s)) was filtered as "
                    + "null/empty — upstream detector likely degraded",
                additions.size());
        }
    }

    public boolean isEmpty() {
        return entities.isEmpty();
    }

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
            warn(
                "MaskEntityMap.applyTo: skipped {} null/empty value(s) across {} type(s) — upstream detector emitted "
                    + "malformed pairs",
                skippedNullOrEmpty, entities.size());
        }

        if (pairs.size() > LARGE_ENTITY_COUNT_THRESHOLD) {
            warn(
                "MaskEntityMap received {} entity pairs (> {} threshold). Each pair runs a full-text scan, so latency "
                    + "scales linearly with count. Check upstream detector output for runaway results.",
                pairs.size(), LARGE_ENTITY_COUNT_THRESHOLD);
        }

        List<Span> spans = new ArrayList<>();
        Map<String, Pattern> patternCache = new HashMap<>();
        CharSequence bounded = RegexParserUtils.bounded(text);

        for (Pair pair : pairs) {
            Pattern pattern = patternCache.computeIfAbsent(pair.value(), MaskEntityMapUtils::boundaryAwarePattern);
            Matcher matcher = pattern.matcher(bounded);

            while (matcher.find()) {
                spans.add(new Span(matcher.start(), matcher.end(), pair.type()));
            }
        }

        if (spans.isEmpty()) {
            return text;
        }

        List<Span> deduplicated = deduplicateOverlaps(spans);

        StringBuilder result = new StringBuilder(text);
        int lastStart = Integer.MAX_VALUE;

        for (Span span : deduplicated) {
            if (span.end() > lastStart) {
                throw new IllegalStateException(
                    "out-of-order spans: span.end=" + span.end() + " > lastStart=" + lastStart);
            }

            result.replace(span.start(), span.end(), "<" + span.type() + ">");

            lastStart = span.start();
        }

        return result.toString();
    }

    private void warn(String format, Object... args) {
        context.log(contextLog -> contextLog.warn(format, args));
    }

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

    private static boolean isWordChar(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private record Span(int start, int end, String type) {
    }
}
