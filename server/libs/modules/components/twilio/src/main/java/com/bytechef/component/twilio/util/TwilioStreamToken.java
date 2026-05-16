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
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Mints the signed stream token bound to a {@code callSid} that guards the Twilio Media Streams WebSocket. The inbound
 * trigger runs at the component layer and cannot reach the platform signer, so this is a deliberate copy of the
 * platform's {@code TwilioStreamToken} format ({@code v1.<expEpochSeconds>.<base64url(HMAC-SHA256(secret,
 * callSid + "." + exp))>}) — the two must agree, and the operator sets the same secret on both
 * ({@code bytechef.twilio.stream-token.secret} on the platform, the trigger's Stream Token Secret here).
 *
 * @author Ivica Cardic
 */
public final class TwilioStreamToken {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String PREFIX = "v1";

    private TwilioStreamToken() {
    }

    public static String mint(String secret, String callSid, long ttlSeconds, long nowEpochSeconds) {
        long expiry = nowEpochSeconds + ttlSeconds;

        String signature;

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));

            byte[] signatureBytes = mac.doFinal((callSid + "." + expiry).getBytes(StandardCharsets.UTF_8));

            signature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(signatureBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute Twilio stream token signature", exception);
        }

        return PREFIX + "." + expiry + "." + signature;
    }
}
