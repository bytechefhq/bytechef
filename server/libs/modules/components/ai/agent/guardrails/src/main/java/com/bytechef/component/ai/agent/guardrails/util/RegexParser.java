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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses regex expressions written in the JS-style literal form {@code /pattern/flags} and returns a compiled
 * {@link Pattern}. Falls back to compiling the input as a bare regex when no leading slash is present.
 *
 * <p>
 * Supported flags:
 * <ul>
 * <li>{@code i} → {@link Pattern#CASE_INSENSITIVE} | {@link Pattern#UNICODE_CASE}</li>
 * <li>{@code m} → {@link Pattern#MULTILINE}</li>
 * <li>{@code s} → {@link Pattern#DOTALL}</li>
 * <li>{@code u} → {@link Pattern#UNICODE_CHARACTER_CLASS}</li>
 * <li>{@code x} → {@link Pattern#COMMENTS}</li>
 * </ul>
 * {@code g} (JS "global") is accepted but ignored — Java regex is always non-global at the Matcher level.
 *
 * <p>
 * <b>DoS hardening.</b> User-supplied regex is a denial-of-service vector on two separate fronts:
 * <ol>
 * <li><em>Execution time.</em> A pathological pattern like {@code /(a+)+$/} against {@code "aaaaaaaa...b"} can
 * backtrack for minutes. Callers that run user-supplied {@link Pattern} against user input should wrap the input with
 * {@link #bounded(CharSequence)} before passing it to {@link Pattern#matcher(CharSequence)}. The wrapper enforces a
 * hard cap on total {@code charAt} accesses during matching and throws {@link RegexExecutionLimitException} before the
 * JVM thread can be held for long. {@link #bounded(CharSequence)} additionally rejects inputs larger than
 * {@link #MAX_INPUT_LENGTH} up front.</li>
 * <li><em>Compile time.</em> {@link Pattern#compile(String, int)} itself has no timeout — a sufficiently deeply nested
 * alternation can hang compilation for seconds. {@link #compile(String)} rejects expressions longer than
 * {@link #MAX_EXPRESSION_LENGTH} up front as a length-based proxy: realistic user-supplied patterns stay well under
 * this bound, while a compile-time DoS pattern tends to be pathologically long. This is a coarse but cheap defense; a
 * bounded-time compile would be more precise but requires thread-level interruption which cannot actually stop
 * {@link Pattern#compile}. Operators that need tighter bounds should pre-validate patterns at workflow-save time via
 * {@code CustomRegex.validateRegex}.</li>
 * </ol>
 *
 * @author Ivica Cardic
 */
public final class RegexParser {

    private static final Logger log = LoggerFactory.getLogger(RegexParser.class);

    /**
     * Bounded log-once cache for {@link #logCompileErrorOnce} — keyed by {@code scope + "|" + raw}, value is the
     * timestamp of the last emission in millis. Keeps logs quiet on a misconfigured workflow whose apply() runs every
     * request without losing the first signal nor leaking memory on adversarial inputs. Soft-capped — under heavy
     * concurrency the cap can be exceeded (same best-effort trade-off as {@code UrlDetector.WARNED_MALFORMED_CIDRS}).
     */
    private static final Map<String, Long> WARNED_COMPILE_ERRORS = new ConcurrentHashMap<>();
    private static final int COMPILE_ERROR_CACHE_MAX_SIZE = 1_024;
    private static final long COMPILE_ERROR_TTL_MILLIS = 60_000L;

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

    private RegexParser() {
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

    /** Thrown when user-supplied regex exceeds the DoS bound during matching. */
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
            // Matcher.group() and some third-party regex helpers call subSequence to extract a slice; the raw
            // delegate slice would escape the DoS counter and let a pathological input or caller burn arbitrary CPU
            // inside a single subSequence copy. Charge the slice length to the shared counter up front and return a
            // new BoundedCharSequence sharing the same counter so further charAt calls on the slice continue to
            // count against the original budget.
            int span = Math.max(0, end - start);

            counter.tick(span);

            return new BoundedCharSequence(delegate.subSequence(start, end), counter);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    /** Mutable access counter shared across a {@link BoundedCharSequence} and any slices produced via subSequence. */
    private static final class Counter {

        private final int maxAccesses;
        private int accesses;

        Counter(int maxAccesses) {
            this.maxAccesses = maxAccesses;
        }

        void tick(int delta) {
            // Saturating add — a huge delta (e.g. malicious Matcher slicing a huge range) must not wrap around past
            // Integer.MAX_VALUE and silently re-enter the allowed range. Once the counter saturates the next check
            // always trips.
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
            // Compile-time DoS defense: reject patterns so long that Pattern.compile could hang on deep alternation.
            // See class-level Javadoc for the rationale; callers that need tighter bounds should pre-validate via
            // CustomRegex.validateRegex at workflow-save time.
            throw new IllegalArgumentException(
                "Regex expression exceeds maximum length of " + MAX_EXPRESSION_LENGTH + " characters (got "
                    + expression.length() + ")");
        }

        if (expression.charAt(0) != '/') {
            return Pattern.compile(expression);
        }

        int lastSlash = expression.lastIndexOf('/');

        if (lastSlash <= 0) {
            return Pattern.compile(expression);
        }

        String body = expression.substring(1, lastSlash);
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
                default -> throw new IllegalArgumentException("Unsupported regex flag: " + character);
            }
        }

        return Pattern.compile(body, flagBits);
    }

    /**
     * Log a regex-compile failure at ERROR the first time it is seen for a given {@code scope + raw} pair, and then at
     * most once every {@link #COMPILE_ERROR_TTL_MILLIS} for the same pair. Call at most once per offending regex per
     * request from cluster-element {@code customRegexesOf} paths — the per-request compilation loop runs on every agent
     * invocation, so a single misconfigured workflow would otherwise spam ERROR logs continuously and drown real
     * events.
     *
     * <p>
     * The cache is bounded in size (best-effort under concurrency; same trade-off as
     * {@code UrlDetector.warnMalformedCidrOnce}). Callers MUST still rethrow the causing
     * {@code IllegalArgumentException} so the advisor treats the failure as a fail-closed configuration error — this
     * helper only controls log volume, not the block decision.
     *
     * @param scope short label identifying where the error originated, e.g. {@code "pii"} or {@code "secret-keys"}.
     * @param raw   the offending regex string from operator input.
     * @param cause the underlying compile failure (its message is included in the log).
     */
    public static void logCompileErrorOnce(String scope, String raw, Throwable cause) {
        String key = scope + "|" + raw;
        long now = System.currentTimeMillis();
        Long previousAt = WARNED_COMPILE_ERRORS.get(key);

        if (previousAt != null && now - previousAt < COMPILE_ERROR_TTL_MILLIS) {
            return;
        }

        // Best-effort size cap: first try dropping TTL-expired entries, then drop the oldest if everything is still
        // fresh. Mirrors UrlDetector.warnMalformedCidrOnce.
        if (WARNED_COMPILE_ERRORS.size() >= COMPILE_ERROR_CACHE_MAX_SIZE) {
            WARNED_COMPILE_ERRORS.entrySet()
                .removeIf(entry -> now - entry.getValue() >= COMPILE_ERROR_TTL_MILLIS);

            if (WARNED_COMPILE_ERRORS.size() >= COMPILE_ERROR_CACHE_MAX_SIZE) {
                WARNED_COMPILE_ERRORS.entrySet()
                    .stream()
                    .min(Map.Entry.comparingByValue())
                    .ifPresent(oldest -> WARNED_COMPILE_ERRORS.remove(oldest.getKey(), oldest.getValue()));
            }
        }

        if (previousAt == null
            ? WARNED_COMPILE_ERRORS.putIfAbsent(key, now) == null
            : WARNED_COMPILE_ERRORS.replace(key, previousAt, now)) {

            log.error(
                "{} custom regex '{}' failed to compile; every request will be blocked until the configuration is "
                    + "fixed. Cause: {}",
                scope, raw, cause.getMessage());
        }
    }
}
