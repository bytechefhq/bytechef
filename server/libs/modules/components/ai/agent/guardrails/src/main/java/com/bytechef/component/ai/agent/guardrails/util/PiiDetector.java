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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for detecting and masking personally identifiable information (PII).
 *
 * @author Ivica Cardic
 */
public class PiiDetector {

    /**
     * Default PII patterns for common types of personally identifiable information.
     */
    public static final List<PiiPattern> DEFAULT_PII_PATTERNS = List.of(
        // Email address
        new PiiPattern(
            "EMAIL",
            Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")),
        // Phone number - simple pattern matching 10 consecutive digits with optional separators
        new PiiPattern(
            "PHONE",
            Pattern.compile("\\b[0-9]{3}[-.][0-9]{3}[-.][0-9]{4}\\b|\\([0-9]{3}\\)[-.\\s][0-9]{3}[-.][0-9]{4}")),
        // US Social Security Number
        new PiiPattern(
            "SSN",
            Pattern.compile("\\b[0-9]{3}-[0-9]{2}-[0-9]{4}\\b")),
        // Credit card number (with or without separators)
        new PiiPattern(
            "CREDIT_CARD",
            Pattern.compile("\\b(?:[0-9]{4}[-\\s]?){3}[0-9]{4}\\b")),
        // IP address (IPv4)
        new PiiPattern(
            "IP_ADDRESS",
            Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")));

    private PiiDetector() {
    }

    /**
     * Detect PII in the given content using the provided patterns.
     *
     * @param content  the content to scan
     * @param patterns the patterns to use for detection
     * @return a list of PII matches found
     */
    public static List<PiiMatch> detect(String content, List<PiiPattern> patterns) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        List<PiiMatch> matches = new ArrayList<>();

        for (PiiPattern piiPattern : patterns) {
            Matcher matcher = piiPattern.pattern()
                .matcher(content);

            while (matcher.find()) {
                matches.add(
                    new PiiMatch(
                        matcher.group(),
                        matcher.start(),
                        matcher.end(),
                        piiPattern.type()));
            }
        }

        return matches;
    }

    /**
     * Mask PII in the given content by replacing matches with mask tokens.
     *
     * @param content the content to mask
     * @param matches the PII matches to mask
     * @return the masked content
     */
    public static String mask(String content, List<PiiMatch> matches) {
        if (content == null || content.isEmpty() || matches.isEmpty()) {
            return content;
        }

        // Sort matches by start position in descending order to maintain positions during replacement
        List<PiiMatch> sortedMatches = new ArrayList<>(matches);
        sortedMatches.sort(Comparator.comparingInt(PiiMatch::start)
            .reversed());

        StringBuilder result = new StringBuilder(content);

        for (PiiMatch match : sortedMatches) {
            String maskToken = "<" + match.type() + ">";
            result.replace(match.start(), match.end(), maskToken);
        }

        return result.toString();
    }

    /**
     * A PII pattern definition.
     *
     * @param type    the type of PII (e.g., EMAIL, PHONE)
     * @param pattern the regex pattern to detect this PII type
     */
    public record PiiPattern(String type, Pattern pattern) {
    }

    /**
     * A PII match found in content.
     *
     * @param value the matched value
     * @param start the start position in the content
     * @param end   the end position in the content
     * @param type  the type of PII detected
     */
    public record PiiMatch(String value, int start, int end, String type) {
    }
}
