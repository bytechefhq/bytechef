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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TwilioStreamTokenTest {

    private static final String SECRET = "stream-signing-secret";

    @Test
    void testMintProducesTokenMatchingSharedFormat() throws Exception {
        long now = 1_700_000_000L;
        long ttl = 3600L;

        String token = TwilioStreamToken.mint(SECRET, "CA123", ttl, now);

        // The token must match the exact v1.<exp>.<base64url(HMAC-SHA256(secret, callSid + "." + exp))> format the
        // platform WebSocket handler verifies against; recompute it here to pin cross-module agreement.
        long expiry = now + ttl;

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        String expectedSignature = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(mac.doFinal(("CA123." + expiry).getBytes(StandardCharsets.UTF_8)));

        assertThat(token).isEqualTo("v1." + expiry + "." + expectedSignature);
    }
}
