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

import java.util.regex.Pattern;

/**
 * Parses regex expressions written in the JS-style literal form {@code /pattern/flags} and returns a compiled
 * {@link Pattern}. Falls back to compiling the input as a bare regex when no leading slash is present.
 *
 * <p>
 * Supported flags: {@code i}, {@code m}, {@code s}, {@code u}, {@code x}. {@code g} is accepted but ignored.
 *
 * <p>
 * DoS hardening: wrap input with {@link #bounded(CharSequence)} before matching to cap execution time;
 * {@link #compile(String)} rejects expressions longer than {@link #MAX_EXPRESSION_LENGTH} to bound compile time.
 *
 * @author Ivica Cardic
 */
public final class RegexParserUtils {

    /** Maximum input length (characters) accepted by {@link #bounded(CharSequence)}. ~1 MiB of text. */
    public static final int MAX_INPUT_LENGTH = 1_048_576;

    /**
     * Maximum length (characters) of a regex expression accepted by {@link #compile(String)}. Realistic user-supplied
     * patterns are well below 1 KiB; a compile-time DoS pattern tends to be long due to deeply nested alternation.
     */
    public static final int MAX_EXPRESSION_LENGTH = 4_096;

    /**
     * Upper bound on total {@link CharSequence#charAt(int)} accesses during a single matching session. Catastrophic
     * backtracking typically exceeds this in milliseconds; ordinary matching on the maximum input stays well below.
     */
    public static final int MAX_CHAR_ACCESSES = 10_000_000;

    private RegexParserUtils() {
    }

    /**
     * Wrap {@code input} in a {@link CharSequence} that enforces DoS bounds during regex matching. The returned
     * sequence counts {@link CharSequence#charAt(int)} calls and throws {@link RegexExecutionLimitException} once
     * {@link #MAX_CHAR_ACCESSES} is exceeded. Rejects inputs longer than {@link #MAX_INPUT_LENGTH} immediately.
     */
    public static CharSequence bounded(CharSequence input) {
        if (input == null) {
            return null;
        }

        if (input.length() > MAX_INPUT_LENGTH) {
            throw new RegexExecutionLimitException(
                "Input exceeds maximum regex scan length of " + MAX_INPUT_LENGTH + " characters (got "
                    + input.length() + ")");
        }

        return new BoundedCharSequence(input, MAX_CHAR_ACCESSES);
    }

    public static final class RegexExecutionLimitException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public RegexExecutionLimitException(String message) {
            super(message);
        }

        public RegexExecutionLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class BoundedCharSequence implements CharSequence {

        private final CharSequence delegate;
        private final Counter counter;

        BoundedCharSequence(CharSequence delegate, int maxAccesses) {
            this(delegate, new Counter(maxAccesses));
        }

        private BoundedCharSequence(CharSequence delegate, Counter counter) {
            this.delegate = delegate;
            this.counter = counter;
        }

        @Override
        public int length() {
            return delegate.length();
        }

        @Override
        public char charAt(int index) {
            counter.tick(1);

            return delegate.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            int span = Math.max(0, end - start);

            counter.tick(span);

            return new BoundedCharSequence(delegate.subSequence(start, end), counter);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    private static final class Counter {

        private final int maxAccesses;
        private int accesses;

        Counter(int maxAccesses) {
            this.maxAccesses = maxAccesses;
        }

        void tick(int delta) {
            long next = (long) accesses + delta;

            if (next > Integer.MAX_VALUE) {
                accesses = Integer.MAX_VALUE;
            } else {
                accesses = (int) next;
            }

            if (accesses > maxAccesses) {
                throw new RegexExecutionLimitException(
                    "Regex execution exceeded " + maxAccesses + " character accesses (likely catastrophic "
                        + "backtracking); aborting");
            }
        }
    }

    public static Pattern compile(String expression) {
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("expression must be non-empty");
        }

        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            throw new IllegalArgumentException(
                "expression exceeds max length " + MAX_EXPRESSION_LENGTH + " (got " + expression.length() + ")");
        }

        if (expression.charAt(0) != '/') {
            return Pattern.compile(expression);
        }

        int lastSlash = expression.lastIndexOf('/');

        if (lastSlash <= 0) {
            throw new IllegalArgumentException("missing closing '/' in: " + expression);
        }

        String body = expression.substring(1, lastSlash);

        if (body.isEmpty()) {
            throw new IllegalArgumentException("empty regex body in: " + expression);
        }

        String flags = expression.substring(lastSlash + 1);
        int flagBits = 0;

        for (int index = 0; index < flags.length(); index++) {
            char character = flags.charAt(index);

            switch (character) {
                case 'i' -> flagBits |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
                case 'm' -> flagBits |= Pattern.MULTILINE;
                case 's' -> flagBits |= Pattern.DOTALL;
                case 'u' -> flagBits |= Pattern.UNICODE_CHARACTER_CLASS;
                case 'x' -> flagBits |= Pattern.COMMENTS;
                case 'g' -> {
                    // 'g' (JS global flag) is accepted for compatibility but has no Java Pattern equivalent.
                    continue;
                }
                default -> throw new IllegalArgumentException("unsupported flag: " + character);
            }
        }

        return Pattern.compile(body, flagBits);
    }
}
