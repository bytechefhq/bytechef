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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PiiPatternCatalogTest {

    private static final Map<String, Double> EXPECTED_SCORES = Map.ofEntries(
        Map.entry("EMAIL_ADDRESS", 0.9),
        Map.entry("PHONE_NUMBER", 0.6),
        Map.entry("CREDIT_CARD", 0.9),
        Map.entry("IP_ADDRESS", 0.6),
        Map.entry("IBAN_CODE", 0.9),
        Map.entry("CRYPTO", 0.9),
        Map.entry("DATE_TIME", 0.6),
        Map.entry("LOCATION", 0.6),
        Map.entry("MEDICAL_LICENSE", 0.2),
        Map.entry("US_BANK_NUMBER", 0.2),
        Map.entry("US_DRIVER_LICENSE", 0.2),
        Map.entry("US_ITIN", 0.6),
        Map.entry("US_PASSPORT", 0.2),
        Map.entry("US_SSN", 0.6),
        Map.entry("UK_NHS", 0.6),
        Map.entry("UK_NINO", 0.6),
        Map.entry("ES_NIF", 0.6),
        Map.entry("ES_NIE", 0.6),
        Map.entry("IT_FISCAL_CODE", 0.9),
        Map.entry("IT_DRIVER_LICENSE", 0.2),
        Map.entry("IT_VAT_CODE", 0.9),
        Map.entry("IT_PASSPORT", 0.2),
        Map.entry("IT_IDENTITY_CARD", 0.2),
        Map.entry("PL_PESEL", 0.2),
        Map.entry("SG_NRIC_FIN", 0.6),
        Map.entry("SG_UEN", 0.6),
        Map.entry("AU_ABN", 0.6),
        Map.entry("AU_ACN", 0.6),
        Map.entry("AU_TFN", 0.2),
        Map.entry("AU_MEDICARE", 0.6),
        Map.entry("IN_PAN", 0.6),
        Map.entry("IN_AADHAAR", 0.6),
        Map.entry("IN_VEHICLE_REGISTRATION", 0.6),
        Map.entry("IN_VOTER", 0.2),
        Map.entry("IN_PASSPORT", 0.2),
        Map.entry("FI_PERSONAL_IDENTITY_CODE", 0.6));

    @Test
    void testEveryPatternCarriesAScoreInRange() {
        for (PiiPatternCatalog.PiiPattern piiPattern : PiiPatternCatalog.ALL) {
            assertThat(piiPattern.score())
                .as("score for %s", piiPattern.type())
                .isBetween(0.0, 1.0);
        }
    }

    @Test
    void testBareDigitRunsScoreBelowAnchoredPatterns() {
        double bankNumber = scoreOf("US_BANK_NUMBER");
        double email = scoreOf("EMAIL_ADDRESS");

        assertThat(bankNumber).isLessThan(email);
    }

    @Test
    void testEveryCatalogScoreMatchesTheProvenanceTable() {
        assertThat(PiiPatternCatalog.ALL)
            .hasSize(EXPECTED_SCORES.size());

        for (PiiPatternCatalog.PiiPattern piiPattern : PiiPatternCatalog.ALL) {
            Double expected = EXPECTED_SCORES.get(piiPattern.type());

            assertThat(expected)
                .as("no expected score recorded for catalog type %s", piiPattern.type())
                .isNotNull();
            assertThat(piiPattern.score())
                .as("score for %s", piiPattern.type())
                .isEqualTo(expected);
        }
    }

    @Test
    void testEveryScoreIsOneOfTheThreeBandValues() {
        for (PiiPatternCatalog.PiiPattern piiPattern : PiiPatternCatalog.ALL) {
            assertThat(piiPattern.score())
                .as("score for %s", piiPattern.type())
                .isIn(0.2, 0.6, 0.9);
        }
    }

    @Test
    void testCompactConstructorRejectsScoreOutsideZeroToOne() {
        Pattern pattern = Pattern.compile("x");

        assertThatThrownBy(() -> new PiiPatternCatalog.PiiPattern("X", pattern, 1.1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PiiPatternCatalog.PiiPattern("X", pattern, -0.1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PiiPatternCatalog.PiiPattern("X", pattern, Double.NaN))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testThreeArgumentConstructorLeavesValidatorNull() {
        PiiPatternCatalog.PiiPattern pattern = new PiiPatternCatalog.PiiPattern("X", Pattern.compile("x"), 0.6);

        assertThat(pattern.validator()).isNull();
    }

    @Test
    void testOnlyCreditCardCarriesAValidator() {
        for (PiiPatternCatalog.PiiPattern piiPattern : PiiPatternCatalog.ALL) {
            if ("CREDIT_CARD".equals(piiPattern.type())) {
                assertThat(piiPattern.validator())
                    .as("CREDIT_CARD must carry a Luhn validator")
                    .isNotNull();
            } else {
                assertThat(piiPattern.validator())
                    .as("%s must not carry a validator", piiPattern.type())
                    .isNull();
            }
        }
    }

    @Test
    void testCreditCardValidatorAcceptsLuhnValidAndRejectsInvalid() {
        Predicate<String> validator = patternOf("CREDIT_CARD").validator();

        assertThat(validator).isNotNull();
        assertThat(validator.test("4532015112830366"))
            .as("Luhn-valid card number must pass")
            .isTrue();
        assertThat(validator.test("4532 0151 1283 0366"))
            .as("Luhn-valid card number with spaces must pass")
            .isTrue();
        assertThat(validator.test("1234567890123456"))
            .as("Luhn-invalid 16-digit run must fail")
            .isFalse();
    }

    @Test
    void testUsSsnPatternNoLongerMatchesBareNineDigits() {
        PiiPatternCatalog.PiiPattern usSsn = patternOf("US_SSN");
        PiiPatternCatalog.PiiPattern auTfn = patternOf("AU_TFN");

        assertThat(usSsn.pattern()
            .matcher("123-45-6789")
            .find())
                .as("US_SSN must still match the dashed form")
                .isTrue();
        assertThat(usSsn.pattern()
            .matcher("987654321")
            .find())
                .as("US_SSN must no longer match a bare 9-digit run")
                .isFalse();
        assertThat(auTfn.pattern()
            .matcher("987654321")
            .find())
                .as("AU_TFN must still match the bare 9-digit run US_SSN no longer claims")
                .isTrue();
    }

    @Test
    void testUsSsnScoresMediumNotLow() {
        assertThat(scoreOf("US_SSN")).isEqualTo(0.6);
    }

    @Test
    void testEsNifAndEsNieMatchOwnFormatNotEachOther() {
        PiiPatternCatalog.PiiPattern esNif = patternOf("ES_NIF");
        PiiPatternCatalog.PiiPattern esNie = patternOf("ES_NIE");

        assertThat(esNif.pattern()
            .matcher("12345678Z")
            .find())
                .as("ES_NIF must match the real digits-then-check-letter NIF format")
                .isTrue();
        assertThat(esNie.pattern()
            .matcher("X1234567L")
            .find())
                .as("ES_NIE must match the real X-then-digits-then-check-letter NIE format")
                .isTrue();
        assertThat(esNie.pattern()
            .matcher("Y1234567L")
            .find())
                .as("ES_NIE must match a leading Y")
                .isTrue();
        assertThat(esNie.pattern()
            .matcher("Z1234567L")
            .find())
                .as("ES_NIE must match a leading Z")
                .isTrue();

        assertThat(esNif.pattern()
            .matcher("X1234567L")
            .find())
                .as("ES_NIF must not match an ES_NIE sample")
                .isFalse();
        assertThat(esNie.pattern()
            .matcher("12345678Z")
            .find())
                .as("ES_NIE must not match an ES_NIF sample")
                .isFalse();
        assertThat(esNie.pattern()
            .matcher("A1234567L")
            .find())
                .as("ES_NIE must not match a leading letter outside X/Y/Z")
                .isFalse();
    }

    @Test
    void testEsNifAndEsNieScoreMediumNotLow() {
        assertThat(scoreOf("ES_NIF")).isEqualTo(0.6);
        assertThat(scoreOf("ES_NIE")).isEqualTo(0.6);
    }

    @Test
    void testEsNifEightDigitShapeCollidesWithSgUen() {
        PiiPatternCatalog.PiiPattern esNif = patternOf("ES_NIF");
        PiiPatternCatalog.PiiPattern sgUen = patternOf("SG_UEN");

        Matcher esNifMatcher = esNif.pattern()
            .matcher("12345678Z");
        Matcher sgUenMatcher = sgUen.pattern()
            .matcher("12345678Z");

        assertThat(esNifMatcher.find()).isTrue();
        assertThat(sgUenMatcher.find()).isTrue();
        assertThat(esNifMatcher.start()).isEqualTo(sgUenMatcher.start());
        assertThat(esNifMatcher.end()).isEqualTo(sgUenMatcher.end());
    }

    @Test
    void testFiPersonalIdentityCodeDoesNotMatchBareElevenCharacterBusinessIdentifiers() {
        PiiPatternCatalog.PiiPattern fiCode = patternOf("FI_PERSONAL_IDENTITY_CODE");

        assertThat(fiCode.pattern()
            .matcher("Order 20260825123 shipped")
            .find())
                .as("FI_PERSONAL_IDENTITY_CODE must not match an ordinary 11-digit order number")
                .isFalse();
        assertThat(fiCode.pattern()
            .matcher("invoice 45001239871 total 1234.56")
            .find())
                .as("FI_PERSONAL_IDENTITY_CODE must not match an ordinary 11-digit invoice number")
                .isFalse();
        assertThat(fiCode.pattern()
            .matcher("PESEL 44051401359")
            .find())
                .as("FI_PERSONAL_IDENTITY_CODE must not match a bare 11-digit run generally")
                .isFalse();
        assertThat(fiCode.pattern()
            .matcher("010101-123A")
            .find())
                .as("FI_PERSONAL_IDENTITY_CODE must still match a genuine 1900s-century code")
                .isTrue();
        assertThat(fiCode.pattern()
            .matcher("010101+123A")
            .find())
                .as("FI_PERSONAL_IDENTITY_CODE must still match a genuine 1800s-century code")
                .isTrue();
        assertThat(fiCode.pattern()
            .matcher("010101A123A")
            .find())
                .as("FI_PERSONAL_IDENTITY_CODE must still match a genuine 2000s-century code")
                .isTrue();
    }

    private static double scoreOf(String type) {
        return patternOf(type).score();
    }

    private static PiiPatternCatalog.PiiPattern patternOf(String type) {
        return PiiPatternCatalog.ALL.stream()
            .filter(piiPattern -> type.equals(piiPattern.type()))
            .findFirst()
            .orElseThrow();
    }

    @Test
    void testEveryLowScoredTypeCanBePromotedByContext() {
        List<String> bare = PiiPatternCatalog.ALL.stream()
            .filter(piiPattern -> piiPattern.score() < SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE)
            .filter(piiPattern -> piiPattern.contextRule() == null)
            .map(PiiPatternCatalog.PiiPattern::type)
            .toList();

        assertThat(bare)
            .as("these types score below the default threshold and have no keyword that can lift them, so nothing "
                + "detects them at all; either give them keywords or accept the gap explicitly here")
            .isEmpty();
    }

    @Test
    void testEveryContextRulePromotesAboveTheDefaultThreshold() {
        List<String> tooLow = new ArrayList<>();

        for (PiiPatternCatalog.PiiPattern piiPattern : PiiPatternCatalog.ALL) {
            PiiPatternCatalog.ContextRule contextRule = piiPattern.contextRule();

            if (contextRule != null && contextRule.score() < SensitiveDataRedactor.DEFAULT_MIN_CONFIDENCE) {
                tooLow.add(piiPattern.type());
            }
        }

        assertThat(tooLow).isEmpty();
    }

    @Test
    void testContextKeywordsAreLowerCasedInTheCatalog() {
        List<String> wrongCase = new ArrayList<>();

        for (PiiPatternCatalog.PiiPattern piiPattern : PiiPatternCatalog.ALL) {
            PiiPatternCatalog.ContextRule contextRule = piiPattern.contextRule();

            if (contextRule == null) {
                continue;
            }

            for (String keyword : contextRule.keywords()) {
                if (!keyword.equals(keyword.toLowerCase(Locale.ROOT))) {
                    wrongCase.add(keyword);
                }
            }
        }

        assertThat(wrongCase).isEmpty();
    }

    @Test
    void testAContextRuleThatWouldLowerConfidenceIsRejected() {
        assertThatThrownBy(
            () -> new PiiPatternCatalog.PiiPattern(
                "TEST_TYPE", Pattern.compile("\\d{9}"), 0.9,
                new PiiPatternCatalog.ContextRule(Set.of("ssn"), 40, 0.2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RAISE");
    }
}
