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

package com.bytechef.platform.webhook.web.websocket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;

/**
 * Stateless, self-contained token binding a Twilio Media Streams WebSocket connection to a specific {@code callSid}
 * with an expiry. A WebSocket upgrade cannot carry Twilio's {@code X-Twilio-Signature} header, so the token is minted
 * into the {@code <Stream url="wss://…?streamToken=…">} at TwiML-build time and verified on connect.
 *
 * <p>
 * Format: {@code v1.<expEpochSeconds>.<base64url(HMAC-SHA256(secret, callSid + "." + exp))>}. Verification recomputes
 * the signature and checks expiry, so no server-side state is needed and it works across process boundaries (the minter
 * and the WebSocket handler only need the same secret).
 * </p>
 *
 * <p>
 * Pure and side-effect-free (the current time is passed in) so it can be unit-tested deterministically. Deliberately
 * duplicated as a component-local copy in the Twilio component (which cannot depend on this platform module) for the
 * inbound-call path — both must agree on this format.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class TwilioStreamToken {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String PREFIX = "v1";

    private TwilioStreamToken() {
    }

    /**
     * Mints a token for {@code callSid} valid for {@code ttlSeconds} from {@code nowEpochSeconds}.
     */
    public static String mint(String secret, String callSid, long ttlSeconds, long nowEpochSeconds) {
        long expiry = nowEpochSeconds + ttlSeconds;

        return PREFIX + "." + expiry + "." + sign(secret, callSid, expiry);
    }

    /**
     * Returns whether {@code token} is a valid, unexpired token for {@code callSid} at {@code nowEpochSeconds}.
     */
    public static boolean verify(
        String secret, String callSid, @Nullable String token, long nowEpochSeconds) {

        if (secret == null || secret.isBlank() || callSid == null || token == null) {
            return false;
        }

        String[] parts = token.split("\\.", 3);

        if (parts.length != 3 || !PREFIX.equals(parts[0])) {
            return false;
        }

        long expiry;

        try {
            expiry = Long.parseLong(parts[1]);
        } catch (NumberFormatException numberFormatException) {
            return false;
        }

        if (expiry < nowEpochSeconds) {
            return false;
        }

        String expected = sign(secret, callSid, expiry);

        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8));
    }

    private static String sign(String secret, String callSid, long expiry) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);

            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));

            byte[] signature = mac.doFinal((callSid + "." + expiry).getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute Twilio stream token signature", exception);
        }
    }
}
