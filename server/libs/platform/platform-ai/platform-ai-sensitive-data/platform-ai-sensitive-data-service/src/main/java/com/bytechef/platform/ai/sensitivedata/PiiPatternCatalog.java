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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("REDOS")
public final class PiiPatternCatalog {

    public static final Set<String> CONTEXTUAL_TYPES = Set.of("DATE_TIME", "LOCATION");

    private static final int CONTEXT_WINDOW = 40;

    private static final double CONTEXT_PROMOTED_SCORE = 0.9;

    public static final List<PiiPattern> ALL = List.of(
        new PiiPattern(
            "EMAIL_ADDRESS",
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"),
            0.9),
        new PiiPattern(
            "PHONE_NUMBER",
            Pattern.compile("\\b[+]?[(]?[0-9]{3}[)]?[-\\s.][0-9]{3}[-\\s.][0-9]{4,6}\\b"),
            0.6),
        new PiiPattern(
            "CREDIT_CARD",
            Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b"),
            0.9,
            PiiPatternCatalog::isLuhnValid),
        new PiiPattern(
            "IP_ADDRESS",
            Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"),
            0.6),
        new PiiPattern(
            "IBAN_CODE",
            Pattern.compile("\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}([A-Z0-9]?){0,16}\\b"),
            0.9),
        new PiiPattern(
            "CRYPTO",
            Pattern.compile("\\b[13][a-km-zA-HJ-NP-Z1-9]{25,34}\\b"),
            0.9),
        new PiiPattern(
            "DATE_TIME",
            Pattern.compile(
                "\\b\\d{4}-\\d{2}-\\d{2}(?:[T ]\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:?\\d{2})?)?\\b"),
            0.6),
        new PiiPattern(
            "LOCATION",
            Pattern.compile(
                "\\b(?:[A-Za-z]++\\s+)++(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Drive|Dr|Lane|Ln"
                    + "|Place|Pl|Court|Ct|Way|Highway|Hwy)\\b"),
            0.6),
        new PiiPattern(
            "MEDICAL_LICENSE",
            Pattern.compile("\\b[A-Z]{2}\\d{6}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "medical license",
                    "medical licence",
                    "dea number",
                    "physician license",
                    "practitioner number"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "US_BANK_NUMBER",
            Pattern.compile("\\b\\d{8,17}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "bank account",
                    "account number",
                    "acct no",
                    "routing number",
                    "checking account",
                    "savings account"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "US_DRIVER_LICENSE",
            Pattern.compile("\\b[A-Z]\\d{7}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "driver license",
                    "driver's license",
                    "driving license",
                    "driving licence",
                    "dln"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "US_ITIN",
            Pattern.compile("\\b9\\d{2}-\\d{2}-\\d{4}\\b"),
            0.6),
        new PiiPattern(
            "US_PASSPORT",
            Pattern.compile("\\b[A-Z]\\d{8}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "passport"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "US_SSN",
            Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b"),
            0.6),
        new PiiPattern(
            "UK_NHS",
            Pattern.compile("\\b\\d{3} \\d{3} \\d{4}\\b"),
            0.6),
        new PiiPattern(
            "UK_NINO",
            Pattern.compile("\\b[A-Z]{2}\\d{6}[A-Z]\\b"),
            0.6),
        new PiiPattern(
            "ES_NIF",
            Pattern.compile("\\b\\d{8}[A-Z]\\b"),
            0.6),
        new PiiPattern(
            "ES_NIE",
            Pattern.compile("\\b[XYZ]\\d{7}[A-Z]\\b"),
            0.6),
        new PiiPattern(
            "IT_FISCAL_CODE",
            Pattern.compile("\\b[A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z]\\b"),
            0.9),
        new PiiPattern(
            "IT_DRIVER_LICENSE",
            Pattern.compile("\\b[A-Z]{2}\\d{7}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "patente di guida",
                    "patente",
                    "driver license",
                    "driving licence"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "IT_VAT_CODE",
            Pattern.compile("\\bIT\\d{11}\\b"),
            0.9),
        new PiiPattern(
            "IT_PASSPORT",
            Pattern.compile("\\b[A-Z]{2}\\d{7}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "passaporto",
                    "passport"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "IT_IDENTITY_CARD",
            Pattern.compile("\\b[A-Z]{2}\\d{7}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "carta d'identita",
                    "carta di identita",
                    "identity card",
                    "documento di identita"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "PL_PESEL",
            Pattern.compile("\\b\\d{11}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "pesel",
                    "numer pesel"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "SG_NRIC_FIN",
            Pattern.compile("\\b[A-Z]\\d{7}[A-Z]\\b"),
            0.6),
        new PiiPattern(
            "SG_UEN",
            Pattern.compile("\\b\\d{8}[A-Z]\\b|\\b\\d{9}[A-Z]\\b"),
            0.6),
        new PiiPattern(
            "AU_ABN",
            Pattern.compile("\\b\\d{2} \\d{3} \\d{3} \\d{3}\\b"),
            0.6),
        new PiiPattern(
            "AU_ACN",
            Pattern.compile("\\b\\d{3} \\d{3} \\d{3}\\b"),
            0.6),
        new PiiPattern(
            "AU_TFN",
            Pattern.compile("\\b\\d{9}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "tax file number",
                    "tax file no",
                    "tfn"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "AU_MEDICARE",
            Pattern.compile("\\b\\d{4} \\d{5} \\d{1}\\b"),
            0.6),
        new PiiPattern(
            "IN_PAN",
            Pattern.compile("\\b[A-Z]{5}\\d{4}[A-Z]\\b"),
            0.6),
        new PiiPattern(
            "IN_AADHAAR",
            Pattern.compile("\\b\\d{4} \\d{4} \\d{4}\\b"),
            0.6),
        new PiiPattern(
            "IN_VEHICLE_REGISTRATION",
            Pattern.compile("\\b[A-Z]{2}\\d{2}[A-Z]{2}\\d{4}\\b"),
            0.6),
        new PiiPattern(
            "IN_VOTER",
            Pattern.compile("\\b[A-Z]{3}\\d{7}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "voter id",
                    "epic no",
                    "voter card",
                    "elector"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "IN_PASSPORT",
            Pattern.compile("\\b[A-Z]\\d{7}\\b"),
            0.2,
            new ContextRule(
                Set.of(
                    "passport"),
                CONTEXT_WINDOW, CONTEXT_PROMOTED_SCORE)),
        new PiiPattern(
            "FI_PERSONAL_IDENTITY_CODE",
            Pattern.compile("\\b\\d{6}[+A-]\\d{3}[A-Z0-9]\\b"),
            0.6));

    private PiiPatternCatalog() {
    }

    public static List<PiiPattern> curatedDefault() {
        return ALL.stream()
            .filter(piiPattern -> !CONTEXTUAL_TYPES.contains(piiPattern.type()))
            .toList();
    }

    public static List<PiiPattern> filterByTypes(List<String> selectedTypes) {
        if (selectedTypes == null || selectedTypes.isEmpty()) {
            return List.of();
        }

        return ALL.stream()
            .filter(piiPattern -> selectedTypes.contains(piiPattern.type()))
            .toList();
    }

    public record PiiPattern(
        String type, Pattern pattern, double score, @Nullable Predicate<String> validator,
        @Nullable ContextRule contextRule) {

        public PiiPattern {
            if (!Double.isFinite(score) || score < 0.0 || score > 1.0) {
                throw new IllegalArgumentException("score must be between 0.0 and 1.0, got: " + score);
            }

            if (contextRule != null && contextRule.score() <= score) {
                throw new IllegalArgumentException(
                    "a context rule must RAISE confidence: " + type + " scores " + score +
                        " and its rule offers " + contextRule.score());
            }
        }

        public PiiPattern(String type, Pattern pattern, double score) {
            this(type, pattern, score, null, null);
        }

        public PiiPattern(String type, Pattern pattern, double score, @Nullable Predicate<String> validator) {
            this(type, pattern, score, validator, null);
        }

        public PiiPattern(String type, Pattern pattern, double score, ContextRule contextRule) {
            this(type, pattern, score, null, contextRule);
        }
    }

    public static double promotedScore(
        @Nullable ContextRule contextRule, double baseScore, String text, int matchStart, int matchEnd) {

        if (contextRule == null) {
            return baseScore;
        }

        int window = contextRule.window();
        String context = text.substring(
            Math.max(0, matchStart - window), Math.min(text.length(), matchEnd + window))
            .toLowerCase(Locale.ROOT);

        for (String keyword : contextRule.keywords()) {
            if (context.contains(keyword)) {
                return contextRule.score();
            }
        }

        return baseScore;
    }

    public record ContextRule(Set<String> keywords, int window, double score) {

        public ContextRule {
            if (keywords == null || keywords.isEmpty()) {
                throw new IllegalArgumentException("keywords must not be empty");
            }

            keywords = Set.copyOf(keywords);

            if (window <= 0) {
                throw new IllegalArgumentException("window must be > 0, got: " + window);
            }

            if (!Double.isFinite(score) || score <= 0.0 || score > 1.0) {
                throw new IllegalArgumentException("score must be in (0.0, 1.0], got: " + score);
            }
        }
    }

    private static boolean isLuhnValid(String matchedText) {
        String digits = matchedText.replaceAll("[-\\s]", "");

        int sum = 0;
        boolean doubleDigit = false;

        for (int index = digits.length() - 1; index >= 0; index--) {
            int digit = digits.charAt(index) - '0';

            if (doubleDigit) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }
}
