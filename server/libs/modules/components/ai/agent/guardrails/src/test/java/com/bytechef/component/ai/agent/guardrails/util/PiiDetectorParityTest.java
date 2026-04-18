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

import com.bytechef.component.ai.agent.guardrails.util.PiiDetector.PiiMatch;
import java.util.List;
import org.junit.jupiter.api.Test;

class PiiDetectorParityTest {

    @Test
    void detectsItalianDriverLicense() {
        assertDetectsType("Patente AB1234567 rilasciata", "IT_DRIVER_LICENSE");
    }

    @Test
    void detectsItalianPassport() {
        assertDetectsType("Passaporto YA1234567", "IT_PASSPORT");
    }

    @Test
    void detectsItalianIdentityCard() {
        // n8n pattern is 2 letters + 7 digits (same shape as passport / driver license; ambiguous by design).
        assertDetectsType("Carta identita CA1234567 emessa", "IT_IDENTITY_CARD");
    }

    @Test
    void detectsSingaporeUen() {
        assertDetectsType("UEN 201912345K", "SG_UEN");
    }

    @Test
    void detectsAustralianAcn() {
        assertDetectsType("ACN 123 456 789", "AU_ACN");
    }

    @Test
    void detectsIndianVehicleRegistration() {
        assertDetectsType("Car: MH12AB1234", "IN_VEHICLE_REGISTRATION");
    }

    @Test
    void usesSgNricFinName() {
        List<PiiMatch> matches = PiiDetector.detect("ID S1234567D", PiiDetector.DEFAULT_PII_PATTERNS);

        assertThat(matches)
            .extracting(PiiMatch::type)
            .contains("SG_NRIC_FIN");
    }

    @Test
    void usesFiPersonalIdentityCodeName() {
        List<PiiMatch> matches = PiiDetector.detect("HETU 131052-308T", PiiDetector.DEFAULT_PII_PATTERNS);

        assertThat(matches)
            .extracting(PiiMatch::type)
            .contains("FI_PERSONAL_IDENTITY_CODE");
    }

    @Test
    void detectsLocationKeyword() {
        // n8n adds LOCATION via street-address keyword matching; ensure the Java port pins the same behaviour.
        assertDetectsType("Visit 123 Main Street, Springfield", "LOCATION");
    }

    @Test
    void renamedTypesNoLongerPresentUnderOldNames() {
        List<PiiMatch> matches = PiiDetector.detect(
            "S1234567D HETU 131052-308T", PiiDetector.DEFAULT_PII_PATTERNS);

        assertThat(matches)
            .extracting(PiiMatch::type)
            .doesNotContain("SG_NRIC", "FI_PIC");
    }

    private static void assertDetectsType(String content, String expectedType) {
        List<PiiMatch> matches = PiiDetector.detect(content, PiiDetector.DEFAULT_PII_PATTERNS);

        assertThat(matches)
            .as("Expected PII type %s to be detected in: %s", expectedType, content)
            .extracting(PiiMatch::type)
            .contains(expectedType);
    }
}
