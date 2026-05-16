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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TwilioStreamTokenTest {

    private static final String SECRET = "stream-signing-secret";
    private static final long NOW = 1_700_000_000L;

    @Test
    void testMintedTokenVerifies() {
        String token = TwilioStreamToken.mint(SECRET, "CA123", 60, NOW);

        assertThat(TwilioStreamToken.verify(SECRET, "CA123", token, NOW + 30)).isTrue();
    }

    @Test
    void testExpiredTokenRejected() {
        String token = TwilioStreamToken.mint(SECRET, "CA123", 60, NOW);

        assertThat(TwilioStreamToken.verify(SECRET, "CA123", token, NOW + 61)).isFalse();
    }

    @Test
    void testWrongCallSidRejected() {
        String token = TwilioStreamToken.mint(SECRET, "CA123", 60, NOW);

        assertThat(TwilioStreamToken.verify(SECRET, "CA999", token, NOW + 30)).isFalse();
    }

    @Test
    void testWrongSecretRejected() {
        String token = TwilioStreamToken.mint(SECRET, "CA123", 60, NOW);

        assertThat(TwilioStreamToken.verify("other-secret", "CA123", token, NOW + 30)).isFalse();
    }

    @Test
    void testTamperedOrMalformedTokenRejected() {
        assertThat(TwilioStreamToken.verify(SECRET, "CA123", "v1.9999999999.tampered", NOW)).isFalse();
        assertThat(TwilioStreamToken.verify(SECRET, "CA123", "garbage", NOW)).isFalse();
        assertThat(TwilioStreamToken.verify(SECRET, "CA123", null, NOW)).isFalse();
        assertThat(TwilioStreamToken.verify("", "CA123", "v1.9999999999.x", NOW)).isFalse();
    }
}
