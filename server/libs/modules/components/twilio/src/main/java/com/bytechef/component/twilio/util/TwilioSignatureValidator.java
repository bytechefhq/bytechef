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

package com.bytechef.component.twilio.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;

/**
 * Validates the {@code X-Twilio-Signature} header on incoming Twilio webhook requests, per
 * <a href="https://www.twilio.com/docs/usage/security#validating-requests">Twilio's request-validation scheme</a>: the
 * signature is {@code Base64(HMAC-SHA1(authToken, fullUrl + concatenation of each POST param name+value sorted by
 * name))}.
 *
 * <p>
 * Pure and side-effect-free so it can be unit-tested against Twilio's published test vector.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class TwilioSignatureValidator {

    private static final String HMAC_SHA1 = "HmacSHA1";

    private TwilioSignatureValidator() {
    }

    /**
     * Returns whether {@code signature} is a valid Twilio signature for {@code url} + {@code params} using
     * {@code authToken}.
     *
     * @param authToken the Twilio account auth token
     * @param url       the exact full URL Twilio requested (scheme, host, path and query as Twilio saw it)
     * @param params    the POST form parameters (empty for GET requests)
     * @param signature the value of the {@code X-Twilio-Signature} header
     */
    public static boolean isValid(
        @Nullable String authToken, @Nullable String url, @Nullable Map<String, String> params,
        @Nullable String signature) {

        if (authToken == null || authToken.isBlank() || url == null || signature == null || signature.isBlank()) {
            return false;
        }

        String expected = computeSignature(authToken, url, params);

        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    public static String computeSignature(String authToken, String url, @Nullable Map<String, String> params) {
        StringBuilder data = new StringBuilder(url);

        if (params != null && !params.isEmpty()) {
            // Sort parameters by name, then append name+value with no separators.
            for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
                data.append(entry.getKey())
                    .append(entry.getValue() == null ? "" : entry.getValue());
            }
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA1);

            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), HMAC_SHA1));

            byte[] signatureBytes = mac.doFinal(data.toString()
                .getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder()
                .encodeToString(signatureBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute Twilio signature", exception);
        }
    }
}
