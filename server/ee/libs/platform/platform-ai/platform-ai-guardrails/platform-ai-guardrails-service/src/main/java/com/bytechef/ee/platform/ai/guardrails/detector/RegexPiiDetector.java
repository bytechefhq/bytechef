/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.detector;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Locates structured personally-identifiable data by regular expression: email addresses, US social-security numbers,
 * credit-card numbers, phone numbers and IPv4 addresses.
 *
 * <p>
 * Every pattern is linear and backtracking-safe — no nested optional quantifiers, so none can be driven into
 * catastrophic backtracking by hostile input. These are the exact patterns the guardrail engine applied before the
 * detector SPI existed; the category names are chosen so that {@link SensitiveSpan#placeholder()} derives the same
 * placeholder strings the engine emitted then.
 * </p>
 *
 * <p>
 * Regex matching is local, so this detector is stream-safe.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RegexPiiDetector implements SensitiveDataDetector {

    // A fixed iteration order matters here: RESOLUTION_ORDER in SensitiveDataRedactor is total, but only because every
    // detector's spans are produced deterministically. Map.of salts its iteration order per JVM run, which would make
    // this detector emit spans in a different sequence on every run -- harmless on its own, but it would mask a bug in
    // RESOLUTION_ORDER by making a broken tiebreak fail intermittently instead of every time. A LinkedHashMap keeps
    // detection order fixed and matches the pattern order the deleted String.replaceAll chain used.
    private static final Map<String, Pattern> PATTERNS = Collections.unmodifiableMap(createPatterns());

    private static Map<String, Pattern> createPatterns() {
        Map<String, Pattern> patterns = new LinkedHashMap<>();

        patterns.put("EMAIL", Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"));
        patterns.put("SSN", Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"));
        patterns.put("CC", Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b"));
        // Linear, backtracking-safe: a 3-3-4 grouping with a single required separator between groups.
        patterns.put("PHONE", Pattern.compile("\\b\\d{3}[-.\\s]\\d{3}[-.\\s]\\d{4}\\b"));
        patterns.put("IP", Pattern.compile(
            "\\b(?:(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1?\\d?\\d)\\b"));

        return patterns;
    }

    @Override
    public String name() {
        return "regex-pii";
    }

    @Override
    public List<SensitiveSpan> detect(String text) {
        List<SensitiveSpan> spans = new ArrayList<>();

        for (Map.Entry<String, Pattern> entry : PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue()
                .matcher(text);

            while (matcher.find()) {
                spans.add(SensitiveSpan.of(SensitiveKind.PII, entry.getKey(), matcher.start(), matcher.end()));
            }
        }

        return spans;
    }
}
