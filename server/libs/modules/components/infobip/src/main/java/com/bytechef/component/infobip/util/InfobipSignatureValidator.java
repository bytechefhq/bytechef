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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;

/**
 * Validates the HMAC signature that Infobip sends on signed inbound webhook requests: the signature is
 * {@code HMAC-SHA256(signingSecret, rawRequestBody)}, transmitted in a header.
 *
 * <p>
 * The exact header name and the signature encoding could not be verified against Infobip's live API reference (it is
 * bot-blocked from this environment). The scheme below follows the common HMAC-over-raw-body convention: the computed
 * digest is matched constant-time against the header value in <em>both</em> hex and Base64 encodings so either of
 * Infobip's documented representations validates. Confirm the header name and encoding against your account's webhook
 * configuration before relying on this in production.
 * </p>
 *
 * <p>
 * Pure and side-effect-free so it can be unit-tested.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class InfobipSignatureValidator {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private InfobipSignatureValidator() {
    }

    /**
     * Returns whether {@code signature} is a valid Infobip signature for {@code rawBody} using {@code signingSecret}.
     *
     * @param signingSecret the shared webhook signing secret configured in Infobip
     * @param rawBody       the exact raw request body bytes as Infobip sent them
     * @param signature     the value of the signature header
     */
    public static boolean isValid(
        @Nullable String signingSecret, @Nullable String rawBody, @Nullable String signature) {

        if (signingSecret == null || signingSecret.isBlank() || signature == null || signature.isBlank()) {
            return false;
        }

        byte[] digest = computeDigest(signingSecret, rawBody == null ? "" : rawBody);

        String expectedHex = HexFormat.of()
            .formatHex(digest);
        String expectedBase64 = Base64.getEncoder()
            .encodeToString(digest);

        return constantTimeEquals(expectedHex, signature) || constantTimeEquals(expectedBase64, signature);
    }

    static byte[] computeDigest(String signingSecret, String rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));

            return mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute Infobip signature", exception);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
