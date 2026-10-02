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

package com.bytechef.platform.ai.sensitivedata.tokenization;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 */
public record PiiToken(String category, int ordinal, String sessionId) {

    public static final int SESSION_ID_LENGTH = 4;

    public static final String SESSION_ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static final String SESSION_ID_CHARACTER_CLASS = "[a-z0-9]";

    private static final Pattern PATTERN = Pattern.compile(
        "\\[PII_([A-Z][A-Z0-9_]*)_(\\d+)_(" + SESSION_ID_CHARACTER_CLASS + "{" + SESSION_ID_LENGTH + "})\\]");

    private static final Pattern CATEGORY_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Pattern SESSION_ID_PATTERN =
        Pattern.compile(SESSION_ID_CHARACTER_CLASS + "{" + SESSION_ID_LENGTH + "}");

    public PiiToken {
        Objects.requireNonNull(category, "category must not be null");
        Objects.requireNonNull(sessionId, "sessionId must not be null");

        if (!CATEGORY_PATTERN.matcher(category)
            .matches()) {

            throw new IllegalArgumentException("category must match [A-Z][A-Z0-9_]*, got: " + category);
        }

        if (ordinal < 1) {
            throw new IllegalArgumentException("ordinal must be >= 1, got: " + ordinal);
        }

        if (!SESSION_ID_PATTERN.matcher(sessionId)
            .matches()) {

            throw new IllegalArgumentException(
                "sessionId must be " + SESSION_ID_LENGTH + " lowercase alphanumerics, got: " + sessionId);
        }
    }

    public static Pattern pattern() {
        return PATTERN;
    }

    public static Optional<PiiToken> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }

        Matcher matcher = PATTERN.matcher(text);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                new PiiToken(matcher.group(1), Integer.parseInt(matcher.group(2)), matcher.group(3)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public String text() {
        return "[PII_" + category + "_" + ordinal + "_" + sessionId + "]";
    }

    public static Optional<String> sessionIdOf(Map<String, String> tokens) {
        return tokens.keySet()
            .stream()
            .map(PiiToken::parse)
            .flatMap(Optional::stream)
            .map(PiiToken::sessionId)
            .findFirst();
    }
}
