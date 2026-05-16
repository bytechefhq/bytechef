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

package com.bytechef.component.infobip.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class InfobipSignatureValidatorTest {

    private static final String SECRET = "s3cr3t-signing-key";
    private static final String BODY = "{\"callId\":\"abc-123\",\"from\":\"+15551234567\",\"state\":\"RINGING\"}";

    @Test
    void testIsValidAcceptsHexSignature() {
        String hexSignature = HexFormat.of()
            .formatHex(InfobipSignatureValidator.computeDigest(SECRET, BODY));

        assertThat(InfobipSignatureValidator.isValid(SECRET, BODY, hexSignature)).isTrue();
    }

    @Test
    void testIsValidAcceptsBase64Signature() {
        String base64Signature = Base64.getEncoder()
            .encodeToString(InfobipSignatureValidator.computeDigest(SECRET, BODY));

        assertThat(InfobipSignatureValidator.isValid(SECRET, BODY, base64Signature)).isTrue();
    }

    @Test
    void testIsValidRejectsTamperedBody() {
        String hexSignature = HexFormat.of()
            .formatHex(InfobipSignatureValidator.computeDigest(SECRET, BODY));

        assertThat(InfobipSignatureValidator.isValid(SECRET, BODY + "tampered", hexSignature)).isFalse();
    }

    @Test
    void testIsValidRejectsWrongSecret() {
        String hexSignature = HexFormat.of()
            .formatHex(InfobipSignatureValidator.computeDigest(SECRET, BODY));

        assertThat(InfobipSignatureValidator.isValid("other-secret", BODY, hexSignature)).isFalse();
    }

    @Test
    void testIsValidRejectsBlankInputs() {
        assertThat(InfobipSignatureValidator.isValid(null, BODY, "sig")).isFalse();
        assertThat(InfobipSignatureValidator.isValid("", BODY, "sig")).isFalse();
        assertThat(InfobipSignatureValidator.isValid(SECRET, BODY, null)).isFalse();
        assertThat(InfobipSignatureValidator.isValid(SECRET, BODY, "")).isFalse();
    }
}
