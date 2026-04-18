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

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Option;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
// REDOS is suppressed at class scope because findsecbugs attributes Pattern.compile() calls in the
// DEFAULT_PII_PATTERNS List.of(...) initializer to the static initializer method rather than the field —
// a field-level suppression is therefore ignored (flagged by US_USELESS_SUPPRESSION_ON_FIELD). Every flagged
// pattern here (PHONE_NUMBER, DATE_TIME, IBAN_CODE) uses only fixed {N,M} or {N} quantifiers, so catastrophic
// backtracking is structurally impossible. Parity with n8n regexes is tracked by PiiDetectorParityTest.
@SuppressFBWarnings("REDOS")
public final class PiiDetector {

    /**
     * Default PII patterns. Entity names and regexes mirror n8n's {@code Guardrails/helpers/pii.ts} so workflows
     * exported from ByteChef round-trip to n8n (and vice-versa) without option-value renames or token-shape drift.
     * {@code DATE_TIME} is an intentional divergence — we keep ISO-8601 because the n8n American MM/DD/YYYY form is
     * surprising outside the US.
     */
    public static final List<PiiPattern> DEFAULT_PII_PATTERNS = List.of(
        // Global
        new PiiPattern(
            "EMAIL_ADDRESS",
            // The literal '|' inside the trailing TLD character class [A-Z|a-z] is a known n8n upstream typo. It
            // makes the class match the pipe character as well as letters; a TLD containing '|' would therefore
            // match (and a TLD-only-digits domain would not). Do NOT "fix" by removing the '|' here without making
            // the same change in n8n — the round-trip parity contract above relies on this divergence being
            // intentional. See PiiDetectorParityTest for the regression pin.
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")),
        new PiiPattern(
            "PHONE_NUMBER",
            Pattern.compile("\\b[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}\\b")),
        new PiiPattern(
            "CREDIT_CARD",
            Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b")),
        new PiiPattern(
            "IP_ADDRESS",
            Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b")),
        new PiiPattern(
            "IBAN_CODE",
            Pattern.compile("\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}([A-Z0-9]?){0,16}\\b")),
        new PiiPattern(
            "CRYPTO",
            Pattern.compile("\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b")),
        // DATE_TIME stays ISO-8601 — intentional divergence from n8n (which uses US-style MM/DD/YYYY).
        new PiiPattern(
            "DATE_TIME",
            Pattern.compile(
                "\\b\\d{4}-\\d{2}-\\d{2}(?:[T ]\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:?\\d{2})?)?\\b")),
        // Street-address keyword match ("… Main Street", "… Elm Ave", etc.).
        new PiiPattern(
            "LOCATION",
            Pattern.compile(
                "\\b[A-Za-z\\s]+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Drive|Dr|Lane|Ln"
                    + "|Place|Pl|Court|Ct|Way|Highway|Hwy)\\b")),
        new PiiPattern(
            "MEDICAL_LICENSE",
            Pattern.compile("\\b[A-Z]{2}\\d{6}\\b")),
        // USA
        new PiiPattern(
            "US_BANK_NUMBER",
            Pattern.compile("\\b\\d{8,17}\\b")),
        new PiiPattern(
            "US_DRIVER_LICENSE",
            Pattern.compile("\\b[A-Z]\\d{7}\\b")),
        new PiiPattern(
            "US_ITIN",
            Pattern.compile("\\b9\\d{2}-\\d{2}-\\d{4}\\b")),
        new PiiPattern(
            "US_PASSPORT",
            Pattern.compile("\\b[A-Z]\\d{8}\\b")),
        new PiiPattern(
            "US_SSN",
            Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b|\\b\\d{9}\\b")),
        // UK
        new PiiPattern(
            "UK_NHS",
            Pattern.compile("\\b\\d{3} \\d{3} \\d{4}\\b")),
        new PiiPattern(
            "UK_NINO",
            Pattern.compile("\\b[A-Z]{2}\\d{6}[A-Z]\\b")),
        // Spain
        new PiiPattern(
            "ES_NIF",
            Pattern.compile("\\b[A-Z]\\d{8}\\b")),
        new PiiPattern(
            "ES_NIE",
            Pattern.compile("\\b[A-Z]\\d{8}\\b")),
        // Italy
        new PiiPattern(
            "IT_FISCAL_CODE",
            Pattern.compile("\\b[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]\\b")),
        new PiiPattern(
            "IT_DRIVER_LICENSE",
            Pattern.compile("\\b[A-Z]{2}\\d{7}\\b")),
        new PiiPattern(
            "IT_VAT_CODE",
            Pattern.compile("\\bIT\\d{11}\\b")),
        new PiiPattern(
            "IT_PASSPORT",
            Pattern.compile("\\b[A-Z]{2}\\d{7}\\b")),
        new PiiPattern(
            "IT_IDENTITY_CARD",
            Pattern.compile("\\b[A-Z]{2}\\d{7}\\b")),
        // Poland
        new PiiPattern(
            "PL_PESEL",
            Pattern.compile("\\b\\d{11}\\b")),
        // Singapore
        new PiiPattern(
            "SG_NRIC_FIN",
            Pattern.compile("\\b[A-Z]\\d{7}[A-Z]\\b")),
        new PiiPattern(
            "SG_UEN",
            Pattern.compile("\\b\\d{8}[A-Z]\\b|\\b\\d{9}[A-Z]\\b")),
        // Australia
        new PiiPattern(
            "AU_ABN",
            Pattern.compile("\\b\\d{2} \\d{3} \\d{3} \\d{3}\\b")),
        new PiiPattern(
            "AU_ACN",
            Pattern.compile("\\b\\d{3} \\d{3} \\d{3}\\b")),
        new PiiPattern(
            "AU_TFN",
            Pattern.compile("\\b\\d{9}\\b")),
        new PiiPattern(
            "AU_MEDICARE",
            Pattern.compile("\\b\\d{4} \\d{5} \\d{1}\\b")),
        // India
        new PiiPattern(
            "IN_PAN",
            Pattern.compile("\\b[A-Z]{5}\\d{4}[A-Z]\\b")),
        new PiiPattern(
            "IN_AADHAAR",
            Pattern.compile("\\b\\d{4} \\d{4} \\d{4}\\b")),
        new PiiPattern(
            "IN_VEHICLE_REGISTRATION",
            Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z]{2}\\d{4}\\b")),
        new PiiPattern(
            "IN_VOTER",
            Pattern.compile("\\b[A-Z]{3}\\d{7}\\b")),
        new PiiPattern(
            "IN_PASSPORT",
            Pattern.compile("\\b[A-Z]\\d{7}\\b")),
        // Finland
        new PiiPattern(
            "FI_PERSONAL_IDENTITY_CODE",
            Pattern.compile("\\b\\d{6}[+-A]\\d{3}[A-Z0-9]\\b")));

    public static List<Option<String>> getPiiDetectionOptions() {
        return List.of(
            ComponentDsl.option("Email address", "EMAIL_ADDRESS"),
            ComponentDsl.option("Phone number", "PHONE_NUMBER"),
            ComponentDsl.option("Credit card number", "CREDIT_CARD"),
            ComponentDsl.option("IP address (IPv4)", "IP_ADDRESS"),
            ComponentDsl.option("IBAN code", "IBAN_CODE"),
            ComponentDsl.option("Bitcoin/crypto address", "CRYPTO"),
            ComponentDsl.option("Date or date-time (ISO-8601)", "DATE_TIME"),
            ComponentDsl.option("Street address / location", "LOCATION"),
            ComponentDsl.option("Medical license", "MEDICAL_LICENSE"),
            ComponentDsl.option("US Social Security Number", "US_SSN"),
            ComponentDsl.option("US bank account number", "US_BANK_NUMBER"),
            ComponentDsl.option("US driver license", "US_DRIVER_LICENSE"),
            ComponentDsl.option("US Individual Taxpayer Identification Number (ITIN)", "US_ITIN"),
            ComponentDsl.option("US passport number", "US_PASSPORT"),
            ComponentDsl.option("UK NHS number", "UK_NHS"),
            ComponentDsl.option("UK National Insurance Number", "UK_NINO"),
            ComponentDsl.option("Spanish NIF", "ES_NIF"),
            ComponentDsl.option("Spanish NIE", "ES_NIE"),
            ComponentDsl.option("Italian fiscal code (Codice Fiscale)", "IT_FISCAL_CODE"),
            ComponentDsl.option("Italian VAT code (Partita IVA)", "IT_VAT_CODE"),
            ComponentDsl.option("Italian driver license", "IT_DRIVER_LICENSE"),
            ComponentDsl.option("Italian passport", "IT_PASSPORT"),
            ComponentDsl.option("Italian identity card", "IT_IDENTITY_CARD"),
            ComponentDsl.option("Polish PESEL", "PL_PESEL"),
            ComponentDsl.option("Singapore NRIC/FIN", "SG_NRIC_FIN"),
            ComponentDsl.option("Singapore UEN (Unique Entity Number)", "SG_UEN"),
            ComponentDsl.option("Australian Business Number (ABN)", "AU_ABN"),
            ComponentDsl.option("Australian Company Number (ACN)", "AU_ACN"),
            ComponentDsl.option("Australian Tax File Number (TFN)", "AU_TFN"),
            ComponentDsl.option("Australian Medicare card number", "AU_MEDICARE"),
            ComponentDsl.option("Indian Aadhaar number", "IN_AADHAAR"),
            ComponentDsl.option("Indian Permanent Account Number (PAN)", "IN_PAN"),
            ComponentDsl.option("Indian passport number", "IN_PASSPORT"),
            ComponentDsl.option("Indian vehicle registration", "IN_VEHICLE_REGISTRATION"),
            ComponentDsl.option("Indian voter ID (EPIC)", "IN_VOTER"),
            ComponentDsl.option("Finnish Personal Identity Code (HETU)", "FI_PERSONAL_IDENTITY_CODE"));
    }

    private PiiDetector() {
    }

    /**
     * Filter the default PII patterns to only those matching the given type names.
     *
     * @param selectedTypes the list of PII type names to include (e.g., "EMAIL_ADDRESS", "PHONE_NUMBER")
     * @return filtered list of PII patterns
     */
    public static List<PiiPattern> filterByTypes(List<String> selectedTypes) {
        if (selectedTypes == null || selectedTypes.isEmpty()) {
            return Collections.emptyList();
        }

        return DEFAULT_PII_PATTERNS.stream()
            .filter(piiPattern -> selectedTypes.contains(piiPattern.type()))
            .toList();
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
        List<RegexParser.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        // Per-pattern bounded(content) call: the RegexParser budget is per-CharSequence, so a fresh wrapper per pattern
        // prevents cumulative exhaustion from letting later patterns silently skip coverage. Without this, a long but
        // legitimate input could exhaust the budget partway through the 35+ built-in patterns, leaving the tail of the
        // list unscanned — a fail-open classifier would then report partial coverage as full.
        for (PiiPattern piiPattern : patterns) {
            try {
                CharSequence bounded = RegexParser.bounded(content);
                Matcher matcher = piiPattern.pattern()
                    .matcher(bounded);

                while (matcher.find()) {
                    matches.add(
                        new PiiMatch(
                            matcher.group(),
                            matcher.start(),
                            matcher.end(),
                            piiPattern.type()));
                }
            } catch (RegexParser.RegexExecutionLimitException e) {
                budgetFailures.add(new RegexParser.RegexExecutionLimitException(
                    "pattern '" + piiPattern.type() + "': " + e.getMessage(), e));
            }
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

    /**
     * Detect PII in the given content using the provided patterns and additional user-supplied regexes. Matches from
     * {@code extraRegexes} are tagged with the {@code "CUSTOM"} type.
     *
     * @param content      the content to scan
     * @param patterns     the built-in PII patterns to use for detection
     * @param extraRegexes user-supplied regex patterns to detect alongside the built-ins
     * @return a list of PII matches found
     */
    public static List<PiiMatch> detect(String content, List<PiiPattern> patterns, List<Pattern> extraRegexes) {
        List<PiiMatch> matches = new ArrayList<>(detect(content, patterns));

        if (extraRegexes == null || extraRegexes.isEmpty()) {
            return matches;
        }

        List<RegexParser.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        // Per-pattern bounded isolates user-supplied regex DoS from each other and from the built-in patterns above.
        int index = 0;

        for (Pattern pattern : extraRegexes) {
            try {
                CharSequence bounded = RegexParser.bounded(content);
                Matcher matcher = pattern.matcher(bounded);

                while (matcher.find()) {
                    matches.add(new PiiMatch(matcher.group(), matcher.start(), matcher.end(), "CUSTOM"));
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

    /**
     * Mask PII in the given content by replacing matches with mask tokens.
     *
     * <p>
     * Overlapping matches are deduplicated with a longest-wins policy before masking: when two patterns match the same
     * span (e.g. {@code [A-Z]\d{7}} fires for both US_DRIVER_LICENSE and IT_PASSPORT, or {@code \b\d{9}\b} overlaps
     * US_SSN and AU_TFN), only the longest span is kept. Without this, the first {@code builder.replace(start,end,…)}
     * mutates the buffer so the second replace writes into the middle of the first mask token and corrupts the output.
     *
     * @param content the content to mask
     * @param matches the PII matches to mask
     * @return the masked content
     */
    public static String mask(String content, List<PiiMatch> matches) {
        if (content == null || content.isEmpty() || matches.isEmpty()) {
            return content;
        }

        List<PiiMatch> deduplicated = deduplicateOverlaps(matches);

        StringBuilder result = new StringBuilder(content);

        for (PiiMatch match : deduplicated) {
            result.replace(match.start(), match.end(), "<" + match.type() + ">");
        }

        return result.toString();
    }

    /**
     * Longest-wins overlap dedup. Mirrors {@code SecretKeyDetector.deduplicateOverlaps}: sort candidates by length desc
     * (ties broken by start), greedily keep those that do not overlap anything already kept, then sort by start desc
     * for the reverse-order replace loop.
     *
     * <p>
     * <b>Ambiguous-label caveat:</b> some entries in {@link #DEFAULT_PII_PATTERNS} share identical regexes by design
     * (n8n parity) — e.g. {@code IT_DRIVER_LICENSE}, {@code IT_PASSPORT}, and {@code IT_IDENTITY_CARD} are all
     * {@code \b[A-Z]{2}\d{7}\b}; {@code ES_NIF} and {@code ES_NIE} are both {@code \b[A-Z]\d{8}\b}. When a user's text
     * contains such a value, every equivalent pattern fires on the same span and this method keeps only one. The
     * <em>winning type label is determined by the order they appear in {@code DEFAULT_PII_PATTERNS}</em> (first to
     * enter {@code byLength} with that span wins, because later candidates overlap the kept one and are dropped). The
     * label therefore carries no semantic claim about which sub-type of ID was actually present — operators who need to
     * disambiguate should either disable the specific variants they do not want or rely on the masking behaviour (the
     * span is the same either way). Consumers of {@code match.type()} must not rely on label stability when multiple
     * ambiguous patterns are enabled.
     */
    private static List<PiiMatch> deduplicateOverlaps(List<PiiMatch> matches) {
        List<PiiMatch> byLength = new ArrayList<>(matches);

        byLength.sort(Comparator.<PiiMatch>comparingInt(match -> match.end() - match.start())
            .reversed()
            .thenComparingInt(PiiMatch::start));

        List<PiiMatch> kept = new ArrayList<>();

        for (PiiMatch candidate : byLength) {
            boolean overlaps = false;

            for (PiiMatch existing : kept) {
                if (candidate.start() < existing.end() && candidate.end() > existing.start()) {
                    overlaps = true;

                    break;
                }
            }

            if (!overlaps) {
                kept.add(candidate);
            }
        }

        kept.sort(Comparator.comparingInt(PiiMatch::start)
            .reversed());

        return kept;
    }

    /**
     * A PII pattern definition.
     *
     * @param type    the type of PII (e.g., EMAIL_ADDRESS, PHONE_NUMBER)
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

        /**
         * Redacted {@code toString} — the whole point of this record is to hold a detected PII value (email, SSN, phone
         * number, etc.). Leaking the value through any accidental {@code log.debug("match={}", match)} or error-message
         * concatenation would defeat the detector's purpose. Render the type and span only; callers that genuinely need
         * the value must go through {@link #value()} explicitly.
         */
        @Override
        public String toString() {
            return "PiiMatch{type=" + type + ", span=[" + start + ".." + end + "]}";
        }
    }
}
