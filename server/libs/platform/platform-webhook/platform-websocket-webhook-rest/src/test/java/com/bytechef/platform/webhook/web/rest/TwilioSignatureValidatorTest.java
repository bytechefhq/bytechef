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

package com.bytechef.platform.webhook.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TwilioSignatureValidatorTest {

    // Twilio's published request-validation example
    // (https://www.twilio.com/docs/usage/security#validating-requests).
    private static final String AUTH_TOKEN = "12345";
    private static final String URL = "https://mycompany.com/myapp.php?foo=1&bar=2";
    private static final Map<String, String> PARAMS = Map.of(
        "CallSid", "CA1234567890ABCDE",
        "Caller", "+14158675309",
        "Digits", "1234",
        "From", "+14158675309",
        "To", "+18005551212");
    private static final String EXPECTED_SIGNATURE = "RSOYDt4T1cUTdK1PDd93/VVr8B8=";

    @Test
    void testComputeSignatureMatchesTwilioVector() {
        assertThat(TwilioSignatureValidator.computeSignature(AUTH_TOKEN, URL, PARAMS)).isEqualTo(EXPECTED_SIGNATURE);
    }

    @Test
    void testIsValidAcceptsCorrectSignature() {
        assertThat(TwilioSignatureValidator.isValid(AUTH_TOKEN, URL, PARAMS, EXPECTED_SIGNATURE)).isTrue();
    }

    @Test
    void testIsValidRejectsTamperedSignature() {
        assertThat(TwilioSignatureValidator.isValid(AUTH_TOKEN, URL, PARAMS, "not-the-signature")).isFalse();
    }

    @Test
    void testIsValidRejectsWrongAuthToken() {
        assertThat(TwilioSignatureValidator.isValid("wrong-token", URL, PARAMS, EXPECTED_SIGNATURE)).isFalse();
    }

    @Test
    void testIsValidRejectsMissingSignatureOrToken() {
        assertThat(TwilioSignatureValidator.isValid(AUTH_TOKEN, URL, PARAMS, null)).isFalse();
        assertThat(TwilioSignatureValidator.isValid(AUTH_TOKEN, URL, PARAMS, "")).isFalse();
        assertThat(TwilioSignatureValidator.isValid(null, URL, PARAMS, EXPECTED_SIGNATURE)).isFalse();
        assertThat(TwilioSignatureValidator.isValid("", URL, PARAMS, EXPECTED_SIGNATURE)).isFalse();
    }
}
