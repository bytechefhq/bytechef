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

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Ivica Cardic
 */
public final class CustomPatternValidator {

    private static final Pattern TYPE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");

    private static final int MAX_PATTERN_LENGTH = 512;

    private static final Duration TIMING_BUDGET = Duration.ofMillis(250);

    private static final int ADVERSARIAL_LENGTH = 4000;

    private CustomPatternValidator() {
    }

    public static void validate(String type, String regex) {
        if (type == null || !TYPE_PATTERN.matcher(type)
            .matches()) {

            throw new CustomPatternRejectedException(
                Defence.NAME,
                "a rule name must match [A-Z][A-Z0-9_]{1,63}, or it mints tokens that cannot be parsed back: " + type);
        }

        if (isBuiltInType(type)) {
            throw new CustomPatternRejectedException(
                Defence.NAME,
                "a rule may not redefine the built-in type " + type +
                    "; rules are additive, and an override would weaken a shipped protection through a settings field");
        }

        if (regex == null || regex.isEmpty()) {
            throw new CustomPatternRejectedException(Defence.LENGTH, "a rule needs a pattern");
        }

        if (regex.length() > MAX_PATTERN_LENGTH) {
            throw new CustomPatternRejectedException(
                Defence.LENGTH,
                "a pattern may be at most " + MAX_PATTERN_LENGTH + " characters, got " + regex.length());
        }

        String unbounded = findUnboundedQuantifier(regex);

        if (unbounded != null) {
            throw new CustomPatternRejectedException(
                Defence.SYNTAX,
                "every quantifier must carry an upper bound, but found '" + unbounded +
                    "'; write {n,m} instead (e.g. \\d{1,20} rather than \\d+)");
        }

        Pattern pattern = compile(regex);

        assertRunsQuickly(pattern, regex);
    }

    private static Pattern compile(String regex) {
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException patternSyntaxException) {
            throw new CustomPatternRejectedException(
                Defence.SYNTAX, "not a valid regular expression: " + patternSyntaxException.getDescription());
        }
    }

    private static void assertRunsQuickly(Pattern pattern, String regex) {
        for (String haystack : adversarialInputs(regex)) {
            MatchDeadline deadline = MatchDeadline.in(TIMING_BUDGET);

            try {
                pattern.matcher(deadline.bound(haystack))
                    .find();
            } catch (DetectionTimeoutException detectionTimeoutException) {
                throw new CustomPatternRejectedException(
                    Defence.TIMING,
                    "the pattern ran longer than " + TIMING_BUDGET.toMillis() +
                        "ms on a generated input of " + haystack.length() +
                        " characters, so it would stall detection on ordinary traffic");
            } catch (StackOverflowError stackOverflowError) {
                throw new CustomPatternRejectedException(
                    Defence.TIMING,
                    "the pattern recursed deeply enough to overflow the stack on a generated input of " +
                        haystack.length() + " characters");
            }
        }
    }

    private static List<String> adversarialInputs(String regex) {
        String prefix = literalPrefix(regex);

        return List.of(
            prefix + "a".repeat(ADVERSARIAL_LENGTH),
            prefix + "A".repeat(ADVERSARIAL_LENGTH),
            prefix + "0".repeat(ADVERSARIAL_LENGTH),
            prefix + "a1".repeat(ADVERSARIAL_LENGTH / 2),
            prefix + "-".repeat(ADVERSARIAL_LENGTH),
            prefix + "x".repeat(ADVERSARIAL_LENGTH),
            prefix + " ".repeat(ADVERSARIAL_LENGTH));
    }

    private static String literalPrefix(String regex) {
        StringBuilder prefix = new StringBuilder();
        int index = regex.startsWith("\\b") ? 2 : 0;

        while (index < regex.length()) {
            char character = regex.charAt(index);

            if ("\\[](){}.*+?|^$".indexOf(character) >= 0) {
                break;
            }

            prefix.append(character);

            index++;
        }

        return prefix.toString();
    }

    private static String findUnboundedQuantifier(String regex) {
        boolean inCharacterClass = false;

        for (int index = 0; index < regex.length(); index++) {
            char character = regex.charAt(index);

            if (character == '\\') {
                index++;

                continue;
            }

            if (inCharacterClass) {
                if (character == ']') {
                    inCharacterClass = false;
                }

                continue;
            }

            if (character == '[') {
                inCharacterClass = true;

                continue;
            }

            if (character == '*' || character == '+') {
                return String.valueOf(character);
            }

            if (character == '{') {
                int close = regex.indexOf('}', index);

                if (close > index) {
                    String bounds = regex.substring(index + 1, close);

                    if (bounds.matches("\\d+,")) {
                        return "{" + bounds + "}";
                    }
                }
            }
        }

        return null;
    }

    private static boolean isBuiltInType(String type) {
        for (PiiPatternCatalog.PiiPattern piiPattern : PiiPatternCatalog.ALL) {
            if (piiPattern.type()
                .equals(type)) {

                return true;
            }
        }

        for (SecretPatternCatalog.SecretPattern secretPattern : SecretPatternCatalog.ALL) {
            if (secretPattern.type()
                .equals(type)) {

                return true;
            }
        }

        return false;
    }

    public enum Defence {

        NAME,
        LENGTH,
        SYNTAX,
        TIMING
    }

    public static class CustomPatternRejectedException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final Defence defence;

        public CustomPatternRejectedException(Defence defence, String message) {
            super(message);

            this.defence = defence;
        }

        public Defence getDefence() {
            return defence;
        }
    }
}
