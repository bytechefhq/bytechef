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

package com.bytechef.automation.ai.a2a.server.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class A2aTaskIdTest {

    private static final UUID NONCE = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    @Test
    void testRoundTrip() {
        A2aTaskId a2aTaskId = new A2aTaskId(42L, NONCE);

        assertThat(A2aTaskId.parse(a2aTaskId.asString())).contains(a2aTaskId);
    }

    @Test
    void testRejectsANonCanonicalNonceSpelling() {
        assertThat(A2aTaskId.parse(encode("a2a:42:" + NONCE.toString()
            .toUpperCase()))).isEmpty();
        assertThat(A2aTaskId.parse(encode("a2a:42:1-1-1-1-1"))).isEmpty();
    }

    @Test
    void testRejectsAPaddedEncoding() {
        String value = "a2a:42:" + NONCE;

        String padded = Base64.getUrlEncoder()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));

        assertThat(padded).endsWith("=");
        assertThat(A2aTaskId.parse(padded)).isEmpty();
    }

    @Test
    void testRejectsMalformedValues() {
        assertThat(A2aTaskId.parse("not base64!")).isEmpty();
        assertThat(A2aTaskId.parse(encode("job:42:" + NONCE))).isEmpty();
        assertThat(A2aTaskId.parse(encode("a2a:x:" + NONCE))).isEmpty();
        assertThat(A2aTaskId.parse(encode("a2a:42"))).isEmpty();
        assertThat(A2aTaskId.parse(encode("a2a:42:not-a-uuid"))).isEmpty();
        assertThat(A2aTaskId.parse(encode("a2a:042:" + NONCE))).isEmpty();
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
