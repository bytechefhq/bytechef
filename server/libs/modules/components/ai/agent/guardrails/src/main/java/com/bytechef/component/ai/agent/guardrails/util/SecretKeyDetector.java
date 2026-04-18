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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects and masks secret keys / API credentials with three permissiveness levels.
 *
 * @author Ivica Cardic
 */
public final class SecretKeyDetector {

    private static final double LOG2 = Math.log(2);

    /**
     * Detection level — counter-intuitively, {@link #STRICT} detects the <em>most</em> patterns (highest sensitivity,
     * highest false-positive rate), while {@link #PERMISSIVE} detects the <em>fewest</em> (only named providers, lowest
     * false-positive rate). Naming reflects the strictness of the security posture, not the breadth of detection.
     *
     * <ul>
     * <li>{@code STRICT} — named providers + high-entropy tokens + generic key=value pairs</li>
     * <li>{@code BALANCED} — named providers + high-entropy tokens (default)</li>
     * <li>{@code PERMISSIVE} — named providers only</li>
     * </ul>
     */
    public enum Permissiveness {
        STRICT, BALANCED, PERMISSIVE
    }

    private record NamedPattern(String type, Pattern pattern) {
    }

    private static final List<NamedPattern> NAMED_PROVIDER_PATTERNS = List.of(
        new NamedPattern("AWS_ACCESS_KEY", Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b")),
        new NamedPattern("AWS_SECRET_KEY", Pattern.compile("(?i)aws.{0,20}?[\"'][0-9a-zA-Z/+]{40}[\"']")),
        new NamedPattern("GITHUB_PAT", Pattern.compile("\\bghp_[0-9A-Za-z]{36}\\b")),
        new NamedPattern("GITHUB_FINE_GRAINED_PAT", Pattern.compile("\\bgithub_pat_[0-9A-Za-z_]{82}\\b")),
        new NamedPattern("SLACK_TOKEN", Pattern.compile("\\bxox[abp]-[0-9A-Za-z-]{10,48}\\b")),
        new NamedPattern("STRIPE_KEY", Pattern.compile("\\b(?:sk|pk)_(?:live|test)_[0-9A-Za-z]{16,}\\b")),
        new NamedPattern("GOOGLE_API_KEY", Pattern.compile("\\bAIza[0-9A-Za-z_-]{35}\\b")),
        new NamedPattern("OPENAI_KEY", Pattern.compile("\\bsk-[0-9A-Za-z]{20,}\\b")),
        new NamedPattern("JWT",
            Pattern.compile("\\bey[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+\\.[0-9A-Za-z_-]+\\b")));

    private static final NamedPattern KEY_EQUALS_VALUE = new NamedPattern(
        "GENERIC_SECRET",
        // The (?<![A-Za-z]) lookbehind anchors the key noun on the left so a substring such as 'notapikey=...' or
        // 'mySecretValue=...' is NOT matched as 'apikey'/'secret'. Without it any identifier ending in one of the key
        // words would be flagged as a generic secret assignment, producing both false positives and noisy violations
        // that hide the real ones. Right-anchoring is provided by the explicit `\s*[:=]` separator.
        Pattern.compile(
            "(?i)(?<![A-Za-z])(?:api[_-]?key|secret|token|password|auth)\\s*[:=]\\s*[\"']?([A-Za-z0-9_\\-]{16,})[\"']?"));

    /**
     * Token-shape thresholds per level. STRICT lowers the bar (more flags, more false positives); PERMISSIVE raises it
     * (fewer flags, fewer false positives). Per-level tuning lets operators pick the trade-off without having to fork
     * the detector.
     */
    private record LevelThresholds(int minLength, double minEntropyBits, int minCharDiversity) {
    }

    /**
     * Thresholds mirror n8n's {@code Guardrails/helpers/secretKeys.ts}:
     * <ul>
     * <li>STRICT — minLength 10, minEntropy 3.0, minDiversity 2, strict_mode ON (bypasses shape allowlist)</li>
     * <li>BALANCED — minLength 10, minEntropy 3.8, minDiversity 3</li>
     * <li>PERMISSIVE — minLength 30, minEntropy 4.0, minDiversity 2</li>
     * </ul>
     * Diversity is measured as distinct character classes (lowercase, uppercase, digit, special) — max value 4 — not
     * distinct characters, matching n8n's behaviour.
     */
    private static final Map<Permissiveness, LevelThresholds> LEVEL_THRESHOLDS = Map.of(
        Permissiveness.STRICT, new LevelThresholds(10, 3.0, 2),
        Permissiveness.BALANCED, new LevelThresholds(10, 3.8, 3),
        Permissiveness.PERMISSIVE, new LevelThresholds(30, 4.0, 2));

    /**
     * Well-known provider-token prefixes. When a token contains any of these it is flagged regardless of length or
     * entropy — no legitimate data should contain {@code AKIA}, {@code sk-}, etc. Mirrors n8n's list minus
     * {@code "SHA:"} and {@code "Bearer "}, which n8n includes but which never match here: {@link #isTokenChar} does
     * not admit colons or spaces, so tokens assembled by the scanner cannot contain them. Content-level scanning for
     * those two prefixes is handled separately by upstream callers (HTTP clients that log request bodies, for example)
     * and should not be this detector's responsibility.
     */
    private static final List<String> COMMON_KEY_PREFIXES = List.of(
        "key-", "sk-", "sk_", "pk_", "pk-", "ghp_", "AKIA", "xox", "SG.", "hf_",
        "api-", "apikey-", "token-", "secret-");

    /** Characters allowed in the host part of the URL-shape allowlist check. */
    private static final String URL_HOST_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789.-";

    /** Characters allowed in the path part of the URL-shape allowlist check (a superset of host characters). */
    private static final String URL_PATH_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789./_-";

    /**
     * File extensions allowlisted at token-shape level. A standalone token ending in one of these is treated as a file
     * path and excluded from entropy-based flagging. Mirrors n8n's {@code ALLOWED_EXTENSIONS} (48 entries covering
     * source, config, docs, binary, and media extensions).
     */
    private static final List<String> DEFAULT_ALLOWED_EXTENSIONS = List.of(
        ".py", ".js", ".html", ".css", ".json", ".md", ".txt", ".csv", ".xml", ".yaml", ".yml",
        ".ini", ".conf", ".config", ".log", ".sql", ".sh", ".bat",
        ".dll", ".so", ".dylib", ".jar", ".war",
        ".php", ".rb", ".go", ".rs", ".ts", ".jsx", ".tsx", ".vue",
        ".cpp", ".c", ".h", ".cs", ".fs", ".vb", ".java", ".kt", ".gradle",
        ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".pdf",
        ".jpg", ".jpeg", ".png");

    private SecretKeyDetector() {
    }

    public static List<SecretMatch> detect(String content, Permissiveness level) {
        if (content == null || content.isEmpty()) {
            return List.of();
        }

        List<SecretMatch> matches = new ArrayList<>();
        List<RegexParser.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        // Per-pattern bounded(content) so one pattern's DoS exhaustion doesn't silently skip subsequent scans. Without
        // isolation, a pathological input that trips the budget on the first pattern would hide the fact that patterns
        // 2..N might have matched real keys. Every pattern runs; exhaustion on any pattern is recorded and surfaced as
        // a combined exception at the end so the advisor still fail-closed-blocks (we can't certify the detector ran).
        for (NamedPattern namedPattern : NAMED_PROVIDER_PATTERNS) {
            try {
                CharSequence bounded = RegexParser.bounded(content);

                Matcher matcher = namedPattern.pattern()
                    .matcher(bounded);

                while (matcher.find()) {
                    matches.add(new SecretMatch(matcher.group(), matcher.start(), matcher.end(), namedPattern.type()));
                }
            } catch (RegexParser.RegexExecutionLimitException e) {
                budgetFailures.add(new RegexParser.RegexExecutionLimitException(
                    "pattern '" + namedPattern.type() + "': " + e.getMessage(), e));
            }
        }

        matches.addAll(detectPrefixedTokens(content));
        matches.addAll(detectHighEntropyTokens(content, level));

        if (level == Permissiveness.STRICT) {
            try {
                CharSequence bounded = RegexParser.bounded(content);

                Matcher matcher = KEY_EQUALS_VALUE.pattern()
                    .matcher(bounded);

                while (matcher.find()) {
                    // Deliberately storing the whole match (group 0) including the `api_key=` prefix, not just the
                    // captured value (group 1). STRICT mode masks the whole assignment —
                    // "api_key=\"<GENERIC_SECRET>\"" would leak the key name and quote shape; replacing the entire
                    // assignment with "<GENERIC_SECRET>" removes the surrounding context that could hint at which
                    // credential leaked.
                    matches.add(
                        new SecretMatch(matcher.group(), matcher.start(), matcher.end(), KEY_EQUALS_VALUE.type()));
                }
            } catch (RegexParser.RegexExecutionLimitException e) {
                budgetFailures.add(new RegexParser.RegexExecutionLimitException(
                    "pattern '" + KEY_EQUALS_VALUE.type() + "': " + e.getMessage(), e));
            }
        }

        if (!budgetFailures.isEmpty()) {
            // Surface at least one budget failure so the advisor's isConfigurationError blocks fail-closed. Additional
            // failures are attached as suppressed so operators see every pattern that hit the cap, not just the first.
            RegexParser.RegexExecutionLimitException headline = budgetFailures.getFirst();

            budgetFailures.stream()
                .skip(1)
                .forEach(headline::addSuppressed);

            throw headline;
        }

        return matches;
    }

    public static List<SecretMatch> detect(String content, Permissiveness level, List<Pattern> extraRegexes) {
        return detect(content, level, extraRegexes, List.of());
    }

    /**
     * Detect secrets in {@code content}, skipping any content inside markdown fenced code blocks whose language tag
     * matches one of {@code allowedFileExtensions}. Useful when the input legitimately contains code samples: a Python
     * fence full of long identifiers that look like secrets would otherwise produce dozens of false positives.
     *
     * <p>
     * The allowlist strip preserves offsets — blocked content is replaced by spaces (keeping newlines) so positions of
     * other matches outside the block remain accurate.
     */
    public static List<SecretMatch> detect(
        String content, Permissiveness level, List<Pattern> extraRegexes, List<String> allowedFileExtensions) {

        String scanned = stripAllowedCodeBlocks(content, allowedFileExtensions);

        List<SecretMatch> matches = new ArrayList<>(detect(scanned, level));

        if (extraRegexes == null || extraRegexes.isEmpty()) {
            return matches;
        }

        List<RegexParser.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        // Per-pattern bounded(scanned) isolates user-supplied regex DoS from each other — a single pathological custom
        // regex can't silently skip later ones (or mask findings from the built-in scan above).
        int index = 0;

        for (Pattern pattern : extraRegexes) {
            try {
                CharSequence bounded = RegexParser.bounded(scanned);
                Matcher matcher = pattern.matcher(bounded);

                while (matcher.find()) {
                    matches.add(new SecretMatch(matcher.group(), matcher.start(), matcher.end(), "CUSTOM"));
                }
            } catch (RegexParser.RegexExecutionLimitException e) {
                budgetFailures.add(new RegexParser.RegexExecutionLimitException(
                    "extraRegex[" + index + "] '" + pattern.pattern() + "': " + e.getMessage(), e));
            }

            index++;
        }

        if (!budgetFailures.isEmpty()) {
            RegexParser.RegexExecutionLimitException headline = budgetFailures.getFirst();

            budgetFailures.stream()
                .skip(1)
                .forEach(headline::addSuppressed);

            throw headline;
        }

        return matches;
    }

    private static String stripAllowedCodeBlocks(String content, List<String> extensions) {
        if (extensions == null || extensions.isEmpty() || content == null || content.isEmpty()) {
            return content;
        }

        List<String> quoted = extensions.stream()
            .filter(extension -> extension != null && !extension.isBlank())
            .map(Pattern::quote)
            .toList();

        if (quoted.isEmpty()) {
            return content;
        }

        // Lookahead (?=[\s`\n]) defends against a language tag being a prefix of a longer word (e.g. `py` in `python`).
        // The existing \s*\n already blocks most cases by requiring whitespace-then-newline after the tag, but an
        // explicit word-boundary lookahead makes the intent unmistakeable and guards against future regex edits.
        Pattern fence = Pattern.compile(
            "```(?:" + String.join("|", quoted) + ")(?=[\\s`\\n])\\s*\\n(.*?)```",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

        Matcher matcher = fence.matcher(content);

        if (!matcher.find()) {
            return content;
        }

        StringBuilder result = new StringBuilder(content);

        do {
            int blockStart = matcher.start(1);
            int blockEnd = matcher.end(1);

            for (int index = blockStart; index < blockEnd; index++) {
                if (result.charAt(index) != '\n') {
                    result.setCharAt(index, ' ');
                }
            }
        } while (matcher.find());

        return result.toString();
    }

    private static List<SecretMatch> detectPrefixedTokens(String content) {
        String normalized = stripMarkdownEmphasis(content);

        List<SecretMatch> matches = new ArrayList<>();
        int length = normalized.length();
        int start = -1;

        for (int index = 0; index <= length; index++) {
            char character = index < length ? normalized.charAt(index) : ' ';

            if (isTokenChar(character)) {
                if (start < 0) {
                    start = index;
                }
            } else if (start >= 0) {
                int end = index;
                String token = normalized.substring(start, end);

                if (hasKnownPrefix(token)) {
                    matches.add(new SecretMatch(token, start, end, "PREFIXED_SECRET"));
                }

                start = -1;
            }
        }

        return matches;
    }

    private static List<SecretMatch> detectHighEntropyTokens(String content, Permissiveness level) {
        LevelThresholds thresholds = LEVEL_THRESHOLDS.get(level);
        String normalized = stripMarkdownEmphasis(content);
        String scanned = level == Permissiveness.STRICT ? normalized : stripUrlSpans(normalized);

        List<SecretMatch> matches = new ArrayList<>();
        int length = scanned.length();
        int start = -1;

        for (int index = 0; index <= length; index++) {
            char character = index < length ? scanned.charAt(index) : ' ';

            if (isTokenChar(character)) {
                if (start < 0) {
                    start = index;
                }
            } else {
                if (start >= 0) {
                    int end = index;
                    String token = scanned.substring(start, end);

                    boolean gatedByShape = level != Permissiveness.STRICT && tokenIsAllowedByShape(token);

                    if (!gatedByShape && qualifiesAsSecret(token, thresholds)) {
                        matches.add(new SecretMatch(token, start, end, "HIGH_ENTROPY_TOKEN"));
                    }

                    start = -1;
                }
            }
        }

        return matches;
    }

    /**
     * Replace markdown emphasis characters ({@code *}, {@code #}) with spaces. A key decorated as {@code **sk-abc**}
     * should still flag on the inner {@code sk-abc}; leaving the stars in place would split the token-boundary scan and
     * hide the prefix from {@link #hasKnownPrefix(String)}.
     */
    private static String stripMarkdownEmphasis(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        StringBuilder result = new StringBuilder(content);

        for (int index = 0; index < result.length(); index++) {
            char character = result.charAt(index);

            if (character == '*' || character == '#') {
                result.setCharAt(index, ' ');
            }
        }

        return result.toString();
    }

    private static final Pattern URL_SPAN = Pattern.compile("\\bhttps?://[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);

    /**
     * Replace {@code http://} / {@code https://} URL spans in {@code content} with spaces so token-boundary scanning
     * cannot flag URL path segments as high-entropy secrets. Positions are preserved — other secret offsets stay
     * accurate. URLs containing a known secret prefix are left intact so the prefix scan still fires.
     */
    private static String stripUrlSpans(String content) {
        Matcher matcher = URL_SPAN.matcher(content);

        if (!matcher.find()) {
            return content;
        }

        StringBuilder result = new StringBuilder(content);

        do {
            int spanStart = matcher.start();
            int spanEnd = matcher.end();
            String span = content.substring(spanStart, spanEnd);

            if (hasKnownPrefixAnywhere(span)) {
                continue;
            }

            for (int index = spanStart; index < spanEnd; index++) {
                if (result.charAt(index) != '\n') {
                    result.setCharAt(index, ' ');
                }
            }
        } while (matcher.find());

        return result.toString();
    }

    private static boolean hasKnownPrefixAnywhere(String span) {
        for (String prefix : COMMON_KEY_PREFIXES) {
            if (span.contains(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static boolean tokenIsAllowedByShape(String token) {
        // A known-prefix substring anywhere in the token keeps the scanner engaged — an API key embedded inside a URL
        // path or file-shaped token should still fire. n8n parity: secretKeys.ts checks `contains`, not `startsWith`.
        for (String prefix : COMMON_KEY_PREFIXES) {
            if (token.contains(prefix)) {
                return false;
            }
        }

        if (looksLikeUrl(token)) {
            return true;
        }

        String lower = token.toLowerCase(java.util.Locale.ROOT);

        for (String extension : DEFAULT_ALLOWED_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Linear-time {@code http(s)://host[/path]} shape check. Equivalent to the previous URL_TOKEN_PATTERN regex
     * {@code (?i)^https?://[a-z0-9.-]+(?:/[a-z0-9./_-]*)?$} but implemented via a single forward scan so ReDOS scanners
     * cannot flag it and the intent reads as plain control flow.
     */
    private static boolean looksLikeUrl(String token) {
        String lower = token.toLowerCase(java.util.Locale.ROOT);
        int cursor;

        if (lower.startsWith("https://")) {
            cursor = 8;
        } else if (lower.startsWith("http://")) {
            cursor = 7;
        } else {
            return false;
        }

        int hostStart = cursor;

        while (cursor < lower.length()) {
            char character = lower.charAt(cursor);

            if (character == '/') {
                break;
            }

            if (URL_HOST_CHARS.indexOf(character) < 0) {
                return false;
            }

            cursor++;
        }

        if (cursor == hostStart) {
            return false;
        }

        for (int index = cursor; index < lower.length(); index++) {
            if (URL_PATH_CHARS.indexOf(lower.charAt(index)) < 0) {
                return false;
            }
        }

        return true;
    }

    private static boolean isTokenChar(char character) {
        return Character.isLetterOrDigit(character) || character == '-' || character == '_' || character == '.';
    }

    private static boolean hasKnownPrefix(String token) {
        for (String prefix : COMMON_KEY_PREFIXES) {
            if (token.regionMatches(false, 0, prefix, 0, prefix.length())) {
                return true;
            }
        }

        return false;
    }

    private static boolean qualifiesAsSecret(String token, LevelThresholds thresholds) {
        if (token.length() < thresholds.minLength()) {
            return false;
        }

        if (shannonEntropy(token) < thresholds.minEntropyBits()) {
            return false;
        }

        return charClassDiversity(token) >= thresholds.minCharDiversity();
    }

    /**
     * Count how many of {lowercase, uppercase, digit, special} character classes appear in {@code token}. Max value is
     * 4. Mirrors n8n's {@code charDiversity} in {@code secretKeys.ts} so the level thresholds translate 1:1 between the
     * two implementations.
     */
    private static int charClassDiversity(String token) {
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int index = 0; index < token.length(); index++) {
            char character = token.charAt(index);

            if (Character.isLowerCase(character)) {
                hasLower = true;
            } else if (Character.isUpperCase(character)) {
                hasUpper = true;
            } else if (Character.isDigit(character)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        return (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
    }

    private static double shannonEntropy(String token) {
        int[] counts = new int[128];

        for (int index = 0; index < token.length(); index++) {
            char character = token.charAt(index);

            if (character < 128) {
                counts[character]++;
            }
        }

        double entropy = 0.0;
        int length = token.length();

        for (int count : counts) {
            if (count == 0) {
                continue;
            }

            double probability = (double) count / length;
            entropy -= probability * (Math.log(probability) / LOG2);
        }

        return entropy;
    }

    public static String mask(String content, List<SecretMatch> matches) {
        if (content == null || content.isEmpty() || matches == null || matches.isEmpty()) {
            return content;
        }

        List<SecretMatch> deduplicated = deduplicateOverlaps(matches);

        StringBuilder builder = new StringBuilder(content);

        for (SecretMatch match : deduplicated) {
            builder.replace(match.start(), match.end(), "<" + match.type() + ">");
        }

        return builder.toString();
    }

    private static List<SecretMatch> deduplicateOverlaps(List<SecretMatch> matches) {
        List<SecretMatch> byLength = new ArrayList<>(matches);

        byLength.sort(Comparator.<SecretMatch>comparingInt(match -> match.end() - match.start())
            .reversed()
            .thenComparingInt(SecretMatch::start));

        List<SecretMatch> kept = new ArrayList<>();

        for (SecretMatch candidate : byLength) {
            boolean overlaps = false;

            for (SecretMatch existing : kept) {
                if (candidate.start() < existing.end() && candidate.end() > existing.start()) {
                    overlaps = true;

                    break;
                }
            }

            if (!overlaps) {
                kept.add(candidate);
            }
        }

        kept.sort(Comparator.comparingInt(SecretMatch::start)
            .reversed());

        return kept;
    }

    public record SecretMatch(String value, int start, int end, String type) {

        /**
         * Redacted {@code toString} — the whole point of this record is to hold a detected secret value, so leaking
         * that value through any accidental {@code log.debug("match={}", match)} or error-message concatenation would
         * defeat the detector. Render the type and span only; callers that genuinely need the value must go through
         * {@link #value()} explicitly.
         */
        @Override
        public String toString() {
            return "SecretMatch{type=" + type + ", span=[" + start + ".." + end + "]}";
        }
    }
}
