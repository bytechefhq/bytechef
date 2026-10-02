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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RegexPiiDetectorTest {

    private final RegexPiiDetector detector = new RegexPiiDetector();

    @Test
    void testDetectsEmailUnderItsEntityTypeName() {
        List<SensitiveSpan> spans = detector.detect("mail bob@acme.io please");

        assertThat(spans)
            .extracting(SensitiveSpan::category)
            .contains("EMAIL_ADDRESS");
    }

    @Test
    void testDetectsInternationalIdentifiersTheOldDetectorMissed() {
        assertThat(detector.detect("ID S1234567D"))
            .extracting(SensitiveSpan::category)
            .contains("SG_NRIC_FIN");

        assertThat(detector.detect("HETU 131052-308T"))
            .extracting(SensitiveSpan::category)
            .contains("FI_PERSONAL_IDENTITY_CODE");

        assertThat(detector.detect("ACN 123 456 789"))
            .extracting(SensitiveSpan::category)
            .contains("AU_ACN");
    }

    @Test
    void testAmbiguousNineDigitRunIsNotRedactedThroughTheRealPipeline() {
        String text = "Ticket 987654321 escalated";

        List<SensitiveSpan> candidates = detector.detect(text);

        assertThat(candidates)
            .extracting(SensitiveSpan::category)
            .contains("AU_TFN")
            .doesNotContain("US_SSN");

        SensitiveDataRedactor redactor = new SensitiveDataRedactor(List.of(detector));

        SensitiveDataRedactor.RedactionResult result = redactor.redactWithSpans(
            text, Set.of(SensitiveKind.PII), null);

        assertThat(result.text()).isEqualTo(text);
        assertThat(result.accepted()).isEmpty();
    }

    @Test
    void testCreditCardDetectionRequiresLuhnValidChecksum() {
        assertThat(detector.detect("card 4532015112830366 ok"))
            .extracting(SensitiveSpan::category)
            .contains("CREDIT_CARD");

        assertThat(detector.detect("Order 1234567890123456 shipped"))
            .extracting(SensitiveSpan::category)
            .doesNotContain("CREDIT_CARD");
    }

    @Test
    void testEverySpanIsPiiKindWithOffsetsInsideTheText() {
        String text = "mail bob@acme.io from 10.0.0.1";

        for (SensitiveSpan span : detector.detect(text)) {
            assertThat(span.kind()).isEqualTo(SensitiveKind.PII);
            assertThat(span.start()).isGreaterThanOrEqualTo(0);
            assertThat(span.end()).isLessThanOrEqualTo(text.length());
            assertThat(span.start()).isLessThan(span.end());
        }
    }

    @Test
    void testEveryCatalogTypeIsAValidSpanCategory() {
        for (PiiPatternCatalog.PiiPattern piiPattern : PiiPatternCatalog.ALL) {
            assertThat(piiPattern.type()).matches("[A-Z][A-Z0-9_]*");
        }
    }

    @Test
    void testCatalogHasTheFullTaxonomyAndTheDefaultIsCurated() {
        assertThat(PiiPatternCatalog.ALL).hasSize(36);

        assertThat(PiiPatternCatalog.curatedDefault())
            .extracting(PiiPatternCatalog.PiiPattern::type)
            .doesNotContain("DATE_TIME", "LOCATION")
            .hasSize(34);
    }

    @Test
    void testLowSpecificityTypeIsStillSelectableFromTheCatalog() {
        assertThat(PiiPatternCatalog.filterByTypes(List.of("US_BANK_NUMBER")))
            .extracting(PiiPatternCatalog.PiiPattern::type)
            .containsExactly("US_BANK_NUMBER");
    }

    @Test
    void testCuratedDefaultExcludesOnlyTheContextualTypes() {
        assertThat(PiiPatternCatalog.curatedDefault())
            .extracting(PiiPatternCatalog.PiiPattern::type)
            .doesNotContain("DATE_TIME", "LOCATION")
            .contains("US_BANK_NUMBER");
    }

    @Test
    void testContextualTextProducesNoSpansByDefault() {
        assertThat(detector.detect("meet on 2026-08-25 at Baker Street")).isEmpty();
    }

    @Test
    void testContextualTypesAreStillSelectableFromTheCatalog() {
        assertThat(PiiPatternCatalog.filterByTypes(List.of("DATE_TIME")))
            .extracting(PiiPatternCatalog.PiiPattern::type)
            .containsExactly("DATE_TIME");
    }

    @Test
    void testSpanCarriesItsPatternScoreRatherThanFullConfidence() {
        List<SensitiveSpan> spans = detector.detect("mail bob@acme.io please");

        SensitiveSpan emailSpan = spans.stream()
            .filter(span -> "EMAIL_ADDRESS".equals(span.category()))
            .findFirst()
            .orElseThrow();

        double expected = PiiPatternCatalog.ALL.stream()
            .filter(piiPattern -> "EMAIL_ADDRESS".equals(piiPattern.type()))
            .findFirst()
            .orElseThrow()
            .score();

        assertThat(emailSpan.confidence()).isEqualTo(expected);
    }

    @Test
    void testWeakPatternProducesALowerConfidenceSpanThanAStrongOne() {
        double weak = confidenceOfCategory("account 123456789012", "US_BANK_NUMBER");
        double strong = confidenceOfCategory("mail bob@acme.io", "EMAIL_ADDRESS");

        assertThat(weak).isLessThan(strong);
    }

    private double confidenceOfCategory(String text, String category) {
        List<SensitiveSpan> spans = detector.detect(text);

        return spans.stream()
            .filter(span -> category.equals(span.category()))
            .findFirst()
            .orElseThrow()
            .confidence();
    }

    @Test
    void testANamedIdentifierIsRedactedWhereABareOneIsNot() {
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

        assertThat(redactor.redact("Passport: A12345678", EnumSet.allOf(SensitiveKind.class), null))
            .as("a naming keyword beside the shape is what makes it identifiable")
            .isEqualTo("Passport: [REDACTED_US_PASSPORT]");

        assertThat(redactor.redact("Order A12345678", EnumSet.allOf(SensitiveKind.class), null))
            .as("the same shape with no keyword is an order code, and the false positive the confidence work fixed")
            .isEqualTo("Order A12345678");
    }

    @Test
    void testTheTaxFileNumberCaseFromTheConsolidationSpec() {
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

        assertThat(redactor.redact("TFN 123456789", EnumSet.allOf(SensitiveKind.class), null))
            .isEqualTo("TFN [REDACTED_AU_TFN]");
        assertThat(redactor.redact("ticket 123456789", EnumSet.allOf(SensitiveKind.class), null))
            .isEqualTo("ticket 123456789");
    }

    @Test
    void testAKeywordOutsideTheWindowDoesNotPromote() {
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());
        String text = "passport" + " ".repeat(80) + "A12345678";

        assertThat(redactor.redact(text, EnumSet.allOf(SensitiveKind.class), null)).isEqualTo(text);
    }

    @Test
    void testKeywordMatchingIsCaseInsensitive() {
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

        assertThat(redactor.redact("PASSPORT: A12345678", EnumSet.allOf(SensitiveKind.class), null))
            .isEqualTo("PASSPORT: [REDACTED_US_PASSPORT]");
    }

    @Test
    void testAKeywordAfterTheMatchPromotesToo() {
        SensitiveDataRedactor redactor = new SensitiveDataRedactor(SensitiveDataDetectors.builtIn());

        assertThat(redactor.redact("A12345678 (passport)", EnumSet.allOf(SensitiveKind.class), null))
            .isEqualTo("[REDACTED_US_PASSPORT] (passport)");
    }

    @Test
    void testEmailRegexDoesNotAcceptPipeInTld() {
        assertThat(detector.detect("foo@bar.|x"))
            .as("'foo@bar.|x' must not be detected as an EMAIL_ADDRESS — '|' is not a valid TLD character")
            .extracting(SensitiveSpan::category)
            .doesNotContain("EMAIL_ADDRESS");
        assertThat(detector.detect("foo@bar.X|Y|Z"))
            .as("'foo@bar.X|Y|Z' must not be detected as an EMAIL_ADDRESS")
            .extracting(SensitiveSpan::category)
            .doesNotContain("EMAIL_ADDRESS");
    }

    @Test
    void testEmailDetectionAcceptsSubdomainsAndPlusTags() {
        assertThat(detector.detect("Email: first.last+filter@mail.sub.example.co.uk"))
            .filteredOn(span -> "EMAIL_ADDRESS".equals(span.category()))
            .isNotEmpty();
    }

    @Test
    void testPhoneDetectionAcceptsDashDotAndParenthesisSeparators() {
        assertDetectsType("Call 555-123-4567 today", "PHONE_NUMBER");
        assertDetectsType("Call (555).123.4567 today", "PHONE_NUMBER");
    }

    @Test
    void testRawDigitsWithNoSeparatorAreNoLongerPhoneNumber() {
        List<SensitiveSpan> spans = detector.detect("Call 5551234567 now");

        assertThat(spans)
            .extracting(SensitiveSpan::category)
            .doesNotContain("PHONE_NUMBER")
            .contains("US_BANK_NUMBER");
    }

    @Test
    void testCreditCardDetectionAcceptsTheDashedForm() {
        assertDetectsType("CC: 4111-1111-1111-1111", "CREDIT_CARD");
    }

    @Test
    void testSsnDetectionAcceptsTheDashedForm() {
        assertDetectsType("SSN: 123-45-6789", "US_SSN");
    }

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void testIpAddressDetection() {
        assertDetectsType("Server at 192.168.1.100", "IP_ADDRESS");
    }

    @Test
    void testEmptyAndNullInputProduceNoSpans() {
        assertThat(detector.detect("")).isEmpty();
        assertThat(detector.detect(null)).isEmpty();
    }

    @Test
    void testFilterByTypesIgnoresUnknownTypesAndEmptySelections() {
        assertThat(PiiPatternCatalog.filterByTypes(List.of("EMAIL_ADDRESS", "PHONE_NUMBER")))
            .extracting(PiiPatternCatalog.PiiPattern::type)
            .containsExactlyInAnyOrder("EMAIL_ADDRESS", "PHONE_NUMBER");
        assertThat(PiiPatternCatalog.filterByTypes(List.of("EMAIL_ADDRESS", "BOGUS")))
            .extracting(PiiPatternCatalog.PiiPattern::type)
            .containsExactly("EMAIL_ADDRESS");
        assertThat(PiiPatternCatalog.filterByTypes(List.of())).isEmpty();
        assertThat(PiiPatternCatalog.filterByTypes(null)).isEmpty();
    }

    @Test
    void testCleanProseEmitsNoSpans() {
        assertThat(detector.detect("the quick brown fox jumps over the lazy dog")).isEmpty();
    }

    @Test
    void testNumericAndVersionProseFireKnownFalsePositiveCandidates() {
        assertThat(detector.detect("see page 123456789 of the manual"))
            .extracting(SensitiveSpan::category)
            .contains("US_BANK_NUMBER", "AU_TFN");
        assertThat(detector.detect("running version 1.2.3.4 of the library"))
            .extracting(SensitiveSpan::category)
            .contains("IP_ADDRESS");
    }

    @Test
    void testDetectsTheGlobalAndLocaleSpecificVocabulary() {
        assertDetectsType("Send funds to DE89370400440532013000 today", "IBAN_CODE");
        assertDetectsType("Wallet: 1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2", "CRYPTO");
        assertDetectsType("Provider CA123456 confirmed", "MEDICAL_LICENSE");
        assertDetectsType("Account 123456789 verified", "US_BANK_NUMBER");
        assertDetectsType("DL D1234567 issued", "US_DRIVER_LICENSE");
        assertDetectsType("ITIN 912-71-1234 on file", "US_ITIN");
        assertDetectsType("Passport A12345678 expires soon", "US_PASSPORT");
        assertDetectsType("NHS 943 476 5919 verified", "UK_NHS");
        assertDetectsType("NINO AB123456C provided", "UK_NINO");
        assertDetectsType("NIF 12345678Z provided", "ES_NIF");
        assertDetectsType("NIE X1234567L provided", "ES_NIE");
        assertDetectsType("CF RSSMRA85M01H501U registered", "IT_FISCAL_CODE");
        assertDetectsType("VAT IT12345678901 invoiced", "IT_VAT_CODE");
        assertDetectsType("Patente AB1234567 rilasciata", "IT_DRIVER_LICENSE");
        assertDetectsType("Passaporto YA1234567", "IT_PASSPORT");
        assertDetectsType("Carta identita CA1234567 emessa", "IT_IDENTITY_CARD");
        assertDetectsType("PESEL 44051401358 confirmed", "PL_PESEL");
        assertDetectsType("UEN 201912345K", "SG_UEN");
        assertDetectsType("ABN 12 345 678 901 listed", "AU_ABN");
        assertDetectsType("TFN 123456789 supplied", "AU_TFN");
        assertDetectsType("Medicare 2123 45678 1 valid", "AU_MEDICARE");
        assertDetectsType("Aadhaar 1234 5678 9012 captured", "IN_AADHAAR");
        assertDetectsType("PAN ABCDE1234F linked", "IN_PAN");
        assertDetectsType("Indian passport A1234567 issued", "IN_PASSPORT");
        assertDetectsType("Car: MH12AB1234", "IN_VEHICLE_REGISTRATION");
        assertDetectsType("Voter ID ABC1234567 active", "IN_VOTER");
        assertDetectsType("HETU 131052-308T verified", "FI_PERSONAL_IDENTITY_CODE");
    }

    @Test
    void testDetectsTheContextualVocabularyWhenExplicitlySelected() {
        RegexPiiDetector wholeCatalog = new RegexPiiDetector(PiiPatternCatalog.ALL);

        assertThat(wholeCatalog.detect("Appointment 2024-01-15T13:45:00Z scheduled"))
            .extracting(SensitiveSpan::category)
            .contains("DATE_TIME");
        assertThat(wholeCatalog.detect("Visit 123 Main Street, Springfield"))
            .extracting(SensitiveSpan::category)
            .contains("LOCATION");
    }

    @Test
    void testRenamedTypesAreNoLongerPresentUnderTheirOldNames() {
        assertThat(detector.detect("S1234567D HETU 131052-308T"))
            .extracting(SensitiveSpan::category)
            .doesNotContain("SG_NRIC", "FI_PIC");
    }

    private void assertDetectsType(String content, String expectedType) {
        assertThat(detector.detect(content))
            .as("Expected PII type %s to be detected in: %s", expectedType, content)
            .extracting(SensitiveSpan::category)
            .contains(expectedType);
    }
}
