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

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.ai.agent.guardrails.util.PiiDetectorUtils.PiiMatch;
import com.bytechef.component.ai.agent.guardrails.util.PiiDetectorUtils.PiiPattern;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PiiDetectorUtilsTest {

    @Test
    void testEmailDetectionBasicAddress() {
        List<PiiMatch> matches =
            PiiDetectorUtils.detect("Contact: user@example.com", PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches)
            .hasSize(1)
            .first()
            .extracting(PiiMatch::type, PiiMatch::value)
            .containsExactly("EMAIL_ADDRESS", "user@example.com");
    }

    @Test
    void testEmailRegexDoesNotAcceptPipeInTld() {
        // Regression: the email pattern used [A-Z|a-z]{2,} for the TLD, which is a character class containing
        // the literal '|' rather than alternation. Strings like "foo@bar.|x" or "foo@bar.X|Y|Z" matched as valid
        // emails and leaked into PII masking. Pin the corrected [A-Za-z]{2,} behavior.
        List<PiiMatch> match1 = PiiDetectorUtils.detect("foo@bar.|x", PiiDetectorUtils.DEFAULT_PII_PATTERNS);
        List<PiiMatch> match2 = PiiDetectorUtils.detect("foo@bar.X|Y|Z", PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(match1)
            .as("'foo@bar.|x' must not be detected as an EMAIL_ADDRESS — '|' is not a valid TLD character")
            .noneMatch(piiMatch -> "EMAIL_ADDRESS".equals(piiMatch.type()));
        assertThat(match2)
            .as("'foo@bar.X|Y|Z' must not be detected as an EMAIL_ADDRESS")
            .noneMatch(piiMatch -> "EMAIL_ADDRESS".equals(piiMatch.type()));
    }

    @Test
    void testEmailDetectionSubdomainAndTags() {
        List<PiiMatch> matches = PiiDetectorUtils.detect(
            "Email: first.last+filter@mail.sub.example.co.uk",
            PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches).anyMatch(match -> "EMAIL_ADDRESS".equals(match.type()) &&
            "first.last+filter@mail.sub.example.co.uk".equals(match.value()));
    }

    @Test
    void testPhoneDetectionWithDashes() {
        List<PiiMatch> matches =
            PiiDetectorUtils.detect("Call 555-123-4567 today", PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches).anyMatch(match -> "PHONE_NUMBER".equals(match.type()) && match.value()
            .startsWith("555"));
    }

    @Test
    void testPhoneDetectionWithParenthesesAndDots() {
        List<PiiMatch> matches =
            PiiDetectorUtils.detect("Call (555).123.4567 today", PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches).anyMatch(match -> "PHONE_NUMBER".equals(match.type()));
    }

    @Test
    void testPhoneDetectionMatchesRawDigits() {
        List<PiiMatch> matches = PiiDetectorUtils.detect("Call 5551234567 now", PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches).anyMatch(match -> "PHONE_NUMBER".equals(match.type()));
    }

    @Test
    void testSsnDetection() {
        List<PiiMatch> matches = PiiDetectorUtils.detect("SSN: 123-45-6789", PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches).anyMatch(match -> "US_SSN".equals(match.type()) && "123-45-6789".equals(match.value()));
    }

    @Test
    void testCreditCardDetection() {
        List<PiiMatch> matches =
            PiiDetectorUtils.detect("CC: 4111-1111-1111-1111", PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches).anyMatch(match -> "CREDIT_CARD".equals(match.type()));
    }

    @Test
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    void testIpAddressDetection() {
        List<PiiMatch> matches =
            PiiDetectorUtils.detect("Server at 192.168.1.100", PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches).anyMatch(match -> "IP_ADDRESS".equals(match.type()) &&
            "192.168.1.100".equals(match.value()));
    }

    @Test
    void testEmptyInputReturnsEmpty() {
        assertThat(PiiDetectorUtils.detect("", PiiDetectorUtils.DEFAULT_PII_PATTERNS)).isEmpty();
        assertThat(PiiDetectorUtils.detect(null, PiiDetectorUtils.DEFAULT_PII_PATTERNS)).isEmpty();
    }

    @Test
    void testFilterByTypesReturnsOnlySelectedPatterns() {
        List<PiiPattern> filtered = PiiDetectorUtils.filterByTypes(List.of("EMAIL_ADDRESS", "PHONE_NUMBER"));

        assertThat(filtered)
            .hasSize(2)
            .extracting(PiiPattern::type)
            .containsExactlyInAnyOrder("EMAIL_ADDRESS", "PHONE_NUMBER");
    }

    @Test
    void testFilterByTypesWithUnknownTypeIgnored() {
        List<PiiPattern> filtered = PiiDetectorUtils.filterByTypes(List.of("EMAIL_ADDRESS", "BOGUS"));

        assertThat(filtered).extracting(PiiPattern::type)
            .containsExactly("EMAIL_ADDRESS");
    }

    @Test
    void testFilterByTypesWithEmptyListReturnsEmpty() {
        assertThat(PiiDetectorUtils.filterByTypes(List.of())).isEmpty();
        assertThat(PiiDetectorUtils.filterByTypes(null)).isEmpty();
    }

    @Test
    void testMaskPreservesOffsetsWithMultipleDifferentLengthMatches() {
        // Two matches of different lengths and types — masking must not shift later offsets.
        String input = "call 555-123-4567 or email a@b.co";

        List<PiiMatch> matches = PiiDetectorUtils.detect(input, PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        String masked = PiiDetectorUtils.mask(input, matches);

        assertThat(masked).isEqualTo("call <PHONE_NUMBER> or email <EMAIL_ADDRESS>");
    }

    @Test
    void testMaskReturnsOriginalWhenNoMatches() {
        String input = "nothing sensitive here";

        assertThat(PiiDetectorUtils.mask(input, List.of())).isEqualTo(input);
    }

    @Test
    void testMaskReturnsNullWhenContentNull() {
        assertThat(PiiDetectorUtils.mask(null, List.of())).isNull();
    }

    @Test
    void detectsIbanCode() {
        assertDetectsType("Send funds to DE89370400440532013000 today", "IBAN_CODE");
    }

    @Test
    void detectsBitcoinAddress() {
        assertDetectsType("Wallet: 1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2", "CRYPTO");
    }

    @Test
    void detectsDateTime() {
        assertDetectsType("Appointment 2024-01-15T13:45:00Z scheduled", "DATE_TIME");
    }

    @Test
    void detectsMedicalLicense() {
        assertDetectsType("Provider CA123456 confirmed", "MEDICAL_LICENSE");
    }

    @Test
    void detectsUsBankNumber() {
        assertDetectsType("Account 123456789 verified", "US_BANK_NUMBER");
    }

    @Test
    void detectsUsDriverLicense() {
        assertDetectsType("DL D1234567 issued", "US_DRIVER_LICENSE");
    }

    @Test
    void detectsUsItin() {
        assertDetectsType("ITIN 912-71-1234 on file", "US_ITIN");
    }

    @Test
    void detectsUsPassport() {
        assertDetectsType("Passport A12345678 expires soon", "US_PASSPORT");
    }

    @Test
    void detectsUkNhsNumber() {
        assertDetectsType("NHS 943 476 5919 verified", "UK_NHS");
    }

    @Test
    void detectsUkNino() {
        assertDetectsType("NINO AB123456C provided", "UK_NINO");
    }

    @Test
    void detectsEsNif() {
        assertDetectsType("NIF A12345678 provided", "ES_NIF");
    }

    @Test
    void detectsEsNie() {
        assertDetectsType("NIE X12345678 provided", "ES_NIE");
    }

    @Test
    void detectsItFiscalCode() {
        assertDetectsType("CF RSSMRA85M01H501U registered", "IT_FISCAL_CODE");
    }

    @Test
    void detectsItVatCode() {
        assertDetectsType("VAT IT12345678901 invoiced", "IT_VAT_CODE");
    }

    @Test
    void detectsPlPesel() {
        assertDetectsType("PESEL 44051401358 confirmed", "PL_PESEL");
    }

    @Test
    void detectsSgNricFin() {
        assertDetectsType("NRIC S1234567A verified", "SG_NRIC_FIN");
    }

    @Test
    void detectsAuAbn() {
        assertDetectsType("ABN 12 345 678 901 listed", "AU_ABN");
    }

    @Test
    void detectsAuTfn() {
        assertDetectsType("TFN 123456789 supplied", "AU_TFN");
    }

    @Test
    void detectsAuMedicare() {
        assertDetectsType("Medicare 2123 45678 1 valid", "AU_MEDICARE");
    }

    @Test
    void detectsInAadhaar() {
        assertDetectsType("Aadhaar 1234 5678 9012 captured", "IN_AADHAAR");
    }

    @Test
    void detectsInPan() {
        assertDetectsType("PAN ABCDE1234F linked", "IN_PAN");
    }

    @Test
    void detectsInPassport() {
        assertDetectsType("Indian passport A1234567 issued", "IN_PASSPORT");
    }

    @Test
    void detectsInVoter() {
        assertDetectsType("Voter ID ABC1234567 active", "IN_VOTER");
    }

    @Test
    void detectsFiPersonalIdentityCode() {
        assertDetectsType("HETU 131052-308T verified", "FI_PERSONAL_IDENTITY_CODE");
    }

    @Test
    void testMaskDeduplicatesOverlappingSpansLongestWins() {
        String content = "ID A1234567 end";
        List<PiiMatch> overlapping = List.of(
            new PiiMatch("A1234567", 3, 11, "US_DRIVER_LICENSE"),
            new PiiMatch("A1234567", 3, 11, "IT_PASSPORT"),
            new PiiMatch("A12345", 3, 9, "IT_IDENTITY_CARD"));

        String masked = PiiDetectorUtils.mask(content, overlapping);

        assertThat(masked)
            .containsAnyOf("<US_DRIVER_LICENSE>", "<IT_PASSPORT>")
            .doesNotContain("<IT_IDENTITY_CARD>")
            .endsWith(" end")
            .startsWith("ID ");
    }

    @Test
    void testMaskProducesValidOutputWhenPatternsOverlapAtSameSpan() {
        String content = "SSN 123456789 here";
        List<PiiMatch> overlapping = List.of(
            new PiiMatch("123456789", 4, 13, "US_SSN"),
            new PiiMatch("123456789", 4, 13, "AU_TFN"));

        String masked = PiiDetectorUtils.mask(content, overlapping);

        assertThat(masked)
            .matches("SSN <(US_SSN|AU_TFN)> here");
    }

    @Test
    void testDetectRejectsPathologicallyLargeInputViaRegexParserBound() {
        // Feeds 2 MiB of text at the 2-arg detect() overload; RegexParser.bounded(...) should reject it up front so
        // future additions to DEFAULT_PII_PATTERNS with quantifier-heavy regex can never hold a thread for long.
        String pathological = "a".repeat(2 * 1024 * 1024);

        org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> PiiDetectorUtils.detect(pathological, PiiDetectorUtils.DEFAULT_PII_PATTERNS))
            .isInstanceOf(RegexParserUtils.RegexExecutionLimitException.class);
    }

    @Test
    void testCleanProseEmitsNoMatches() {
        List<PiiMatch> matches = PiiDetectorUtils.detect(
            "the quick brown fox jumps over the lazy dog",
            PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches).isEmpty();
    }

    @Test
    void testNumericInProseFiresKnownFalsePositives() {
        List<PiiMatch> matches = PiiDetectorUtils.detect(
            "see page 123456789 of the manual",
            PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches)
            .extracting(PiiMatch::type)
            .contains("US_BANK_NUMBER", "AU_TFN");
    }

    @Test
    void testVersionStringFiresAsIpAddress() {
        List<PiiMatch> matches = PiiDetectorUtils.detect(
            "running version 1.2.3.4 of the library",
            PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches)
            .extracting(PiiMatch::type)
            .contains("IP_ADDRESS");
    }

    private void assertDetectsType(String content, String expectedType) {
        List<PiiMatch> matches = PiiDetectorUtils.detect(content, PiiDetectorUtils.DEFAULT_PII_PATTERNS);

        assertThat(matches)
            .extracting(PiiMatch::type)
            .contains(expectedType);
    }
}
