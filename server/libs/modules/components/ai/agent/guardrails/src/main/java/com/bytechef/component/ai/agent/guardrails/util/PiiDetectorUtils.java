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
// REDOS is suppressed because every pattern here uses only fixed {N,M}/{N} or possessive quantifiers; no
// catastrophic backtracking is possible. Field-level suppression is ignored due to how findsecbugs attributes
// Pattern.compile in the static initializer, so class-level is required.
@SuppressFBWarnings("REDOS")
public final class PiiDetectorUtils {

    public static final List<PiiPattern> DEFAULT_PII_PATTERNS = List.of(
        // Global
        new PiiPattern(
            "EMAIL_ADDRESS",
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b")),
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
        new PiiPattern(
            "DATE_TIME",
            Pattern.compile(
                "\\b\\d{4}-\\d{2}-\\d{2}(?:[T ]\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:?\\d{2})?)?\\b")),
        new PiiPattern(
            "LOCATION",
            Pattern.compile(
                "\\b(?:[A-Za-z]++\\s+)++(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Drive|Dr|Lane|Ln"
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

    private PiiDetectorUtils() {
    }

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
        List<RegexParserUtils.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        for (PiiPattern piiPattern : patterns) {
            try {
                CharSequence bounded = RegexParserUtils.bounded(content);
                Pattern pattern = piiPattern.pattern();

                Matcher matcher = pattern.matcher(bounded);

                while (matcher.find()) {
                    matches.add(
                        new PiiMatch(matcher.group(), matcher.start(), matcher.end(), piiPattern.type()));
                }
            } catch (RegexParserUtils.RegexExecutionLimitException exception) {
                budgetFailures.add(
                    new RegexParserUtils.RegexExecutionLimitException(
                        "pattern '" + piiPattern.type() + "': " + exception.getMessage(), exception));
            }
        }

        if (!budgetFailures.isEmpty()) {
            RegexParserUtils.RegexExecutionLimitException headline = budgetFailures.getFirst();

            budgetFailures.stream()
                .skip(1)
                .forEach(headline::addSuppressed);

            throw headline;
        }

        return matches;
    }

    public static List<PiiMatch> detect(String content, List<PiiPattern> patterns, List<Pattern> extraRegexes) {
        List<PiiMatch> matches = new ArrayList<>(detect(content, patterns));

        if (extraRegexes == null || extraRegexes.isEmpty()) {
            return matches;
        }

        List<RegexParserUtils.RegexExecutionLimitException> budgetFailures = new ArrayList<>();

        int index = 0;

        for (Pattern pattern : extraRegexes) {
            try {
                CharSequence bounded = RegexParserUtils.bounded(content);
                Matcher matcher = pattern.matcher(bounded);

                while (matcher.find()) {
                    matches.add(new PiiMatch(matcher.group(), matcher.start(), matcher.end(), "CUSTOM"));
                }
            } catch (RegexParserUtils.RegexExecutionLimitException exception) {
                budgetFailures.add(new RegexParserUtils.RegexExecutionLimitException(
                    "extraRegex[" + index + "] '" + pattern.pattern() + "': " + exception.getMessage(), exception));
            }

            index++;
        }

        if (!budgetFailures.isEmpty()) {
            RegexParserUtils.RegexExecutionLimitException headline = budgetFailures.getFirst();

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
        int lastStart = Integer.MAX_VALUE;

        for (PiiMatch match : deduplicated) {
            if (match.end() > lastStart) {
                throw new IllegalStateException(
                    "out-of-order match: [" + match.start() + "," + match.end() + ") overlaps " + lastStart);
            }

            result.replace(match.start(), match.end(), "<" + match.type() + ">");

            lastStart = match.start();
        }

        return result.toString();
    }

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

        @Override
        public String toString() {
            return "PiiMatch{type=" + type + ", span=[" + start + ".." + end + "]}";
        }
    }
}
